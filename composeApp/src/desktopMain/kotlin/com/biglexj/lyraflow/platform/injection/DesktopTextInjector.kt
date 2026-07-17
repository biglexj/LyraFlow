package com.biglexj.lyraflow.platform.injection

import java.awt.Robot
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.KeyEvent

class DesktopTextInjector {
    fun inject(text: String) {
        if (text.isBlank()) return
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)

        val robot = Robot().apply { autoDelay = 25 }
        val modifier = if (isMac()) KeyEvent.VK_META else KeyEvent.VK_CONTROL
        robot.keyPress(modifier)
        robot.keyPress(KeyEvent.VK_V)
        robot.keyRelease(KeyEvent.VK_V)
        robot.keyRelease(modifier)
    }

    private fun isMac(): Boolean = System.getProperty("os.name")
        .contains("mac", ignoreCase = true)
}
