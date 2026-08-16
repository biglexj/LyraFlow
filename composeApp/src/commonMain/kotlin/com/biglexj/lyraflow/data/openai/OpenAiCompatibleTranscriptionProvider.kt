package com.biglexj.lyraflow.data.openai

import com.biglexj.lyraflow.domain.transcription.TranscriptionProvider
import com.biglexj.lyraflow.domain.transcription.TranscriptionRequest
import com.biglexj.lyraflow.domain.transcription.TranscriptionResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.TimeSource

/**
 * Envía audio e instrucción en un único turno a una API compatible con Chat
 * Completions. El modelo seleccionado debe admitir entrada de audio; la
 * compatibilidad del protocolo no garantiza que cualquier modelo entienda WAV o MP3.
 */
class OpenAiCompatibleTranscriptionProvider(
    private val client: HttpClient,
    private val apiKey: () -> String,
    private val endpoint: () -> String,
) : TranscriptionProvider {

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun transcribe(request: TranscriptionRequest): TranscriptionResult {
        val key = apiKey().trim()
        require(key.isNotEmpty()) { "Configura una clave de API para transcribir." }
        val model = request.model.trim()
        require(model.isNotEmpty()) { "Configura un modelo OpenAI-compatible para transcribir." }
        val url = endpoint().trim()
        require(url.isNotEmpty()) { "Configura el endpoint OpenAI-compatible." }

        val started = TimeSource.Monotonic.markNow()
        val response = client.post(url) {
            header(HttpHeaders.Authorization, "Bearer $key")
            header(HttpHeaders.Accept, ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(createBody(request, model))
        }

        check(response.status.isSuccess()) {
            if (response.status.value == 429) {
                "⚠️ Has alcanzado el límite de uso o cuota del servicio OpenAI-compatible. Revisa tu saldo o intenta más tarde."
            } else {
                val detail = response.bodyAsText().trim().take(240)
                "El endpoint OpenAI-compatible respondió HTTP ${response.status.value}." +
                    if (detail.isBlank()) "" else " Detalle: $detail"
            }
        }

        val text = response.body<OpenAiChatResponse>()
            .choices
            .firstOrNull()
            ?.message
            ?.content
            .asText()
            .trim()

        check(text.isNotEmpty()) { "El endpoint no devolvió texto transcrito." }
        return TranscriptionResult(
            rawText = text,
            provider = "OpenAI-compatible",
            model = model,
            elapsedMillis = started.elapsedNow().inWholeMilliseconds,
        )
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun createBody(request: TranscriptionRequest, model: String): OpenAiChatRequest {
        val format = request.mimeType.audioFormat()
        val prompt = request.systemPrompt.trim().ifBlank { TRANSCRIPTION_PROMPT }
        return OpenAiChatRequest(
            model = model,
            messages = listOf(
                OpenAiMessage(
                    role = "user",
                    content = listOf(
                        OpenAiContentPart(text = prompt, type = "text"),
                        OpenAiContentPart(
                            type = "input_audio",
                            inputAudio = OpenAiInputAudio(
                                data = Base64.encode(request.audio),
                                format = format,
                            ),
                        ),
                    ),
                ),
            ),
        )
    }

    private fun String.audioFormat(): String = when {
        contains("wav", ignoreCase = true) -> "wav"
        contains("mp3", ignoreCase = true) || contains("mpeg", ignoreCase = true) -> "mp3"
        else -> error("El protocolo OpenAI-compatible solo acepta audio WAV o MP3 en esta versión.")
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
                "Transcribe con fidelidad la voz del hablante, resolviendo con naturalidad titubeos o ruido de fondo leve. Si no existe voz audible en absoluto, devuelve únicamente una cadena vacía."
    }
}

private fun JsonElement?.asText(): String = when (this) {
    is JsonPrimitive -> contentOrNull.orEmpty()
    is JsonArray -> joinToString("") { it.asText() }
    is JsonObject -> this["text"]?.asText()
        ?: this["content"]?.asText()
        ?: this["output_text"]?.asText()
        ?: ""
    null -> ""
}
