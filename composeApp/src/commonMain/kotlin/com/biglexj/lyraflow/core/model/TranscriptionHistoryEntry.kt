package com.biglexj.lyraflow.core.model

import kotlinx.serialization.Serializable

@Serializable
data class TranscriptionHistoryEntry(
    val id: String,
    val timestampMs: Long,
    val rawTranscript: String,
    val refinedText: String = "",
    val providerName: String = "",
    val audioDurationMs: Long = 0L
)
