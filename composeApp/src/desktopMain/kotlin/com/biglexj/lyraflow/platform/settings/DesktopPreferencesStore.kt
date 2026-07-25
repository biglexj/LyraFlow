package com.biglexj.lyraflow.platform.settings

import com.biglexj.lyraflow.core.config.AppPreferences
import com.biglexj.lyraflow.core.config.PreferencesStore
import com.biglexj.lyraflow.core.config.ThemeMode
import com.biglexj.lyraflow.core.model.AiProvider
import com.biglexj.lyraflow.core.hotkey.KeyboardShortcut
import com.biglexj.lyraflow.core.hotkey.ShortcutKey
import com.biglexj.lyraflow.core.hotkey.ShortcutModifier
import java.util.prefs.Preferences

class DesktopPreferencesStore(
    private val node: Preferences = Preferences.userRoot().node("com/biglexj/lyraflow"),
) : PreferencesStore {

    override fun load(): AppPreferences = AppPreferences(
        themeMode = enumValue(node.get(THEME, ThemeMode.System.name), ThemeMode.System),
        provider = enumValue(node.get(PROVIDER, AiProvider.Gemini.name), AiProvider.Gemini),
        model = loadModel(),
        endpoint = node.get(ENDPOINT, currentProvider().defaultEndpoint).ifBlank { currentProvider().defaultEndpoint },
        autoInject = node.getBoolean(AUTO_INJECT, true),
        launchAtStartup = node.getBoolean(LAUNCH_AT_STARTUP, true),
        shortcut = loadShortcut(),
    )

    override fun save(preferences: AppPreferences) {
        node.put(THEME, preferences.themeMode.name)
        node.put(PROVIDER, preferences.provider.name)
        node.put(MODEL, preferences.providerConfiguration.model)
        node.put(ENDPOINT, preferences.providerConfiguration.endpoint)
        node.putBoolean(AUTO_INJECT, preferences.autoInject)
        node.putBoolean(LAUNCH_AT_STARTUP, preferences.launchAtStartup)
        node.put(HOTKEY_MODIFIERS, preferences.shortcut.modifiers.joinToString(",") { it.name })
        node.put(HOTKEY_KEY, preferences.shortcut.key.name)
        node.flush()
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

    private fun currentProvider(): AiProvider =
        enumValue(node.get(PROVIDER, AiProvider.Gemini.name), AiProvider.Gemini)

    private fun loadModel(): String {
        val provider = currentProvider()
        val storedModel = node.get(MODEL, node.get(LEGACY_MODEL, "")).trim()
        return when (storedModel) {
            "Fast" -> if (provider == AiProvider.Gemini) "gemini-3.5-flash-lite" else provider.defaultModel
            "Smart" -> if (provider == AiProvider.Gemini) "gemini-3.6-flash" else provider.defaultModel
            "" -> provider.defaultModel
            else -> storedModel
        }
    }

    private companion object {
        const val THEME = "theme"
        const val PROVIDER = "aiProvider"
        const val MODEL = "aiModel"
        const val LEGACY_MODEL = "geminiModel"
        const val ENDPOINT = "aiEndpoint"
        const val AUTO_INJECT = "autoInject"
        const val LAUNCH_AT_STARTUP = "launchAtStartup"
        const val HOTKEY_MODIFIERS = "hotkeyModifiers"
        const val HOTKEY_KEY = "hotkeyKey"
    }
}
