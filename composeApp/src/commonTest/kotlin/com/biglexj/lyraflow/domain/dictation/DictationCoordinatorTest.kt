package com.biglexj.lyraflow.domain.dictation

import com.biglexj.lyraflow.core.model.AiProvider
import com.biglexj.lyraflow.domain.transcription.QuotaExhaustedException
import com.biglexj.lyraflow.domain.transcription.TranscriptionProvider
import com.biglexj.lyraflow.domain.transcription.TranscriptionRequest
import com.biglexj.lyraflow.domain.transcription.TranscriptionResult
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DictationCoordinatorTest {
    @Test
    fun completesDictationWhenHistoryCannotBeSaved() = runTest {
        val history = object : com.biglexj.lyraflow.data.history.TranscriptionHistoryRepository by
            com.biglexj.lyraflow.data.history.InMemoryTranscriptionHistoryRepository() {
            override suspend fun saveEntry(entry: com.biglexj.lyraflow.core.model.TranscriptionHistoryEntry) {
                throw IllegalStateException("Disk unavailable")
            }
        }
        val provider = TranscriptionProvider { TranscriptionResult("texto recuperable", "Fake", "fake", 12) }
        val coordinator = DictationCoordinator(provider, historyRepository = history)
        coordinator.process(TranscriptionRequest(byteArrayOf(1)))
        assertEquals("texto recuperable", assertIs<DictationState.Completed>(coordinator.state.value).rawText)
    }

    @Test
    fun preservesRawTranscript() = runTest {
        val provider = TranscriptionProvider {
            TranscriptionResult("punto por punto", "Fake", "fake", 12)
        }
        val coordinator = DictationCoordinator(provider)

        coordinator.process(TranscriptionRequest(byteArrayOf(1, 2, 3)))

        val completed = assertIs<DictationState.Completed>(coordinator.state.value)
        assertEquals("punto por punto", completed.rawText)
        assertEquals(completed.rawText, completed.refinedText)
    }

    @Test
    fun autoRetriesOnFirstFailureAndSucceedsOnSecondAttempt() = runTest {
        var count = 0
        val flakyProvider = TranscriptionProvider {
            count++
            if (count == 1) {
                throw RuntimeException("Temporary network glitch")
            }
            TranscriptionResult("exito al segundo intento", "Gemini", "gemini-3.6-flash", 100)
        }
        val coordinator = DictationCoordinator(flakyProvider)

        coordinator.process(TranscriptionRequest(byteArrayOf(1, 2, 3)))

        val completed = assertIs<DictationState.Completed>(coordinator.state.value)
        assertEquals("exito al segundo intento", completed.rawText)
        assertEquals(2, count)
    }

    @Test
    fun fallsBackToWhisperOnThirdAttemptWhenTheProviderFailsTwiceNonQuota() = runTest {
        var providerAttempts = 0
        var whisperAttempts = 0

        val failingProvider = TranscriptionProvider {
            providerAttempts++
            throw RuntimeException("Error de conexion no-cuota")
        }
        val whisperProvider = TranscriptionProvider {
            whisperAttempts++
            TranscriptionResult("transcrito por whisper local", "Whisper local", "base", 120)
        }

        val coordinator = DictationCoordinator(
            transcriber = failingProvider,
            fallbackTranscriber = { whisperProvider }
        )

        coordinator.process(TranscriptionRequest(byteArrayOf(1, 2, 3)))

        val completed = assertIs<DictationState.Completed>(coordinator.state.value)
        assertEquals("transcrito por whisper local", completed.rawText)
        assertEquals("Whisper local", completed.provider)
        assertEquals(2, providerAttempts)
        assertEquals(1, whisperAttempts)
        assertNull(coordinator.quotaExhaustion.value)
    }

    @Test
    fun immediatelyFallsBackToWhisperAndSetsQuotaExhaustedOnQuotaError() = runTest {
        var providerAttempts = 0
        var whisperAttempts = 0

        val quotaProvider = TranscriptionProvider {
            providerAttempts++
            throw QuotaExhaustedException("Cuota agotada (HTTP 429)")
        }
        val whisperProvider = TranscriptionProvider {
            whisperAttempts++
            TranscriptionResult("transcrito por whisper tras cuota agotada", "Whisper local", "base", 80)
        }

        val coordinator = DictationCoordinator(
            transcriber = quotaProvider,
            fallbackTranscriber = { whisperProvider }
        )

        coordinator.process(TranscriptionRequest(byteArrayOf(1, 2, 3)))

        val completed = assertIs<DictationState.Completed>(coordinator.state.value)
        assertEquals("transcrito por whisper tras cuota agotada", completed.rawText)
        assertEquals(1, providerAttempts)
        assertEquals(1, whisperAttempts)
        assertNotNull(coordinator.quotaExhaustion.value)

        // Siguiente dictado con la misma seleccion debe ir directo a Whisper sin reintentar la nube
        coordinator.process(TranscriptionRequest(byteArrayOf(4, 5, 6)))

        val secondCompleted = assertIs<DictationState.Completed>(coordinator.state.value)
        assertEquals("transcrito por whisper tras cuota agotada", secondCompleted.rawText)
        assertEquals(1, providerAttempts) // Permanece en 1, la nube no fue llamada de nuevo
        assertEquals(2, whisperAttempts)
    }

    @Test
    fun resetQuotaExhaustedAllowsRetryingTheProviderAgain() = runTest {
        var providerCalls = 0
        val quotaProvider = TranscriptionProvider {
            providerCalls++
            if (providerCalls == 1) {
                throw QuotaExhaustedException("429")
            }
            TranscriptionResult("proveedor recuperado", "Gemini", "gemini-3.6-flash", 90)
        }
        val whisperProvider = TranscriptionProvider {
            TranscriptionResult("Whisper fallback", "Whisper local", "base", 50)
        }

        val coordinator = DictationCoordinator(
            transcriber = quotaProvider,
            fallbackTranscriber = { whisperProvider }
        )

        // Primer intento da error de cuota -> Whisper
        coordinator.process(TranscriptionRequest(byteArrayOf(1, 2, 3)))
        assertNotNull(coordinator.quotaExhaustion.value)

        // Reseteamos cuota
        coordinator.resetQuotaExhausted()
        assertNull(coordinator.quotaExhaustion.value)

        // Segundo dictado debe volver a intentar el proveedor y esta vez tener éxito
        coordinator.process(TranscriptionRequest(byteArrayOf(1, 2, 3)))
        val completed = assertIs<DictationState.Completed>(coordinator.state.value)
        assertEquals("proveedor recuperado", completed.rawText)
        assertEquals("Gemini", completed.provider)
        assertEquals(2, providerCalls)
    }

    @Test
    fun namesTheSelectedProviderInTheQuotaMessage() = runTest {
        val miniMaxProvider = TranscriptionProvider {
            throw QuotaExhaustedException("Has alcanzado el limite de uso o saldo de MiniMax.")
        }
        val coordinator = DictationCoordinator(transcriber = miniMaxProvider)

        coordinator.process(
            TranscriptionRequest(
                audio = byteArrayOf(1, 2, 3),
                model = "asr-1.0",
                provider = AiProvider.MiniMax,
            )
        )

        val failed = assertIs<DictationState.Failed>(coordinator.state.value)
        assertTrue(failed.message.contains("MiniMax"), failed.message)
        assertTrue(failed.message.contains("asr-1.0"), failed.message)
        assertFalse(failed.message.contains("Gemini"), failed.message)
    }

    @Test
    fun quotaOfOneProviderDoesNotBlockAnotherProvider() = runTest {
        var attempts = 0
        val provider = TranscriptionProvider {
            attempts++
            if (attempts == 1) {
                throw QuotaExhaustedException("Cuota de Gemini agotada")
            }
            TranscriptionResult("MiniMax respondio bien", "MiniMax", "asr-1.0", 70)
        }
        val coordinator = DictationCoordinator(transcriber = provider)

        coordinator.process(
            TranscriptionRequest(audio = byteArrayOf(1), model = "gemini-3.5-flash", provider = AiProvider.Gemini)
        )
        assertNotNull(coordinator.quotaExhaustion.value)

        // Cambiar de proveedor debe volver a intentar la transcripcion en la nube
        coordinator.process(
            TranscriptionRequest(audio = byteArrayOf(2), model = "asr-1.0", provider = AiProvider.MiniMax)
        )

        val completed = assertIs<DictationState.Completed>(coordinator.state.value)
        assertEquals("MiniMax respondio bien", completed.rawText)
        assertEquals(2, attempts)
    }

    @Test
    fun quotaOfOneModelDoesNotBlockAnotherModelOfTheSameProvider() = runTest {
        var attempts = 0
        val provider = TranscriptionProvider {
            attempts++
            if (attempts == 1) {
                throw QuotaExhaustedException("Cuota agotada para este modelo")
            }
            TranscriptionResult("el modelo nuevo respondio", "Gemini", "gemini-3.1-flash-lite", 70)
        }
        val coordinator = DictationCoordinator(transcriber = provider)

        coordinator.process(
            TranscriptionRequest(audio = byteArrayOf(1), model = "gemini-3.5-flash", provider = AiProvider.Gemini)
        )
        assertNotNull(coordinator.quotaExhaustion.value)

        // Cambiar de modelo debe volver a intentar la transcripcion en la nube
        coordinator.process(
            TranscriptionRequest(audio = byteArrayOf(2), model = "gemini-3.1-flash-lite", provider = AiProvider.Gemini)
        )

        val completed = assertIs<DictationState.Completed>(coordinator.state.value)
        assertEquals("el modelo nuevo respondio", completed.rawText)
        assertEquals(2, attempts)
    }

    @Test
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun doesNotRemainTranscribingAfterACancelledRequest() = runTest {
        val hangingProvider = TranscriptionProvider {
            awaitCancellation()
            TranscriptionResult("", "", "", 0)
        }
        val coordinator = DictationCoordinator(transcriber = hangingProvider)

        val job = launch { coordinator.process(TranscriptionRequest(byteArrayOf(1, 2, 3))) }
        advanceUntilIdle()
        assertIs<DictationState.Transcribing>(coordinator.state.value)

        job.cancelAndJoin()

        // Un estado atascado en Transcribing dejaria el atajo de dictado inutilizable
        assertFalse(coordinator.state.value is DictationState.Transcribing)
    }
}
