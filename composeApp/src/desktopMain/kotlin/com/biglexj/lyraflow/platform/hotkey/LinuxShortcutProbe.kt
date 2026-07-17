package com.biglexj.lyraflow.platform.hotkey

import com.biglexj.lyraflow.core.hotkey.KeyboardShortcut

class LinuxShortcutProbe : GlobalShortcut {
    private val session = System.getenv("XDG_SESSION_TYPE")?.lowercase().orEmpty()
    override val status: String = when (session) {
        "wayland" -> "Wayland: requiere portal XDG GlobalShortcuts"
        "x11" -> "X11: adaptador JNA pendiente"
        else -> "Linux: sesión gráfica no identificada"
    }

    override fun start(shortcut: KeyboardShortcut, onActivated: () -> Unit) = Unit
    override fun close() = Unit
}
