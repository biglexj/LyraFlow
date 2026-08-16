package com.biglexj.lyraflow.data.scanner

import com.biglexj.lyraflow.core.model.AiProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModelDiscoveryServiceTest {

    @Test
    fun discoversAndFiltersGeminiModelsAccurately() = runTest {
        val geminiMockJson = """
        {
          "models": [
            {
              "name": "models/gemini-3.7-flash",
              "displayName": "Gemini 3.7 Flash",
              "description": "Next generation fast model",
              "supportedGenerationMethods": ["generateContent", "countTokens"]
            },
            {
              "name": "models/gemini-3.5-flash",
              "displayName": "Gemini 3.5 Flash",
              "description": "Fast and intelligent",
              "supportedGenerationMethods": ["generateContent", "countTokens"]
            },
            {
              "name": "models/gemini-2.5-pro",
              "displayName": "Gemini 2.5 Pro",
              "description": "Deep reasoning",
              "supportedGenerationMethods": ["generateContent", "countTokens"]
            },
            {
              "name": "models/text-embedding-004",
              "displayName": "Text Embedding",
              "supportedGenerationMethods": ["embedContent"]
            },
            {
              "name": "models/imagen-3.0-generate-002",
              "displayName": "Imagen 3",
              "supportedGenerationMethods": ["generateImages"]
            },
            {
              "name": "models/aqa",
              "displayName": "Attributed QA",
              "supportedGenerationMethods": ["generateAnswer"]
            }
          ]
        }
        """.trimIndent()

        val client = HttpClient(MockEngine { request ->
            assertTrue(request.url.toString().contains("key=test-gemini-key"))
            respond(
                content = geminiMockJson,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val service = ModelDiscoveryService(client)
        val models = service.discoverModels(AiProvider.Gemini, "test-gemini-key")
        val ids = models.map { it.id }

        assertTrue(ids.contains("gemini-3.7-flash"))
        assertTrue(ids.contains("gemini-3.5-flash"))
        assertTrue(ids.contains("gemini-2.5-pro"))
        assertFalse(ids.contains("text-embedding-004"))
        assertFalse(ids.contains("imagen-3.0-generate-002"))
        assertFalse(ids.contains("aqa"))
    }

    @Test
    fun discoversAndFiltersOpenAiModelsAccurately() = runTest {
        val openAiMockJson = """
        {
          "object": "list",
          "data": [
            {"id": "gpt-5.6-luna", "object": "model", "owned_by": "system"},
            {"id": "gpt-4o", "object": "model", "owned_by": "system"},
            {"id": "gpt-4o-mini", "object": "model", "owned_by": "system"},
            {"id": "text-embedding-3-small", "object": "model", "owned_by": "system"},
            {"id": "dall-e-3", "object": "model", "owned_by": "system"},
            {"id": "tts-1-hd", "object": "model", "owned_by": "system"},
            {"id": "whisper-1", "object": "model", "owned_by": "system"},
            {"id": "text-moderation-latest", "object": "model", "owned_by": "system"}
          ]
        }
        """.trimIndent()

        val client = HttpClient(MockEngine { request ->
            assertEquals("Bearer test-openai-key", request.headers[HttpHeaders.Authorization])
            assertTrue(request.url.toString().endsWith("/models"))
            respond(
                content = openAiMockJson,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val service = ModelDiscoveryService(client)
        val models = service.discoverModels(AiProvider.OpenAiCompatible, "test-openai-key")
        val ids = models.map { it.id }

        assertTrue(ids.contains("gpt-5.6-luna"))
        assertTrue(ids.contains("gpt-4o"))
        assertTrue(ids.contains("gpt-4o-mini"))
        assertFalse(ids.contains("text-embedding-3-small"))
        assertFalse(ids.contains("dall-e-3"))
        assertFalse(ids.contains("tts-1-hd"))
        assertFalse(ids.contains("whisper-1"))
        assertFalse(ids.contains("text-moderation-latest"))
    }

    @Test
    fun returnsFallbackWhenKeyIsEmpty() = runTest {
        val client = HttpClient(MockEngine { respond("") }) {
            install(ContentNegotiation) { json() }
        }
        val service = ModelDiscoveryService(client)
        val models = service.discoverModels(AiProvider.Gemini, "")
        assertEquals(AiProvider.Gemini.suggestedModels, models.map { it.id })
    }
}
