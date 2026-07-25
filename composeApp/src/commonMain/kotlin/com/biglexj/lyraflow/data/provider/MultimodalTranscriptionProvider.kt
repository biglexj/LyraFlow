package com.biglexj.lyraflow.data.provider

import com.biglexj.lyraflow.core.model.AiProvider
import com.biglexj.lyraflow.core.model.ProviderConfiguration
import com.biglexj.lyraflow.data.gemini.GeminiTranscriptionProvider
import com.biglexj.lyraflow.data.openai.OpenAiCompatibleTranscriptionProvider
import com.biglexj.lyraflow.domain.transcription.TranscriptionProvider
import com.biglexj.lyraflow.domain.transcription.TranscriptionRequest
import com.biglexj.lyraflow.domain.transcription.TranscriptionResult
import io.ktor.client.HttpClient

/** Resuelve el protocolo activo sin acoplar el dominio a un proveedor. */
class MultimodalTranscriptionProvider(
    private val client: HttpClient,
    private val apiKey: () -> String,
    private val configuration: () -> ProviderConfiguration,
) : TranscriptionProvider {

    override suspend fun transcribe(request: TranscriptionRequest): TranscriptionResult {
        val selected = configuration()
        val configuredRequest = request.copy(model = selected.model)
        return when (selected.provider) {
            AiProvider.Gemini -> GeminiTranscriptionProvider(client, apiKey).transcribe(configuredRequest)
            AiProvider.OpenAiCompatible -> OpenAiCompatibleTranscriptionProvider(
                client = client,
                apiKey = apiKey,
                endpoint = { selected.endpoint },
            ).transcribe(configuredRequest)
        }
    }
}
