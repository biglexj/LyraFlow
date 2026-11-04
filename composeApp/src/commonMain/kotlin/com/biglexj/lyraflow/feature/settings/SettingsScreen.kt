package com.biglexj.lyraflow.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import com.biglexj.lyraflow.core.config.AppConfiguration
import com.biglexj.lyraflow.core.config.AppPreferences
import com.biglexj.lyraflow.core.config.HistoryRetentionPeriod
import com.biglexj.lyraflow.core.config.SystemPromptMode
import com.biglexj.lyraflow.core.config.ThemeMode
import com.biglexj.lyraflow.core.config.WhisperLanguage
import com.biglexj.lyraflow.core.model.AiProvider

internal val LocalExpandedSettings = staticCompositionLocalOf { false }

private val SETTINGS_TABS = listOf("General", "Modelos")

private fun expandedSettingsTypography(base: Typography): Typography {
    fun enlarged(style: TextStyle, weight: FontWeight? = null) = style.copy(
        fontSize = style.fontSize * 1.15f,
        lineHeight = style.lineHeight * 1.15f,
        fontWeight = weight ?: style.fontWeight,
    )
    return base.copy(
        headlineMedium = enlarged(base.headlineMedium, FontWeight.SemiBold),
        titleMedium = enlarged(base.titleMedium, FontWeight.SemiBold),
        titleSmall = enlarged(base.titleSmall, FontWeight.SemiBold),
        bodyLarge = enlarged(base.bodyLarge),
        bodyMedium = enlarged(base.bodyMedium),
        bodySmall = enlarged(base.bodySmall),
        labelLarge = enlarged(base.labelLarge, FontWeight.SemiBold),
        labelMedium = enlarged(base.labelMedium, FontWeight.Medium),
        labelSmall = enlarged(base.labelSmall, FontWeight.Medium),
    )
}

@Composable
private fun SettingsChoiceChip(
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    label: @Composable () -> Unit,
) {
    val expanded = LocalExpandedSettings.current
    FilterChip(
        selected = selected, onClick = onClick, enabled = enabled, label = label,
        modifier = Modifier.heightIn(min = if (expanded) 44.dp else 32.dp),
        border = FilterChipDefaults.filterChipBorder(
            enabled = enabled, selected = selected,
            borderWidth = if (expanded) 1.5.dp else 1.dp,
            selectedBorderWidth = if (expanded) 1.5.dp else 0.dp,
            selectedBorderColor = if (expanded) MaterialTheme.colorScheme.primary.copy(alpha = .45f)
                else androidx.compose.ui.graphics.Color.Transparent,
        ),
    )
}

@Composable
private fun SettingsTextButton(onClick: () -> Unit, enabled: Boolean = true, content: @Composable () -> Unit) {
    TextButton(
        onClick = onClick, enabled = enabled,
        modifier = Modifier.heightIn(min = if (LocalExpandedSettings.current) 48.dp else 40.dp),
    ) { content() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    configuration: AppConfiguration,
    onPreferencesChange: (AppPreferences) -> Unit,
    onApiKeyChange: (String) -> Unit,
    isScanningModels: Boolean = false,
    onScanModels: () -> Unit = {},
) {
    val preferences = configuration.preferences
    val provider = preferences.provider
    val hasApiKey = configuration.sessionApiKey.isNotBlank()

    val availableModels = preferences.availableModels(provider)
    val discoveredForProvider = preferences.discoveredModels[provider].orEmpty()
    var selectedModelInput by remember(preferences.model) { mutableStateOf(preferences.model) }
    var showDiscoveredList by remember { mutableStateOf(false) }
    var saveFeedback by remember { mutableStateOf<String?>(null) }
    var selectedTab by rememberSaveable { mutableStateOf(0) }

    LaunchedEffect(saveFeedback) {
        if (saveFeedback != null) {
            kotlinx.coroutines.delay(3000)
            saveFeedback = null
        }
    }

    var showExperimental by rememberSaveable { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    LaunchedEffect(selectedTab) { scrollState.scrollTo(0) }

    val connection: @Composable () -> Unit = {
        SettingsSection("Conexión", "Proveedor y credenciales de acceso.") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AiProvider.entries.forEach { option ->
                    SettingsChoiceChip(
                        selected = provider == option,
                        onClick = {
                            if (provider != option) onPreferencesChange(preferences.copy(
                                    provider = option, model = option.defaultModel, endpoint = option.defaultEndpoint,
                            ))
                        },
                        label = { Text(option.label) },
                    )
                }
            }
            ApiKeyField(
                value = configuration.sessionApiKey,
                label = "Clave API de " + provider.label,
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
            Text("La clave se guarda cifrada para tu usuario de Windows.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (provider == AiProvider.OpenAiCompatible || provider == AiProvider.MiniMax) {
                EndpointField(value = preferences.endpoint, enabled = hasApiKey,
                    onValueChange = { onPreferencesChange(preferences.copy(endpoint = it)) })
                Text(
                    if (provider == AiProvider.MiniMax) "URL base del servicio de MiniMax."
                    else "El endpoint y el modelo deben admitir audio mediante Chat Completions.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        SettingsSection("Modelo", "Selecciona el modelo del proveedor conectado.") {
            ModelField(
                value = selectedModelInput,
                enabled = hasApiKey,
                onValueChange = { selectedModelInput = it },
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = {
                        val target = selectedModelInput.trim().ifBlank { provider.defaultModel }
                        selectedModelInput = target
                        onPreferencesChange(preferences.copy(model = target))
                        showDiscoveredList = false
                        saveFeedback = "Modelo guardado: $target"
                    },
                    modifier = Modifier.heightIn(min = if (LocalExpandedSettings.current) 48.dp else 40.dp),
                    enabled = hasApiKey && selectedModelInput.isNotBlank() && selectedModelInput.trim() != preferences.model,
                ) { Text("Guardar modelo") }
                if (discoveredForProvider.isNotEmpty() || availableModels.isNotEmpty()) {
                    SettingsTextButton(onClick = { showDiscoveredList = !showDiscoveredList }, enabled = hasApiKey) {
                        Text(if (showDiscoveredList) "Ocultar modelos" else "Ver modelos")
                    }
                }
                TextButton(onClick = { showDiscoveredList = true; onScanModels() }, enabled = hasApiKey && !isScanningModels) {
                    if (isScanningModels) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(if (isScanningModels) "Escaneando…" else "Escanear")
                }
            }

            if (saveFeedback != null) {
                Text(
                    text = "✅ " + saveFeedback,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            androidx.compose.animation.AnimatedVisibility(visible = showDiscoveredList && hasApiKey) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (discoveredForProvider.isNotEmpty()) {
                            "Modelos detectados (" + discoveredForProvider.size + ") — Pulsa uno para seleccionarlo y luego 'Guardar modelo':"
                        } else {
                            "Modelos sugeridos base — Pulsa uno para seleccionarlo y luego 'Guardar modelo':"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        availableModels.forEach { model ->
                            FilterChip(
                                selected = selectedModelInput == model,
                                enabled = hasApiKey,
                                onClick = {
                                    selectedModelInput = model
                                },
                                label = { Text(model) },
                            )
                        }
                    }
                }
            }

            if (!hasApiKey) {
                Text(
                    text = "Añade tu clave en Conexión para elegir o escanear modelos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
    val transcription: @Composable () -> Unit = {
        SettingsSection("Transcripción", "Modo de dictado, instrucciones y motor local.") {
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
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text("Idioma de Whisper local", style = MaterialTheme.typography.titleSmall)
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
            TextButton(onClick = { showExperimental = !showExperimental }) {
                Text(if (showExperimental) "Ocultar opciones experimentales" else "Opciones experimentales")
            }
            androidx.compose.animation.AnimatedVisibility(visible = showExperimental) {
                SettingSwitch(
                    title = "Whisper + Refinamiento por LLM (Experimental)",
                    supporting = "Utiliza Whisper local para transcribir audio a texto y luego procesa el resultado con un modelo LLM de texto para corregir formato y sintaxis.",
                    checked = preferences.whisperLlmRefinementExperimental,
                    onCheckedChange = { onPreferencesChange(preferences.copy(whisperLlmRefinementExperimental = it)) },
                )
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // Se mide el contenido, no la pantalla: dos columnas también en una ventana normal.
        val twoColumns = maxWidth >= 720.dp
        val expanded = maxWidth >= 1200.dp && maxHeight >= 700.dp
        val horizontalPadding = if (maxWidth < 480.dp) 16.dp else if (expanded) 28.dp else 20.dp
        val typography = MaterialTheme.typography
        val settingsTypography = remember(typography, expanded) {
            if (expanded) expandedSettingsTypography(typography) else typography
        }
        MaterialTheme(typography = settingsTypography) {
            CompositionLocalProvider(LocalExpandedSettings provides expanded) {
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Column(modifier = Modifier.fillMaxWidth()
                        .padding(start = horizontalPadding, end = horizontalPadding, top = if (expanded) 28.dp else 20.dp)) {
                        Text("Ajustes", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(if (expanded) 18.dp else 12.dp))
                        PrimaryTabRow(selectedTabIndex = selectedTab, modifier = Modifier.widthIn(max = if (expanded) 300.dp else 240.dp)) {
                            SETTINGS_TABS.forEachIndexed { index, title ->
                                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
                            }
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(start = horizontalPadding, end = horizontalPadding, top = 16.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(if (expanded) 20.dp else 14.dp),
                    ) {
                        if (selectedTab == 0) {
                            SettingsSection("Control del dictado") {
                                SettingsColumns(twoColumns, first = {
                                    Text("Iniciar y detener", style = MaterialTheme.typography.titleSmall)
                                    ShortcutRecorder(
                                        shortcut = preferences.shortcut,
                                        onShortcutChange = { shortcut ->
                                            if (!preferences.cancelShortcut.conflictsWith(shortcut)) {
                                                onPreferencesChange(preferences.copy(shortcut = shortcut))
                                            }
                                        },
                                        validate = { shortcut ->
                                            shortcut.validationError()
                                            ?: if (preferences.cancelShortcut.conflictsWith(shortcut)) {
                                                "Esa combinacion ya la usa el atajo para cancelar."
                                            } else {
                                                null
                                            }
                                        },
                                    )
                                    }, second = {
                                    Text("Cancelar dictado", style = MaterialTheme.typography.titleSmall)
                                    ShortcutRecorder(
                                        shortcut = preferences.cancelShortcut,
                                        onShortcutChange = { shortcut ->
                                            if (!preferences.shortcut.conflictsWith(shortcut)) {
                                                onPreferencesChange(preferences.copy(cancelShortcut = shortcut))
                                            }
                                        },
                                        validate = { shortcut ->
                                            shortcut.validationError()
                                            ?: if (preferences.shortcut.conflictsWith(shortcut)) {
                                                "Esa combinacion ya la usa el atajo principal."
                                            } else {
                                                null
                                            }
                                        },
                                    )
                                    if (preferences.cancelShortcut.conflictsWith(preferences.shortcut)) {
                                        Text(
                                            text = "⚠️ El atajo de cancelar coincide con el atajo principal. Cambia uno de los dos para que LyraFlow pueda distinguirlos.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                })
                                Text("Combinaciones de 2 o 3 teclas. F12 y Windows + Espacio están reservados.",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            SettingsSection("Comportamiento") {
                                SettingsColumns(twoColumns, first = {
                                    SettingSwitch("Insertar automáticamente", "Pega el resultado en la aplicación activa.", preferences.autoInject,
                                        { onPreferencesChange(preferences.copy(autoInject = it)) })
                                    }, second = {
                                    SettingSwitch("Iniciar con Windows", "Se abre en el área de notificación.", preferences.launchAtStartup,
                                        { onPreferencesChange(preferences.copy(launchAtStartup = it)) })
                                })
                            }
                            SettingsColumns(twoColumns, first = {
                                SettingsSection("Apariencia") {
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        ThemeMode.entries.forEach { mode ->
                                            FilterChip(selected = preferences.themeMode == mode,
                                                onClick = { onPreferencesChange(preferences.copy(themeMode = mode)) },
                                                label = { Text(mode.label) })
                                        }
                                    }
                                }
                                }, second = {
                                SettingsSection("Historial", "Se conserva al cerrar la app. Desactivarlo borra los dictados guardados.") {
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        HistoryRetentionPeriod.entries.forEach { period ->
                                            FilterChip(selected = preferences.historyRetention == period,
                                                onClick = { onPreferencesChange(preferences.copy(historyRetention = period)) },
                                                label = { Text(if (period == HistoryRetentionPeriod.Disabled) period.label else "${period.hours} horas") })
                                        }
                                    }
                                }
                            })

                        } else {
                            SettingsColumns(twoColumns, connection, transcription)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, supporting: String = "", content: @Composable () -> Unit) {
    val expanded = LocalExpandedSettings.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        border = if (expanded) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f)) else null,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .34f),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(Modifier.padding(if (expanded) 24.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(if (expanded) 14.dp else 10.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (supporting.isNotBlank()) {
                    Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            content()
        }
    }
}

@Composable
private fun SettingsColumns(twoColumns: Boolean, first: @Composable () -> Unit, second: @Composable () -> Unit) {
    if (twoColumns) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (LocalExpandedSettings.current) 28.dp else 20.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) { first() }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) { second() }
        }
    } else {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            first()
            second()
        }
    }
}

@Composable
private fun ModelField(value: String, enabled: Boolean = true, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        enabled = enabled,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
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
        modifier = Modifier.fillMaxWidth(),
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
        modifier = Modifier.fillMaxWidth(),
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
        modifier = Modifier.fillMaxWidth().toggleable(
            value = checked, role = Role.Switch, onValueChange = onCheckedChange,
            interactionSource = remember { MutableInteractionSource() }, indication = null,
        )
        .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SystemPromptField(
    currentMode: SystemPromptMode,
    value: String,
    hasApiKey: Boolean,
    onModeSelect: (SystemPromptMode) -> Unit,
    onValueChange: (String) -> Unit,
    onReset: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Modo de transcripción", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SystemPromptMode.entries.forEach { mode ->
                FilterChip(
                    selected = currentMode == mode,
                    enabled = hasApiKey || mode == SystemPromptMode.Literal,
                    onClick = { onModeSelect(mode) },
                    label = { Text(mode.label) },
                )
            }
        }
        Text(
            text = if (hasApiKey) currentMode.description else "Añade una clave API en Conexión para activar el refinamiento con IA.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        androidx.compose.animation.AnimatedVisibility(visible = hasApiKey && currentMode == SystemPromptMode.Custom) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 6,
                    label = { Text("Instrucciones personalizadas") },
                    shape = MaterialTheme.shapes.medium,
                )
                TextButton(onClick = onReset) { Text("Restablecer modo inteligente") }
            }
        }
    }
}
