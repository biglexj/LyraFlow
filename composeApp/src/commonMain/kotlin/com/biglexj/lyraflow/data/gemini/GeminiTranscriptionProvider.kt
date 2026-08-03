package com.biglexj.lyraflow.data.gemini

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
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.TimeSource

import com.biglexj.lyraflow.domain.transcription.QuotaExhaustedException

class GeminiTranscriptionProvider(
    private val client: HttpClient,
    private val apiKey: () -> String,
) : TranscriptionProvider {

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun transcribe(request: TranscriptionRequest): TranscriptionResult {
        val key = apiKey().trim()
        require(key.isNotEmpty()) { "Configura GEMINI_API_KEY para transcribir." }
        val model = request.model.trim()
        require(model.isNotEmpty()) { "Configura un modelo de Gemini para transcribir." }

        val started = TimeSource.Monotonic.markNow()
        val response = client.post(
            "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent",
        ) {
            header("x-goog-api-key", key)
            header(HttpHeaders.Accept, ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(createBody(request))
        }

        if (response.status.value == 429) {
            throw QuotaExhaustedException(
                "⚠️ Has alcanzado el límite de uso o cuota de la API de Gemini. Espera un momento o cambia la API Key / modelo."
            )
        }

        check(response.status.isSuccess()) {
            "Gemini respondió HTTP ${response.status.value}."
        }

        val text = response.body<GeminiResponse>()
            .candidates
            .firstOrNull()
            ?.content
            ?.parts
            ?.mapNotNull { it.text }
            ?.joinToString("")
            ?.trim()
            .orEmpty()

        return TranscriptionResult(
            rawText = text,
            provider = "Gemini",
            model = model,
            elapsedMillis = started.elapsedNow().inWholeMilliseconds,
        )
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun createBody(request: TranscriptionRequest): GeminiRequest {
        val prompt = request.systemPrompt.trim().ifBlank { TRANSCRIPTION_PROMPT }
        return GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(
                            inlineData = GeminiInlineData(
                                mimeType = request.mimeType,
                                data = Base64.encode(request.audio),
                            ),
                        ),
                        GeminiPart(text = prompt),
                    ),
                ),
            ),
        )
    }

    private companion object {
        const val TRANSCRIPTION_PROMPT =
            "Convierte este dictado en texto final, claro y bien escrito, conservando fielmente su intención. " +
                "Preserva de forma íntegra el idioma original expresado por el usuario (incluyendo español, inglés, chino, japonés o cualquier idioma y caracteres CJK o símbolos), sin traducir ni omitir caracteres o uniones. " +
                "Corrige puntuación, concordancia, repeticiones involuntarias y falsos comienzos. " +
                "Ordena las ideas y crea párrafos o listas cuando el hablante enumere elementos. " +
                "Interpreta órdenes de formato habladas según el contexto, por ejemplo 'punto', 'coma', " +
                "'nueva línea' y 'punto por punto', sin escribir literalmente esas órdenes. " +
                "No resumas, no inventes información y no cambies nombres, cifras, rutas ni fragmentos de código. " +
                "Devuelve únicamente el texto final, sin comentarios, comillas ni bloques Markdown. " +
                "Si no hay voz clara, devuelve una cadena vacía."
    }
}
