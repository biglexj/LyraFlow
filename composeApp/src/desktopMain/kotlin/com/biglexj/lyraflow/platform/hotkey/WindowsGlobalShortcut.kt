package com.biglexj.lyraflow.platform.hotkey

import com.biglexj.lyraflow.core.hotkey.KeyboardShortcut
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.LPARAM
import com.sun.jna.platform.win32.WinDef.WPARAM
import com.sun.jna.platform.win32.WinUser
import kotlin.concurrent.thread

class WindowsGlobalShortcut : GlobalShortcut {
    private val hotkeyId = 0x4C59
    private var worker: Thread? = null
    @Volatile private var threadId: Int = 0

    override var status: String = "Windows: pendiente"
        private set

    override fun start(shortcut: KeyboardShortcut, onActivated: () -> Unit) {
        if (worker != null) return
        worker = thread(name = "lyraflow-hotkey", isDaemon = true) {
            threadId = Kernel32.INSTANCE.GetCurrentThreadId()
            val modifiers = shortcut.modifiers.fold(WinUser.MOD_NOREPEAT) { flags, modifier ->
                flags or modifier.windowsFlag
            }
            if (!User32.INSTANCE.RegisterHotKey(null, hotkeyId, modifiers, shortcut.key.windowsCode)) {
                status = "Windows: ${shortcut.label} no está disponible"
                return@thread
            }

            status = "Windows: ${shortcut.label} activo"
            val message = WinUser.MSG()
            while (User32.INSTANCE.GetMessage(message, null, 0, 0) > 0) {
                if (message.message == WinUser.WM_HOTKEY && message.wParam.toInt() == hotkeyId) {
                    onActivated()
                }
            }
            User32.INSTANCE.UnregisterHotKey(null, hotkeyId)
        }
    }

    override fun close() {
        val current = worker ?: return
        if (threadId != 0) {
            User32.INSTANCE.PostThreadMessage(threadId, WinUser.WM_QUIT, WPARAM(0), LPARAM(0))
        }
        current.join(1_000)
        current.interrupt()
        worker = null
        threadId = 0
    }
}
