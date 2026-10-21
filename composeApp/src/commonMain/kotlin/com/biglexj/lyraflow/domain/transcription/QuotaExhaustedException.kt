package com.biglexj.lyraflow.domain.transcription

/**
 * Se lanza cuando el proveedor activo responde con HTTP 429. El mensaje lo
 * redacta cada proveedor con su propio lenguaje; el dominio completa la
 * atribucion con el proveedor y el modelo de la peticion.
 */
class QuotaExhaustedException(
    message: String = "Se agotó la cuota de uso del servicio.",
    cause: Throwable? = null,
) : RuntimeException(message, cause)
