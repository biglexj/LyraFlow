package com.biglexj.lyraflow.core.config

enum class WhisperModel(
    val label: String,
    val fileName: String,
    val description: String,
) {
    Tiny("Tiny", "ggml-tiny.bin", "Ultra ligero y rápido (~75 MB)."),
    Base("Base", "ggml-base.bin", "Dictado estándar rápido (~140 MB)."),
    Small("Small", "ggml-small.bin", "Equilibrio entre precisión y velocidad (~460 MB)."),
    Medium("Medium", "ggml-medium.bin", "Alta precisión para vocabulario amplio (~1.5 GB)."),
    Large("Large", "ggml-large-v3.bin", "Máxima precisión disponible (~3.1 GB)."),
}
