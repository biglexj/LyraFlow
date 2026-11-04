package com.biglexj.lyraflow.platform.settings

import com.biglexj.lyraflow.core.config.AppPreferences
import com.biglexj.lyraflow.core.config.SystemPromptMode
import java.util.UUID
import java.util.prefs.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals

class SystemPromptPreferencesTest {
    private fun withStore(test: (Preferences, DesktopPreferencesStore) -> Unit) {
        val node = Preferences.userRoot().node("com/biglexj/lyraflow-tests/${UUID.randomUUID()}")
        try { test(node, DesktopPreferencesStore(node)) }
        finally { node.removeNode(); Preferences.userRoot().flush() }
    }

    @Test fun builtInModesUseCurrentInstructionsAfterRestart() = withStore { node, store ->
        node.put("systemPrompt", "Plantilla anterior almacenada")
        node.put("systemPromptMode", SystemPromptMode.Literal.name)
        assertEquals(AppPreferences.LITERAL_SYSTEM_PROMPT, store.load().systemPrompt)
        node.put("systemPromptMode", SystemPromptMode.Smart.name)
        assertEquals(AppPreferences.DEFAULT_SYSTEM_PROMPT, store.load().systemPrompt)
    }

    @Test fun preservesUserWrittenCustomPromptAndItsParagraphs() = withStore { _, store ->
        val custom = "Conserva mis expresiones.\n\nOrganiza el resultado en párrafos.\n"
        store.save(AppPreferences(systemPromptMode = SystemPromptMode.Custom, systemPrompt = custom))
        val loaded = store.load()
        assertEquals(SystemPromptMode.Custom, loaded.systemPromptMode)
        assertEquals(custom, loaded.systemPrompt)
    }

    @Test fun upgradesOnlyTheExactLegacyLiteralTemplateCopiedIntoCustom() = withStore { _, store ->
        val legacy = "Transcribe el audio tal cual dice el usuario, de forma literal, palabra por palabra, sin refinar, corregir, estructurar, resumir ni omitir nada. " +
            "Conserva fielmente todas las palabras, modismos e ideas expresadas exactamente como se pronunciaron. " +
            "Devuelve únicamente el texto transcrito de forma directa, sin comentarios, aclaraciones, comillas ni bloques Markdown. " +
            "Transcribe con fidelidad la voz del hablante, resolviendo con naturalidad titubeos o ruido de fondo leve. Si no existe voz audible en absoluto, devuelve únicamente una cadena vacía."
        store.save(AppPreferences(systemPromptMode = SystemPromptMode.Custom, systemPrompt = legacy))
        assertEquals(AppPreferences.LITERAL_SYSTEM_PROMPT, store.load().systemPrompt)
        val edited = "$legacy\nAñade mi instrucción propia."
        store.save(AppPreferences(systemPromptMode = SystemPromptMode.Custom, systemPrompt = edited))
        assertEquals(edited, store.load().systemPrompt)
    }
}
