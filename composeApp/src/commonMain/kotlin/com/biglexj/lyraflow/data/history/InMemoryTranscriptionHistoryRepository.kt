package com.biglexj.lyraflow.data.history

import com.biglexj.lyraflow.core.model.TranscriptionHistoryEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class InMemoryTranscriptionHistoryRepository : TranscriptionHistoryRepository {
    private val _history = MutableStateFlow<List<TranscriptionHistoryEntry>>(emptyList())
    override val history: StateFlow<List<TranscriptionHistoryEntry>> = _history.asStateFlow()

    override suspend fun saveEntry(entry: TranscriptionHistoryEntry) {
        _history.update { currentList ->
            val updated = listOf(entry) + currentList.filterNot { it.id == entry.id }
            updated.sortedByDescending { it.timestampMs }
        }
    }

    override suspend fun deleteEntry(id: String) {
        _history.update { currentList ->
            currentList.filterNot { it.id == id }
        }
    }

    override suspend fun clearHistory() {
        _history.value = emptyList()
    }

    override suspend fun purgeExpired(retentionHours: Long) {
        val cutoffMs = System.currentTimeMillis() - (retentionHours * 3600_000L)
        _history.update { currentList ->
            currentList.filter { it.timestampMs >= cutoffMs }
        }
    }
}
