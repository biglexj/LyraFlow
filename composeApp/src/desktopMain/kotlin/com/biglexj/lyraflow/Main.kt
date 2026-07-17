package com.biglexj.lyraflow

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.biglexj.lyraflow.platform.whisper.WhisperSidecar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

fun main() = application {
    val preferencesStore = remember { DesktopPreferencesStore() }
    var preferences by remember { mutableStateOf(preferencesStore.load()) }
    var apiKey by remember { mutableStateOf(System.getenv("GEMINI_API_KEY").orEmpty()) }
    var recordingTelemetry by remember { mutableStateOf(RecordingTelemetry()) }
    val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
    val audio = remember { DesktopAudioCapture() }
    val injector = remember { DesktopTextInjector() }
    val whisper = remember { WhisperSidecar() }
    var shortcut by remember { mutableStateOf(GlobalShortcutFactory.create()) }
    val coordinator = remember {
        DictationCoordinator(GeminiTranscriptionProvider(createPlatformHttpClient()) { apiKey })
    }
    val state by coordinator.state.collectAsState()
    val recording = remember { mutableStateOf(false) }

    fun toggleRecording() {
        if (!recording.value) {
            recordingTelemetry = RecordingTelemetry()
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

    Window(
        onCloseRequest = {
            shortcut.close()
            exitApplication()
        },
        title = "LyraFlow",
        state = rememberWindowState(
            width = 1200.dp,
            height = 840.dp,
            position = WindowPosition(Alignment.Center),
        ),
    ) {
        LyraFlowApp(
            platform = "${System.getProperty("os.name")} · ${shortcut.status}",
            state = state,
            configuration = AppConfiguration(preferences, apiKey),
            recordingTelemetry = recordingTelemetry,
            whisperStatus = whisper.status,
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
                    if (shortcutChanged) {
                        shortcut.close()
                        shortcut = GlobalShortcutFactory.create().also { replacement ->
                            replacement.start(updated.shortcut) {
                                scope.launch { toggleRecording() }
                            }
                        }
                    }
                },
                updateApiKey = { apiKey = it },
            ),
        )
    }
}
