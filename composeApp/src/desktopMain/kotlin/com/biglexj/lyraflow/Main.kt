package com.biglexj.lyraflow

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.application
import com.biglexj.lyraflow.core.config.AppConfiguration
import com.biglexj.lyraflow.core.config.HistoryRetentionPeriod
import com.biglexj.lyraflow.core.audio.RecordingTelemetry
import com.biglexj.lyraflow.core.model.AiProvider
import com.biglexj.lyraflow.core.network.createPlatformHttpClient
import com.biglexj.lyraflow.data.provider.MultimodalTranscriptionProvider
import com.biglexj.lyraflow.data.history.InMemoryTranscriptionHistoryRepository
import com.biglexj.lyraflow.domain.dictation.DictationCoordinator
import com.biglexj.lyraflow.domain.dictation.DictationState
import com.biglexj.lyraflow.domain.transcription.TranscriptionRequest
import com.biglexj.lyraflow.feature.shell.LyraFlowApp
import com.biglexj.lyraflow.feature.shell.ShellActions
import com.biglexj.lyraflow.platform.audio.DesktopAudioCapture
import com.biglexj.lyraflow.platform.hotkey.GlobalShortcutFactory
import com.biglexj.lyraflow.platform.injection.DesktopTextInjector
import com.biglexj.lyraflow.platform.settings.DesktopPreferencesStore
import com.biglexj.lyraflow.platform.settings.DesktopApiKeyStore
import com.biglexj.lyraflow.platform.settings.WindowsAutoStart
import com.biglexj.lyraflow.platform.whisper.WhisperInstaller
import com.biglexj.lyraflow.platform.whisper.WhisperTranscriptionProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.awt.event.WindowFocusListener

private const val SINGLE_INSTANCE_PORT = 49281

private class SingleInstanceLock(
    private val port: Int = SINGLE_INSTANCE_PORT,
) {
    private var serverSocket: java.net.ServerSocket? = null

    fun tryAcquire(onFocusRequested: () -> Unit): Boolean {
        return try {
            val socket = java.net.ServerSocket(port, 50, java.net.InetAddress.getByName("127.0.0.1"))
            serverSocket = socket
            Thread {
                while (!socket.isClosed) {
                    try {
                        val client = socket.accept()
                        client.close()
                        onFocusRequested()
                    } catch (_: Exception) {
                        break
                    }
                }
            }.apply {
                isDaemon = true
                name = "LyraFlow-SingleInstanceListener"
                start()
            }
            true
        } catch (_: Exception) {
            notifyPrimaryInstance()
            false
        }
    }

    private fun notifyPrimaryInstance() {
        try {
            java.net.Socket("127.0.0.1", port).use { socket ->
                socket.getOutputStream().write("FOCUS\n".toByteArray())
                socket.getOutputStream().flush()
            }
        } catch (_: Exception) {}
    }

    fun release() {
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
    }
}

fun main(args: Array<String>) {
    val lock = SingleInstanceLock()
    var bringToFrontCallback: (() -> Unit)? = null

    val isPrimary = lock.tryAcquire {
        bringToFrontCallback?.invoke()
    }

    if (!isPrimary) {
        kotlin.system.exitProcess(0)
    }

    application {
        DisposableEffect(Unit) {
            onDispose {
                lock.release()
            }
        }

    val preferencesStore = remember { DesktopPreferencesStore() }
    val apiKeyStore = remember { DesktopApiKeyStore() }
    val autoStart = remember { WindowsAutoStart() }
    var preferences by remember { mutableStateOf(preferencesStore.load()) }
    var apiKey by remember {
        mutableStateOf(apiKeyStore.load(preferences.provider).ifBlank { environmentApiKey(preferences.provider) })
    }
    var recordingTelemetry by remember { mutableStateOf(RecordingTelemetry()) }
    val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
    val audio = remember { DesktopAudioCapture() }
    val injector = remember { DesktopTextInjector() }
    val statusOverlay = remember { LyraFlowStatusOverlay() }
    val whisperInstaller = remember { WhisperInstaller() }
    val whisperStatus by whisperInstaller.state.collectAsState()
    val whisperProvider = remember(whisperStatus.model) {
        WhisperTranscriptionProvider { whisperStatus.model }
    }
    var shortcut by remember { mutableStateOf(GlobalShortcutFactory.create()) }
    val startsMinimized = args.any { it.equals("--minimized", ignoreCase = true) }
    var windowVisible by remember { mutableStateOf(!startsMinimized || !isSystemTraySupported()) }
    val historyRepository = remember { InMemoryTranscriptionHistoryRepository() }
    val coordinator = remember(historyRepository) {
        DictationCoordinator(
            transcriber = MultimodalTranscriptionProvider(
                client = createPlatformHttpClient(),
                apiKey = { apiKey },
                configuration = { preferences.providerConfiguration },
            ),
            historyRepository = historyRepository,
            isHistoryEnabled = { preferences.historyRetention != HistoryRetentionPeriod.Disabled },
        )
    }
    val state by coordinator.state.collectAsState()
    val recording = remember { mutableStateOf(false) }

    LaunchedEffect(state, recordingTelemetry.level) {
        statusOverlay.update(state, recordingTelemetry.level)
    }

    LaunchedEffect(preferences.launchAtStartup) {
        autoStart.setEnabled(preferences.launchAtStartup)
    }

    fun toggleRecording() {
        if (coordinator.state.value is DictationState.Transcribing) {
            return
        }
        if (!recording.value) {
            recordingTelemetry = RecordingTelemetry()
            injector.rememberForegroundTarget()
            runCatching {
                audio.start { level, durationMillis ->
                    scope.launch { recordingTelemetry = RecordingTelemetry(level, durationMillis) }
                }
            }
                .onSuccess {
                    recording.value = true
                    coordinator.markListening()
                }
        } else {
            recording.value = false
            val wav = audio.stop()
            scope.launch {
                coordinator.process(
                    TranscriptionRequest(
                        audio = wav,
                        model = preferences.model,
                        systemPrompt = preferences.systemPrompt,
                    ),
                )
                if (preferences.autoInject) {
                    val text = (coordinator.state.value as? DictationState.Completed)?.refinedText.orEmpty()
                    injector.inject(text)
                }
            }
        }
    }

    remember {
        shortcut.start(preferences.shortcut) { scope.launch { toggleRecording() } }
        true
    }

    fun exitLyraFlow() {
        if (recording.value) {
            recording.value = false
            runCatching { audio.stop() }
        }
        shortcut.close()
        statusOverlay.dispose()
        scope.cancel()
        exitApplication()
    }

    val tray = remember {
        if (isSystemTraySupported()) {
            LyraFlowTray(
                onOpen = { windowVisible = true },
                onExit = ::exitLyraFlow,
            )
        } else {
            null
        }
    }
    DisposableEffect(tray) {
        onDispose {
            tray?.close()
            statusOverlay.dispose()
        }
    }

    Window(
        onCloseRequest = {
            if (tray != null) windowVisible = false else exitLyraFlow()
        },
        visible = windowVisible,
        title = "LyraFlow",
        icon = painterResource("Square44x44Logo.png"),
        state = rememberWindowState(
            width = 1210.dp,
            height = 870.dp,
            position = WindowPosition(Alignment.Center),
        ),
    ) {
        DisposableEffect(window) {
            bringToFrontCallback = {
                windowVisible = true
                scope.launch(Dispatchers.Main) {
                    window.isMinimized = false
                    window.toFront()
                    window.requestFocus()
                }
            }

            val focusListener = object : WindowFocusListener {
                override fun windowGainedFocus(event: java.awt.event.WindowEvent) = Unit
                override fun windowLostFocus(event: java.awt.event.WindowEvent) = injector.rememberForegroundTarget()
            }
            window.addWindowFocusListener(focusListener)

            val dropTargetListener = object : java.awt.dnd.DropTargetListener {
                override fun dragEnter(dtde: java.awt.dnd.DropTargetDragEvent) {
                    dtde.acceptDrag(java.awt.dnd.DnDConstants.ACTION_COPY)
                }

                override fun dragOver(dtde: java.awt.dnd.DropTargetDragEvent) {
                    dtde.acceptDrag(java.awt.dnd.DnDConstants.ACTION_COPY)
                }

                override fun dropActionChanged(dtde: java.awt.dnd.DropTargetDragEvent) {
                    dtde.acceptDrag(java.awt.dnd.DnDConstants.ACTION_COPY)
                }

                override fun dragExit(dte: java.awt.dnd.DropTargetEvent) = Unit

                override fun drop(evt: java.awt.dnd.DropTargetDropEvent) {
                    try {
                        evt.acceptDrop(java.awt.dnd.DnDConstants.ACTION_COPY)
                        val droppedFiles = evt.transferable.getTransferData(java.awt.datatransfer.DataFlavor.javaFileListFlavor) as? List<*>
                        val audioFile = droppedFiles?.firstOrNull() as? java.io.File
                        if (audioFile != null && audioFile.exists()) {
                            val bytes = audioFile.readBytes()
                            scope.launch {
                                coordinator.process(
                                    TranscriptionRequest(
                                        audio = bytes,
                                        model = preferences.model,
                                        systemPrompt = preferences.systemPrompt,
                                    ),
                                )
                                if (preferences.autoInject) {
                                    val text = (coordinator.state.value as? DictationState.Completed)?.refinedText.orEmpty()
                                    injector.inject(text)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            fun registerDropTargetRecursively(component: java.awt.Component) {
                try {
                    component.dropTarget = java.awt.dnd.DropTarget(component, dropTargetListener)
                } catch (_: Throwable) {}

                if (component is java.awt.Container) {
                    component.addContainerListener(object : java.awt.event.ContainerAdapter() {
                        override fun componentAdded(e: java.awt.event.ContainerEvent) {
                            registerDropTargetRecursively(e.child)
                        }
                    })
                    component.components.forEach { child ->
                        registerDropTargetRecursively(child)
                    }
                }
            }

            registerDropTargetRecursively(window)

            onDispose {
                window.removeWindowFocusListener(focusListener)
            }
        }
        LaunchedEffect(windowVisible) {
            if (windowVisible) {
                window.toFront()
                window.requestFocus()
            }
        }
        LyraFlowApp(
            platform = "${System.getProperty("os.name")} · ${shortcut.status}",
            state = state,
            configuration = AppConfiguration(
                preferences = preferences,
                sessionApiKey = apiKey,
            ),
            recordingTelemetry = recordingTelemetry,
            whisperStatus = whisperStatus,
            historyRepository = historyRepository,
            actions = ShellActions(
                toggleRecording = ::toggleRecording,
                injectLastResult = {
                    val text = (state as? DictationState.Completed)?.refinedText.orEmpty()
                    injector.inject(text)
                },
                reset = coordinator::reset,
                updatePreferences = { updated ->
                    val shortcutChanged = preferences.shortcut != updated.shortcut
                    if (preferences.provider != updated.provider) {
                        apiKey = apiKeyStore.load(updated.provider).ifBlank { environmentApiKey(updated.provider) }
                    }
                    preferences = updated
                    preferencesStore.save(updated)
                    autoStart.setEnabled(updated.launchAtStartup)
                    if (shortcutChanged) {
                        shortcut.close()
                        shortcut = GlobalShortcutFactory.create().also { replacement ->
                            replacement.start(updated.shortcut) {
                                scope.launch { toggleRecording() }
                            }
                        }
                    }
                },
                updateApiKey = {
                    apiKey = it
                    apiKeyStore.save(preferences.provider, it)
                },
                installWhisper = { model -> scope.launch { whisperInstaller.install(model) } },
                retry = {
                    scope.launch {
                        coordinator.retry()
                        if (preferences.autoInject) {
                            val text = (coordinator.state.value as? DictationState.Completed)?.refinedText.orEmpty()
                            injector.inject(text)
                        }
                    }
                },
                retryWhisper = {
                    scope.launch {
                        coordinator.retry(whisperProvider)
                        if (preferences.autoInject) {
                            val text = (coordinator.state.value as? DictationState.Completed)?.refinedText.orEmpty()
                            injector.inject(text)
                        }
                    }
                },
            ),
        )
    }
}
}

private fun environmentApiKey(provider: AiProvider): String =
    System.getenv("LYRAFLOW_API_KEY").orEmpty().ifBlank {
        System.getenv(provider.apiKeyEnvironmentVariable).orEmpty()
    }
