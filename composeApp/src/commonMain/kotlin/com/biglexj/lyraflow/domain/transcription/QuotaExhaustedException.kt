package com.biglexj.lyraflow.domain.transcription

class QuotaExhaustedException(
    message: String = "Cuota de Gemini agotada.",
    cause: Throwable? = null,
) : RuntimeException(message, cause)
