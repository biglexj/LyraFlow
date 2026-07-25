package com.biglexj.lyraflow.domain.dictation

import com.biglexj.lyraflow.domain.transcription.TranscriptionProvider
import com.biglexj.lyraflow.domain.transcription.TranscriptionRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException

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
        val maxAttempts = 2
        val modelName = request.model.ifBlank { "Modelo seleccionado" }
        var lastThrowable: Throwable? = null

        for (attempt in 1..maxAttempts) {
            mutableState.value = DictationState.Transcribing(
                model = modelName,
                attempt = attempt,
                maxAttempts = maxAttempts,
            )
            try {
                val result = provider.transcribe(request)
                mutableState.value = DictationState.Completed(
                    rawText = result.rawText,
                    refinedText = result.rawText,
                    provider = result.provider,
                    elapsedMillis = result.elapsedMillis,
                )
                return
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                lastThrowable = error
                mutableState.value = DictationState.AttemptFailed(
                    attempt = attempt,
                    maxAttempts = maxAttempts,
                )
                delay(3000L)
            }
        }

        val finalError = lastThrowable?.message ?: "Error desconocido"
        mutableState.value = DictationState.Failed(finalError)
    }
}
