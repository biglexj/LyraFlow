package com.biglexj.lyraflow.platform.settings

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg

/** Registers the installed launcher for the current Windows user only. */
class WindowsAutoStart {
    fun setEnabled(enabled: Boolean) {
        if (!isWindows()) return
        if (enabled) {
            val launcher = ProcessHandle.current().info().command().orElse(null) ?: return
            if (!launcher.endsWith(".exe", ignoreCase = true)) return
            Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER, RUN_KEY, VALUE_NAME, "\"$launcher\" --minimized")
        } else if (Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, RUN_KEY, VALUE_NAME)) {
            Advapi32Util.registryDeleteValue(WinReg.HKEY_CURRENT_USER, RUN_KEY, VALUE_NAME)
        }
    }

    private fun isWindows() = System.getProperty("os.name").contains("windows", ignoreCase = true)

    private companion object {
        const val RUN_KEY = "Software\\Microsoft\\Windows\\CurrentVersion\\Run"
        const val VALUE_NAME = "LyraFlow"
    }
}
