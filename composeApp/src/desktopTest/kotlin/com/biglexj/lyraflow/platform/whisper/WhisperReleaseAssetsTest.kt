package com.biglexj.lyraflow.platform.whisper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WhisperReleaseAssetsTest {
    @Test
    fun skipsReleasesWithoutWindowsPackage() {
        val json = """
            [
              {"tag_name": "v1.9.5", "assets": []},
              {"tag_name": "b5454", "assets": [
                {"name": "whisper-bin-ubuntu-x64.tar.gz", "browser_download_url": "https://example.test/ubuntu.tgz"},
                {"name": "whisper-bin-x64.zip", "browser_download_url": "https://example.test/whisper-bin-x64.zip"}
              ]}
            ]
        """.trimIndent()

        assertEquals(
            "https://example.test/whisper-bin-x64.zip",
            WhisperReleaseAssets.selectWindowsPackage(json),
        )
    }

    @Test
    fun returnsFirstReleaseThatShipsThePackage() {
        val json = """
            [
              {"assets": [{"name": "whisper-bin-x64.zip", "browser_download_url": "https://example.test/newest.zip"}]},
              {"assets": [{"name": "whisper-bin-x64.zip", "browser_download_url": "https://example.test/older.zip"}]}
            ]
        """.trimIndent()

        assertEquals(
            "https://example.test/newest.zip",
            WhisperReleaseAssets.selectWindowsPackage(json),
        )
    }

    @Test
    fun returnsNullWhenNoReleaseShipsThePackage() {
        val json = """
            [{"assets": [{"name": "whisper-bin-ubuntu-x64.tar.gz", "browser_download_url": "https://example.test/ubuntu.tgz"}]}]
        """.trimIndent()

        assertNull(WhisperReleaseAssets.selectWindowsPackage(json))
    }

    @Test
    fun returnsNullForUnexpectedPayloads() {
        assertNull(WhisperReleaseAssets.selectWindowsPackage("{\"message\": \"API rate limit exceeded\"}"))
        assertNull(WhisperReleaseAssets.selectWindowsPackage("not a json payload"))
        assertNull(WhisperReleaseAssets.selectWindowsPackage("[]"))
    }
}
