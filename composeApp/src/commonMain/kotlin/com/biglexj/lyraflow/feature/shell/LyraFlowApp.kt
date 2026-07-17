package com.biglexj.lyraflow.feature.shell

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.biglexj.lyraflow.core.config.AppConfiguration
import com.biglexj.lyraflow.core.config.AppPreferences
import com.biglexj.lyraflow.core.config.WhisperSetupState
import com.biglexj.lyraflow.core.config.next
import com.biglexj.lyraflow.core.audio.RecordingTelemetry
import com.biglexj.lyraflow.core.theme.LyraFlowTheme
import com.biglexj.lyraflow.domain.dictation.DictationState
import com.biglexj.lyraflow.feature.home.HomeScreen
import com.biglexj.lyraflow.feature.settings.SettingsScreen

data class ShellActions(
    val toggleRecording: () -> Unit,
    val injectLastResult: () -> Unit,
    val reset: () -> Unit,
    val updatePreferences: (AppPreferences) -> Unit,
    val updateApiKey: (String) -> Unit,
    val installWhisper: () -> Unit,
)

@Composable
fun LyraFlowApp(
    platform: String,
    state: DictationState,
    configuration: AppConfiguration,
    recordingTelemetry: RecordingTelemetry = RecordingTelemetry(),
    whisperStatus: WhisperSetupState,
    actions: ShellActions,
) {
    LyraFlowTheme(configuration.preferences.themeMode) {
        var destination by remember { mutableStateOf(AppDestination.Home) }
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            BoxWithConstraints {
                val wide = maxWidth >= 720.dp
                if (wide) {
                    Row(Modifier.fillMaxSize()) {
                        LyraNavigationRail(
                            selected = destination,
                            themeMode = configuration.preferences.themeMode,
                            onCycleTheme = {
                                actions.updatePreferences(
                                    configuration.preferences.copy(
                                        themeMode = configuration.preferences.themeMode.next(),
                                    ),
                                )
                            },
                            onSelect = { destination = it },
                        )
                        ScreenContent(destination, platform, state, configuration, recordingTelemetry, whisperStatus, actions, Modifier.weight(1f))
                    }
                } else {
                    Column(Modifier.fillMaxSize()) {
                        ScreenContent(destination, platform, state, configuration, recordingTelemetry, whisperStatus, actions, Modifier.weight(1f))
                        LyraNavigationBar(destination) { destination = it }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenContent(
    destination: AppDestination,
    platform: String,
    state: DictationState,
    configuration: AppConfiguration,
    recordingTelemetry: RecordingTelemetry,
    whisperStatus: WhisperSetupState,
    actions: ShellActions,
    modifier: Modifier,
) {
    Box(modifier) {
        Crossfade(targetState = destination, label = "main-navigation") { current ->
            when (current) {
                AppDestination.Home -> HomeScreen(
                    platform = platform,
                    state = state,
                    configuration = configuration,
                    recordingTelemetry = recordingTelemetry,
                    whisperStatus = whisperStatus,
                    onRecord = actions.toggleRecording,
                    onInject = actions.injectLastResult,
                    onClear = actions.reset,
                    onApiKeyChange = actions.updateApiKey,
                    onInstallWhisper = actions.installWhisper,
                )
                AppDestination.Settings -> SettingsScreen(
                    configuration = configuration,
                    onPreferencesChange = actions.updatePreferences,
                    onApiKeyChange = actions.updateApiKey,
                )
            }
        }
    }
}
