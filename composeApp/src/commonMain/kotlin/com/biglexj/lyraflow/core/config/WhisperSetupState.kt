package com.biglexj.lyraflow.core.config

data class WhisperSetupState(
    val detail: String,
    val available: Boolean = false,
    val downloading: Boolean = false,
    val progress: Float? = null,
    val model: WhisperModel? = null,
) {
    companion object {
        val Unsupported = WhisperSetupState("No disponible en esta plataforma")
    }
}
