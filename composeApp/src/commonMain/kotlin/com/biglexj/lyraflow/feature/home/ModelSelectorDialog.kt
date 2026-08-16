package com.biglexj.lyraflow.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.biglexj.lyraflow.core.model.AiProvider

@Composable
fun ModelSelectorDialog(
    provider: AiProvider,
    selectedModel: String,
    availableModels: List<String> = emptyList(),
    isEnabled: Boolean = true,
    isScanning: Boolean = false,
    onScanModels: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onToggleEnabled: (() -> Unit)? = null,
) {
    val models = buildList {
        add(selectedModel)
        if (availableModels.isNotEmpty()) {
            addAll(availableModels)
        } else {
            addAll(provider.suggestedModels)
        }
    }.filter(String::isNotBlank).distinct()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Modelo de ${provider.label}")
                if (onScanModels != null) {
                    TextButton(
                        onClick = onScanModels,
                        enabled = !isScanning,
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text("  Escaneando…", style = MaterialTheme.typography.labelMedium)
                        } else {
                            Text("🔄 Escanear", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Elige el modelo que procesará tu dictado (${models.size} disponibles):",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    models.forEach { model ->
                        ModelOption(
                            model = model,
                            selected = isEnabled && model == selectedModel,
                            onSelect = { onSelect(model) },
                        )
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
                        text = if (isEnabled) "Desactivar ${provider.label}" else "Activar ${provider.label}",
                        color = if (isEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    )
                }
            }
        } else null,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        },
    )
}

@Composable
private fun ModelOption(model: String, selected: Boolean, onSelect: () -> Unit) {
    Surface(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = onSelect)
            Column {
                Text(model, style = MaterialTheme.typography.titleMedium)
                Text(
                    modelDescription(model),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun modelDescription(model: String): String = when {
    model.contains("3.7", ignoreCase = true) -> "Modelo Gemini 3.7 de última generación"
    model.contains("3.6-flash", ignoreCase = true) -> "Excelente equilibrio entre velocidad y contexto"
    model.contains("3.5-flash", ignoreCase = true) -> "Recomendado para dictados rápidos y precisos"
    model.contains("2.5-pro", ignoreCase = true) || model.contains("3.1-pro", ignoreCase = true) -> "Máxima capacidad de razonamiento y contexto"
    model.contains("flash", ignoreCase = true) -> "Modelo Flash ultra rápido"
    model.contains("pro", ignoreCase = true) -> "Modelo Pro de alta precisión"
    model.contains("gpt-5", ignoreCase = true) -> "Generación avanzada de GPT"
    model.contains("gpt-4o", ignoreCase = true) -> "Modelo multimodal inteligente"
    model.contains("audio", ignoreCase = true) -> "Modelo optimizado para audio"
    else -> "Modelo detectado / configurado"
}
