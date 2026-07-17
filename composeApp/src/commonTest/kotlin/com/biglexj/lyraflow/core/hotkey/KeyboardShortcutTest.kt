package com.biglexj.lyraflow.core.hotkey

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class KeyboardShortcutTest {
    @Test
    fun defaultUsesControlSpace() {
        assertEquals("Ctrl + Espacio", KeyboardShortcut.Default.label)
        assertNull(KeyboardShortcut.Default.validationError())
    }

    @Test
    fun windowsSpaceIsRejected() {
        val shortcut = KeyboardShortcut(setOf(ShortcutModifier.Windows), ShortcutKey.Space)

        assertNotNull(shortcut.validationError())
    }

    @Test
    fun twoAndThreeKeyShortcutsAreAccepted() {
        assertNull(
            KeyboardShortcut(setOf(ShortcutModifier.Control), ShortcutKey.Space).validationError(),
        )
        assertNull(
            KeyboardShortcut(
                setOf(ShortcutModifier.Control, ShortcutModifier.Windows),
                ShortcutKey.K,
            ).validationError(),
        )
    }
}
