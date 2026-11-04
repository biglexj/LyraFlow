package com.biglexj.lyraflow.platform.whisper

import java.io.IOException
import java.net.http.HttpTimeoutException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class WhisperPackageResolutionTest {
    @Test fun usesFallbackAfterApiTransportFailure() {
        assertEquals("backup", resolveWhisperPackage({ throw IOException("offline") }, listOf("backup")) { true })
    }

    @Test fun continuesAfterFirstFallbackTimesOut() {
        assertEquals("second", resolveWhisperPackage({ null }, listOf("first", "second")) {
            if (it == "first") throw HttpTimeoutException("timeout") else true
        })
    }

    @Test fun keepsApiResultWithoutProbingFallbacks() {
        assertEquals("latest", resolveWhisperPackage({ "latest" }, listOf("backup")) { error("Unexpected request") })
    }

    @Test fun returnsNullWhenAllSourcesFail() {
        assertNull(resolveWhisperPackage({ throw IOException() }, listOf("backup")) { throw IOException() })
    }

    @Test fun doesNotSwallowInterruption() {
        assertFailsWith<InterruptedException> {
            resolveWhisperPackage({ throw InterruptedException() }, listOf("backup")) { true }
        }
    }
}
