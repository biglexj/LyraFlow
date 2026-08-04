package com.biglexj.lyraflow.data.provider

import com.biglexj.lyraflow.domain.transcription.TranscriptionProvider
import com.biglexj.lyraflow.domain.transcription.TranscriptionRequest
import com.biglexj.lyraflow.domain.transcription.TranscriptionResult

/** Proveedor deshabilitado para responder limpiamente cuando un servicio se desactiva manualmente. */
class DisabledTranscriptionProvider(
    private val reason: String,
) : TranscriptionProvider {
    override suspend fun transcribe(request: TranscriptionRequest): TranscriptionResult {
        error(reason)
    }
}
