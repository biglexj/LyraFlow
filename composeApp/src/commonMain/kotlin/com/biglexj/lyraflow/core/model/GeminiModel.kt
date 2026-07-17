package com.biglexj.lyraflow.core.model

enum class GeminiModel(
    val id: String,
    val label: String,
) {
    Fast("gemini-3.1-flash-lite", "Gemini 3.1 Flash Lite"),
    Smart("gemini-3.5-flash", "Gemini 3.5 Flash"),
}
