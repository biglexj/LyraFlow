package com.biglexj.lyraflow.domain.transcription

import com.biglexj.lyraflow.core.model.AiProvider

/**
 * Cuota agotada registrada con la identidad del proveedor y del modelo que la
 * consumieron. Al conservar esa identidad, el aviso corresponde a la seleccion
 * vigente y la cuota no bloquea a otro proveedor ni a otro modelo.
 */
data class QuotaExhaustion(
    val provider: AiProvider,
    val model: String,
    val message: String,
) {
    /** Etiqueta legible del origen de la cuota, por ejemplo "MiniMax (asr-1.0)". */
    val source: String
        get() = if (model.isBlank()) provider.label else "${provider.label} ($model)"
}
