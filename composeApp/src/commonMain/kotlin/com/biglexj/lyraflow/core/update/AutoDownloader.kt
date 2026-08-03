package com.biglexj.lyraflow.core.update

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class DownloadProgress(
    val progress: Float,
    val downloadedBytes: Long,
    val totalBytes: Long,
)

class AutoDownloader(
    private val client: HttpClient,
) {
    suspend fun downloadUpdate(
        downloadUrl: String,
        targetFileName: String = "LyraFlow-Update.msi",
        onProgress: (DownloadProgress) -> Unit,
    ): String = withContext(Dispatchers.Default) {
        val response = client.get(downloadUrl)
        val totalBytes = response.contentLength() ?: 35_000_000L
        val channel = response.bodyAsChannel()

        val tempDir = System.getProperty("java.io.tmpdir") ?: "."
        val targetFile = File(tempDir, targetFileName)

        targetFile.outputStream().use { output ->
            val buffer = ByteArray(64 * 1024)
            var downloadedBytes = 0L
            while (!channel.isClosedForRead) {
                val read = channel.readAvailable(buffer, 0, buffer.size)
                if (read <= 0) break
                output.write(buffer, 0, read)
                downloadedBytes += read
                val progress = (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                onProgress(DownloadProgress(progress, downloadedBytes, totalBytes))
            }
        }
        targetFile.absolutePath
    }
}
