package com.biglexj.lyraflow.domain.dictation

import com.biglexj.lyraflow.core.model.TranscriptionHistoryEntry
import com.biglexj.lyraflow.data.history.TranscriptionHistoryRepository
import com.biglexj.lyraflow.domain.transcription.QuotaExhaustedException
import com.biglexj.lyraflow.domain.transcription.QuotaExhaustion
import com.biglexj.lyraflow.domain.transcription.TranscriptionProvider
import com.biglexj.lyraflow.domain.transcription.TranscriptionRequest
import com.biglexj.lyraflow.domain.transcription.TranscriptionResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.TimeMark
import kotlin.time.TimeSource

class DictationCoordinator(
    private val transcriber: TranscriptionProvider,
    private val fallbackTranscriber: () -> TranscriptionProvider? = { null },
    private val historyRepository: TranscriptionHistoryRepository? = null,
    private val isHistoryEnabled: () -> Boolean = { true },
) {
    private val mutableState = MutableStateFlow<DictationState>(DictationState.Idle)
    val state: StateFlow<DictationState> = mutableState.asStateFlow()

    private val mutableQuotaExhaustion = MutableStateFlow<QuotaExhaustion?>(null)
    val quotaExhaustion: StateFlow<QuotaExhaustion?> = mutableQuotaExhaustion.asStateFlow()
    private var quotaExhaustedTimeMark: TimeMark? = null

    private var lastRequest: TranscriptionRequest? = null

    fun markListening() {
        mutableState.value = DictationState.Listening
    }

    fun reset() {
        mutableState.value = DictationState.Idle
    }

    fun resetQuotaExhausted() {
        mutableQuotaExhaustion.value = null
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

        // La cuota agotada solo bloquea al proveedor y al modelo que la consumieron:
        // cambiar de proveedor o de modelo vuelve a intentar la transcripcion en la nube.
        val activeQuota = activeQuotaFor(request)
        if (activeQuota != null) {
            if (fallback != null) {
                transcribeWithFallback(request, fallback, isAutonomous = true, quotaSource = activeQuota.source)
                return
            } else {
                mutableState.value = DictationState.Failed(
                    "⚠️ Cuota de ${activeQuota.source} agotada. ${activeQuota.detail()}"
                )
                return
            }
        }

        // Caso 1: Intentos convencionales con el proveedor principal
        val maxProviderAttempts = 2
        val totalAttempts = if (fallback != null) 3 else 2
        val modelName = request.model.ifBlank { "Modelo seleccionado" }
        var lastThrowable: Throwable? = null

        for (attempt in 1..maxProviderAttempts) {
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
                // Una peticion cancelada no puede dejar el estado en Transcribing:
                // ese estado bloquea el atajo de dictado de forma permanente.
                mutableState.value = DictationState.Idle
                throw error
            } catch (error: QuotaExhaustedException) {
                // Caso 2: el proveedor falla por cuota en el intento actual
                val quota = QuotaExhaustion(
                    provider = request.provider,
                    model = request.model,
                    message = error.message ?: "Se agotó la cuota de ${request.provider.label}.",
                )
                mutableQuotaExhaustion.value = quota
                quotaExhaustedTimeMark = TimeSource.Monotonic.markNow()
                if (fallback != null) {
                    transcribeWithFallback(request, fallback, isAutonomous = false, quotaSource = quota.source)
                    return
                } else {
                    mutableState.value = DictationState.Failed("⚠️ Cuota de ${quota.source} agotada. ${quota.detail()}")
                    return
                }
            } catch (error: Throwable) {
                lastThrowable = error
                mutableState.value = DictationState.AttemptFailed(
                    attempt = attempt,
                    maxAttempts = totalAttempts,
                )
                if (attempt < maxProviderAttempts) {
                    delay(2000L)
                }
            }
        }

        // Caso 1 (3er intento): Si los 2 intentos fallaron por motivo no-cuota y Whisper está disponible
        if (fallback != null) {
            transcribeWithFallback(request, fallback, isAutonomous = false, attempt = 3, totalAttempts = 3)
            return
        }

        val finalError = lastThrowable?.message ?: "Error desconocido"
        mutableState.value = DictationState.Failed(finalError)
    }

    /**
     * Devuelve la cuota vigente para la peticion cuando sigue vigente: mismo
     * proveedor, mismo modelo y dentro de la ventana de enfriamiento.
     */
    private fun activeQuotaFor(request: TranscriptionRequest): QuotaExhaustion? {
        val quota = mutableQuotaExhaustion.value ?: return null
        if (quota.provider != request.provider || quota.model != request.model) return null
        val timeMark = quotaExhaustedTimeMark ?: return null
        if (timeMark.elapsedNow().inWholeSeconds < COOLDOWN_SECONDS) return quota
        // Enfriamiento vencido: la cuota se libera y el proveedor vuelve a ser usado.
        resetQuotaExhausted()
        return null
    }

    private suspend fun transcribeWithFallback(
        request: TranscriptionRequest,
        fallback: TranscriptionProvider,
        isAutonomous: Boolean,
        attempt: Int = 1,
        totalAttempts: Int = 1,
        quotaSource: String? = null,
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
            mutableState.value = DictationState.Idle
            throw error
        } catch (error: Throwable) {
            val failureMsg = if (quotaSource != null) {
                "⚠️ Cuota de $quotaSource agotada y Whisper local falló: ${error.message}"
            } else {
                "Error en ${request.provider.label} y Whisper local: ${error.message}"
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

    private companion object {
        const val COOLDOWN_SECONDS = 60L
    }
}

/** Detalle del aviso de cuota sin repetir el simbolo de advertencia del proveedor. */
private fun QuotaExhaustion.detail(): String =
    message.removePrefix("⚠️").trim().ifBlank { "Espera un momento o cambia de modelo." }
