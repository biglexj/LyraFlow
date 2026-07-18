package com.biglexj.lyraflow.core.config

enum class WhisperModel(
    val label: String,
    val fileName: String,
    val description: String,
) {
    Tiny("Tiny", "ggml-tiny.bin", "El más rápido; menor precisión."),
    Base("Base", "ggml-base.bin", "Buen equilibrio para dictado diario."),
    Small("Small", "ggml-small.bin", "Más precisión, con una descarga mayor."),
    Medium("Medium", "ggml-medium.bin", "Mayor precisión para audio complejo; descarga pesada."),
    Large("Large", "ggml-large-v3.bin", "Máxima precisión disponible; requiere bastante memoria y espacio."),
}
