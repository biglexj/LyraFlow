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
import com.biglexj.lyraflow.core.audio.RecordingTelemetry
import com.biglexj.lyraflow.core.network.createPlatformHttpClient
import com.biglexj.lyraflow.data.gemini.GeminiTranscriptionProvider
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.awt.event.WindowFocusListener

fun main(args: Array<String>) = application {
    val preferencesStore = remember { DesktopPreferencesStore() }
    val apiKeyStore = remember { DesktopApiKeyStore() }
    val autoStart = remember { WindowsAutoStart() }
    var preferences by remember { mutableStateOf(preferencesStore.load()) }
    var apiKey by remember {
        mutableStateOf(apiKeyStore.load().ifBlank { System.getenv("GEMINI_API_KEY").orEmpty() })
    }
    var recordingTelemetry by remember { mutableStateOf(RecordingTelemetry()) }
    val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
    val audio = remember { DesktopAudioCapture() }
    val injector = remember { DesktopTextInjector() }
    val statusOverlay = remember { LyraFlowStatusOverlay() }
    val whisperInstaller = remember { WhisperInstaller() }
    val whisperStatus by whisperInstaller.state.collectAsState()
    var shortcut by remember { mutableStateOf(GlobalShortcutFactory.create()) }
    val startsMinimized = args.any { it.equals("--minimized", ignoreCase = true) }
    var windowVisible by remember { mutableStateOf(!startsMinimized || !isSystemTraySupported()) }
    val coordinator = remember {
        DictationCoordinator(GeminiTranscriptionProvider(createPlatformHttpClient()) { apiKey })
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
                coordinator.process(TranscriptionRequest(wav, model = preferences.model))
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
            val focusListener = object : WindowFocusListener {
                override fun windowGainedFocus(event: java.awt.event.WindowEvent) = Unit
                override fun windowLostFocus(event: java.awt.event.WindowEvent) = injector.rememberForegroundTarget()
            }
            window.addWindowFocusListener(focusListener)
            onDispose { window.removeWindowFocusListener(focusListener) }
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
                apiKeyStorageMessage = "La clave se guarda cifrada para tu usuario de Windows.",
            ),
            recordingTelemetry = recordingTelemetry,
            whisperStatus = whisperStatus,
            actions = ShellActions(
                toggleRecording = ::toggleRecording,
                injectLastResult = {
                    val text = (state as? DictationState.Completed)?.refinedText.orEmpty()
                    injector.inject(text)
                },
                reset = coordinator::reset,
                updatePreferences = { updated ->
                    val shortcutChanged = preferences.shortcut != updated.shortcut
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
                    apiKeyStore.save(it)
                },
                installWhisper = { model -> scope.launch { whisperInstaller.install(model) } },
            ),
        )
    }
}
