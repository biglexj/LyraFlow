package com.biglexj.lyraflow.core.update

import com.biglexj.lyraflow.feature.update.sanitizeReleaseNotes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AutoDownloaderTest {
    @Test
    fun testSanitizeReleaseNotesRemovesRawMarkdown() {
        val rawBody = """
            # Versión 1.2.0
            **Novedades principales:**
            - Mejora en la velocidad de dictado
            - *Corrección* de bugs de red
            - `Fix`: Puntuación automática
        """.trimIndent()

        val clean = sanitizeReleaseNotes(rawBody)

        assertFalse(clean.contains("#"))
        assertFalse(clean.contains("**"))
        assertFalse(clean.contains("`"))
        assertTrue(clean.contains("• Mejora en la velocidad de dictado"))
        assertTrue(clean.contains("• Corrección de bugs de red"))
    }

    @Test
    fun testIsNewerVersionComparison() {
        assertTrue(UpdateChecker.isNewerVersion("1.1.2", "1.1.3"))
        assertTrue(UpdateChecker.isNewerVersion("1.1.2", "1.2.0"))
        assertTrue(UpdateChecker.isNewerVersion("1.1.2", "2.0.0"))
        assertFalse(UpdateChecker.isNewerVersion("1.1.2", "1.1.2"))
        assertFalse(UpdateChecker.isNewerVersion("1.1.2", "1.1.1"))
    }
}
