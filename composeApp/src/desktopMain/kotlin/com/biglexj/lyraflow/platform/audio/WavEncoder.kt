package com.biglexj.lyraflow.platform.audio

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object WavEncoder {
    fun encodePcm16Mono(pcm: ByteArray, sampleRate: Int = 16_000): ByteArray {
        val output = ByteArrayOutputStream(44 + pcm.size)
        output.write("RIFF".encodeToByteArray())
        output.write(int32(36 + pcm.size))
        output.write("WAVEfmt ".encodeToByteArray())
        output.write(int32(16))
        output.write(int16(1))
        output.write(int16(1))
        output.write(int32(sampleRate))
        output.write(int32(sampleRate * 2))
        output.write(int16(2))
        output.write(int16(16))
        output.write("data".encodeToByteArray())
        output.write(int32(pcm.size))
        output.write(pcm)
        return output.toByteArray()
    }

    private fun int16(value: Int): ByteArray = ByteBuffer.allocate(2)
        .order(ByteOrder.LITTLE_ENDIAN).putShort(value.toShort()).array()

    private fun int32(value: Int): ByteArray = ByteBuffer.allocate(4)
        .order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
}
