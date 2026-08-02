package com.biglexj.lyraflow.platform.update

import com.biglexj.lyraflow.core.network.createPlatformHttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

sealed interface UpdateDownloadState {
    data object Idle : UpdateDownloadState
    data class Downloading(val progress: Float, val downloadedMb: String, val totalMb: String) : UpdateDownloadState
    data class ReadyToInstall(val installerFile: File) : UpdateDownloadState
    data class Error(val message: String) : UpdateDownloadState
}

class DesktopAutoUpdater {
    private val client = createPlatformHttpClient()
    private val _state = MutableStateFlow<UpdateDownloadState>(UpdateDownloadState.Idle)
    val state: StateFlow<UpdateDownloadState> = _state.asStateFlow()

    suspend fun downloadInstaller(
        downloadUrl: String,
        version: String,
    ) = withContext(Dispatchers.IO) {
        try {
            _state.value = UpdateDownloadState.Downloading(0f, "0.0", "0.0")

            val response = client.get(downloadUrl)
            if (!response.status.isSuccess()) {
                _state.value = UpdateDownloadState.Error("HTTP ${response.status.value}")
                return@withContext
            }

            val contentLength = response.contentLength() ?: -1L
            val totalMbStr = if (contentLength > 0) "%.1f".format(contentLength / 1024f / 1024f) else "?"

            val ext = if (downloadUrl.endsWith(".msi", ignoreCase = true)) ".msi" else ".exe"
            val tempDir = File(System.getProperty("java.io.tmpdir"))
            val targetFile = File(tempDir, "LyraFlow_v${version}_Update$ext")

            val channel: ByteReadChannel = response.bodyAsChannel()
            var downloadedBytes = 0L
            val buffer = ByteArray(8192)

            FileOutputStream(targetFile).use { output ->
                while (!channel.isClosedForRead) {
                    val read = channel.readAvailable(buffer, 0, buffer.size)
                    if (read <= 0) break
                    output.write(buffer, 0, read)
                    downloadedBytes += read

                    val progress = if (contentLength > 0) (downloadedBytes.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f) else 0f
                    val downloadedMbStr = "%.1f".format(downloadedBytes / 1024f / 1024f)
                    _state.value = UpdateDownloadState.Downloading(progress, downloadedMbStr, totalMbStr)
                }
            }

            _state.value = UpdateDownloadState.ReadyToInstall(targetFile)
        } catch (e: Exception) {
            _state.value = UpdateDownloadState.Error(e.message ?: "Error de descarga")
        }
    }

    fun executeSilentInstallation(file: File, onExitApp: () -> Unit) {
        try {
            val path = file.absolutePath
            val command = if (path.endsWith(".msi", ignoreCase = true)) {
                listOf("msiexec.exe", "/i", path, "/passive", "/norestart")
            } else {
                listOf(path, "/passive")
            }

            ProcessBuilder(command).start()
            onExitApp()
        } catch (e: Exception) {
            _state.value = UpdateDownloadState.Error("Error al ejecutar instalador: ${e.message}")
        }
    }
}
