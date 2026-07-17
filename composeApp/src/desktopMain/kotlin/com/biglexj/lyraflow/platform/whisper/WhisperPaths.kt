package com.biglexj.lyraflow.platform.whisper

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile

object WhisperPaths {
    val root: Path by lazy {
        val localData = System.getenv("LOCALAPPDATA")?.takeIf(String::isNotBlank)
        if (localData != null) Path.of(localData, "LyraFlow", "whisper")
        else Path.of(System.getProperty("user.home"), ".local", "share", "lyraflow", "whisper")
    }
    val model: Path get() = root.resolve("models/ggml-base.bin")

    fun executable(): Path? {
        val configured = System.getenv("LYRAFLOW_WHISPER_BIN")
            ?.takeIf(String::isNotBlank)?.let(Path::of)?.takeIf(Path::isRegularFile)
        if (configured != null) return configured
        if (!Files.exists(root)) return null
        val name = if (System.getProperty("os.name").contains("windows", true)) "whisper-cli.exe" else "whisper-cli"
        return Files.walk(root).use { paths -> paths.filter { it.fileName.toString() == name }.findFirst().orElse(null) }
    }
}
