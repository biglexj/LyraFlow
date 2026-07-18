package com.biglexj.lyraflow.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.biglexj.lyraflow.core.config.AppConfiguration
import com.biglexj.lyraflow.core.config.AppPreferences
import com.biglexj.lyraflow.core.config.ThemeMode
import com.biglexj.lyraflow.core.model.GeminiModel

@Composable
fun SettingsScreen(
    configuration: AppConfiguration,
    onPreferencesChange: (AppPreferences) -> Unit,
    onApiKeyChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Ajustes", style = MaterialTheme.typography.headlineLarge)
            Text("Haz que LyraFlow trabaje a tu manera.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SettingsSection("Apariencia", "Elige cómo se adapta LyraFlow a tu escritorio.") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = configuration.preferences.themeMode == mode,
                        onClick = { onPreferencesChange(configuration.preferences.copy(themeMode = mode)) },
                        label = { Text(mode.label) },
                        modifier = Modifier.height(48.dp),
                        shape = MaterialTheme.shapes.small,
                    )
                }
            }
        }
        SettingsSection("Inteligencia", "Selecciona el equilibrio entre velocidad y contexto.") {
            GeminiModel.entries.forEach { model ->
                ModelOption(
                    model = model,
                    selected = configuration.preferences.model == model,
                    onSelect = { onPreferencesChange(configuration.preferences.copy(model = model)) },
                )
            }
        }
        SettingsSection("Atajo global", "Inicia y detén el dictado desde cualquier aplicación.") {
            ShortcutRecorder(configuration.preferences.shortcut) { shortcut ->
                onPreferencesChange(configuration.preferences.copy(shortcut = shortcut))
            }
        }
        SettingsSection("Gemini API", configuration.apiKeyStorageMessage) {
            ApiKeyField(configuration.sessionApiKey, onApiKeyChange)
        }
        SettingsSection("Después de transcribir", "Controla qué sucede cuando el texto está listo.") {
            SettingSwitch(
                title = "Insertar automáticamente",
                supporting = "Pega el resultado en la aplicación que estabas usando.",
                checked = configuration.preferences.autoInject,
                onCheckedChange = { onPreferencesChange(configuration.preferences.copy(autoInject = it)) },
            )
        }
        SettingsSection("Inicio", "LyraFlow puede quedar listo desde que enciendes el equipo.") {
            SettingSwitch(
                title = "Iniciar con Windows",
                supporting = "Se abre minimizado en el área de notificación.",
                checked = configuration.preferences.launchAtStartup,
                onCheckedChange = { onPreferencesChange(configuration.preferences.copy(launchAtStartup = it)) },
            )
        }
    }
}

@Composable
private fun SettingsSection(title: String, supporting: String, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .34f),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(supporting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            content()
        }
    }
}

@Composable
private fun ModelOption(model: GeminiModel, selected: Boolean, onSelect: () -> Unit) {
    Surface(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RadioButton(selected = selected, onClick = onSelect)
            Column {
                Text(model.label, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (model == GeminiModel.Fast) "Respuesta rápida para dictado cotidiano" else "Más contexto para textos complejos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ApiKeyField(value: String, onValueChange: (String) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp),
        label = { Text("GEMINI_API_KEY") },
        placeholder = { Text("Pega aquí tu clave de Gemini") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            TextButton(onClick = { visible = !visible }) { Text(if (visible) "Ocultar" else "Mostrar") }
        },
        shape = MaterialTheme.shapes.medium,
    )
}

@Composable
private fun SettingSwitch(title: String, supporting: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(supporting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
