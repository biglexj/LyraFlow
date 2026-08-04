package com.biglexj.lyraflow.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import com.biglexj.lyraflow.core.config.AppConfiguration
import com.biglexj.lyraflow.core.config.AppPreferences
import com.biglexj.lyraflow.core.config.AppVersion
import com.biglexj.lyraflow.core.config.HistoryRetentionPeriod
import com.biglexj.lyraflow.core.config.SystemPromptMode
import com.biglexj.lyraflow.core.config.ThemeMode
import com.biglexj.lyraflow.core.config.WhisperLanguage
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
    val hasApiKey = configuration.sessionApiKey.isNotBlank()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Ajustes", style = MaterialTheme.typography.headlineLarge)
            Text("Haz que LyraFlow trabaje a tu manera.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SettingsSection("Apariencia", "Elige cómo se adapta LyraFlow a tu escritorio.") {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            ) {
                Row(
                    modifier = Modifier.padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ThemeMode.entries.forEach { mode ->
                        val selected = preferences.themeMode == mode
                        Surface(
                            onClick = { onPreferencesChange(preferences.copy(themeMode = mode)) },
                            shape = MaterialTheme.shapes.small,
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                            border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) else null,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                val icon = when (mode) {
                                    ThemeMode.System -> "💻"
                                    ThemeMode.Light -> "☀️"
                                    ThemeMode.Dark -> "🌙"
                                }
                                Text(icon, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
        SettingsSection("Historial y Retención", "Define el tiempo de conservación de tus dictados recientes antes del borrado automático.") {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            ) {
                Row(
                    modifier = Modifier.padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    HistoryRetentionPeriod.entries.forEach { period ->
                        val selected = preferences.historyRetention == period
                        Surface(
                            onClick = { onPreferencesChange(preferences.copy(historyRetention = period)) },
                            shape = MaterialTheme.shapes.small,
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                            border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) else null,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text("🕒", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = period.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
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
                enabled = hasApiKey,
                onValueChange = { onPreferencesChange(preferences.copy(model = it)) },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                provider.suggestedModels.forEach { model ->
                    FilterChip(
                        selected = preferences.model == model,
                        enabled = hasApiKey,
                        onClick = { onPreferencesChange(preferences.copy(model = model)) },
                        label = { Text(model) },
                    )
                }
            }
            if (!hasApiKey) {
                Text(
                    text = "🔒 Se requiere API Key: Ingresa tu clave en la sección '${provider.label} API' abajo para desbloquear la selección de modelos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (provider == AiProvider.OpenAiCompatible) {
            SettingsSection("Endpoint OpenAI-compatible", "Compatible con OpenAI, gateways y servidores locales que acepten Chat Completions.") {
                EndpointField(
                    value = preferences.endpoint,
                    enabled = hasApiKey,
                    onValueChange = { onPreferencesChange(preferences.copy(endpoint = it)) },
                )
                Text(
                    text = "ℹ️ Nota: Integración experimental con proveedores OpenAI-compatible / pendiente de comprobación completa con todos los endpoints. Si encuentras algún problema con un servidor específico, por favor reporta el inconveniente en GitHub o en nuestro canal de Feedback.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        SettingsSection("${provider.label} API", "La clave se conserva cifrada para tu usuario de Windows.") {
            ApiKeyField(
                value = configuration.sessionApiKey,
                label = provider.apiKeyEnvironmentVariable,
                onValueChange = { newKey ->
                    val wasEmpty = configuration.sessionApiKey.isBlank()
                    onApiKeyChange(newKey)
                    if (wasEmpty && newKey.isNotBlank() && preferences.systemPromptMode == SystemPromptMode.Literal) {
                        onPreferencesChange(
                            preferences.copy(
                                systemPromptMode = SystemPromptMode.Smart,
                                systemPrompt = AppPreferences.DEFAULT_SYSTEM_PROMPT,
                            ),
                        )
                    }
                },
            )
        }
        SettingsSection("Instrucciones de transcripción (System Prompt)", "Elige entre transcripción inteligente refinada o voz original literal sin alterar conceptos.") {
            val effectiveMode = if (!hasApiKey) SystemPromptMode.Literal else preferences.systemPromptMode
            SystemPromptField(
                currentMode = effectiveMode,
                value = preferences.systemPrompt,
                hasApiKey = hasApiKey,
                onModeSelect = { mode ->
                    if (!hasApiKey && mode != SystemPromptMode.Literal) return@SystemPromptField
                    val newPrompt = when (mode) {
                        SystemPromptMode.Smart -> AppPreferences.DEFAULT_SYSTEM_PROMPT
                        SystemPromptMode.Literal -> AppPreferences.LITERAL_SYSTEM_PROMPT
                        SystemPromptMode.Custom -> preferences.systemPrompt
                    }
                    onPreferencesChange(preferences.copy(systemPromptMode = mode, systemPrompt = newPrompt))
                },
                onValueChange = { newValue ->
                    if (!hasApiKey) return@SystemPromptField
                    val newMode = when (newValue.trim()) {
                        AppPreferences.DEFAULT_SYSTEM_PROMPT.trim() -> SystemPromptMode.Smart
                        AppPreferences.LITERAL_SYSTEM_PROMPT.trim() -> SystemPromptMode.Literal
                        else -> SystemPromptMode.Custom
                    }
                    onPreferencesChange(preferences.copy(systemPromptMode = newMode, systemPrompt = newValue))
                },
                onReset = {
                    if (hasApiKey) {
                        onPreferencesChange(
                            preferences.copy(
                                systemPromptMode = SystemPromptMode.Smart,
                                systemPrompt = AppPreferences.DEFAULT_SYSTEM_PROMPT,
                            ),
                        )
                    }
                },
            )
        }
        SettingsSection("Atajo global", "Inicia y detén el dictado desde cualquier aplicación.") {
            ShortcutRecorder(preferences.shortcut) { shortcut ->
                onPreferencesChange(preferences.copy(shortcut = shortcut))
            }
        }
        @OptIn(ExperimentalLayoutApi::class)
        SettingsSection(
            "Idioma de Whisper local",
            "Selecciona el idioma de transcripción para el motor offline de Whisper. Por defecto, 'Automático' detecta el idioma del sistema operativo.",
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                WhisperLanguage.entries.forEach { lang ->
                    FilterChip(
                        selected = preferences.whisperLanguage == lang,
                        onClick = { onPreferencesChange(preferences.copy(whisperLanguage = lang)) },
                        label = { Text(lang.label) },
                    )
                }
            }
        }
        SettingsSection("Funciones Experimentales", "Características avanzadas en fase de prueba.") {
            SettingSwitch(
                title = "Whisper + Refinamiento por LLM (Experimental)",
                supporting = "Utiliza Whisper local para transcribir audio a texto y luego procesa el resultado con un modelo LLM de texto para corregir formato y sintaxis.",
                checked = preferences.whisperLlmRefinementExperimental,
                onCheckedChange = { onPreferencesChange(preferences.copy(whisperLlmRefinementExperimental = it)) },
            )
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
                    Text("LyraFlow v${AppVersion.CURRENT}", style = MaterialTheme.typography.titleMedium)
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
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            RadioButton(selected = selected, onClick = onSelect)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(provider.label, style = MaterialTheme.typography.titleMedium)
                Text(provider.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ModelField(value: String, enabled: Boolean = true, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        enabled = enabled,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp),
        label = { Text("Identificador del modelo") },
        placeholder = { Text("Ejemplo: gpt-5.6-sol") },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
    )
}

@Composable
private fun EndpointField(value: String, enabled: Boolean = true, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        enabled = enabled,
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
private fun SystemPromptField(
    currentMode: SystemPromptMode,
    value: String,
    hasApiKey: Boolean,
    onModeSelect: (SystemPromptMode) -> Unit,
    onValueChange: (String) -> Unit,
    onReset: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        ) {
            Row(
                modifier = Modifier.padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SystemPromptMode.entries.forEach { mode ->
                    val selected = currentMode == mode
                    val enabled = hasApiKey || mode == SystemPromptMode.Literal
                    Surface(
                        onClick = { if (enabled) onModeSelect(mode) },
                        shape = MaterialTheme.shapes.small,
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) else null,
                        modifier = Modifier.then(
                            if (!enabled) Modifier.padding(0.dp) else Modifier
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            val icon = when (mode) {
                                SystemPromptMode.Smart -> if (hasApiKey) "🧠" else "🔒"
                                SystemPromptMode.Literal -> "🎙️"
                                SystemPromptMode.Custom -> if (hasApiKey) "✏️" else "🔒"
                            }
                            Text(icon, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.titleMedium,
                                color = when {
                                    selected -> MaterialTheme.colorScheme.onPrimaryContainer
                                    !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }
            }
        }
        Text(
            text = if (!hasApiKey) {
                "ℹ️ Sin API Key configurada: Se utiliza 'Voz original' (transcripción literal offline). Para refinamiento con IA ('Inteligente' o 'Personalizado'), agrega tu API Key de Gemini u OpenAI arriba."
            } else {
                currentMode.description
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (!hasApiKey) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (hasApiKey) {
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
                    Text("Restablecer modo Inteligente predeterminado")
                }
            }
        }
    }
}
