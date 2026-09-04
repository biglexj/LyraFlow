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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.application
import com.biglexj.lyraflow.core.config.AppConfiguration
import com.biglexj.lyraflow.core.config.HistoryRetentionPeriod
import com.biglexj.lyraflow.core.audio.RecordingTelemetry
import com.biglexj.lyraflow.core.model.AiProvider
import com.biglexj.lyraflow.core.network.createPlatformHttpClient
import com.biglexj.lyraflow.data.provider.MultimodalTranscriptionProvider
import com.biglexj.lyraflow.data.provider.DisabledTranscriptionProvider
import com.biglexj.lyraflow.data.scanner.ModelDiscoveryService
import com.biglexj.lyraflow.domain.transcription.TranscriptionProvider
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
import com.biglexj.lyraflow.platform.SingleInstanceLock
import com.biglexj.lyraflow.platform.whisper.WhisperInstaller
import com.biglexj.lyraflow.platform.whisper.WhisperTranscriptionProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.awt.event.WindowFocusListener

fun main(args: Array<String>) {
    val isDev = SingleInstanceLock.isDevMode()

    val isPrimary = SingleInstanceLock.acquireOrTransfer(args)

    if (!isPrimary) {
        kotlin.system.exitProcess(0)
    }

    application {
        DisposableEffect(Unit) {
            onDispose {
                SingleInstanceLock.release()
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
        val modelDiscoveryService = remember { ModelDiscoveryService(createPlatformHttpClient()) }
        var isScanningModels by remember { mutableStateOf(false) }
        val whisperInstaller = remember { WhisperInstaller() }
        val whisperStatus by whisperInstaller.state.collectAsState()
        val whisperProvider = remember(whisperStatus.model, preferences.whisperLanguage, preferences.whisperLlmRefinementExperimental, apiKey, preferences.model) {
            val base = WhisperTranscriptionProvider(
                currentModel = { whisperStatus.model },
                whisperLanguage = { preferences.whisperLanguage },
            )
            com.biglexj.lyraflow.platform.whisper.WhisperLlmRefinerProvider(
                baseWhisperProvider = base,
                client = createPlatformHttpClient(),
                apiKey = { apiKey },
                model = { preferences.model },
                isRefinementEnabled = { preferences.whisperLlmRefinementExperimental },
            )
        }
        var shortcut by remember { mutableStateOf(GlobalShortcutFactory.create()) }
        val startsMinimized = args.any { it.equals("--minimized", ignoreCase = true) }
        var windowVisible by remember { mutableStateOf(!startsMinimized) }

        val historyRepository = remember { InMemoryTranscriptionHistoryRepository() }
        val coordinator = remember(
            historyRepository,
            whisperStatus.available,
            whisperProvider,
            preferences.isProviderEnabled,
            preferences.isWhisperEnabled,
        ) {
            val cloudTranscriber = MultimodalTranscriptionProvider(
                client = createPlatformHttpClient(),
                apiKey = { apiKey },
                configuration = { preferences.providerConfiguration },
            )
            val primary: TranscriptionProvider = when {
                preferences.isProviderEnabled -> cloudTranscriber
                preferences.isWhisperEnabled && whisperStatus.available -> whisperProvider
                preferences.isWhisperEnabled -> DisabledTranscriptionProvider("⚠️ Whisper local está activado pero el modelo aún no se ha instalado.")
                else -> DisabledTranscriptionProvider("⚠️ Tanto el dictado en la nube como Whisper local están desactivados.")
            }
            val fallback: () -> TranscriptionProvider? = {
                if (preferences.isProviderEnabled && preferences.isWhisperEnabled && whisperStatus.available) {
                    whisperProvider
                } else {
                    null
                }
            }
            DictationCoordinator(
                transcriber = primary,
                fallbackTranscriber = fallback,
                historyRepository = historyRepository,
                isHistoryEnabled = { preferences.historyRetention != HistoryRetentionPeriod.Disabled },
            )
        }
        val state by coordinator.state.collectAsState()
        val geminiQuotaExhausted by coordinator.geminiQuotaExhausted.collectAsState()
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

        fun scanModels(autoSelectBest: Boolean = false, keyOverride: String? = null) {
            val currentKey = keyOverride ?: apiKey
            if (currentKey.isBlank() || isScanningModels) return
            scope.launch {
                isScanningModels = true
                runCatching {
                    val discovered = modelDiscoveryService.discoverModels(
                        provider = preferences.provider,
                        apiKey = currentKey,
                        endpoint = preferences.endpoint,
                    )
                    val modelIds = discovered.map { it.id }
                    if (modelIds.isNotEmpty()) {
                        val updatedMap = preferences.discoveredModels.toMutableMap()
                        updatedMap[preferences.provider] = modelIds
                        val newModel = if (autoSelectBest || preferences.model.isBlank()) {
                            modelIds.first()
                        } else {
                            preferences.model
                        }
                        val updatedPrefs = preferences.copy(
                            discoveredModels = updatedMap,
                            model = newModel,
                        )
                        preferences = updatedPrefs
                        preferencesStore.save(updatedPrefs)
                    }
                }
                isScanningModels = false
            }
        }

        val savedWindowState = remember { preferencesStore.loadWindowState() }
        val initialPlacement = if (savedWindowState.isMaximized) WindowPlacement.Maximized else WindowPlacement.Floating
        val windowState = rememberWindowState(
            placement = initialPlacement,
            position = WindowPosition(Alignment.Center),
            width = savedWindowState.widthDp.dp,
            height = savedWindowState.heightDp.dp,
        )

        fun saveCurrentWindowState() {
            val isMaximized = windowState.placement == WindowPlacement.Maximized
            val widthDp = if (isMaximized) savedWindowState.widthDp else windowState.size.width.value.toInt().coerceAtLeast(600)
            val heightDp = if (isMaximized) savedWindowState.heightDp else windowState.size.height.value.toInt().coerceAtLeast(400)
            preferencesStore.saveWindowState(widthDp = widthDp, heightDp = heightDp, isMaximized = isMaximized)
        }

        fun exitLyraFlow() {
            saveCurrentWindowState()
            SingleInstanceLock.release()
            if (recording.value) {
                recording.value = false
                runCatching { audio.stop() }
            }
            shortcut.close()
            statusOverlay.close()
            scope.cancel()
            exitApplication()
        }

        var restoreWindowAction by remember { mutableStateOf<(() -> Unit)?>(null) }

        val tray = remember {
            if (isSystemTraySupported()) {
                LyraFlowTray(
                    onOpen = {
                        restoreWindowAction?.invoke() ?: run { windowVisible = true }
                    },
                    onExit = ::exitLyraFlow,
                )
            } else {
                null
            }
        }
        DisposableEffect(tray) {
            onDispose {
                tray?.close()
                statusOverlay.close()
            }
        }

        val appIcon = remember {
            runCatching {
                val stream = Thread.currentThread().contextClassLoader.getResourceAsStream("Square44x44Logo.png")
                    ?: LyraFlowTray::class.java.classLoader.getResourceAsStream("Square44x44Logo.png")
                stream?.use { androidx.compose.ui.res.loadImageBitmap(it) }?.let { androidx.compose.ui.graphics.painter.BitmapPainter(it) }
            }.getOrNull()
        }

        Window(
            onCloseRequest = {
                saveCurrentWindowState()
                if (tray != null) windowVisible = false else exitLyraFlow()
            },
            visible = windowVisible,
            title = if (isDev) "LyraFlow [Dev]" else "LyraFlow",
            icon = appIcon,
            state = windowState,
        ) {
            DisposableEffect(window) {
                runCatching {
                    val stream = Thread.currentThread().contextClassLoader.getResourceAsStream("Square44x44Logo.png")
                        ?: LyraFlowTray::class.java.classLoader.getResourceAsStream("Square44x44Logo.png")
                    stream?.use { javax.imageio.ImageIO.read(it) }?.let { window.iconImage = it }
                }

                fun forceNativeForeground() {
                    window.isMinimized = false
                    window.isVisible = true
                    window.toFront()
                    window.requestFocus()
                    runCatching {
                        val hwnd = com.sun.jna.platform.win32.WinDef.HWND(com.sun.jna.Native.getWindowPointer(window))
                        if (hwnd.pointer != null && hwnd.pointer != com.sun.jna.Pointer.NULL) {
                            com.sun.jna.platform.win32.User32.INSTANCE.ShowWindow(hwnd, com.sun.jna.platform.win32.WinUser.SW_RESTORE)
                            com.sun.jna.platform.win32.User32.INSTANCE.ShowWindow(hwnd, com.sun.jna.platform.win32.WinUser.SW_SHOW)
                            com.sun.jna.platform.win32.User32.INSTANCE.SetForegroundWindow(hwnd)
                        }
                    }
                }

                val doRestore = {
                    windowVisible = true
                    java.awt.EventQueue.invokeLater {
                        forceNativeForeground()
                    }
                }
                restoreWindowAction = doRestore

                SingleInstanceLock.registerActivationListener {
                    doRestore()
                }

                if (isDev) {
                    java.awt.EventQueue.invokeLater {
                        forceNativeForeground()
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
                            val targetFile = droppedFiles?.firstOrNull() as? java.io.File
                            if (targetFile != null && targetFile.exists()) {
                                val name = targetFile.name.lowercase()
                                val isAudio = name.endsWith(".wav") || name.endsWith(".mp3") || name.endsWith(".m4a") ||
                                    name.endsWith(".ogg") || name.endsWith(".flac") || name.endsWith(".aac") || name.endsWith(".wma")
                                val isImage = name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                                    name.endsWith(".webp") || name.endsWith(".bmp") || name.endsWith(".gif")

                                if (isAudio || isImage) {
                                    val bytes = targetFile.readBytes()
                                    val mimeType = when {
                                        name.endsWith(".png") -> "image/png"
                                        name.endsWith(".jpg") || name.endsWith(".jpeg") -> "image/jpeg"
                                        name.endsWith(".webp") -> "image/webp"
                                        name.endsWith(".bmp") -> "image/bmp"
                                        name.endsWith(".gif") -> "image/gif"
                                        name.endsWith(".mp3") -> "audio/mp3"
                                        name.endsWith(".m4a") -> "audio/m4a"
                                        name.endsWith(".ogg") -> "audio/ogg"
                                        name.endsWith(".flac") -> "audio/flac"
                                        else -> "audio/wav"
                                    }
                                    val prompt = if (isImage) {
                                        "Extrae y transcribe fielmente todo el texto visible en esta imagen (reconocimiento OCR), o describe con claridad y precisión el contenido si no contiene texto visible. Devuelve únicamente el resultado final limpio, estructurado y sin comentarios adicionales."
                                    } else {
                                        preferences.systemPrompt
                                    }

                                    scope.launch {
                                        coordinator.process(
                                            TranscriptionRequest(
                                                audio = bytes,
                                                mimeType = mimeType,
                                                model = preferences.model,
                                                systemPrompt = prompt,
                                            ),
                                        )
                                        if (preferences.autoInject) {
                                            val text = (coordinator.state.value as? DictationState.Completed)?.refinedText.orEmpty()
                                            injector.inject(text)
                                        }
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
                    saveCurrentWindowState()
                    window.removeWindowFocusListener(focusListener)
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
                        val providerChanged = preferences.provider != updated.provider
                        if (providerChanged) {
                            apiKey = apiKeyStore.load(updated.provider).ifBlank { environmentApiKey(updated.provider) }
                            if (apiKey.isNotBlank() && updated.discoveredModels[updated.provider].isNullOrEmpty()) {
                                scanModels(autoSelectBest = false)
                            }
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
                        val wasEmpty = apiKey.isBlank()
                        apiKey = it
                        apiKeyStore.save(preferences.provider, it)
                        coordinator.resetQuotaExhausted()
                        if (it.isNotBlank()) {
                            scanModels(autoSelectBest = wasEmpty, keyOverride = it)
                        }
                    },
                    installWhisper = { model -> scope.launch { whisperInstaller.install(model) } },
                    scanModels = { scanModels(autoSelectBest = false) },
                    isScanningModels = isScanningModels,
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
                    geminiQuotaExhausted = geminiQuotaExhausted,
                    resetQuotaExhausted = coordinator::resetQuotaExhausted,
                ),
            )
        }
    }
}

private fun environmentApiKey(provider: AiProvider): String =
    System.getenv("LYRAFLOW_API_KEY").orEmpty().ifBlank {
        System.getenv(provider.apiKeyEnvironmentVariable).orEmpty()
    }
