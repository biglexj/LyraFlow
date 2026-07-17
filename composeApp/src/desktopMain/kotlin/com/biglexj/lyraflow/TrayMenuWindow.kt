package com.biglexj.lyraflow

import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GraphicsEnvironment
import java.awt.GridLayout
import java.awt.MouseInfo
import java.awt.RenderingHints
import java.awt.Window
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.geom.RoundRectangle2D
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JWindow
import javax.swing.border.EmptyBorder

class TrayMenuWindow(onOpen: () -> Unit, onExit: () -> Unit) : JWindow() {
    init {
        type = Window.Type.POPUP
        isAlwaysOnTop = true
        focusableWindowState = true
        background = Color(0, 0, 0, 0)
        size = Dimension(224, 108)
        shape = RoundRectangle2D.Float(0f, 0f, width.toFloat(), height.toFloat(), 18f, 18f)
        contentPane = RoundedMenuPanel().apply {
            layout = GridLayout(2, 1, 0, 5)
            border = EmptyBorder(9, 9, 9, 9)
            add(TrayMenuButton("Abrir LyraFlow", true, onOpen))
            add(TrayMenuButton("Salir completamente", false, onExit))
        }
        addWindowFocusListener(object : WindowAdapter() {
            override fun windowLostFocus(event: WindowEvent) = hideMenu()
        })
    }

    fun showAtPointer() {
        val pointer = MouseInfo.getPointerInfo()?.location ?: return
        val area = GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
        val x = (pointer.x - width + 18).coerceIn(area.x, area.x + area.width - width)
        val y = (pointer.y - height - 8).coerceIn(area.y, area.y + area.height - height)
        setLocation(x, y)
        isVisible = true
        toFront()
        requestFocus()
    }

    fun hideMenu() {
        isVisible = false
    }
}

private class RoundedMenuPanel : JPanel() {
    init {
        isOpaque = false
    }

    override fun paintComponent(graphics: Graphics) {
        val canvas = graphics.create() as Graphics2D
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        canvas.color = Color(0x17, 0x1B, 0x20)
        canvas.fillRoundRect(0, 0, width - 1, height - 1, 18, 18)
        canvas.color = Color(0x1E, 0x8F, 0x8B)
        canvas.drawRoundRect(0, 0, width - 1, height - 1, 18, 18)
        canvas.dispose()
        super.paintComponent(graphics)
    }
}

private class TrayMenuButton(text: String, accent: Boolean, action: () -> Unit) : JButton(text) {
    private val resting = if (accent) Color(0x00, 0x6A, 0x68) else Color(0x17, 0x1B, 0x20)
    private val hover = if (accent) Color(0x00, 0x84, 0x80) else Color(0x25, 0x3A, 0x39)

    init {
        foreground = Color.WHITE
        font = font.deriveFont(13f)
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        border = EmptyBorder(0, 14, 0, 14)
        isContentAreaFilled = false
        isFocusPainted = false
        horizontalAlignment = LEFT
        addActionListener { action() }
    }

    override fun paintComponent(graphics: Graphics) {
        val canvas = graphics.create() as Graphics2D
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        canvas.color = if (model.isRollover || model.isPressed) hover else resting
        canvas.fillRoundRect(0, 0, width, height, 12, 12)
        canvas.dispose()
        super.paintComponent(graphics)
    }
}
