package com.biglexj.lyraflow.core.update

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object UpdateChecker {
    private val jsonParser = Json { ignoreUnknownKeys = true }

    fun parseUpdateRelease(json: String): UpdateRelease? {
        val root = runCatching { jsonParser.parseToJsonElement(json).jsonObject }.getOrNull() ?: return null
        val tagName = root["tag_name"]?.jsonPrimitive?.content ?: return null
        val htmlUrl = root["html_url"]?.jsonPrimitive?.content ?: return null
        val body = root["body"]?.jsonPrimitive?.content.orEmpty()

        val assets = root["assets"]?.jsonArray.orEmpty()

        // Priorizar específicamente el instalador EXE (ej. LyraFlow-Windows-X.Y.Z.exe), evitando colisión con SHA256SUMS.txt
        val exeAsset = assets.firstOrNull { asset ->
            val name = asset.jsonObject["name"]?.jsonPrimitive?.content.orEmpty()
            val url = asset.jsonObject["browser_download_url"]?.jsonPrimitive?.content.orEmpty()
            name.endsWith(".exe", ignoreCase = true) || url.endsWith(".exe", ignoreCase = true)
        }

        val downloadUrl = exeAsset?.jsonObject?.get("browser_download_url")?.jsonPrimitive?.content
            ?: assets.firstOrNull()?.jsonObject?.get("browser_download_url")?.jsonPrimitive?.content
            ?: htmlUrl

        val cleanVersion = tagName.removePrefix("v").trim()
        return UpdateRelease(
            version = cleanVersion,
            downloadUrl = downloadUrl,
            releasePageUrl = htmlUrl,
            body = body,
        )
    }

    fun isNewerVersion(current: String, remote: String): Boolean {
        val currentParts = current.split(".").mapNotNull { it.takeWhile { c -> c.isDigit() }.toIntOrNull() }
        val remoteParts = remote.split(".").mapNotNull { it.takeWhile { c -> c.isDigit() }.toIntOrNull() }
        val maxLen = maxOf(currentParts.size, remoteParts.size)
        for (i in 0 until maxLen) {
            val c = currentParts.getOrElse(i) { 0 }
            val r = remoteParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (c > r) return false
        }
        return false
    }
}
