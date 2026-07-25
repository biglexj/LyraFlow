package com.biglexj.lyraflow.platform.settings

import java.util.UUID
import java.util.prefs.Preferences
import com.biglexj.lyraflow.core.model.AiProvider
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopApiKeyStoreTest {
    @Test
    fun keySurvivesAStoreReloadAndCanBeCleared() {
        val node = Preferences.userRoot().node("com/biglexj/lyraflow/test/${UUID.randomUUID()}")
        val store = DesktopApiKeyStore(node, TestProtector)
        try {
            store.save("gemini-secret")
            assertEquals("gemini-secret", DesktopApiKeyStore(node, TestProtector).load())

            store.save("")
            assertEquals("", store.load())
        } finally {
            node.removeNode()
        }
    }

    @Test
    fun dpapiRoundTripUsesTheCurrentWindowsUser() {
        if (!System.getProperty("os.name").contains("windows", ignoreCase = true)) return
        val protector = DpapiApiKeyProtector()
        assertEquals("gemini-secret", protector.unprotect(protector.protect("gemini-secret")))
    }

    @Test
    fun keepsASeparateKeyForEachCloudProvider() {
        val node = Preferences.userRoot().node("com/biglexj/lyraflow/test/${UUID.randomUUID()}")
        val store = DesktopApiKeyStore(node, TestProtector)
        try {
            store.save(AiProvider.Gemini, "gemini-secret")
            store.save(AiProvider.OpenAiCompatible, "openai-secret")

            assertEquals("gemini-secret", store.load(AiProvider.Gemini))
            assertEquals("openai-secret", store.load(AiProvider.OpenAiCompatible))
        } finally {
            node.removeNode()
        }
    }

    @Test
    fun readsTheLegacyGeminiKeyAfterTheStorageMigration() {
        val node = Preferences.userRoot().node("com/biglexj/lyraflow/test/${UUID.randomUUID()}")
        try {
            node.put("geminiApiKey.protected", "protected:legacy-secret")

            assertEquals("legacy-secret", DesktopApiKeyStore(node, TestProtector).load(AiProvider.Gemini))
        } finally {
            node.removeNode()
        }
    }

    private object TestProtector : ApiKeyProtector {
        override fun protect(value: String) = "protected:$value"
        override fun unprotect(value: String) = value.removePrefix("protected:")
    }
}
