package com.biglexj.lyraflow.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
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
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val models = buildList {
        add(selectedModel)
        addAll(provider.suggestedModels)
    }.filter(String::isNotBlank).distinct()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modelo de ${provider.label}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Elige el modelo que procesará tu dictado. Puedes configurar otro identificador desde Ajustes.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                models.forEach { model ->
                    ModelOption(
                        model = model,
                        selected = model == selectedModel,
                        onSelect = { onSelect(model) },
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
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
    model.contains("3.5-flash-lite", ignoreCase = true) -> "Rápido y eficiente para dictados cotidianos"
    model.contains("3.6-flash", ignoreCase = true) -> "Mejor equilibrio entre velocidad y contexto"
    model.contains("audio", ignoreCase = true) -> "Modelo con entrada de audio"
    else -> "Modelo configurado manualmente"
}
