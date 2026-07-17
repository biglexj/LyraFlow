package com.biglexj.lyraflow.platform.whisper

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class WhisperSidecar(
    private val executable: Path? = discoverExecutable(),
) {
    val status: String
        get() = executable?.let { "disponible en ${it.fileName}" } ?: "binario no instalado"

    fun transcribe(wav: Path, model: Path): String {
        val binary = requireNotNull(executable) { "No se encontró whisper-cli." }
        require(Files.isRegularFile(wav)) { "No existe el WAV: $wav" }
        require(Files.isRegularFile(model)) { "No existe el modelo: $model" }

        val outputPrefix = Files.createTempFile("lyraflow-whisper-", "")
        Files.deleteIfExists(outputPrefix)
        val process = ProcessBuilder(
            binary.absolutePathString(),
            "-m", model.absolutePathString(),
            "-f", wav.absolutePathString(),
            "-otxt",
            "-of", outputPrefix.absolutePathString(),
        ).redirectErrorStream(true).start()

        val log = process.inputStream.bufferedReader().readText()
        check(process.waitFor() == 0) { "whisper-cli falló: ${log.takeLast(500)}" }
        val textFile = Path.of(outputPrefix.absolutePathString() + ".txt")
        return Files.readString(textFile).trim().also {
            Files.deleteIfExists(textFile)
        }
    }

    private companion object {
        fun discoverExecutable(): Path? {
            val configured = System.getenv("LYRAFLOW_WHISPER_BIN")
                ?.takeIf(String::isNotBlank)
                ?.let(Path::of)
            if (configured != null && Files.isRegularFile(configured)) return configured

            val name = if (System.getProperty("os.name").contains("windows", true)) {
                "whisper-cli.exe"
            } else {
                "whisper-cli"
            }
            return Path.of("tools", "whisper", name).takeIf(Files::isRegularFile)
        }
    }
}
