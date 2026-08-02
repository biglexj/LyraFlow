package com.biglexj.lyraflow.feature.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.biglexj.lyraflow.core.config.AppConfiguration
import com.biglexj.lyraflow.core.config.AppPreferences
import com.biglexj.lyraflow.core.config.WhisperSetupState
import com.biglexj.lyraflow.core.config.WhisperModel
import com.biglexj.lyraflow.core.config.next
import com.biglexj.lyraflow.core.audio.RecordingTelemetry
import com.biglexj.lyraflow.core.network.createPlatformHttpClient
import com.biglexj.lyraflow.core.theme.LyraFlowTheme
import com.biglexj.lyraflow.core.update.UpdateChecker
import com.biglexj.lyraflow.core.update.UpdateRelease
import com.biglexj.lyraflow.core.update.UpdateService
import com.biglexj.lyraflow.domain.dictation.DictationState
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.biglexj.lyraflow.data.history.InMemoryTranscriptionHistoryRepository
import com.biglexj.lyraflow.data.history.TranscriptionHistoryRepository
import com.biglexj.lyraflow.feature.about.AboutDialog
import com.biglexj.lyraflow.feature.history.HistoryScreen
import com.biglexj.lyraflow.feature.home.HomeScreen
import com.biglexj.lyraflow.feature.settings.SettingsScreen
import com.biglexj.lyraflow.feature.update.UpdateBanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ShellActions(
    val toggleRecording: () -> Unit,
    val injectLastResult: () -> Unit,
    val reset: () -> Unit,
    val updatePreferences: (AppPreferences) -> Unit,
    val updateApiKey: (String) -> Unit,
    val installWhisper: (WhisperModel) -> Unit,
    val retry: () -> Unit = {},
    val retryWhisper: () -> Unit = {},
)

@Composable
fun LyraFlowApp(
    platform: String,
    state: DictationState,
    configuration: AppConfiguration,
    recordingTelemetry: RecordingTelemetry = RecordingTelemetry(),
    whisperStatus: WhisperSetupState,
    actions: ShellActions,
    historyRepository: TranscriptionHistoryRepository = remember { InMemoryTranscriptionHistoryRepository() },
) {
    LyraFlowTheme(configuration.preferences.themeMode) {
        var destination by remember { mutableStateOf(AppDestination.Home) }
        var showAboutDialog by remember { mutableStateOf(false) }
        var availableUpdate by remember { mutableStateOf<UpdateRelease?>(null) }
        var upToDate by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val updateService = remember { UpdateService(createPlatformHttpClient()) }

        // Verificación silenciosa al iniciar: no muestra ningún mensaje si todo está al día.
        val checkSilent: suspend () -> Unit = {
            val remoteRelease = updateService.checkLatestRelease()
            if (remoteRelease != null && UpdateChecker.isNewerVersion("1.1.1", remoteRelease.version)) {
                availableUpdate = remoteRelease
            }
        }

        // Verificación manual: muestra el mensaje de "al día" si no hay actualización.
        val checkForUpdates: () -> Unit = {
            upToDate = false
            scope.launch {
                val remoteRelease = updateService.checkLatestRelease()
                if (remoteRelease != null && UpdateChecker.isNewerVersion("1.1.1", remoteRelease.version)) {
                    availableUpdate = remoteRelease
                    upToDate = false
                } else {
                    upToDate = true
                }
            }
        }

        val historyEnabled = configuration.preferences.historyRetention != com.biglexj.lyraflow.core.config.HistoryRetentionPeriod.Disabled
        val visibleDestinations = remember(historyEnabled) {
            if (historyEnabled) AppDestination.entries else AppDestination.entries.filter { it != AppDestination.History }
        }

        LaunchedEffect(Unit) { checkSilent() }

        LaunchedEffect(configuration.preferences.historyRetention) {
            if (!historyEnabled && destination == AppDestination.History) {
                destination = AppDestination.Home
            }
            historyRepository.purgeExpired(configuration.preferences.historyRetention.hours)
        }

        // Auto-ocultar el toast de "al día" tras 4 segundos.
        LaunchedEffect(upToDate) {
            if (upToDate) {
                delay(4_000)
                upToDate = false
            }
        }

        if (showAboutDialog) {
            AboutDialog(
                onDismiss = { showAboutDialog = false },
                onCheckForUpdates = checkForUpdates,
            )
        }

        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize()) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
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
                                visibleDestinations = visibleDestinations,
                                onOpenAbout = { showAboutDialog = true },
                                onSelect = { destination = it },
                            )
                            ScreenContent(
                                destination = destination,
                                platform = platform,
                                state = state,
                                configuration = configuration,
                                recordingTelemetry = recordingTelemetry,
                                whisperStatus = whisperStatus,
                                actions = actions,
                                availableUpdate = availableUpdate,
                                onDismissUpdate = { availableUpdate = null },
                                onOpenAbout = { showAboutDialog = true },
                                onCheckForUpdates = checkForUpdates,
                                historyRepository = historyRepository,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        Column(Modifier.fillMaxSize()) {
                            ScreenContent(
                                destination = destination,
                                platform = platform,
                                state = state,
                                configuration = configuration,
                                recordingTelemetry = recordingTelemetry,
                                whisperStatus = whisperStatus,
                                actions = actions,
                                availableUpdate = availableUpdate,
                                onDismissUpdate = { availableUpdate = null },
                                onOpenAbout = { showAboutDialog = true },
                                onCheckForUpdates = checkForUpdates,
                                historyRepository = historyRepository,
                                modifier = Modifier.weight(1f),
                            )
                            LyraNavigationBar(
                                selected = destination,
                                visibleDestinations = visibleDestinations,
                                onOpenAbout = { showAboutDialog = true },
                            ) { destination = it }
                        }
                    }
                }

                // Toast global de "al día" — visible sobre cualquier pantalla.
                AnimatedVisibility(
                    visible = upToDate,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                    enter = fadeIn() + slideInVertically { -it },
                    exit = fadeOut() + slideOutVertically { -it },
                ) {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    ) {
                        Text(
                            text = "✅ Estás en la última versión de LyraFlow.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        )
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
    availableUpdate: UpdateRelease?,
    onDismissUpdate: () -> Unit,
    onOpenAbout: () -> Unit,
    onCheckForUpdates: () -> Unit,
    historyRepository: TranscriptionHistoryRepository,
    modifier: Modifier,
) {
    val scope = rememberCoroutineScope()
    val historyEntries by historyRepository.history.collectAsState()
    val clipboardManager = LocalClipboardManager.current
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
                    onPreferencesChange = actions.updatePreferences,
                    onInstallWhisper = actions.installWhisper,
                    onRetry = actions.retry,
                    onRetryWhisper = actions.retryWhisper,
                )
                AppDestination.History -> HistoryScreen(
                    entries = historyEntries,
                    onCopyText = { text ->
                        clipboardManager.setText(AnnotatedString(text))
                    },
                    onDeleteEntry = { id ->
                        scope.launch { historyRepository.deleteEntry(id) }
                    },
                    onClearAll = {
                        scope.launch { historyRepository.clearHistory() }
                    },
                )
                AppDestination.Settings -> SettingsScreen(
                    configuration = configuration,
                    onPreferencesChange = actions.updatePreferences,
                    onApiKeyChange = actions.updateApiKey,
                    onOpenAbout = onOpenAbout,
                    onCheckForUpdates = onCheckForUpdates,
                )
            }
        }
        if (availableUpdate != null) {
            UpdateBanner(
                release = availableUpdate,
                onDismiss = onDismissUpdate,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }
    }
}
