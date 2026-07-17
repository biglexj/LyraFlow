package com.biglexj.lyraflow.platform.settings

import com.biglexj.lyraflow.core.config.AppPreferences
import com.biglexj.lyraflow.core.config.PreferencesStore
import com.biglexj.lyraflow.core.config.ThemeMode
import com.biglexj.lyraflow.core.model.GeminiModel
import com.biglexj.lyraflow.core.hotkey.KeyboardShortcut
import com.biglexj.lyraflow.core.hotkey.ShortcutKey
import com.biglexj.lyraflow.core.hotkey.ShortcutModifier
import java.util.prefs.Preferences

class DesktopPreferencesStore : PreferencesStore {
    private val node = Preferences.userRoot().node("com/biglexj/lyraflow")

    override fun load(): AppPreferences = AppPreferences(
        themeMode = enumValue(node.get(THEME, ThemeMode.System.name), ThemeMode.System),
        model = enumValue(node.get(MODEL, GeminiModel.Fast.name), GeminiModel.Fast),
        autoInject = node.getBoolean(AUTO_INJECT, true),
        shortcut = loadShortcut(),
    )

    override fun save(preferences: AppPreferences) {
        node.put(THEME, preferences.themeMode.name)
        node.put(MODEL, preferences.model.name)
        node.putBoolean(AUTO_INJECT, preferences.autoInject)
        node.put(HOTKEY_MODIFIERS, preferences.shortcut.modifiers.joinToString(",") { it.name })
        node.put(HOTKEY_KEY, preferences.shortcut.key.name)
    }

    private fun loadShortcut(): KeyboardShortcut {
        val defaultModifiers = KeyboardShortcut.Default.modifiers.joinToString(",") { it.name }
        val modifiers = node.get(HOTKEY_MODIFIERS, defaultModifiers)
            .split(',')
            .mapNotNull { value -> ShortcutModifier.entries.firstOrNull { it.name == value } }
            .toSet()
        val defaultKey = KeyboardShortcut.Default.key
        val key = enumValue(node.get(HOTKEY_KEY, defaultKey.name), defaultKey)
        return KeyboardShortcut(modifiers, key).takeIf { it.validationError() == null }
            ?: KeyboardShortcut.Default
    }

    private inline fun <reified T : Enum<T>> enumValue(value: String, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: fallback

    private companion object {
        const val THEME = "theme"
        const val MODEL = "geminiModel"
        const val AUTO_INJECT = "autoInject"
        const val HOTKEY_MODIFIERS = "hotkeyModifiers"
        const val HOTKEY_KEY = "hotkeyKey"
    }
}
