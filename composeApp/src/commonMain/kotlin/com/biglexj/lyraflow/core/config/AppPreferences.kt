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
) {
    val providerConfiguration: ProviderConfiguration
        get() = ProviderConfiguration(
            provider = provider,
            model = model.trim().ifBlank { provider.defaultModel },
            endpoint = endpoint.trim().ifBlank { provider.defaultEndpoint },
        )

    companion object {
        const val DEFAULT_SYSTEM_PROMPT =
            "Convierte este dictado en texto final, claro y bien escrito, conservando fielmente su intención. " +
                "Preserva de forma íntegra el idioma original expresado por el usuario (incluyendo español, inglés, chino, japonés o cualquier idioma y caracteres CJK o símbolos), sin traducir ni omitir caracteres o uniones. " +
                "Filtra y omite únicamente insultos o palabras soeces dirigidas a modo de agresión directa. " +
                "Mantén con naturalidad las expresiones cotidianas de frustración o modismos (como 'carajo', 'diablos', 'no puede ser', '¿qué pasó acá?'). " +
                "Corrige puntuación, concordancia, repeticiones involuntarias y falsos comienzos. " +
                "Ordena las ideas y crea párrafos o listas cuando el hablante enumere elementos. " +
                "Interpreta órdenes de formato habladas según el contexto, por ejemplo 'punto', 'coma', " +
                "'nueva línea' y 'punto por punto', sin escribir literalmente esas órdenes. " +
                "No resumas, no inventes información y no cambies nombres, cifras, rutas ni fragmentos de código. " +
                "Devuelve únicamente el texto final, sin comentarios, comillas ni bloques Markdown. " +
                "Si no hay voz clara, devuelve una cadena vacía."

        const val LITERAL_SYSTEM_PROMPT =
            "Transcribe el audio tal cual dice el usuario, de forma literal, palabra por palabra, sin refinar, corregir, estructurar, resumir ni omitir nada. " +
                "Conserva fielmente todas las palabras, modismos e ideas expresadas exactamente como se pronunciaron. " +
                "Devuelve únicamente el texto transcrito de forma directa, sin comentarios, aclaraciones, comillas ni bloques Markdown. " +
                "Si no hay voz clara, devuelve una cadena vacía."
    }
}

data class AppConfiguration(
    val preferences: AppPreferences = AppPreferences(),
    val sessionApiKey: String = "",
)
