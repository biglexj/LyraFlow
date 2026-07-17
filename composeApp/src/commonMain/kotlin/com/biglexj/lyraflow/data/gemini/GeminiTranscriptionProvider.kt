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

class GeminiTranscriptionProvider(
    private val client: HttpClient,
    private val apiKey: () -> String,
) : TranscriptionProvider {

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun transcribe(request: TranscriptionRequest): TranscriptionResult {
        val key = apiKey().trim()
        require(key.isNotEmpty()) { "Configura GEMINI_API_KEY para transcribir." }

        val started = TimeSource.Monotonic.markNow()
        val response = client.post(
            "https://generativelanguage.googleapis.com/v1beta/models/${request.model.id}:generateContent",
        ) {
            header("x-goog-api-key", key)
            header(HttpHeaders.Accept, ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(createBody(request))
        }

        check(response.status.isSuccess()) {
            "Gemini respondió HTTP ${response.status.value}."
        }

        val text = response.body<GeminiResponse>()
            .candidates
            .firstOrNull()
            ?.content
            ?.parts
            ?.firstNotNullOfOrNull { it.text }
            ?.trim()
            .orEmpty()

        return TranscriptionResult(
            rawText = text,
            provider = "Gemini",
            model = request.model.id,
            elapsedMillis = started.elapsedNow().inWholeMilliseconds,
        )
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun createBody(request: TranscriptionRequest) = GeminiRequest(
        contents = listOf(
            GeminiContent(
                parts = listOf(
                    GeminiPart(
                        inlineData = GeminiInlineData(
                            mimeType = request.mimeType,
                            data = Base64.encode(request.audio),
                        ),
                    ),
                    GeminiPart(text = TRANSCRIPTION_PROMPT),
                ),
            ),
        ),
    )

    private companion object {
        const val TRANSCRIPTION_PROMPT =
            "Convierte este dictado en texto final, claro y bien escrito, conservando fielmente su intención. " +
                "Corrige puntuación, concordancia, repeticiones involuntarias y falsos comienzos. " +
                "Ordena las ideas y crea párrafos o listas cuando el hablante enumere elementos. " +
                "Interpreta órdenes de formato habladas según el contexto, por ejemplo 'punto', 'coma', " +
                "'nueva línea' y 'punto por punto', sin escribir literalmente esas órdenes. " +
                "No resumas, no inventes información y no cambies nombres, cifras, rutas ni fragmentos de código. " +
                "Devuelve únicamente el texto final, sin comentarios, comillas ni bloques Markdown. " +
                "Si no hay voz clara, devuelve una cadena vacía."
    }
}
