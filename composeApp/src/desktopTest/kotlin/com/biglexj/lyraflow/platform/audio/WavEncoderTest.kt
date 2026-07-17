package com.biglexj.lyraflow.platform.audio

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class WavEncoderTest {
    @Test
    fun createsValidPcmHeader() {
        val wav = WavEncoder.encodePcm16Mono(byteArrayOf(1, 2, 3, 4))

        assertEquals(48, wav.size)
        assertContentEquals("RIFF".encodeToByteArray(), wav.copyOfRange(0, 4))
        assertContentEquals("WAVE".encodeToByteArray(), wav.copyOfRange(8, 12))
        assertContentEquals("data".encodeToByteArray(), wav.copyOfRange(36, 40))
    }
}
