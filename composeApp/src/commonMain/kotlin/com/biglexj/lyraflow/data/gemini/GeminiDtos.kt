package com.biglexj.lyraflow.data.gemini

import kotlinx.serialization.Serializable

@Serializable
internal data class GeminiRequest(val contents: List<GeminiContent>)

@Serializable
internal data class GeminiContent(val parts: List<GeminiPart>)

@Serializable
internal data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null,
)

@Serializable
internal data class GeminiInlineData(
    val mimeType: String,
    val data: String,
)

@Serializable
internal data class GeminiResponse(val candidates: List<GeminiCandidate> = emptyList())

@Serializable
internal data class GeminiCandidate(val content: GeminiContent? = null)
