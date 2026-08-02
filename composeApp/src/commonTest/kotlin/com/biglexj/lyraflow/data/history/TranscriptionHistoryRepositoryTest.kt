package com.biglexj.lyraflow.data.history

import com.biglexj.lyraflow.core.model.TranscriptionHistoryEntry
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TranscriptionHistoryRepositoryTest {

    @Test
    fun saveAndRetrieveHistory_ordersByTimestampDescending() = runTest {
        val repo = InMemoryTranscriptionHistoryRepository()

        val entry1 = TranscriptionHistoryEntry(
            id = "1",
            timestampMs = 1000L,
            rawTranscript = "Primera transcripción"
        )
        val entry2 = TranscriptionHistoryEntry(
            id = "2",
            timestampMs = 2000L,
            rawTranscript = "Segunda transcripción"
        )

        repo.saveEntry(entry1)
        repo.saveEntry(entry2)

        val history = repo.history.value
        assertEquals(2, history.size)
        assertEquals("2", history[0].id)
        assertEquals("1", history[1].id)
    }

    @Test
    fun deleteEntry_removesSpecificEntry() = runTest {
        val repo = InMemoryTranscriptionHistoryRepository()

        val entry1 = TranscriptionHistoryEntry(id = "1", timestampMs = 1000L, rawTranscript = "Test 1")
        val entry2 = TranscriptionHistoryEntry(id = "2", timestampMs = 2000L, rawTranscript = "Test 2")

        repo.saveEntry(entry1)
        repo.saveEntry(entry2)
        repo.deleteEntry("1")

        val history = repo.history.value
        assertEquals(1, history.size)
        assertEquals("2", history[0].id)
    }

    @Test
    fun purgeExpired_removesEntriesOlderThanRetentionWindow() = runTest {
        val repo = InMemoryTranscriptionHistoryRepository()
        val now = System.currentTimeMillis()

        val oldEntry = TranscriptionHistoryEntry(
            id = "old",
            timestampMs = now - (30 * 3600_000L), // 30 hours ago
            rawTranscript = "Viejo dictado"
        )
        val recentEntry = TranscriptionHistoryEntry(
            id = "recent",
            timestampMs = now - (5 * 3600_000L), // 5 hours ago
            rawTranscript = "Dictado reciente"
        )

        repo.saveEntry(oldEntry)
        repo.saveEntry(recentEntry)

        // Retention of 24 hours
        repo.purgeExpired(24L)

        val history = repo.history.value
        assertEquals(1, history.size)
        assertEquals("recent", history[0].id)
    }
}
