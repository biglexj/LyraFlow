package com.biglexj.lyraflow.platform.settings

import com.biglexj.lyraflow.core.config.SystemPromptMode
import com.biglexj.lyraflow.core.model.AiProvider
import java.util.UUID
import java.util.prefs.Preferences
import kotlin.test.assertNotNull
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopPreferencesStoreTest {
    @Test
    fun verifiesIconResourceExists() {
        val resource = this::class.java.classLoader.getResource("Square44x44Logo.png")
        assertNotNull(resource, "Square44x44Logo.png should be available on classpath")
    }
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
    fun migratesObsoleteOpenAiModelNamesToCurrentDefault() {
        val node = Preferences.userRoot().node("com/biglexj/lyraflow/test/${UUID.randomUUID()}")
        try {
            node.put("aiProvider", AiProvider.OpenAiCompatible.name)
            node.put("aiModel", "gpt-audio-1.5")

            val preferences = DesktopPreferencesStore(node).load()

            assertEquals(AiProvider.OpenAiCompatible, preferences.provider)
            assertEquals("gpt-5.6-luna", preferences.model)
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

    @Test
    fun persistsDiscoveredModelsPerProvider() {
        val node = Preferences.userRoot().node("com/biglexj/lyraflow/test/${UUID.randomUUID()}")
        try {
            val original = com.biglexj.lyraflow.core.config.AppPreferences(
                discoveredModels = mapOf(
                    AiProvider.Gemini to listOf("gemini-3.7-flash", "gemini-3.5-flash"),
                    AiProvider.OpenAiCompatible to listOf("gpt-4o", "gpt-4o-mini"),
                ),
            )
            DesktopPreferencesStore(node).save(original)

            val restored = DesktopPreferencesStore(node).load()

            assertEquals(
                listOf("gemini-3.7-flash", "gemini-3.5-flash"),
                restored.discoveredModels[AiProvider.Gemini],
            )
            assertEquals(
                listOf("gpt-4o", "gpt-4o-mini"),
                restored.discoveredModels[AiProvider.OpenAiCompatible],
            )
        } finally {
            node.removeNode()
        }
    }
}
