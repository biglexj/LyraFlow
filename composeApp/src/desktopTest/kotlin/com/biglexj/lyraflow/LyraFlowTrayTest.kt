package com.biglexj.lyraflow

import java.awt.event.ActionEvent
import java.awt.SystemTray
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LyraFlowTrayTest {
    @Test
    fun trayCanOpenAndExitWithoutClosingTheProcess() {
        if (!SystemTray.isSupported()) return

        var opened = false
        var exited = false
        val existingIcons = SystemTray.getSystemTray().trayIcons.toSet()
        val tray = LyraFlowTray(
            onOpen = { opened = true },
            onExit = { exited = true },
        )
        val icon = (SystemTray.getSystemTray().trayIcons.toSet() - existingIcons).single()

        icon.actionListeners.forEach {
            it.actionPerformed(ActionEvent(icon, ActionEvent.ACTION_PERFORMED, "open"))
        }
        icon.popupMenu.getItem(2).actionListeners.forEach {
            it.actionPerformed(ActionEvent(icon, ActionEvent.ACTION_PERFORMED, "exit"))
        }

        assertTrue(opened)
        assertTrue(exited)
        tray.close()
        assertFalse(SystemTray.getSystemTray().trayIcons.contains(icon))
    }
}
