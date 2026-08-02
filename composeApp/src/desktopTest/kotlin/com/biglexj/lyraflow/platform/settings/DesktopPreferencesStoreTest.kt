package com.biglexj.lyraflow.platform.settings

import com.biglexj.lyraflow.core.config.SystemPromptMode
import com.biglexj.lyraflow.core.model.AiProvider
import java.util.UUID
import java.util.prefs.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopPreferencesStoreTest {
    @Test
    fun migratesTheLegacyGeminiModelNamesToTheCurrentCatalog() {
        val node = Preferences.userRoot().node("com/biglexj/lyraflow/test/${UUID.randomUUID()}")
        try {
            node.put("aiProvider", AiProvider.Gemini.name)
            node.put("geminiModel", "Fast")

            val preferences = DesktopPreferencesStore(node).load()

            assertEquals(AiProvider.Gemini, preferences.provider)
            assertEquals("gemini-3.5-flash-lite", preferences.model)
        } finally {
            node.removeNode()
        }
    }

    @Test
    fun persistsTheCompatibleEndpointAndModel() {
        val node = Preferences.userRoot().node("com/biglexj/lyraflow/test/${UUID.randomUUID()}")
        try {
            val original = com.biglexj.lyraflow.core.config.AppPreferences(
                provider = AiProvider.OpenAiCompatible,
                model = "qwen-audio",
                endpoint = "http://localhost:8000/v1/chat/completions",
            )
            DesktopPreferencesStore(node).save(original)

            val restored = DesktopPreferencesStore(node).load()

            assertEquals(AiProvider.OpenAiCompatible, restored.provider)
            assertEquals("qwen-audio", restored.model)
            assertEquals("http://localhost:8000/v1/chat/completions", restored.endpoint)
        } finally {
            node.removeNode()
        }
    }

    @Test
    fun persistsSystemPromptModeAndCustomPrompt() {
        val node = Preferences.userRoot().node("com/biglexj/lyraflow/test/${UUID.randomUUID()}")
        try {
            val original = com.biglexj.lyraflow.core.config.AppPreferences(
                systemPromptMode = SystemPromptMode.Literal,
                systemPrompt = com.biglexj.lyraflow.core.config.AppPreferences.LITERAL_SYSTEM_PROMPT,
            )
            DesktopPreferencesStore(node).save(original)

            val restored = DesktopPreferencesStore(node).load()

            assertEquals(SystemPromptMode.Literal, restored.systemPromptMode)
            assertEquals(com.biglexj.lyraflow.core.config.AppPreferences.LITERAL_SYSTEM_PROMPT, restored.systemPrompt)
        } finally {
            node.removeNode()
        }
    }
}
