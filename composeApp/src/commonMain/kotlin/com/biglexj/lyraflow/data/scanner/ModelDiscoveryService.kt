package com.biglexj.lyraflow.data.scanner

import com.biglexj.lyraflow.core.model.AiProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess

class ModelDiscoveryService(
    private val client: HttpClient,
) {
    suspend fun discoverModels(
        provider: AiProvider,
        apiKey: String,
        endpoint: String = "",
    ): List<DiscoveredModel> {
        val key = apiKey.trim()
        if (key.isEmpty()) {
            return provider.suggestedModels.map { DiscoveredModel(id = it, displayName = it) }
        }

        return when (provider) {
            AiProvider.Gemini -> discoverGeminiModels(key)
            AiProvider.OpenAiCompatible -> discoverOpenAiModels(key, endpoint)
        }
    }

    private suspend fun discoverGeminiModels(apiKey: String): List<DiscoveredModel> {
        val url = "https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey"
        val response = client.get(url) {
            header(HttpHeaders.Accept, "application/json")
        }

        if (!response.status.isSuccess()) {
            throw IllegalStateException("Error al consultar modelos de Gemini (HTTP ${response.status.value}).")
        }

        val parsed = response.body<GeminiModelsListResponse>()
        val filtered = parsed.models.filter { model ->
            val id = model.name.removePrefix("models/").lowercase()
            val supportsGenerate = model.supportedGenerationMethods.any { it.equals("generateContent", ignoreCase = true) }
            val isEmbedding = id.contains("embedding") || id.contains("aqa")
            val isPureImage = id.contains("imagen") || id.contains("image-generation")
            val isLegacyPalm = id.contains("bison") || id.contains("gecko")

            supportsGenerate && !isEmbedding && !isPureImage && !isLegacyPalm
        }

        if (filtered.isEmpty()) {
            return AiProvider.Gemini.suggestedModels.map { DiscoveredModel(id = it, displayName = it) }
        }

        return filtered.map { model ->
            val cleanId = model.name.removePrefix("models/")
            DiscoveredModel(
                id = cleanId,
                displayName = model.displayName?.ifBlank { cleanId } ?: cleanId,
                description = model.description.orEmpty(),
            )
        }.sortedWith(
            compareByDescending<DiscoveredModel> { it.id.contains("flash", ignoreCase = true) }
                .thenByDescending { it.id }
        )
    }

    private suspend fun discoverOpenAiModels(apiKey: String, customEndpoint: String): List<DiscoveredModel> {
        val targetUrl = resolveOpenAiModelsUrl(customEndpoint)
        val response = client.get(targetUrl) {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            header(HttpHeaders.Accept, "application/json")
        }

        if (!response.status.isSuccess()) {
            throw IllegalStateException("Error al consultar modelos OpenAI-compatible (HTTP ${response.status.value}).")
        }

        val parsed = response.body<OpenAiModelsListResponse>()
        val filtered = parsed.data.filter { model ->
            val id = model.id.lowercase()
            val isEmbedding = id.contains("embedding")
            val isImageGen = id.contains("dall-e")
            val isTts = id.startsWith("tts-")
            val isWhisper = id.startsWith("whisper-")
            val isModeration = id.contains("moderation")
            val isLegacy = id.contains("babbage") || id.contains("davinci")

            !isEmbedding && !isImageGen && !isTts && !isWhisper && !isModeration && !isLegacy
        }

        if (filtered.isEmpty()) {
            return AiProvider.OpenAiCompatible.suggestedModels.map { DiscoveredModel(id = it, displayName = it) }
        }

        return filtered.map { model ->
            DiscoveredModel(
                id = model.id,
                displayName = model.id,
                description = model.ownedBy?.let { "Propietario: $it" }.orEmpty(),
            )
        }.sortedByDescending { it.id }
    }

    private fun resolveOpenAiModelsUrl(customEndpoint: String): String {
        val trimmed = customEndpoint.trim()
        if (trimmed.isBlank() || trimmed.contains("api.openai.com")) {
            return "https://api.openai.com/v1/models"
        }
        return if (trimmed.endsWith("/chat/completions")) {
            trimmed.removeSuffix("/chat/completions") + "/models"
        } else if (trimmed.endsWith("/models")) {
            trimmed
        } else {
            "${trimmed.trimEnd('/')}/models"
        }
    }
}
