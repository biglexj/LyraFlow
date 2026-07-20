package com.biglexj.lyraflow.domain.dictation

import com.biglexj.lyraflow.domain.transcription.TranscriptionProvider
import com.biglexj.lyraflow.domain.transcription.TranscriptionRequest
import com.biglexj.lyraflow.domain.transcription.TranscriptionResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

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
    fun retryOnErrorWorksWithAlternativeProvider() = runTest {
        var count = 0
        val failingProvider = TranscriptionProvider {
            count++
            throw RuntimeException("API Error")
        }
        val coordinator = DictationCoordinator(failingProvider)

        coordinator.process(TranscriptionRequest(byteArrayOf(5, 6, 7)))
        assertIs<DictationState.Failed>(coordinator.state.value)
        assertEquals(1, count)

        val successfulProvider = TranscriptionProvider {
            TranscriptionResult("retry text", "Whisper", "tiny", 42)
        }
        coordinator.retry(successfulProvider)

        val completed = assertIs<DictationState.Completed>(coordinator.state.value)
        assertEquals("retry text", completed.rawText)
        assertEquals("Whisper", completed.provider)
    }
}
