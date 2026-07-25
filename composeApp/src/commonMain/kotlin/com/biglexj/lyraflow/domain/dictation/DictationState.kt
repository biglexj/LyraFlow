package com.biglexj.lyraflow.domain.dictation

sealed interface DictationState {
    data object Idle : DictationState
    data object Listening : DictationState
    data class Transcribing(
        val model: String,
        val attempt: Int = 1,
        val maxAttempts: Int = 2,
    ) : DictationState

    data class AttemptFailed(
        val attempt: Int,
        val maxAttempts: Int = 2,
    ) : DictationState

    data class Completed(
        val rawText: String,
        val refinedText: String,
        val provider: String,
        val elapsedMillis: Long,
    ) : DictationState

    data class Failed(val message: String) : DictationState
}

