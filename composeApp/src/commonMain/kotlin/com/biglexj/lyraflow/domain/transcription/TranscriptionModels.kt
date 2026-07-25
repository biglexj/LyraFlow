package com.biglexj.lyraflow.domain.transcription

data class TranscriptionRequest(
    val audio: ByteArray,
    val mimeType: String = "audio/wav",
    val model: String = "",
    val systemPrompt: String = "",
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
