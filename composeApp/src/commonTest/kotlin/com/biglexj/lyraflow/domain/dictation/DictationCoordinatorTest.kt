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
}
