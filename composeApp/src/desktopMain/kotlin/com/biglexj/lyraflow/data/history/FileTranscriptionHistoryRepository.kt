package com.biglexj.lyraflow.data.history

import com.biglexj.lyraflow.core.model.TranscriptionHistoryEntry
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Historial local: reemplazo atómico y retención aplicada al cargar y al guardar. */
class FileTranscriptionHistoryRepository(
    private val file: Path = defaultHistoryPath(),
    private var retentionHours: Long = 24L,
    private val now: () -> Long = System::currentTimeMillis,
) : TranscriptionHistoryRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Mutex()
    private val mutableHistory = MutableStateFlow(load())
    override val history = mutableHistory.asStateFlow()

    override suspend fun saveEntry(entry: TranscriptionHistoryEntry) = update {
        listOf(entry) + it.filterNot { previous -> previous.id == entry.id }
    }

    override suspend fun deleteEntry(id: String) = update { entries -> entries.filterNot { it.id == id } }
    override suspend fun clearHistory() = update { emptyList() }

    override suspend fun purgeExpired(retentionHours: Long) = withContext(Dispatchers.IO) {
        lock.withLock {
            this@FileTranscriptionHistoryRepository.retentionHours = retentionHours
            val entries = retained(mutableHistory.value)
            persist(entries)
            mutableHistory.value = entries
        }
    }

    private fun retained(entries: List<TranscriptionHistoryEntry>): List<TranscriptionHistoryEntry> {
        if (retentionHours <= 0) return emptyList()
        val cutoff = now() - retentionHours * 3_600_000L
        return entries.filter { it.timestampMs >= cutoff }.distinctBy { it.id }.sortedByDescending { it.timestampMs }
    }

    private fun load(): List<TranscriptionHistoryEntry> {
        if (!Files.isRegularFile(file)) return emptyList()
        return try {
            val entries = retained(json.decodeFromString<List<TranscriptionHistoryEntry>>(Files.readString(file)))
            persist(entries)
            entries
        } catch (failure: SerializationException) {
            // No crear copias que escapen al borrado y a la retención del usuario.
            System.err.println("No se pudo leer el historial local; se iniciará un historial vacío.")
            emptyList()
        } catch (failure: java.io.IOException) {
            System.err.println("No se pudo cargar el historial local: ${failure.javaClass.simpleName}")
            emptyList()
        }
    }

    private suspend fun update(transform: (List<TranscriptionHistoryEntry>) -> List<TranscriptionHistoryEntry>) =
        withContext(Dispatchers.IO) {
            lock.withLock {
                val entries = retained(transform(mutableHistory.value))
                persist(entries)
                mutableHistory.value = entries
            }
        }

    private fun persist(entries: List<TranscriptionHistoryEntry>) {
        if (entries.isEmpty()) {
            Files.deleteIfExists(file)
            return
        }
        Files.createDirectories(file.toAbsolutePath().parent)
        val temporary = Files.createTempFile(file.toAbsolutePath().parent, "history-", ".tmp")
        try {
            Files.writeString(temporary, json.encodeToString(entries))
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
    }
}

private fun defaultHistoryPath(): Path {
    val localData = System.getenv("LOCALAPPDATA")?.takeIf(String::isNotBlank)
    return if (localData != null) Path.of(localData, "LyraFlow", "history.json")
    else Path.of(System.getProperty("user.home"), ".local", "share", "lyraflow", "history.json")
}
