package com.biglexj.lyraflow.platform.whisper

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WhisperSidecar(
    private val executable: () -> Path? = WhisperPaths::executable,
) {
    val status: String
        get() = executable()?.let { "disponible en ${it.fileName}" } ?: "binario no instalado"

    suspend fun transcribe(wav: Path, model: Path, language: String = "auto"): String = withContext(Dispatchers.IO) {
        val binary = requireNotNull(executable()) { "No se encontró whisper-cli." }
        require(Files.isRegularFile(wav)) { "No existe el WAV: $wav" }
        require(Files.isRegularFile(model)) { "No existe el modelo: $model" }

        val outputPrefix = Files.createTempFile("lyraflow-whisper-", "")
        Files.deleteIfExists(outputPrefix)

        val targetLanguage = if (language.isBlank() || language.equals("auto", ignoreCase = true)) {
            val systemLang = java.util.Locale.getDefault().language
            if (systemLang.isNotBlank()) systemLang else "es"
        } else {
            language.lowercase()
        }

        val threads = Runtime.getRuntime().availableProcessors().coerceIn(2, 8).toString()
        val process = ProcessBuilder(
            binary.absolutePathString(),
            "-m", model.absolutePathString(),
            "-f", wav.absolutePathString(),
            "-l", targetLanguage,
            "-nt",
            "-np",
            "-t", threads,
            "-otxt",
            "-of", outputPrefix.absolutePathString(),
        ).redirectErrorStream(true).start()

        val log = process.inputStream.bufferedReader().readText()
        check(process.waitFor() == 0) { "whisper-cli falló: ${log.takeLast(500)}" }
        val textFile = Path.of(outputPrefix.absolutePathString() + ".txt")
        if (!Files.exists(textFile)) return@withContext ""
        return@withContext Files.readString(textFile).trim().also {
            Files.deleteIfExists(textFile)
        }
    }
}
