package com.biglexj.lyraflow

import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.TrayIcon
import javax.imageio.ImageIO

class LyraFlowTray(
    onOpen: () -> Unit,
    onExit: () -> Unit,
) : AutoCloseable {
    private val systemTray = SystemTray.getSystemTray()
    private val trayIcon = TrayIcon(loadIcon(), "LyraFlow", createMenu(onOpen, onExit))

    init {
        trayIcon.isImageAutoSize = true
        trayIcon.addActionListener { onOpen() }
        systemTray.add(trayIcon)
    }

    override fun close() {
        systemTray.remove(trayIcon)
    }

    private companion object {
        fun loadIcon() = LyraFlowTray::class.java.classLoader
            .getResource("Square44x44Logo.png")
            ?.let(ImageIO::read)
            ?: error("No se encontró el icono de LyraFlow")

        fun createMenu(onOpen: () -> Unit, onExit: () -> Unit) = PopupMenu().apply {
            add(MenuItem("Abrir LyraFlow").apply { addActionListener { onOpen() } })
            addSeparator()
            add(MenuItem("Salir").apply { addActionListener { onExit() } })
        }
    }
}

fun isSystemTraySupported(): Boolean = SystemTray.isSupported()
