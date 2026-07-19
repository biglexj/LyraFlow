package com.biglexj.lyraflow

import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.imageio.ImageIO

class LyraFlowTray(
    private val onOpen: () -> Unit,
    private val onExit: () -> Unit,
) : AutoCloseable {
    private val systemTray = SystemTray.getSystemTray()
    private val menu = TrayMenuWindow(
        onOpen = { requestOpen(onOpen) },
        onExit = { requestExit(onExit) },
    )
    private val trayIcon = TrayIcon(loadIcon(), "LyraFlow")

    init {
        trayIcon.isImageAutoSize = true
        trayIcon.addActionListener { requestOpen(onOpen) }
        trayIcon.addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent) {
                when {
                    e.isPopupTrigger || e.button == MouseEvent.BUTTON3 -> {
                        if (menu.isVisible) {
                            menu.hideMenu()
                        } else if (!menu.wasRecentlyHidden()) {
                            menu.showAt(e)
                        }
                    }
                }
            }
        })
        systemTray.add(trayIcon)
    }

    internal fun requestOpen(action: () -> Unit) {
        menu.hideMenu()
        action()
    }

    internal fun requestExit(action: () -> Unit) {
        menu.hideMenu()
        action()
    }

    override fun close() {
        menu.close()
        systemTray.remove(trayIcon)
    }

    private companion object {
        fun loadIcon() = LyraFlowTray::class.java.classLoader
            .getResource("Square44x44Logo.png")
            ?.let(ImageIO::read)
            ?: error("No se encontró el icono de LyraFlow")
    }
}

fun isSystemTraySupported(): Boolean = SystemTray.isSupported()
