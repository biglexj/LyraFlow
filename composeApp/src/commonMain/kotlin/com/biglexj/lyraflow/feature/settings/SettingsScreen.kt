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
import com.biglexj.lyraflow.core.model.AiProvider

@Composable
fun SettingsScreen(
    configuration: AppConfiguration,
    onPreferencesChange: (AppPreferences) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onOpenAbout: () -> Unit = {},
    onCheckForUpdates: () -> Unit = {},
) {
    val preferences = configuration.preferences
    val provider = preferences.provider
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
                        selected = preferences.themeMode == mode,
                        onClick = { onPreferencesChange(preferences.copy(themeMode = mode)) },
                        label = { Text(mode.label) },
                        modifier = Modifier.height(48.dp),
                        shape = MaterialTheme.shapes.small,
                    )
                }
            }
        }
        SettingsSection("Proveedor multimodal", "Una sola llamada recibe el audio y devuelve el texto corregido.") {
            AiProvider.entries.forEach { option ->
                ProviderOption(
                    provider = option,
                    selected = provider == option,
                    onSelect = {
                        onPreferencesChange(
                            preferences.copy(
                                provider = option,
                                model = option.defaultModel,
                                endpoint = option.defaultEndpoint,
                            ),
                        )
                    },
                )
            }
        }
        SettingsSection("Modelo multimodal", "Escribe cualquier identificador de modelo que acepte audio y texto.") {
            ModelField(
                value = preferences.model,
                onValueChange = { onPreferencesChange(preferences.copy(model = it)) },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                provider.suggestedModels.forEach { model ->
                    FilterChip(
                        selected = preferences.model == model,
                        onClick = { onPreferencesChange(preferences.copy(model = model)) },
                        label = { Text(model) },
                    )
                }
            }
        }
        if (provider == AiProvider.OpenAiCompatible) {
            SettingsSection("Endpoint OpenAI-compatible", "Compatible con OpenAI, gateways y servidores locales que acepten Chat Completions.") {
                EndpointField(
                    value = preferences.endpoint,
                    onValueChange = { onPreferencesChange(preferences.copy(endpoint = it)) },
                )
            }
        }
        SettingsSection("${provider.label} API", "La clave se conserva cifrada para tu usuario de Windows.") {
            ApiKeyField(
                value = configuration.sessionApiKey,
                label = provider.apiKeyEnvironmentVariable,
                onValueChange = onApiKeyChange,
            )
        }
        SettingsSection("Instrucciones de transcripción (System Prompt)", "Personaliza cómo la IA procesa y aplica formato a tus dictados por voz.") {
            SystemPromptField(
                value = preferences.systemPrompt,
                onValueChange = { onPreferencesChange(preferences.copy(systemPrompt = it)) },
                onReset = { onPreferencesChange(preferences.copy(systemPrompt = AppPreferences.DEFAULT_SYSTEM_PROMPT)) },
            )
        }
        SettingsSection("Atajo global", "Inicia y detén el dictado desde cualquier aplicación.") {
            ShortcutRecorder(preferences.shortcut) { shortcut ->
                onPreferencesChange(preferences.copy(shortcut = shortcut))
            }
        }
        SettingsSection("Después de transcribir", "Controla qué sucede cuando el texto está listo.") {
            SettingSwitch(
                title = "Insertar automáticamente",
                supporting = "Pega el resultado en la aplicación que estabas usando.",
                checked = preferences.autoInject,
                onCheckedChange = { onPreferencesChange(preferences.copy(autoInject = it)) },
            )
        }
        SettingsSection("Inicio", "LyraFlow puede quedar listo desde que enciendes el equipo.") {
            SettingSwitch(
                title = "Iniciar con Windows",
                supporting = "Se abre minimizado en el área de notificación.",
                checked = preferences.launchAtStartup,
                onCheckedChange = { onPreferencesChange(preferences.copy(launchAtStartup = it)) },
            )
        }
        SettingsSection("Acerca de la Aplicación", "Información de versión, autoría y comprobación de actualizaciones.") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("LyraFlow v1.0.8", style = MaterialTheme.typography.titleMedium)
                    Text("Creado por biglexj (2026) • Licencia MIT", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onCheckForUpdates) {
                        Text("Buscar actualizaciones")
                    }
                    TextButton(onClick = onOpenAbout) {
                        Text("Ver detalles")
                    }
                }
            }
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
private fun ProviderOption(provider: AiProvider, selected: Boolean, onSelect: () -> Unit) {
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
                Text(provider.label, style = MaterialTheme.typography.titleMedium)
                Text(provider.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ModelField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp),
        label = { Text("Identificador del modelo") },
        placeholder = { Text("Ejemplo: gemini-3.6-flash") },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
    )
}

@Composable
private fun EndpointField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().widthIn(max = 900.dp),
        label = { Text("URL completa del endpoint") },
        placeholder = { Text("https://api.openai.com/v1/chat/completions") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        shape = MaterialTheme.shapes.medium,
    )
}

@Composable
private fun ApiKeyField(value: String, label: String, onValueChange: (String) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp),
        label = { Text(label) },
        placeholder = { Text("Pega aquí tu clave de API") },
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

@Composable
private fun SystemPromptField(value: String, onValueChange: (String) -> Unit, onReset: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            maxLines = 8,
            shape = MaterialTheme.shapes.medium,
            placeholder = { Text("Escribe tus instrucciones personalizadas...") },
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onReset) {
                Text("Restablecer instrucciones predeterminadas")
            }
        }
    }
}
