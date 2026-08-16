package com.biglexj.lyraflow.data.scanner

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class GeminiModelsListResponse(
    val models: List<GeminiModelDto> = emptyList(),
)

@Serializable
internal data class GeminiModelDto(
    val name: String,
    val displayName: String? = null,
    val description: String? = null,
    val supportedGenerationMethods: List<String> = emptyList(),
)

@Serializable
internal data class OpenAiModelsListResponse(
    val data: List<OpenAiModelDto> = emptyList(),
)

@Serializable
internal data class OpenAiModelDto(
    val id: String,
    @SerialName("owned_by")
    val ownedBy: String? = null,
)

data class DiscoveredModel(
    val id: String,
    val displayName: String,
    val description: String = "",
)
