package com.biglexj.lyraflow.data.openai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
)

@Serializable
internal data class OpenAiMessage(
    val role: String,
    val content: List<OpenAiContentPart>,
)

@Serializable
internal data class OpenAiContentPart(
    val type: String,
    val text: String? = null,
    @SerialName("input_audio")
    val inputAudio: OpenAiInputAudio? = null,
)

@Serializable
internal data class OpenAiInputAudio(
    val data: String,
    val format: String,
)

@Serializable
internal data class OpenAiChatResponse(
    val choices: List<OpenAiChoice> = emptyList(),
)

@Serializable
internal data class OpenAiChoice(
    val message: OpenAiResponseMessage? = null,
)

@Serializable
internal data class OpenAiResponseMessage(
    val content: JsonElement? = null,
)
