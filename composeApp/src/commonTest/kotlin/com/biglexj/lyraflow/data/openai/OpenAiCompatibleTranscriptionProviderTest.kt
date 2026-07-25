package com.biglexj.lyraflow.data.openai

import com.biglexj.lyraflow.domain.transcription.TranscriptionRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalEncodingApi::class)
class OpenAiCompatibleTranscriptionProviderTest {
    @Test
    fun sendsAudioAndInstructionInOneChatCompletionRequest() = runTest {
        var requestBody = ""
        val client = HttpClient(MockEngine { request ->
            assertEquals("Bearer test-key", request.headers[HttpHeaders.Authorization])
            requestBody = (request.body as TextContent).text
            respond(
                content = """{"choices":[{"message":{"content":"Texto corregido"}}]}""",
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val result = OpenAiCompatibleTranscriptionProvider(
            client = client,
            apiKey = { "test-key" },
            endpoint = { "https://example.test/v1/chat/completions" },
        ).transcribe(
            TranscriptionRequest(
                audio = byteArrayOf(1, 2, 3),
                model = "audio-model",
            ),
        )

        assertEquals("Texto corregido", result.rawText)
        assertEquals("audio-model", result.model)
        assertTrue(requestBody.contains("\"type\":\"input_audio\""))
        assertTrue(requestBody.contains("\"format\":\"wav\""))
        assertTrue(requestBody.contains(Base64.encode(byteArrayOf(1, 2, 3))))
        assertTrue(requestBody.contains("Convierte este dictado"))
    }

    @Test
    fun readsTextFromAnArrayOfResponseParts() = runTest {
        val client = HttpClient(MockEngine {
            respond(
                content = """{"choices":[{"message":{"content":[{"type":"output_text","text":"Parte uno"},{"text":" y parte dos"}]}}]}""",
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val result = OpenAiCompatibleTranscriptionProvider(
            client = client,
            apiKey = { "test-key" },
            endpoint = { "https://example.test/v1/chat/completions" },
        ).transcribe(TranscriptionRequest(byteArrayOf(9), model = "audio-model"))

        assertEquals("Parte uno y parte dos", result.rawText)
    }
}
