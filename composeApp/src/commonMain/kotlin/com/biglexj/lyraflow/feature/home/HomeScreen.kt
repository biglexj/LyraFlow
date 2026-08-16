package com.biglexj.lyraflow.feature.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.biglexj.lyraflow.core.config.AppConfiguration
import com.biglexj.lyraflow.core.config.AppPreferences
import com.biglexj.lyraflow.core.config.WhisperSetupState
import com.biglexj.lyraflow.core.config.WhisperModel
import com.biglexj.lyraflow.core.audio.RecordingTelemetry
import com.biglexj.lyraflow.domain.dictation.DictationState
import com.biglexj.lyraflow.feature.components.LyraIcon
import com.biglexj.lyraflow.feature.components.LyraIconType

@Composable
fun HomeScreen(
    platform: String,
    state: DictationState,
    configuration: AppConfiguration,
    whisperStatus: WhisperSetupState,
    recordingTelemetry: RecordingTelemetry,
    onRecord: () -> Unit,
    onInject: () -> Unit,
    onClear: () -> Unit,
    onApiKeyChange: (String) -> Unit,
    onPreferencesChange: (AppPreferences) -> Unit,
    onInstallWhisper: (WhisperModel) -> Unit,
    isScanningModels: Boolean = false,
    onScanModels: () -> Unit = {},
    onRetry: () -> Unit = {},
    onRetryWhisper: () -> Unit = {},
    geminiQuotaExhausted: Boolean = false,
    onResetQuotaExhausted: () -> Unit = {},
) {
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }
    var showWhisperModelDialog by remember { mutableStateOf(false) }
    if (showApiKeyDialog) {
        ApiKeyDialog(
            initialValue = configuration.sessionApiKey,
            provider = configuration.preferences.provider,
            onDismiss = { showApiKeyDialog = false },
            onSave = {
                onApiKeyChange(it)
                onResetQuotaExhausted()
                showApiKeyDialog = false
            },
        )
    }
    if (showModelDialog) {
        val provider = configuration.preferences.provider
        ModelSelectorDialog(
            provider = provider,
            selectedModel = configuration.preferences.model,
            availableModels = configuration.preferences.availableModels(provider),
            isEnabled = configuration.preferences.isProviderEnabled,
            isScanning = isScanningModels,
            onScanModels = if (configuration.sessionApiKey.isNotBlank()) onScanModels else null,
            onDismiss = { showModelDialog = false },
            onSelect = { model ->
                onPreferencesChange(configuration.preferences.copy(model = model, isProviderEnabled = true))
                showModelDialog = false
            },
            onToggleEnabled = {
                onPreferencesChange(
                    configuration.preferences.copy(isProviderEnabled = !configuration.preferences.isProviderEnabled)
                )
            },
        )
    }
    if (showWhisperModelDialog) {
        WhisperModelDialog(
            currentModel = whisperStatus.model,
            currentLanguage = configuration.preferences.whisperLanguage,
            isWhisperEnabled = configuration.preferences.isWhisperEnabled,
            onDismiss = { showWhisperModelDialog = false },
            onInstall = {
                onPreferencesChange(configuration.preferences.copy(isWhisperEnabled = true))
                onInstallWhisper(it)
                showWhisperModelDialog = false
            },
            onLanguageSelect = { lang ->
                onPreferencesChange(configuration.preferences.copy(whisperLanguage = lang))
            },
            onToggleEnabled = {
                onPreferencesChange(
                    configuration.preferences.copy(isWhisperEnabled = !configuration.preferences.isWhisperEnabled)
                )
            },
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Tu voz, ya bien escrita", style = MaterialTheme.typography.headlineLarge)
            Text(platform, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (geminiQuotaExhausted) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LyraIcon(LyraIconType.Settings, Modifier.size(24.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "⚠️ Cuota de Gemini Agotada",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "Transcripción autónoma con Whisper local activa. Las siguientes peticiones se procesarán localmente.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                        )
                    }
                    TextButton(
                        onClick = { showApiKeyDialog = true },
                    ) {
                        Text("Actualizar Key")
                    }
                }
            }
        }
        DictationHero(state, configuration, recordingTelemetry, onRecord)
        ResultCard(
            state = state,
            onInject = onInject,
            onClear = onClear,
            onRetry = onRetry,
            onRetryWhisper = onRetryWhisper,
            providerLabel = configuration.preferences.provider.label,
            whisperAvailable = whisperStatus.available,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val isProviderActive = configuration.preferences.isProviderEnabled
            val providerTitle = if (isProviderActive) configuration.preferences.provider.label else "${configuration.preferences.provider.label} (Desactivado)"
            val providerDetail = when {
                !isProviderActive -> "Dictado por nube desactivado"
                configuration.sessionApiKey.isBlank() -> "Añade tu API key en Ajustes"
                else -> configuration.preferences.model
            }
            StatusCard(
                title = providerTitle,
                detail = providerDetail,
                available = isProviderActive && configuration.sessionApiKey.isNotBlank(),
                modifier = Modifier.weight(1f),
                onClick = {
                    if (configuration.sessionApiKey.isBlank()) {
                        showApiKeyDialog = true
                    } else {
                        showModelDialog = true
                    }
                },
            )

            val isWhisperActive = configuration.preferences.isWhisperEnabled
            val whisperTitle = if (isWhisperActive) "Whisper local" else "Whisper local (Desactivado)"
            val whisperDetail = if (!isWhisperActive) "Dictado offline desactivado" else whisperStatus.detail
            StatusCard(
                title = whisperTitle,
                detail = whisperDetail,
                available = isWhisperActive && whisperStatus.available,
                modifier = Modifier.weight(1f),
                progress = whisperStatus.progress,
                onClick = if (!whisperStatus.downloading) { { showWhisperModelDialog = true } } else null,
            )
        }
    }
}

@Composable
private fun WhisperModelDialog(
    currentModel: WhisperModel?,
    currentLanguage: com.biglexj.lyraflow.core.config.WhisperLanguage = com.biglexj.lyraflow.core.config.WhisperLanguage.Auto,
    isWhisperEnabled: Boolean = true,
    onDismiss: () -> Unit,
    onInstall: (WhisperModel) -> Unit,
    onLanguageSelect: (com.biglexj.lyraflow.core.config.WhisperLanguage) -> Unit = {},
    onToggleEnabled: (() -> Unit)? = null,
) {
    var selected by remember { mutableStateOf(currentModel ?: WhisperModel.Base) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modelo de Whisper local") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Idioma de transcripción:", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        com.biglexj.lyraflow.core.config.WhisperLanguage.entries.forEach { lang ->
                            FilterChip(
                                selected = currentLanguage == lang,
                                onClick = { onLanguageSelect(lang) },
                                label = { Text(lang.label, style = MaterialTheme.typography.labelMedium) },
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Text("Elige la variante de Whisper que procesará tus dictados offline:")
                WhisperModel.entries.forEach { model ->
                    val isActive = isWhisperEnabled && model == currentModel
                    Surface(
                        onClick = { selected = model },
                        shape = MaterialTheme.shapes.medium,
                        color = if (selected == model) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f),
                    ) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(model.label, style = MaterialTheme.typography.titleMedium)
                                if (isActive) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        shape = MaterialTheme.shapes.extraSmall,
                                    ) {
                                        Text(
                                            "Activo",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                            }
                            Text(model.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        dismissButton = if (onToggleEnabled != null) {
            {
                TextButton(
                    onClick = {
                        onToggleEnabled()
                        onDismiss()
                    },
                ) {
                    Text(
                        text = if (isWhisperEnabled) "Desactivar Whisper" else "Activar Whisper",
                        color = if (isWhisperEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    )
                }
            }
        } else null,
        confirmButton = {
            val isSame = selected == currentModel
            TextButton(
                onClick = {
                    if (!isSame) {
                        onInstall(selected)
                    } else {
                        onDismiss()
                    }
                },
            ) {
                Text(if (isSame) "Cerrar" else "Instalar")
            }
        },
    )
}

@Composable
private fun DictationHero(
    state: DictationState,
    configuration: AppConfiguration,
    telemetry: RecordingTelemetry,
    onRecord: () -> Unit,
) {
    val listening = state is DictationState.Listening
    val attemptFailed = state is DictationState.AttemptFailed
    val processing = state is DictationState.Transcribing || attemptFailed
    ElevatedCard(shape = MaterialTheme.shapes.large) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium) {
                LyraIcon(LyraIconType.Mic, Modifier.padding(14.dp).size(34.dp))
            }
            Text(statusTitle(state), style = MaterialTheme.typography.headlineMedium)
            Text(
                when {
                    listening -> "Habla con naturalidad. LyraFlow se encarga del resto."
                    processing -> "Procesando el audio y organizando tus ideas…"
                    else -> "Pulsa ${configuration.preferences.shortcut.label} o arrastra un archivo de audio o imagen aquí."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (telemetry.durationMillis > 0L) {
                Text(
                    if (listening) "Grabando · ${formatDuration(telemetry.durationMillis)}"
                    else "Audio capturado · ${formatDuration(telemetry.durationMillis)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            DictationVisualizer(
                listening = listening,
                processing = state is DictationState.Transcribing,
                attemptFailed = attemptFailed,
                level = telemetry.level,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            )
            Button(
                onClick = onRecord,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.small,
                enabled = !processing && (configuration.sessionApiKey.isNotBlank() || listening),
            ) {
                if (processing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f),
                    )
                    Text("  Procesando…")
                } else {
                    Text(if (listening) "Terminar y transcribir" else "Comenzar dictado")
                }
            }
        }
    }
}

@Composable
private fun ResultCard(
    state: DictationState,
    onInject: () -> Unit,
    onClear: () -> Unit,
    onRetry: () -> Unit,
    onRetryWhisper: () -> Unit,
    providerLabel: String,
    whisperAvailable: Boolean,
) {
    val completed = state as? DictationState.Completed
    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .35f), shape = MaterialTheme.shapes.large) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Último resultado", style = MaterialTheme.typography.titleLarge)
            Text(
                completed?.refinedText?.ifBlank { "No se detectó voz clara." }
                    ?: if (state is DictationState.Failed) state.message else "Tu transcripción aparecerá aquí.",
                style = MaterialTheme.typography.bodyLarge,
                color = if (state is DictationState.Failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = onInject, enabled = completed != null, modifier = Modifier.height(48.dp), shape = MaterialTheme.shapes.small) {
                    LyraIcon(LyraIconType.Copy, Modifier.size(20.dp))
                    Text("  Insertar")
                }
                if (state is DictationState.Failed) {
                    FilledTonalButton(
                        onClick = onRetry,
                        modifier = Modifier.height(48.dp),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text("Reintentar $providerLabel")
                    }
                    if (whisperAvailable) {
                        FilledTonalButton(
                            onClick = onRetryWhisper,
                            modifier = Modifier.height(48.dp),
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Text("Reintentar con Whisper")
                        }
                    }
                }
                OutlinedButton(onClick = onClear, enabled = state !is DictationState.Idle, modifier = Modifier.height(48.dp), shape = MaterialTheme.shapes.small) {
                    LyraIcon(LyraIconType.Clear, Modifier.size(20.dp))
                    Text("  Limpiar")
                }
            }
        }
    }
}

private fun statusTitle(state: DictationState): String = when (state) {
    DictationState.Idle -> "Listo para escucharte"
    DictationState.Listening -> "Te estoy escuchando"
    is DictationState.Transcribing, is DictationState.AttemptFailed -> "Dándole forma a tus ideas"
    is DictationState.Completed -> "Listo en ${state.elapsedMillis} ms"
    is DictationState.Failed -> "Algo interrumpió el dictado"
}

private fun formatDuration(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1_000L
    val minutes = (totalSeconds / 60L).toString().padStart(2, '0')
    val seconds = (totalSeconds % 60L).toString().padStart(2, '0')
    return "$minutes:$seconds"
}
