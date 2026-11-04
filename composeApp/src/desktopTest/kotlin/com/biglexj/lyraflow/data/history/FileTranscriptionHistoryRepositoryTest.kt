package com.biglexj.lyraflow.data.history

import com.biglexj.lyraflow.core.model.TranscriptionHistoryEntry
import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileTranscriptionHistoryRepositoryTest {
    private val now = 200_000_000L
    private fun entry(id: String, ageHours: Long = 0) = TranscriptionHistoryEntry(id, now - ageHours * 3_600_000L, "Dictado $id")

    @Test fun survivesRestartAndPreservesUnicode() = runTest {
        val file = Files.createTempDirectory("lyra-history").resolve("history.json")
        try {
            FileTranscriptionHistoryRepository(file, now = { now }).saveEntry(entry("mañana"))
            assertEquals(listOf(entry("mañana")), FileTranscriptionHistoryRepository(file, now = { now }).history.value)
        } finally { Files.deleteIfExists(file); Files.deleteIfExists(file.parent) }
    }

    @Test fun expiresOnRestartAndOnSave() = runTest {
        val file = Files.createTempDirectory("lyra-history").resolve("history.json")
        try {
            val repo = FileTranscriptionHistoryRepository(file, retentionHours = 72, now = { now })
            repo.saveEntry(entry("old", 30)); repo.saveEntry(entry("recent", 5))
            val reopened = FileTranscriptionHistoryRepository(file, now = { now })
            assertEquals(listOf("recent"), reopened.history.value.map { it.id })
            reopened.saveEntry(entry("expired", 25))
            assertEquals(listOf("recent"), reopened.history.value.map { it.id })
        } finally { Files.deleteIfExists(file); Files.deleteIfExists(file.parent) }
    }

    @Test fun deleteClearAndDisablePersist() = runTest {
        val file = Files.createTempDirectory("lyra-history").resolve("history.json")
        try {
            val repo = FileTranscriptionHistoryRepository(file, now = { now })
            repo.saveEntry(entry("a")); repo.saveEntry(entry("b")); repo.deleteEntry("a")
            assertEquals(listOf("b"), FileTranscriptionHistoryRepository(file, now = { now }).history.value.map { it.id })
            repo.clearHistory(); assertFalse(Files.exists(file))
            repo.saveEntry(entry("a")); repo.purgeExpired(0)
            assertFalse(Files.exists(file))
            repo.saveEntry(entry("b")); assertTrue(repo.history.value.isEmpty()); assertFalse(Files.exists(file))
        } finally { Files.deleteIfExists(file); Files.deleteIfExists(file.parent) }
    }

    @Test fun disabledAtStartupDeletesPreviouslyStoredHistory() = runTest {
        val file = Files.createTempDirectory("lyra-history").resolve("history.json")
        try {
            FileTranscriptionHistoryRepository(file, now = { now }).saveEntry(entry("a"))
            val disabled = FileTranscriptionHistoryRepository(file, retentionHours = 0, now = { now })
            assertTrue(disabled.history.value.isEmpty())
            assertFalse(Files.exists(file))
        } finally { Files.deleteIfExists(file); Files.deleteIfExists(file.parent) }
    }
}
