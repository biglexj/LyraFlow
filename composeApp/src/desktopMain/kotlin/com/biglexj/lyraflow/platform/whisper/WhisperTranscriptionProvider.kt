package com.biglexj.lyraflow.platform.whisper

import com.biglexj.lyraflow.domain.transcription.TranscriptionProvider
import com.biglexj.lyraflow.domain.transcription.TranscriptionRequest
import com.biglexj.lyraflow.domain.transcription.TranscriptionResult
import com.biglexj.lyraflow.core.config.WhisperModel
import java.nio.file.Files
import kotlin.time.TimeSource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.biglexj.lyraflow.core.config.WhisperLanguage

class WhisperTranscriptionProvider(
    private val sidecar: WhisperSidecar = WhisperSidecar(),
    private val currentModel: () -> WhisperModel?,
    private val whisperLanguage: () -> WhisperLanguage = { WhisperLanguage.Auto }
) : TranscriptionProvider {

    override suspend fun transcribe(request: TranscriptionRequest): TranscriptionResult = withContext(Dispatchers.IO) {
        val model = requireNotNull(currentModel()) { "No hay un modelo de Whisper seleccionado o instalado." }
        val modelPath = WhisperPaths.model(model)
        require(Files.isRegularFile(modelPath)) { "El archivo del modelo de Whisper no existe: ${modelPath.toAbsolutePath()}" }

        val started = TimeSource.Monotonic.markNow()
        val tempWav = Files.createTempFile("lyraflow-whisper-input-", ".wav")
        try {
            Files.write(tempWav, request.audio)
            val lang = whisperLanguage()
            val text = sidecar.transcribe(tempWav, modelPath, lang.code)

            return@withContext TranscriptionResult(
                rawText = text,
                provider = "Whisper local",
                model = model.label,
                elapsedMillis = started.elapsedNow().inWholeMilliseconds,
            )
        } finally {
            Files.deleteIfExists(tempWav)
        }
    }
}
