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

    fun markListening() {
        mutableState.value = DictationState.Listening
    }

    fun reset() {
        mutableState.value = DictationState.Idle
    }

    suspend fun process(request: TranscriptionRequest) {
        mutableState.value = DictationState.Transcribing(request.model.label)
        mutableState.value = try {
            val result = transcriber.transcribe(request)
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
