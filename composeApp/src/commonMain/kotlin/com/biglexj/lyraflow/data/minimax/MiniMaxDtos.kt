package com.biglexj.lyraflow.data.minimax

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class MiniMaxSpeechToTextResponse(
    val text: String? = null,
    val duration: Double? = null,
    @SerialName("trace_id") val traceId: String? = null,
    @SerialName("base_resp") val baseResp: MiniMaxBaseResp? = null,
)

@Serializable
internal data class MiniMaxBaseResp(
    @SerialName("status_code") val statusCode: Int? = null,
    @SerialName("status_msg") val statusMsg: String? = null,
)

@Serializable
internal data class MiniMaxChatRequest(
    val model: String,
    val messages: List<MiniMaxChatMessage>,
)

@Serializable
internal data class MiniMaxChatMessage(
    val role: String,
    val content: String,
)

@Serializable
internal data class MiniMaxChatResponse(
    val choices: List<MiniMaxChatChoice> = emptyList(),
)

@Serializable
internal data class MiniMaxChatChoice(
    val message: MiniMaxChatResponseMessage? = null,
)

@Serializable
internal data class MiniMaxChatResponseMessage(
    val content: JsonElement? = null,
)
