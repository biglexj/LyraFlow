package com.biglexj.lyraflow.domain.transcription

import com.biglexj.lyraflow.core.model.GeminiModel

data class TranscriptionRequest(
    val audio: ByteArray,
    val mimeType: String = "audio/wav",
    val model: GeminiModel = GeminiModel.Fast,
)

data class TranscriptionResult(
    val rawText: String,
    val provider: String,
    val model: String,
    val elapsedMillis: Long,
)

fun interface TranscriptionProvider {
    suspend fun transcribe(request: TranscriptionRequest): TranscriptionResult
}
