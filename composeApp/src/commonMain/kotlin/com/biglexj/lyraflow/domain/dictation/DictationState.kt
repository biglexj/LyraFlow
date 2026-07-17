package com.biglexj.lyraflow.domain.dictation

sealed interface DictationState {
    data object Idle : DictationState
    data object Listening : DictationState
    data class Transcribing(val model: String) : DictationState
    data class Completed(
        val rawText: String,
        val refinedText: String,
        val provider: String,
        val elapsedMillis: Long,
    ) : DictationState

    data class Failed(val message: String) : DictationState
}
