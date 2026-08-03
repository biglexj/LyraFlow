package com.biglexj.lyraflow.domain.dictation

import com.biglexj.lyraflow.domain.transcription.QuotaExhaustedException
import com.biglexj.lyraflow.domain.transcription.TranscriptionProvider
import com.biglexj.lyraflow.domain.transcription.TranscriptionRequest
import com.biglexj.lyraflow.domain.transcription.TranscriptionResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DictationCoordinatorTest {
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
    fun fallsBackToWhisperOnThirdAttemptWhenGeminiFailsTwiceNonQuota() = runTest {
        var geminiAttempts = 0
        var whisperAttempts = 0

        val geminiFailingProvider = TranscriptionProvider {
            geminiAttempts++
            throw RuntimeException("Error de conexion no-cuota")
        }
        val whisperProvider = TranscriptionProvider {
            whisperAttempts++
            TranscriptionResult("transcrito por whisper local", "Whisper local", "base", 120)
        }

        val coordinator = DictationCoordinator(
            transcriber = geminiFailingProvider,
            fallbackTranscriber = { whisperProvider }
        )

        coordinator.process(TranscriptionRequest(byteArrayOf(1, 2, 3)))

        val completed = assertIs<DictationState.Completed>(coordinator.state.value)
        assertEquals("transcrito por whisper local", completed.rawText)
        assertEquals("Whisper local", completed.provider)
        assertEquals(2, geminiAttempts)
        assertEquals(1, whisperAttempts)
        assertFalse(coordinator.geminiQuotaExhausted.value)
    }

    @Test
    fun immediatelyFallsBackToWhisperAndSetsQuotaExhaustedOnQuotaError() = runTest {
        var geminiAttempts = 0
        var whisperAttempts = 0

        val geminiQuotaProvider = TranscriptionProvider {
            geminiAttempts++
            throw QuotaExhaustedException("Cuota de Gemini agotada (HTTP 429)")
        }
        val whisperProvider = TranscriptionProvider {
            whisperAttempts++
            TranscriptionResult("transcrito por whisper tras cuota agotada", "Whisper local", "base", 80)
        }

        val coordinator = DictationCoordinator(
            transcriber = geminiQuotaProvider,
            fallbackTranscriber = { whisperProvider }
        )

        coordinator.process(TranscriptionRequest(byteArrayOf(1, 2, 3)))

        val completed = assertIs<DictationState.Completed>(coordinator.state.value)
        assertEquals("transcrito por whisper tras cuota agotada", completed.rawText)
        assertEquals(1, geminiAttempts)
        assertEquals(1, whisperAttempts)
        assertTrue(coordinator.geminiQuotaExhausted.value)

        // Siguiente dictado debe ir directamente a Whisper sin intentar Gemini
        coordinator.process(TranscriptionRequest(byteArrayOf(4, 5, 6)))

        val secondCompleted = assertIs<DictationState.Completed>(coordinator.state.value)
        assertEquals("transcrito por whisper tras cuota agotada", secondCompleted.rawText)
        assertEquals(1, geminiAttempts) // Permanece en 1, Gemini no fue llamado de nuevo
        assertEquals(2, whisperAttempts)
    }

    @Test
    fun resetQuotaExhaustedAllowsRetryingGeminiAgain() = runTest {
        var geminiCalls = 0
        val geminiQuotaProvider = TranscriptionProvider {
            geminiCalls++
            if (geminiCalls == 1) {
                throw QuotaExhaustedException("429")
            }
            TranscriptionResult("Gemini recuperado", "Gemini", "gemini-3.6-flash", 90)
        }
        val whisperProvider = TranscriptionProvider {
            TranscriptionResult("Whisper fallback", "Whisper local", "base", 50)
        }

        val coordinator = DictationCoordinator(
            transcriber = geminiQuotaProvider,
            fallbackTranscriber = { whisperProvider }
        )

        // Primer intento da error de cuota -> Whisper
        coordinator.process(TranscriptionRequest(byteArrayOf(1, 2, 3)))
        assertTrue(coordinator.geminiQuotaExhausted.value)

        // Reseteamos cuota
        coordinator.resetQuotaExhausted()
        assertFalse(coordinator.geminiQuotaExhausted.value)

        // Segundo dictado debe volver a intentar Gemini y esta vez tener éxito
        coordinator.process(TranscriptionRequest(byteArrayOf(1, 2, 3)))
        val completed = assertIs<DictationState.Completed>(coordinator.state.value)
        assertEquals("Gemini recuperado", completed.rawText)
        assertEquals("Gemini", completed.provider)
        assertEquals(2, geminiCalls)
    }
}
