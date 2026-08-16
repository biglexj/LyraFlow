package com.biglexj.lyraflow.domain.dictation

import com.biglexj.lyraflow.core.model.TranscriptionHistoryEntry
import com.biglexj.lyraflow.data.history.TranscriptionHistoryRepository
import com.biglexj.lyraflow.domain.transcription.QuotaExhaustedException
import com.biglexj.lyraflow.domain.transcription.TranscriptionProvider
import com.biglexj.lyraflow.domain.transcription.TranscriptionRequest
import com.biglexj.lyraflow.domain.transcription.TranscriptionResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DictationCoordinator(
    private val transcriber: TranscriptionProvider,
    private val fallbackTranscriber: () -> TranscriptionProvider? = { null },
    private val historyRepository: TranscriptionHistoryRepository? = null,
    private val isHistoryEnabled: () -> Boolean = { true },
) {
    private val mutableState = MutableStateFlow<DictationState>(DictationState.Idle)
    val state: StateFlow<DictationState> = mutableState.asStateFlow()

    private val mutableGeminiQuotaExhausted = MutableStateFlow(false)
    val geminiQuotaExhausted: StateFlow<Boolean> = mutableGeminiQuotaExhausted.asStateFlow()
    private var quotaExhaustedTimeMark: kotlin.time.TimeMark? = null

    private var lastRequest: TranscriptionRequest? = null

    fun markListening() {
        mutableState.value = DictationState.Listening
    }

    fun reset() {
        mutableState.value = DictationState.Idle
    }

    fun resetQuotaExhausted() {
        mutableGeminiQuotaExhausted.value = false
        quotaExhaustedTimeMark = null
    }

    suspend fun process(request: TranscriptionRequest) {
        lastRequest = request
        processInternal(request, transcriber)
    }

    suspend fun retry(alternativeTranscriber: TranscriptionProvider? = null) {
        val request = lastRequest ?: return
        // Al reintentar manualmente, intentar restaurar la cuota si han transcurrido segundos
        resetQuotaExhausted()
        processInternal(request, alternativeTranscriber ?: transcriber)
    }

    private suspend fun processInternal(request: TranscriptionRequest, provider: TranscriptionProvider) {
        val fallback = fallbackTranscriber()

        // Auto-restaurar estado de cuota de Gemini tras 60 segundos de cooldown
        val timeMark = quotaExhaustedTimeMark
        if (mutableGeminiQuotaExhausted.value && timeMark != null && timeMark.elapsedNow().inWholeSeconds >= 60) {
            mutableGeminiQuotaExhausted.value = false
            quotaExhaustedTimeMark = null
        }

        // Caso 2: Gemini ya fue detectado con cuota agotada previamente dentro de la ventana de enfriamiento
        if (mutableGeminiQuotaExhausted.value) {
            if (fallback != null) {
                transcribeWithFallback(request, fallback, isAutonomous = true)
                return
            } else {
                mutableState.value = DictationState.Failed(
                    "⚠️ Cuota de Gemini agotada temporalmente (espera 1 minuto o instala Whisper local)."
                )
                return
            }
        }

        // Caso 1: Intentos convencionales con el proveedor principal (Gemini)
        val maxGeminiAttempts = 2
        val totalAttempts = if (fallback != null) 3 else 2
        val modelName = request.model.ifBlank { "Modelo seleccionado" }
        var lastThrowable: Throwable? = null

        for (attempt in 1..maxGeminiAttempts) {
            mutableState.value = DictationState.Transcribing(
                model = modelName,
                attempt = attempt,
                maxAttempts = totalAttempts,
            )
            try {
                val result = provider.transcribe(request)
                saveHistoryAndComplete(result)
                return
            } catch (error: CancellationException) {
                throw error
            } catch (error: QuotaExhaustedException) {
                // Caso 2: Gemini falla por cuotas en el intento actual
                mutableGeminiQuotaExhausted.value = true
                quotaExhaustedTimeMark = kotlin.time.TimeSource.Monotonic.markNow()
                if (fallback != null) {
                    transcribeWithFallback(request, fallback, isAutonomous = false)
                    return
                } else {
                    mutableState.value = DictationState.Failed(
                        error.message ?: "⚠️ Cuota de Gemini agotada. Instala Whisper local o actualiza tu API Key."
                    )
                    return
                }
            } catch (error: Throwable) {
                lastThrowable = error
                mutableState.value = DictationState.AttemptFailed(
                    attempt = attempt,
                    maxAttempts = totalAttempts,
                )
                if (attempt < maxGeminiAttempts) {
                    delay(2000L)
                }
            }
        }

        // Caso 1 (3er intento): Si los 2 intentos con Gemini fallaron por motivo no-cuota y Whisper está disponible
        if (fallback != null) {
            transcribeWithFallback(request, fallback, isAutonomous = false, attempt = 3, totalAttempts = 3)
            return
        }

        val finalError = lastThrowable?.message ?: "Error desconocido"
        mutableState.value = DictationState.Failed(finalError)
    }

    private suspend fun transcribeWithFallback(
        request: TranscriptionRequest,
        fallback: TranscriptionProvider,
        isAutonomous: Boolean,
        attempt: Int = 1,
        totalAttempts: Int = 1,
    ) {
        val modelLabel = if (isAutonomous) "Whisper local (Autónomo)" else "Whisper local"
        mutableState.value = DictationState.Transcribing(
            model = modelLabel,
            attempt = attempt,
            maxAttempts = totalAttempts,
        )
        try {
            val result = fallback.transcribe(request)
            saveHistoryAndComplete(result)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            val failureMsg = if (mutableGeminiQuotaExhausted.value) {
                "⚠️ Cuota de Gemini agotada y Whisper local falló: ${error.message}"
            } else {
                "Error en Gemini y Whisper local: ${error.message}"
            }
            mutableState.value = DictationState.Failed(failureMsg)
        }
    }

    private suspend fun saveHistoryAndComplete(result: TranscriptionResult) {
        if (result.rawText.isNotBlank() && isHistoryEnabled()) {
            historyRepository?.saveEntry(
                TranscriptionHistoryEntry(
                    id = (System.currentTimeMillis().toString() + "-" + (1000..9999).random()),
                    timestampMs = System.currentTimeMillis(),
                    rawTranscript = result.rawText,
                    refinedText = result.rawText,
                    providerName = result.provider,
                    audioDurationMs = result.elapsedMillis
                )
            )
        }
        mutableState.value = DictationState.Completed(
            rawText = result.rawText,
            refinedText = result.rawText,
            provider = result.provider,
            elapsedMillis = result.elapsedMillis,
        )
    }
}
