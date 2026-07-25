package com.biglexj.lyraflow.data.gemini

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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeminiTranscriptionProviderTest {
    @Test
    fun usesTheCurrentModelAndGeminiMultimodalFieldNames() = runTest {
        var requestBody = ""
        val client = HttpClient(MockEngine { request ->
            assertEquals("test-key", request.headers["x-goog-api-key"])
            requestBody = (request.body as TextContent).text
            respond(
                content = """{"candidates":[{"content":{"parts":[{"text":"Texto de Gemini"}]}}]}""",
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val result = GeminiTranscriptionProvider(client) { "test-key" }.transcribe(
            TranscriptionRequest(byteArrayOf(1, 2), model = "gemini-3.6-flash"),
        )

        assertEquals("Texto de Gemini", result.rawText)
        assertEquals("gemini-3.6-flash", result.model)
        assertTrue(requestBody.contains("\"inline_data\""))
        assertTrue(requestBody.contains("\"mime_type\":\"audio/wav\""))
        assertFalse(requestBody.contains("\"inlineData\""))
    }
}
