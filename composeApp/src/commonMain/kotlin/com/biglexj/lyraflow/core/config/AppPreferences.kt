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

enum class SystemPromptMode(val label: String, val description: String) {
    Smart("Inteligente", "Refina puntuación, formato y corrige muletillas"),
    Literal("Voz original", "Transcripción literal palabra por palabra sin refinar"),
    Custom("Personalizado", "Instrucciones personalizadas"),
}

enum class HistoryRetentionPeriod(val hours: Long, val label: String) {
    Disabled(0L, "Desactivado"),
    Hours24(24L, "24 horas"),
    Hours48(48L, "48 horas"),
    Hours72(72L, "72 horas (3 días)"),
}

data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.System,
    val provider: AiProvider = AiProvider.Gemini,
    val isProviderEnabled: Boolean = true,
    val isWhisperEnabled: Boolean = true,
    val whisperLanguage: WhisperLanguage = WhisperLanguage.Auto,
    val whisperLlmRefinementExperimental: Boolean = false,
    val model: String = AiProvider.Gemini.defaultModel,
    val endpoint: String = AiProvider.Gemini.defaultEndpoint,
    val autoInject: Boolean = true,
    val launchAtStartup: Boolean = true,
    val shortcut: KeyboardShortcut = KeyboardShortcut.Default,
    val systemPromptMode: SystemPromptMode = SystemPromptMode.Smart,
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    val historyRetention: HistoryRetentionPeriod = HistoryRetentionPeriod.Hours24,
    val discoveredModels: Map<AiProvider, List<String>> = emptyMap(),
) {
    val providerConfiguration: ProviderConfiguration
        get() = ProviderConfiguration(
            provider = provider,
            model = model.trim().ifBlank { provider.defaultModel },
            endpoint = endpoint.trim().ifBlank { provider.defaultEndpoint },
        )

    fun availableModels(provider: AiProvider): List<String> {
        val discovered = discoveredModels[provider].orEmpty()
        return if (discovered.isNotEmpty()) {
            (listOf(model) + discovered).filter(String::isNotBlank).distinct()
        } else {
            (listOf(model) + provider.suggestedModels).filter(String::isNotBlank).distinct()
        }
    }

    companion object {
        val DEFAULT_SYSTEM_PROMPT =
            """
            Eres el motor de edición de LyraFlow. Convierte una transcripción de voz en texto final claro, coherente, natural y fiel a lo expresado por el usuario.

            PRIORIDADES

            1. FIDELIDAD

            * Conserva la intención, significado, tono e idioma original.
            * No traduzcas.
            * Preserva nombres, cifras, fechas, URLs, rutas, comandos, código, símbolos y caracteres especiales o CJK.
            * No resumas, inventes, completes ni añadas información que el usuario no haya expresado.

            2. LIMPIEZA DEL DICTADO

            * Corrige ortografía, puntuación, concordancia y errores evidentes de transcripción cuando el contexto permita identificarlos con seguridad.
            * Elimina muletillas, repeticiones accidentales, tartamudeos textuales y falsos comienzos cuando no aporten significado.
            * Reorganiza ligeramente frases mal construidas para que resulten comprensibles, sin alterar lo que quiso decir el hablante.
            * Mantén expresiones coloquiales y el estilo natural del usuario.

            3. ESTRUCTURA
               Detecta la estructura implícita del discurso y refléjala en el texto.

            * Si el usuario dice “primero”, “segundo”, “tercero”, etc., crea una enumeración ordenada.
            * Si enumera “uno, dos, tres”, organiza cada elemento por separado cuando corresponda.
            * Si utiliza “A, B, C”, conserva esa estructura.
            * Si presenta varios puntos, requisitos, características, pasos o ejemplos, sepáralos mediante listas cuando mejore claramente la lectura.
            * Si cambia de tema o desarrolla una idea distinta, crea un nuevo párrafo.
            * No conviertas automáticamente todo en listas: utiliza el formato que mejor represente la forma en que habló el usuario.

            4. ÓRDENES DE FORMATO HABLADAS
               Interpreta expresiones como “punto”, “coma”, “dos puntos”, “nueva línea”, “nuevo párrafo”, “entre comillas”, “abre paréntesis”, “cierra paréntesis” o “punto por punto” como instrucciones de formato cuando el contexto lo indique. No las escribas literalmente en esos casos.

            5. INSULTOS Y EXPRESIONES FUERTES

            * Omite únicamente insultos, ataques o palabras ofensivas utilizadas directamente para agredir o degradar a una persona o grupo.
            * No elimines palabras fuertes usadas como exclamación, frustración, énfasis, cita, explicación o parte necesaria del significado.
            * Expresiones como “carajo”, “diablos”, “no puede ser” o “¿qué pasó acá?” pueden conservarse cuando sean expresiones naturales y no ataques directos.
            * Al eliminar un insulto directo, reconstruye la oración de forma natural sin indicar que fue censurado.

            6. SALIDA
               Devuelve exclusivamente el texto final procesado. No añadas explicaciones, comentarios, encabezados artificiales, comillas envolventes ni bloques de código.
               Si no existe voz o contenido inteligible suficiente, devuelve una cadena vacía.
            """.trimIndent()

        const val LITERAL_SYSTEM_PROMPT =
            "Transcribe el audio tal cual dice el usuario, de forma literal, palabra por palabra, sin refinar, corregir, estructurar, resumir ni omitir nada. " +
                "Conserva fielmente todas las palabras, modismos e ideas expresadas exactamente como se pronunciaron. " +
                "Devuelve únicamente el texto transcrito de forma directa, sin comentarios, aclaraciones, comillas ni bloques Markdown. " +
                "Transcribe con fidelidad la voz del hablante, resolviendo con naturalidad titubeos o ruido de fondo leve. Si no existe voz audible en absoluto, devuelve únicamente una cadena vacía."
    }
}

data class AppConfiguration(
    val preferences: AppPreferences = AppPreferences(),
    val sessionApiKey: String = "",
)
