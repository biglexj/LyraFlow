package com.biglexj.lyraflow.platform.hotkey

object GlobalShortcutFactory {
    fun create(): GlobalShortcut {
        val os = System.getProperty("os.name").lowercase()
        return if (os.contains("windows")) WindowsGlobalShortcut() else LinuxShortcutProbe()
    }
}
