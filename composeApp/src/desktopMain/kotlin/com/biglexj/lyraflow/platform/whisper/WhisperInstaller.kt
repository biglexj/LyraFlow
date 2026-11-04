package com.biglexj.lyraflow.platform.whisper

import com.biglexj.lyraflow.core.config.WhisperSetupState
import com.biglexj.lyraflow.core.config.WhisperModel
import java.net.URI
import java.io.IOException
import java.time.Duration
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex

class WhisperInstaller {
    private val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20))
        .followRedirects(HttpClient.Redirect.ALWAYS).build()
    private val installationLock = Mutex()
    private var activeModelPreference: WhisperModel? = null
    private val mutableState = MutableStateFlow(currentState())
    val state: StateFlow<WhisperSetupState> = mutableState

    suspend fun install(model: WhisperModel) {
        if (!installationLock.tryLock()) return
        try {
            activeModelPreference = model
            mutableState.value = WhisperSetupState("Preparando Whisper", downloading = true)
            withContext(Dispatchers.IO) { installFiles(model) }
            mutableState.value = currentState(model)
        } catch (cancelled: CancellationException) {
            mutableState.value = currentState(model)
            throw cancelled
        } catch (failure: Exception) {
            mutableState.value = WhisperSetupState("Error: ${failure.message ?: "no se pudo instalar"}")
        } finally {
            installationLock.unlock()
        }
    }

    private fun installFiles(model: WhisperModel) {
        Files.createDirectories(WhisperPaths.root)
        val exec = WhisperPaths.executable()
        if (exec == null) {
            val archive = WhisperPaths.root.resolve("${WhisperReleaseAssets.WINDOWS_PACKAGE}.part")
            download(resolveWindowsPackage(), archive, 0f, .35f, "Descargando Whisper")
            extract(archive, WhisperPaths.root.resolve("runtime"))
            Files.deleteIfExists(archive)
        }

        val modelPath = WhisperPaths.model(model)
        if (!Files.isRegularFile(modelPath)) {
            val startProgress = if (exec == null) .35f else 0f
            download(modelUrl(model), modelPath, startProgress, 1f, "Descargando modelo ${model.label}")
        }
        checkNotNull(WhisperPaths.executable()) { "El paquete no incluyó whisper-cli.exe" }
    }

    /**
     * URL del paquete de Whisper para Windows. Primero se consulta la API de GitHub y, si no hay
     * respuesta utilizable (límite de peticiones, release sin adjuntos), se prueban las URL fijadas.
     */
    private fun resolveWindowsPackage(): String {
        return resolveWhisperPackage(::latestWindowsAsset, FALLBACK_PACKAGES, ::isReachable)
            ?: error("No se encontró el paquete de Whisper para Windows. Revisa tu conexión a internet e inténtalo de nuevo.")
    }

    private fun latestWindowsAsset(): String? {
        val request = HttpRequest.newBuilder(URI.create(RELEASES_API))
            .timeout(Duration.ofSeconds(20))
            .header("Accept", "application/vnd.github+json").header("User-Agent", "LyraFlow").build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) return null
        return WhisperReleaseAssets.selectWindowsPackage(response.body())
    }

    private fun isReachable(url: String): Boolean {
        val request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(15))
            .method("HEAD", HttpRequest.BodyPublishers.noBody()).header("User-Agent", "LyraFlow").build()
        val status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode()
        return status in 200..399
    }

    private fun download(url: String, destination: java.nio.file.Path, start: Float, end: Float, label: String) {
        Files.createDirectories(destination.parent)
        val request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofMinutes(30))
            .header("User-Agent", "LyraFlow").build()
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
            check(total < 0 || received == total) { "La descarga quedó incompleta. Inténtalo de nuevo." }
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

    private fun currentState(preferredModel: WhisperModel? = activeModelPreference): WhisperSetupState {
        val activeModel = when {
            preferredModel != null && Files.isRegularFile(WhisperPaths.model(preferredModel)) -> preferredModel
            else -> WhisperModel.entries.firstOrNull { Files.isRegularFile(WhisperPaths.model(it)) }
        }
        return if (WhisperPaths.executable() != null && activeModel != null) {
            WhisperSetupState("Whisper ${activeModel.label} listo", available = true, model = activeModel)
        } else WhisperSetupState("Clic para instalar Whisper local")
    }

    private fun modelUrl(model: WhisperModel) = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/${model.fileName}"

    private companion object {
        const val RELEASES_API = "https://api.github.com/repos/ggml-org/whisper.cpp/releases?per_page=30"

        /** Respaldo cuando la API de GitHub no responde (límite de peticiones) o no publica adjuntos. */
        val FALLBACK_PACKAGES = listOf(
            "https://github.com/ggml-org/whisper.cpp/releases/download/b5454/${WhisperReleaseAssets.WINDOWS_PACKAGE}",
            "https://github.com/ggml-org/whisper.cpp/releases/download/v1.9.0/${WhisperReleaseAssets.WINDOWS_PACKAGE}",
        )
    }
}

/** Un fallo de transporte de la API o de un respaldo no descarta los siguientes. */
internal fun resolveWhisperPackage(
    latest: () -> String?,
    fallbacks: List<String>,
    reachable: (String) -> Boolean,
): String? {
    try { latest()?.let { return it } } catch (_: IOException) { /* probar respaldo */ }
    for (url in fallbacks) {
        try { if (reachable(url)) return url } catch (_: IOException) { /* probar siguiente */ }
    }
    return null
}
