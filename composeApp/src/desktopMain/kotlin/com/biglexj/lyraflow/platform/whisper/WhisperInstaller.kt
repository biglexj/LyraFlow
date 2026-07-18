package com.biglexj.lyraflow.platform.whisper

import com.biglexj.lyraflow.core.config.WhisperSetupState
import com.biglexj.lyraflow.core.config.WhisperModel
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class WhisperInstaller {
    private val client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build()
    private val mutableState = MutableStateFlow(currentState())
    val state: StateFlow<WhisperSetupState> = mutableState

    suspend fun install(model: WhisperModel) {
        if (mutableState.value.downloading) return
        withContext(Dispatchers.IO) {
            runCatching {
                Files.createDirectories(WhisperPaths.root)
                val archive = WhisperPaths.root.resolve("whisper-bin-x64.zip.part")
                download(latestWindowsAsset(), archive, 0f, .35f, "Descargando Whisper")
                extract(archive, WhisperPaths.root.resolve("runtime"))
                Files.deleteIfExists(archive)
                download(modelUrl(model), WhisperPaths.model(model), .35f, 1f, "Descargando modelo ${model.label}")
                checkNotNull(WhisperPaths.executable()) { "El paquete no incluyó whisper-cli.exe" }
            }.onSuccess {
                mutableState.value = currentState()
            }.onFailure {
                mutableState.value = WhisperSetupState("Error: ${it.message ?: "no se pudo instalar"}")
            }
        }
    }

    private fun latestWindowsAsset(): String {
        val request = HttpRequest.newBuilder(URI.create(LATEST_RELEASE_API))
            .header("Accept", "application/vnd.github+json").header("User-Agent", "LyraFlow").build()
        val body = client.send(request, HttpResponse.BodyHandlers.ofString()).also {
            check(it.statusCode() in 200..299) { "GitHub respondió ${it.statusCode()}" }
        }.body()
        return Json.parseToJsonElement(body).jsonObject.getValue("assets").jsonArray
            .map { it.jsonObject }
            .first { it.getValue("name").jsonPrimitive.content == "whisper-bin-x64.zip" }
            .getValue("browser_download_url").jsonPrimitive.content
    }

    private fun download(url: String, destination: java.nio.file.Path, start: Float, end: Float, label: String) {
        Files.createDirectories(destination.parent)
        val request = HttpRequest.newBuilder(URI.create(url)).header("User-Agent", "LyraFlow").build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
        check(response.statusCode() in 200..299) { "Descarga respondió ${response.statusCode()}" }
        val total = response.headers().firstValueAsLong("Content-Length").orElse(-1L)
        val temporary = destination.resolveSibling(destination.fileName.toString() + ".part")
        response.body().use { input -> Files.newOutputStream(temporary).use { output ->
            val buffer = ByteArray(64 * 1024)
            var received = 0L
            while (true) {
                val count = input.read(buffer).takeIf { it > 0 } ?: break
                output.write(buffer, 0, count)
                received += count
                val fraction = if (total > 0) received.toFloat() / total else 0f
                mutableState.value = WhisperSetupState(label, downloading = true, progress = start + (end - start) * fraction)
            }
        } }
        Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING)
    }

    private fun extract(archive: java.nio.file.Path, destination: java.nio.file.Path) {
        Files.createDirectories(destination)
        ZipInputStream(Files.newInputStream(archive)).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entry ->
                val target = destination.resolve(entry.name).normalize()
                check(target.startsWith(destination)) { "Entrada ZIP insegura" }
                if (entry.isDirectory) Files.createDirectories(target)
                else {
                    Files.createDirectories(target.parent)
                    Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
    }

    private fun currentState(): WhisperSetupState {
        val installedModel = WhisperModel.entries.firstOrNull { Files.isRegularFile(WhisperPaths.model(it)) }
        return if (WhisperPaths.executable() != null && installedModel != null) {
            WhisperSetupState("Whisper ${installedModel.label} listo", available = true, model = installedModel)
        } else WhisperSetupState("Clic para instalar Whisper local")
    }

    private fun modelUrl(model: WhisperModel) = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/${model.fileName}"

    private companion object {
        const val LATEST_RELEASE_API = "https://api.github.com/repos/ggml-org/whisper.cpp/releases/latest"
    }
}
