package com.biglexj.lyraflow.data.minimax

import com.biglexj.lyraflow.domain.transcription.QuotaExhaustedException
import com.biglexj.lyraflow.domain.transcription.TranscriptionRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MiniMaxSpeechToTextProviderTest {

    private fun jsonClient(handler: (HttpRequestData) -> Pair<HttpStatusCode, String>): HttpClient =
        HttpClient(MockEngine { request ->
            val (status, body) = handler(request)
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

    @Test
    fun transcribesThenRefinesWithTheConfiguredPrompt() = runTest {
        val seenUrls = mutableListOf<String>()
        val client = jsonClient { request ->
            seenUrls += request.url.toString()
            assertEquals("Bearer test-key", request.headers[HttpHeaders.Authorization])
            if (request.url.toString().endsWith("/speech_to_text")) {
                HttpStatusCode.OK to """{"text":"eh bueno esto","duration":3.2,"trace_id":"abc"}"""
            } else {
                HttpStatusCode.OK to """{"choices":[{"message":{"content":"Esto."}}]}"""
            }
        }

        val result = MiniMaxSpeechToTextProvider(
            client = client,
            apiKey = { "test-key" },
            endpoint = { "https://api.minimax.test/v1" },
        ).transcribe(
            TranscriptionRequest(
                audio = byteArrayOf(1, 2, 3),
                model = "asr-1.0",
                systemPrompt = "Limpia el dictado.",
            ),
        )

        assertEquals("Esto.", result.rawText)
        assertTrue(result.provider.contains("MiniMax ASR"))
        assertEquals("https://api.minimax.test/v1/speech_to_text", seenUrls[0])
        assertEquals("https://api.minimax.test/v1/chat/completions", seenUrls[1])
    }

    @Test
    fun keepsRawTranscriptionWhenRefinementIsNotConfigured() = runTest {
        val client = jsonClient { HttpStatusCode.OK to """{"text":"texto plano"}""" }

        val result = MiniMaxSpeechToTextProvider(
            client = client,
            apiKey = { "test-key" },
            endpoint = { "https://api.minimax.test/v1" },
        ).transcribe(TranscriptionRequest(byteArrayOf(1), model = "asr-1.0"))

        assertEquals("texto plano", result.rawText)
        assertEquals("MiniMax ASR", result.provider)
    }

    @Test
    fun reportsQuotaExhaustionSoWhisperCanTakeOver() = runTest {
        val client = jsonClient {
            HttpStatusCode.TooManyRequests to """{"base_resp":{"status_code":1002,"status_msg":"insufficient balance"}}"""
        }

        assertFailsWith<QuotaExhaustedException> {
            MiniMaxSpeechToTextProvider(
                client = client,
                apiKey = { "test-key" },
                endpoint = { "https://api.minimax.test/v1" },
            ).transcribe(TranscriptionRequest(byteArrayOf(1), model = "asr-1.0"))
        }
    }

    @Test
    fun explainsTheDocumentedLengthLimitOnBadRequest() = runTest {
        val client = jsonClient { HttpStatusCode.BadRequest to """{"error":"invalid audio"}""" }

        val error = assertFailsWith<IllegalStateException> {
            MiniMaxSpeechToTextProvider(
                client = client,
                apiKey = { "test-key" },
                endpoint = { "https://api.minimax.test/v1" },
            ).transcribe(TranscriptionRequest(byteArrayOf(1), model = "asr-1.0"))
        }
        assertTrue(error.message.orEmpty().contains("500 segundos"))
    }
}
