package com.biglexj.lyraflow.feature.home

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
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
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
    onInstallWhisper: (WhisperModel) -> Unit,
) {
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showWhisperModelDialog by remember { mutableStateOf(false) }
    if (showApiKeyDialog) {
        ApiKeyDialog(
            initialValue = configuration.sessionApiKey,
            onDismiss = { showApiKeyDialog = false },
            onSave = {
                onApiKeyChange(it)
                showApiKeyDialog = false
            },
        )
    }
    if (showWhisperModelDialog) {
        WhisperModelDialog(
            onDismiss = { showWhisperModelDialog = false },
            onInstall = {
                onInstallWhisper(it)
                showWhisperModelDialog = false
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
        DictationHero(state, configuration, recordingTelemetry, onRecord)
        ResultCard(state, onInject, onClear)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusCard(
                title = "Gemini",
                detail = if (configuration.sessionApiKey.isNotBlank()) configuration.preferences.model.label else "Añade tu API key en Ajustes",
                available = configuration.sessionApiKey.isNotBlank(),
                modifier = Modifier.weight(1f),
                onClick = if (configuration.sessionApiKey.isBlank()) {{ showApiKeyDialog = true }} else null,
            )
            StatusCard(
                title = "Whisper local",
                detail = whisperStatus.detail,
                available = whisperStatus.available,
                modifier = Modifier.weight(1f),
                progress = whisperStatus.progress,
                onClick = if (!whisperStatus.available && !whisperStatus.downloading) {{ showWhisperModelDialog = true }} else null,
            )
        }
    }
}

@Composable
private fun WhisperModelDialog(onDismiss: () -> Unit, onInstall: (WhisperModel) -> Unit) {
    var selected by remember { mutableStateOf(WhisperModel.Base) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Elige el modelo de Whisper") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Puedes instalar otro modelo más adelante si lo necesitas.")
                WhisperModel.entries.forEach { model ->
                    Surface(
                        onClick = { selected = model },
                        shape = MaterialTheme.shapes.medium,
                        color = if (selected == model) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f),
                    ) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(model.label, style = MaterialTheme.typography.titleMedium)
                            Text(model.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onInstall(selected) }) { Text("Instalar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
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
    val processing = state is DictationState.Transcribing
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
                    else -> "Pulsa ${configuration.preferences.shortcut.label} o comienza desde aquí."
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
                processing = processing,
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
private fun ResultCard(state: DictationState, onInject: () -> Unit, onClear: () -> Unit) {
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
    is DictationState.Transcribing -> "Dándole forma a tus ideas"
    is DictationState.Completed -> "Listo en ${state.elapsedMillis} ms"
    is DictationState.Failed -> "Algo interrumpió el dictado"
}

private fun formatDuration(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1_000L
    val minutes = (totalSeconds / 60L).toString().padStart(2, '0')
    val seconds = (totalSeconds % 60L).toString().padStart(2, '0')
    return "$minutes:$seconds"
}
