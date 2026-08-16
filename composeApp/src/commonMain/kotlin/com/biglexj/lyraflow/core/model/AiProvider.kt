package com.biglexj.lyraflow.core.model

/**
 * Protocolos compatibles con la capa de transcripción en la nube.
 *
 * La opción OpenAI-compatible mantiene editables el endpoint y el modelo para
 * conectar gateways, servidores locales y proveedores con el mismo contrato.
 */
enum class AiProvider(
    val label: String,
    val description: String,
    val defaultModel: String,
    val defaultEndpoint: String,
    val apiKeyEnvironmentVariable: String,
    val suggestedModels: List<String>,
) {
    Gemini(
        label = "Gemini",
        description = "API nativa de Google con entrada de audio multimodal.",
        defaultModel = "gemini-3.5-flash",
        defaultEndpoint = "https://generativelanguage.googleapis.com/v1beta",
        apiKeyEnvironmentVariable = "GEMINI_API_KEY",
        suggestedModels = listOf(
            "gemini-3.5-flash",
            "gemini-2.5-flash",
            "gemini-2.5-pro",
        ),
    ),
    OpenAiCompatible(
        label = "OpenAI / compatible",
        description = "OpenAI, gateways, Qwen, modelos locales y otros endpoints compatibles.",
        defaultModel = "gpt-5.6-luna",
        defaultEndpoint = "https://api.openai.com/v1/chat/completions",
        apiKeyEnvironmentVariable = "OPENAI_API_KEY",
        suggestedModels = listOf(
            "gpt-5.6-luna",
            "gpt-5.6-terra",
            "gpt-5.6-sol",
            "gpt-audio-5.6",
        ),
    ),
}

data class ProviderConfiguration(
    val provider: AiProvider,
    val model: String,
    val endpoint: String,
)
