package com.biglexj.lyraflow.core.update

object UpdateChecker {
    private fun extractStringValue(json: String, key: String): String? {
        val search = "\"$key\":"
        val keyIndex = json.indexOf(search)
        if (keyIndex == -1) return null
        val start = json.indexOf("\"", keyIndex + search.length)
        if (start == -1) return null
        val end = json.indexOf("\"", start + 1)
        if (end == -1) return null
        return json.substring(start + 1, end)
    }

    fun parseUpdateRelease(json: String): UpdateRelease? {
        val tagName = extractStringValue(json, "tag_name") ?: return null
        val htmlUrl = extractStringValue(json, "html_url") ?: return null
        val body = extractStringValue(json, "body").orEmpty()

        val downloadKeySearch = "\"browser_download_url\":"
        val downloadKeyIndex = json.indexOf(downloadKeySearch)
        val downloadUrl = if (downloadKeyIndex != -1) {
            val start = json.indexOf("\"", downloadKeyIndex + downloadKeySearch.length)
            if (start != -1) {
                val end = json.indexOf("\"", start + 1)
                if (end != -1) json.substring(start + 1, end) else htmlUrl
            } else htmlUrl
        } else htmlUrl

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
