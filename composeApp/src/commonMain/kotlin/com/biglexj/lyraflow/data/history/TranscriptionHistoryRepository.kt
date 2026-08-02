package com.biglexj.lyraflow.data.history

import com.biglexj.lyraflow.core.model.TranscriptionHistoryEntry
import kotlinx.coroutines.flow.StateFlow

interface TranscriptionHistoryRepository {
    val history: StateFlow<List<TranscriptionHistoryEntry>>
    suspend fun saveEntry(entry: TranscriptionHistoryEntry)
    suspend fun deleteEntry(id: String)
    suspend fun clearHistory()
    suspend fun purgeExpired(retentionHours: Long)
}
