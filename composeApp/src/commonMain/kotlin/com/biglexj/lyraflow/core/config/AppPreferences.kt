package com.biglexj.lyraflow.core.config

import com.biglexj.lyraflow.core.model.GeminiModel
import com.biglexj.lyraflow.core.hotkey.KeyboardShortcut

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
    val model: GeminiModel = GeminiModel.Fast,
    val autoInject: Boolean = true,
    val launchAtStartup: Boolean = true,
    val shortcut: KeyboardShortcut = KeyboardShortcut.Default,
)

data class AppConfiguration(
    val preferences: AppPreferences = AppPreferences(),
    val sessionApiKey: String = "",
    val apiKeyStorageMessage: String = "La clave solo vive durante esta sesión.",
)
