package com.biglexj.lyraflow.core.hotkey

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

    @Test
    fun cancelDefaultDoesNotCollideWithTheRecordShortcut() {
        assertEquals("Ctrl + Alt + Espacio", KeyboardShortcut.DefaultCancel.label)
        assertFalse(KeyboardShortcut.DefaultCancel.conflictsWith(KeyboardShortcut.Default))
        assertNull(KeyboardShortcut.DefaultCancel.validationError())
    }

    @Test
    fun conflictsWithDetectsIdenticalBindingsAndIgnoresMissingOnes() {
        assertTrue(KeyboardShortcut.Default.conflictsWith(KeyboardShortcut.Default))
        assertFalse(KeyboardShortcut.Default.conflictsWith(null))
        assertFalse(
            KeyboardShortcut.Default.conflictsWith(
                KeyboardShortcut(setOf(ShortcutModifier.Control), ShortcutKey.A),
            ),
        )
    }
}
