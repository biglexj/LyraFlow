package com.biglexj.lyraflow.platform.whisper

import com.biglexj.lyraflow.data.gemini.GeminiContent
import com.biglexj.lyraflow.data.gemini.GeminiPart
import com.biglexj.lyraflow.data.gemini.GeminiRequest
import com.biglexj.lyraflow.data.gemini.GeminiResponse
import com.biglexj.lyraflow.domain.transcription.TranscriptionProvider
import com.biglexj.lyraflow.domain.transcription.TranscriptionRequest
import com.biglexj.lyraflow.domain.transcription.TranscriptionResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Proveedor experimental que combina Whisper local (transcripción audio->texto)
 * con un modelo LLM de texto (ej. Gemini/OpenAI) para post-procesar y refinar el dictado.
 */
class WhisperLlmRefinerProvider(
    private val baseWhisperProvider: TranscriptionProvider,
    private val client: HttpClient,
    private val apiKey: () -> String,
    private val model: () -> String,
    private val isRefinementEnabled: () -> Boolean,
) : TranscriptionProvider {

    override suspend fun transcribe(request: TranscriptionRequest): TranscriptionResult {
        val whisperResult = baseWhisperProvider.transcribe(request)

        if (!isRefinementEnabled() || whisperResult.rawText.isBlank()) {
            return whisperResult
        }

        val key = apiKey().trim()
        if (key.isEmpty()) return whisperResult

        return try {
            val refined = refineTextWithLlm(
                text = whisperResult.rawText,
                systemPrompt = request.systemPrompt,
                key = key,
                modelName = model().trim().ifBlank { "gemini-3.6-flash" },
            )
            whisperResult.copy(
                rawText = refined.ifBlank { whisperResult.rawText },
                provider = "${whisperResult.provider} + LLM Refiner (Exp)",
            )
        } catch (_: Throwable) {
            whisperResult
        }
    }

    private suspend fun refineTextWithLlm(text: String, systemPrompt: String, key: String, modelName: String): String = withContext(Dispatchers.IO) {
        val prompt = "${systemPrompt.trim()}\n\nTexto transcrito a refinar:\n\"$text\""
        val response = client.post("https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent") {
            header("x-goog-api-key", key)
            header(HttpHeaders.Accept, ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(
                GeminiRequest(
                    contents = listOf(
                        GeminiContent(
                            parts = listOf(GeminiPart(text = prompt))
                        )
                    )
                )
            )
        }

        if (!response.status.isSuccess()) return@withContext text

        val refined = response.body<GeminiResponse>()
            .candidates
            .firstOrNull()
            ?.content
            ?.parts
            ?.mapNotNull { it.text }
            ?.joinToString("")
            ?.trim()
            .orEmpty()

        if (refined.isNotBlank()) refined else text
    }
}
