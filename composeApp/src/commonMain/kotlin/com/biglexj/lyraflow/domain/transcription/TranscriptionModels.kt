package com.biglexj.lyraflow.domain.transcription

import com.biglexj.lyraflow.core.model.AiProvider

data class TranscriptionRequest(
    val audio: ByteArray,
    val mimeType: String = "audio/wav",
    val model: String = "",
    val systemPrompt: String = "",
    /**
     * Proveedor que ejecuta la peticion. Permite atribuir la cuota agotada al
     * servicio realmente seleccionado y no a un proveedor fijo.
     */
    val provider: AiProvider = AiProvider.Gemini,
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
