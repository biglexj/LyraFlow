package com.biglexj.lyraflow.platform.settings

import com.biglexj.lyraflow.core.config.AppPreferences
import com.biglexj.lyraflow.core.config.PreferencesStore
import com.biglexj.lyraflow.core.config.ThemeMode
import com.biglexj.lyraflow.core.model.AiProvider
import com.biglexj.lyraflow.core.hotkey.KeyboardShortcut
import com.biglexj.lyraflow.core.hotkey.ShortcutKey
import com.biglexj.lyraflow.core.hotkey.ShortcutModifier
import java.util.prefs.Preferences

import com.biglexj.lyraflow.core.config.SystemPromptMode

data class WindowStatePreferences(
    val widthDp: Int = 1210,
    val heightDp: Int = 870,
    val isMaximized: Boolean = false,
)

class DesktopPreferencesStore(
    private val node: Preferences = Preferences.userRoot().node("com/biglexj/lyraflow"),
) : PreferencesStore {

    fun loadWindowState(): WindowStatePreferences = WindowStatePreferences(
        widthDp = node.getInt(WINDOW_WIDTH, 1210).coerceIn(600, 3840),
        heightDp = node.getInt(WINDOW_HEIGHT, 870).coerceIn(400, 2160),
        isMaximized = node.getBoolean(WINDOW_MAXIMIZED, false),
    )

    fun saveWindowState(widthDp: Int, heightDp: Int, isMaximized: Boolean) {
        node.putInt(WINDOW_WIDTH, widthDp)
        node.putInt(WINDOW_HEIGHT, heightDp)
        node.putBoolean(WINDOW_MAXIMIZED, isMaximized)
        node.flush()
    }

    override fun load(): AppPreferences {
        val provider = currentProvider()
        val promptMode = enumValue(node.get(SYSTEM_PROMPT_MODE, SystemPromptMode.Smart.name), SystemPromptMode.Smart)
        val fallbackPrompt = when (promptMode) {
            SystemPromptMode.Smart -> AppPreferences.DEFAULT_SYSTEM_PROMPT
            SystemPromptMode.Literal -> AppPreferences.LITERAL_SYSTEM_PROMPT
            SystemPromptMode.Custom -> AppPreferences.DEFAULT_SYSTEM_PROMPT
        }
        return AppPreferences(
            themeMode = enumValue(node.get(THEME, ThemeMode.System.name), ThemeMode.System),
            provider = provider,
            isProviderEnabled = node.getBoolean(PROVIDER_ENABLED, true),
            isWhisperEnabled = node.getBoolean(WHISPER_ENABLED, true),
            whisperLanguage = enumValue(node.get(WHISPER_LANGUAGE, com.biglexj.lyraflow.core.config.WhisperLanguage.Auto.name), com.biglexj.lyraflow.core.config.WhisperLanguage.Auto),
            whisperLlmRefinementExperimental = node.getBoolean(WHISPER_LLM_REFINEMENT_EXPERIMENTAL, false),
            model = loadModel(),
            endpoint = node.get(ENDPOINT, provider.defaultEndpoint).ifBlank { provider.defaultEndpoint },
            autoInject = node.getBoolean(AUTO_INJECT, true),
            launchAtStartup = node.getBoolean(LAUNCH_AT_STARTUP, true),
            shortcut = loadShortcut(),
            systemPromptMode = promptMode,
            systemPrompt = node.get(SYSTEM_PROMPT, fallbackPrompt),
            discoveredModels = loadDiscoveredModels(),
        )
    }

    override fun save(preferences: AppPreferences) {
        node.put(THEME, preferences.themeMode.name)
        node.put(PROVIDER, preferences.provider.name)
        node.putBoolean(PROVIDER_ENABLED, preferences.isProviderEnabled)
        node.putBoolean(WHISPER_ENABLED, preferences.isWhisperEnabled)
        node.put(WHISPER_LANGUAGE, preferences.whisperLanguage.name)
        node.putBoolean(WHISPER_LLM_REFINEMENT_EXPERIMENTAL, preferences.whisperLlmRefinementExperimental)
        node.put(MODEL, preferences.providerConfiguration.model)
        node.put(ENDPOINT, preferences.providerConfiguration.endpoint)
        node.putBoolean(AUTO_INJECT, preferences.autoInject)
        node.putBoolean(LAUNCH_AT_STARTUP, preferences.launchAtStartup)
        node.put(HOTKEY_MODIFIERS, preferences.shortcut.modifiers.joinToString(",") { it.name })
        node.put(HOTKEY_KEY, preferences.shortcut.key.name)
        node.put(SYSTEM_PROMPT_MODE, preferences.systemPromptMode.name)
        node.put(SYSTEM_PROMPT, preferences.systemPrompt)
        preferences.discoveredModels.forEach { (prov, models) ->
            node.put(discoveredModelsKey(prov), models.joinToString("\n"))
        }
        node.flush()
    }

    private fun loadDiscoveredModels(): Map<AiProvider, List<String>> {
        return AiProvider.entries.associateWith { prov ->
            node.get(discoveredModelsKey(prov), "")
                .split('\n')
                .map(String::trim)
                .filter(String::isNotEmpty)
        }.filterValues { it.isNotEmpty() }
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
            "gpt-audio-1.5", "gpt-4o-audio-preview" -> provider.defaultModel
            "" -> provider.defaultModel
            else -> storedModel
        }
    }

    private companion object {
        const val THEME = "theme"
        const val PROVIDER = "aiProvider"
        const val PROVIDER_ENABLED = "providerEnabled"
        const val WHISPER_ENABLED = "whisperEnabled"
        const val WHISPER_LANGUAGE = "whisperLanguage"
        const val WHISPER_LLM_REFINEMENT_EXPERIMENTAL = "whisperLlmRefinementExperimental"
        const val MODEL = "aiModel"
        const val LEGACY_MODEL = "geminiModel"
        const val ENDPOINT = "aiEndpoint"
        const val AUTO_INJECT = "autoInject"
        const val LAUNCH_AT_STARTUP = "launchAtStartup"
        const val HOTKEY_MODIFIERS = "hotkeyModifiers"
        const val HOTKEY_KEY = "hotkeyKey"
        const val SYSTEM_PROMPT_MODE = "systemPromptMode"
        const val SYSTEM_PROMPT = "systemPrompt"
        const val WINDOW_WIDTH = "windowWidth"
        const val WINDOW_HEIGHT = "windowHeight"
        const val WINDOW_MAXIMIZED = "windowMaximized"
        const val DISCOVERED_MODELS_PREFIX = "discoveredModels."

        fun discoveredModelsKey(provider: AiProvider): String = "$DISCOVERED_MODELS_PREFIX${provider.name}"
    }
}
