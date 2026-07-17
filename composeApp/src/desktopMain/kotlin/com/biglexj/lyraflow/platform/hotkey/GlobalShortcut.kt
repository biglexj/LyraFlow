package com.biglexj.lyraflow.platform.hotkey

import com.biglexj.lyraflow.core.hotkey.KeyboardShortcut

interface GlobalShortcut : AutoCloseable {
    val status: String
    fun start(shortcut: KeyboardShortcut, onActivated: () -> Unit)
}
