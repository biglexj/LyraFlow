package com.biglexj.lyraflow.platform.injection

import java.awt.Robot
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.KeyEvent
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND

class DesktopTextInjector {
    private var targetWindow: HWND? = null

    /** Remembers the text target before the global shortcut changes application state. */
    fun rememberForegroundTarget() {
        if (!isWindows()) return
        val foreground = User32.INSTANCE.GetForegroundWindow() ?: return
        val title = CharArray(256).also { User32.INSTANCE.GetWindowText(foreground, it, it.size) }
            .concatToString().trimEnd('\u0000')
        if (title != "LyraFlow") targetWindow = foreground
    }

    fun inject(text: String) {
        if (text.isBlank()) return
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)

        val robot = Robot().apply { autoDelay = 25 }
        targetWindow?.let { target ->
            User32.INSTANCE.SetForegroundWindow(target)
            robot.delay(120)
        }
        val modifier = if (isMac()) KeyEvent.VK_META else KeyEvent.VK_CONTROL
        robot.keyPress(modifier)
        robot.keyPress(KeyEvent.VK_V)
        robot.keyRelease(KeyEvent.VK_V)
        robot.keyRelease(modifier)
    }

    private fun isMac(): Boolean = System.getProperty("os.name")
        .contains("mac", ignoreCase = true)

    private fun isWindows(): Boolean = System.getProperty("os.name")
        .contains("windows", ignoreCase = true)
}
