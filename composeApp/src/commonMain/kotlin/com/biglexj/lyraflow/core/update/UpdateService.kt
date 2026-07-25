package com.biglexj.lyraflow.core.update

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpdateService(
    private val client: HttpClient,
    private val owner: String = "biglexj",
    private val repo: String = "LyraFlow",
) {
    suspend fun checkLatestRelease(): UpdateRelease? = withContext(Dispatchers.Default) {
        runCatching {
            val response = client.get("https://api.github.com/repos/$owner/$repo/releases/latest") {
                header("User-Agent", "$repo-App-Updater")
            }
            val json = response.bodyAsText()
            UpdateChecker.parseUpdateRelease(json)
        }.getOrNull()
    }
}
