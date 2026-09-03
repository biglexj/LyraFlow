package com.biglexj.lyraflow.core.update

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UpdateCheckerTest {

    @Test
    fun testParseUpdateReleaseValidJson() {
        val sampleJson = """
            {
              "tag_name": "v1.1.0",
              "html_url": "https://github.com/biglexj/LyraFlow/releases/tag/v1.1.0",
              "body": "Nuevas funcionalidades y correcciones de errores.",
              "assets": [
                {
                  "name": "LyraFlow-v1.1.0.exe",
                  "browser_download_url": "https://github.com/biglexj/LyraFlow/releases/download/v1.1.0/LyraFlow-1.1.0.exe"
                }
              ]
            }
        """.trimIndent()

        val release = UpdateChecker.parseUpdateRelease(sampleJson)

        assertNotNull(release)
        assertEquals("1.1.0", release.version)
        assertEquals("https://github.com/biglexj/LyraFlow/releases/download/v1.1.0/LyraFlow-1.1.0.exe", release.downloadUrl)
        assertEquals("https://github.com/biglexj/LyraFlow/releases/tag/v1.1.0", release.releasePageUrl)
        assertEquals("Nuevas funcionalidades y correcciones de errores.", release.body)
    }

    @Test
    fun testParseUpdateReleasePrioritizesExeOverChecksums() {
        val jsonWithMultipleAssets = """
            {
              "tag_name": "v1.1.6",
              "html_url": "https://github.com/biglexj/LyraFlow/releases/tag/v1.1.6",
              "body": "Mejoras de rendimiento.",
              "assets": [
                {
                  "name": "SHA256SUMS.txt",
                  "browser_download_url": "https://github.com/biglexj/LyraFlow/releases/download/v1.1.6/SHA256SUMS.txt"
                },
                {
                  "name": "LyraFlow-Windows-1.1.6.exe",
                  "browser_download_url": "https://github.com/biglexj/LyraFlow/releases/download/v1.1.6/LyraFlow-Windows-1.1.6.exe"
                }
              ]
            }
        """.trimIndent()

        val release = UpdateChecker.parseUpdateRelease(jsonWithMultipleAssets)

        assertNotNull(release)
        assertEquals("1.1.6", release.version)
        assertEquals("https://github.com/biglexj/LyraFlow/releases/download/v1.1.6/LyraFlow-Windows-1.1.6.exe", release.downloadUrl)
    }

    @Test
    fun testParseUpdateReleaseInvalidJsonReturnsNull() {
        val release = UpdateChecker.parseUpdateRelease("{}")
        assertNull(release)
    }

    @Test
    fun testIsNewerVersion() {
        assertTrue(UpdateChecker.isNewerVersion("1.0.0", "1.0.1"))
        assertTrue(UpdateChecker.isNewerVersion("1.0.0", "1.1.0"))
        assertTrue(UpdateChecker.isNewerVersion("1.0.0", "2.0.0"))

        assertFalse(UpdateChecker.isNewerVersion("1.0.0", "1.0.0"))
        assertFalse(UpdateChecker.isNewerVersion("1.1.0", "1.0.0"))
        assertFalse(UpdateChecker.isNewerVersion("2.0.0", "1.9.9"))
    }
}
