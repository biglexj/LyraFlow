package com.biglexj.lyraflow.domain.dictation

import com.biglexj.lyraflow.domain.transcription.TranscriptionProvider
import com.biglexj.lyraflow.domain.transcription.TranscriptionRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DictationCoordinator(
    private val transcriber: TranscriptionProvider,
) {
    private val mutableState = MutableStateFlow<DictationState>(DictationState.Idle)
    val state: StateFlow<DictationState> = mutableState.asStateFlow()

    private var lastRequest: TranscriptionRequest? = null

    fun markListening() {
        mutableState.value = DictationState.Listening
    }

    fun reset() {
        mutableState.value = DictationState.Idle
    }

    suspend fun process(request: TranscriptionRequest) {
        lastRequest = request
        processInternal(request, transcriber)
    }

    suspend fun retry(alternativeTranscriber: TranscriptionProvider? = null) {
        val request = lastRequest ?: return
        processInternal(request, alternativeTranscriber ?: transcriber)
    }

    private suspend fun processInternal(request: TranscriptionRequest, provider: TranscriptionProvider) {
        val modelLabel = if (provider::class.simpleName?.contains("Whisper", ignoreCase = true) == true) {
            "Whisper local"
        } else {
            request.model.label
        }
        mutableState.value = DictationState.Transcribing(modelLabel)
        mutableState.value = try {
            val result = provider.transcribe(request)
            DictationState.Completed(
                rawText = result.rawText,
                refinedText = result.rawText,
                provider = result.provider,
                elapsedMillis = result.elapsedMillis,
            )
        } catch (error: Throwable) {
            DictationState.Failed(error.message ?: "Error desconocido")
        }
    }
}
