package com.biglexj.lyraflow.data.minimax

import com.biglexj.lyraflow.domain.transcription.QuotaExhaustedException
import com.biglexj.lyraflow.domain.transcription.TranscriptionProvider
import com.biglexj.lyraflow.domain.transcription.TranscriptionRequest
import com.biglexj.lyraflow.domain.transcription.TranscriptionResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlin.time.TimeSource

/**
 * Transcriptor de voz de MiniMax mediante su endpoint dedicado `speech_to_text`.
 *
 * A diferencia de Gemini y del adaptador OpenAI-compatible, MiniMax no acepta
 * audio en Chat Completions: el audio entra por un multipart/form-data aparte que
 * devuelve texto plano. Ese texto se envia despues al modelo de texto de MiniMax
 * para aplicar las mismas instrucciones de refinamiento que usa el resto de la app.
 *
 * Limites documentados por MiniMax: 500 segundos y 50 MB por solicitud.
 */
class MiniMaxSpeechToTextProvider(
    private val client: HttpClient,
    private val apiKey: () -> String,
    private val endpoint: () -> String,
    private val refinementModel: String = REFINEMENT_MODEL,
) : TranscriptionProvider {

    override suspend fun transcribe(request: TranscriptionRequest): TranscriptionResult {
        val key = apiKey().trim()
        require(key.isNotEmpty()) { "Configura MINIMAX_API_KEY para transcribir." }
        val model = request.model.trim().ifBlank { DEFAULT_ASR_MODEL }
        val base = normalizeBaseUrl(endpoint())
        require(base.isNotEmpty()) { "Configura el endpoint de MiniMax." }

        val started = TimeSource.Monotonic.markNow()
        val rawText = transcribeRaw(base = base, key = key, model = model, request = request)

        val prompt = request.systemPrompt.trim()
        val refined = if (rawText.isBlank() || prompt.isBlank()) {
            null
        } else {
            runCatching { refine(base = base, key = key, text = rawText, prompt = prompt) }.getOrNull()
        }

        val finalText = refined?.takeIf { it.isNotBlank() } ?: rawText
        return TranscriptionResult(
            rawText = finalText,
            provider = if (refined.isNullOrBlank()) "MiniMax ASR" else "MiniMax ASR + $refinementModel",
            model = model,
            elapsedMillis = started.elapsedNow().inWholeMilliseconds,
        )
    }

    private suspend fun transcribeRaw(
        base: String,
        key: String,
        model: String,
        request: TranscriptionRequest,
    ): String {
        val response = client.post("$base/speech_to_text") {
            header(HttpHeaders.Authorization, "Bearer $key")
            header(HttpHeaders.Accept, ContentType.Application.Json)
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("model", model)
                        append("response_format", "json")
                        append(
                            key = "file",
                            value = request.audio,
                            headers = Headers.build {
                                append(HttpHeaders.ContentType, request.mimeType)
                                append(HttpHeaders.ContentDisposition, "filename=lyraflow-dictado.wav")
                            },
                        )
                    },
                ),
            )
        }

        val status = response.status.value
        if (status == 429) {
            throw QuotaExhaustedException(
                "Has alcanzado el limite de uso o saldo de MiniMax. Revisa tu cuenta en MiniMax o espera un momento."
            )
        }

        if (!response.status.isSuccess()) {
            val detail = response.bodyAsText().trim().take(240)
            val hint = when (status) {
                400 -> "Revisa el formato del audio: MiniMax acepta WAV, MP3, FLAC, M4A, AAC, OGG y Opus. Los audios de mas de 500 segundos tambien se rechazan con HTTP 400."
                413 -> "El audio supera el maximo de 50 MB por solicitud de MiniMax."
                else -> ""
            }
            throw IllegalStateException(
                buildString {
                    append("MiniMax respondio HTTP $status al transcribir.")
                    if (detail.isNotBlank()) append(" Detalle: $detail")
                    if (hint.isNotBlank()) append(" $hint")
                },
            )
        }

        val parsed = response.body<MiniMaxSpeechToTextResponse>()
        val failure = parsed.baseResp?.takeIf { (it.statusCode ?: 0) != 0 }
        if (failure != null) {
            throw IllegalStateException(
                "MiniMax rechazo la transcripcion: " + (failure.statusMsg ?: "codigo ${failure.statusCode}"),
            )
        }

        return parsed.text.orEmpty().trim()
    }

    /** Segunda pasada de texto: aplica las instrucciones de refinamiento de LyraFlow. */
    private suspend fun refine(base: String, key: String, text: String, prompt: String): String {
        val response = client.post("$base/chat/completions") {
            header(HttpHeaders.Authorization, "Bearer $key")
            header(HttpHeaders.Accept, ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(
                MiniMaxChatRequest(
                    model = refinementModel,
                    messages = listOf(
                        MiniMaxChatMessage(role = "system", content = prompt),
                        MiniMaxChatMessage(role = "user", content = text),
                    ),
                ),
            )
        }

        if (!response.status.isSuccess()) return text

        val refined = response.body<MiniMaxChatResponse>()
            .choices
            .firstOrNull()
            ?.message
            ?.content
            .asText()
            .trim()

        return refined.ifBlank { text }
    }

    private companion object {
        const val DEFAULT_ASR_MODEL = "asr-1.0"
        const val REFINEMENT_MODEL = "MiniMax-M3.1-Flash"

        /** Acepta tanto la base (.../v1) como una URL completa ya terminada. */
        fun normalizeBaseUrl(raw: String): String {
            val trimmed = raw.trim().trimEnd('/')
            if (trimmed.isEmpty()) return ""
            return trimmed
                .removeSuffix("/speech_to_text")
                .removeSuffix("/chat/completions")
        }
    }
}

private fun JsonElement?.asText(): String = when (this) {
    is JsonPrimitive -> contentOrNull.orEmpty()
    is JsonArray -> joinToString("") { it.asText() }
    else -> ""
}
