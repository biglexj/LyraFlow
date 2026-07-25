package com.biglexj.lyraflow.core.config

import com.biglexj.lyraflow.core.hotkey.KeyboardShortcut
import com.biglexj.lyraflow.core.model.AiProvider
import com.biglexj.lyraflow.core.model.ProviderConfiguration

enum class ThemeMode(val label: String) {
    System("Automático"),
    Light("Claro"),
    Dark("Oscuro"),
}

fun ThemeMode.next(): ThemeMode = when (this) {
    ThemeMode.System -> ThemeMode.Light
    ThemeMode.Light -> ThemeMode.Dark
    ThemeMode.Dark -> ThemeMode.System
}

data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.System,
    val provider: AiProvider = AiProvider.Gemini,
    val model: String = AiProvider.Gemini.defaultModel,
    val endpoint: String = AiProvider.Gemini.defaultEndpoint,
    val autoInject: Boolean = true,
    val launchAtStartup: Boolean = true,
    val shortcut: KeyboardShortcut = KeyboardShortcut.Default,
) {
    val providerConfiguration: ProviderConfiguration
        get() = ProviderConfiguration(
            provider = provider,
            model = model.trim().ifBlank { provider.defaultModel },
            endpoint = endpoint.trim().ifBlank { provider.defaultEndpoint },
        )
}

data class AppConfiguration(
    val preferences: AppPreferences = AppPreferences(),
    val sessionApiKey: String = "",
)
