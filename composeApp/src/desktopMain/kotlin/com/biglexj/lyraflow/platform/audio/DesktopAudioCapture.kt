package com.biglexj.lyraflow.platform.audio

import java.io.ByteArrayOutputStream
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine
import kotlin.concurrent.thread
import kotlin.math.sqrt

class DesktopAudioCapture {
    private val format = AudioFormat(16_000f, 16, 1, true, false)
    private var line: TargetDataLine? = null
    private var worker: Thread? = null
    private var buffer: ByteArrayOutputStream? = null

    @Synchronized
    fun start(onTelemetry: (level: Float, durationMillis: Long) -> Unit = { _, _ -> }) {
        check(line == null) { "La captura ya está activa." }
        val nextLine = openBestTargetDataLine(format)
        val nextBuffer = ByteArrayOutputStream()
        nextLine.open(format)
        nextLine.start()

        line = nextLine
        buffer = nextBuffer
        worker = thread(name = "lyraflow-audio", isDaemon = true) {
            val chunk = ByteArray(4_096)
            while (!Thread.currentThread().isInterrupted) {
                val read = nextLine.read(chunk, 0, chunk.size)
                if (read > 0) {
                    synchronized(nextBuffer) {
                        nextBuffer.write(chunk, 0, read)
                    }
                    onTelemetry(
                        audioLevel(chunk, read),
                        nextBuffer.size().toLong() * 1_000L / BYTES_PER_SECOND,
                    )
                }
            }
        }
    }

    @Synchronized
    fun stop(): ByteArray {
        val currentLine = line ?: return byteArrayOf()
        currentLine.stop()
        worker?.interrupt()
        worker?.join(300)

        val targetBuffer = buffer
        if (targetBuffer != null) {
            val available = currentLine.available()
            if (available > 0) {
                val remaining = ByteArray(available)
                val read = currentLine.read(remaining, 0, available)
                if (read > 0) {
                    synchronized(targetBuffer) {
                        targetBuffer.write(remaining, 0, read)
                    }
                }
            }
        }
        currentLine.close()

        val pcm = buffer?.toByteArray() ?: byteArrayOf()
        line = null
        worker = null
        buffer = null
        return WavEncoder.encodePcm16Mono(pcm)
    }

    private fun openBestTargetDataLine(audioFormat: AudioFormat): TargetDataLine {
        val info = DataLine.Info(TargetDataLine::class.java, audioFormat)
        for (mixerInfo in AudioSystem.getMixerInfo()) {
            val name = mixerInfo.name.lowercase()
            if (name.contains("primary") || name.contains("micrófono") || name.contains("microphone") || name.contains("captura") || name.contains("input")) {
                val mixer = AudioSystem.getMixer(mixerInfo)
                if (mixer.isLineSupported(info)) {
                    val lineResult = runCatching { mixer.getLine(info) as TargetDataLine }.getOrNull()
                    if (lineResult != null) return lineResult
                }
            }
        }
        return AudioSystem.getTargetDataLine(audioFormat)
    }

    private fun audioLevel(bytes: ByteArray, length: Int): Float {
        var sum = 0.0
        var samples = 0
        var index = 0
        while (index + 1 < length) {
            val sample = ((bytes[index + 1].toInt() shl 8) or (bytes[index].toInt() and 0xFF)).toShort().toInt()
            sum += sample.toDouble() * sample
            samples++
            index += 2
        }
        if (samples == 0) return 0f
        return (sqrt(sum / samples) / Short.MAX_VALUE * LEVEL_GAIN).toFloat().coerceIn(0f, 1f)
    }

    private companion object {
        const val BYTES_PER_SECOND = 32_000L
        const val LEVEL_GAIN = 4.5
    }
}
