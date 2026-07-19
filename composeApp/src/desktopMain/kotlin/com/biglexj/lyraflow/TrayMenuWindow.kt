package com.biglexj.lyraflow

import java.awt.AWTEvent
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.RenderingHints
import java.awt.Toolkit
import java.awt.event.AWTEventListener
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import javax.swing.AbstractAction
import javax.swing.BoxLayout
import javax.swing.JComponent
import java.awt.Window
import javax.swing.JDialog
import javax.swing.JPanel
import javax.swing.JSeparator
import javax.swing.KeyStroke
import javax.swing.border.EmptyBorder

enum class TrayIconType { APP, EXIT }

class TrayMenuWindow(
    private val onOpen: () -> Unit,
    private val onExit: () -> Unit,
) : JDialog(null as java.awt.Window?), AutoCloseable {

    private val itemWidth = 150
    private val itemHeight = 32

    private val globalMouseListener = AWTEventListener { event ->
        if (event is MouseEvent && event.id == MouseEvent.MOUSE_PRESSED) {
            if (isVisible) {
                val point = event.locationOnScreen
                if (!bounds.contains(point)) {
                    hideMenu()
                }
            }
        }
    }

    init {
        isUndecorated = true
        type = Window.Type.POPUP
        isAlwaysOnTop = true
        background = Color(0, 0, 0, 0)
        setFocusableWindowState(true)

        // 1. Auto-dismiss when window loses focus
        addWindowFocusListener(object : WindowFocusListener {
            override fun windowGainedFocus(e: WindowEvent) {}
            override fun windowLostFocus(e: WindowEvent) = hideMenu()
        })

        // 2. Global mouse listener to guarantee closing when clicking anywhere outside
        runCatching {
            Toolkit.getDefaultToolkit().addAWTEventListener(
                globalMouseListener,
                AWTEvent.MOUSE_EVENT_MASK
            )
        }

        // 3. Key bindings for Escape & Space to dismiss menu
        val closeAction = object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) = hideMenu()
        }
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).apply {
            put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeTrayMenu")
            put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "closeTrayMenu")
        }
        rootPane.actionMap.put("closeTrayMenu", closeAction)

        val mainPanel = object : JPanel() {
            init {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                border = EmptyBorder(6, 6, 6, 6)
                isOpaque = false
            }

            override fun paintComponent(g: Graphics) {
                val g2 = g.create() as Graphics2D
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                val isDark = isSystemDarkMode()

                // Dynamic Light/Dark Card surface
                g2.color = if (isDark) Color(0x1a, 0x1b, 0x26) else Color(0xf4, 0xf4, 0xf6)
                g2.fillRoundRect(0, 0, width, height, 12, 12)

                // Dynamic Border
                g2.color = if (isDark) Color(0x2e, 0x30, 0x46) else Color(0xe1, 0xe3, 0xe8)
                g2.drawRoundRect(0, 0, width - 1, height - 1, 12, 12)
                g2.dispose()
                super.paintComponent(g)
            }
        }

        mainPanel.add(createItem("Abrir LyraFlow", TrayIconType.APP) {
            hideMenu()
            onOpen()
        })

        mainPanel.add(object : JSeparator() {
            init {
                alignmentX = Component.LEFT_ALIGNMENT
                minimumSize = Dimension(itemWidth, 2)
                preferredSize = Dimension(itemWidth, 2)
                maximumSize = Dimension(itemWidth, 2)
                border = EmptyBorder(2, 4, 2, 4)
            }

            override fun paintComponent(g: Graphics) {
                val g2 = g.create() as Graphics2D
                val isDark = isSystemDarkMode()
                g2.color = if (isDark) Color(0x2e, 0x30, 0x46) else Color(0xe1, 0xe3, 0xe8)
                g2.drawLine(4, 1, width - 8, 1)
                g2.dispose()
            }
        })

        mainPanel.add(createItem("Salir", TrayIconType.EXIT) {
            hideMenu()
            onExit()
        })

        contentPane = mainPanel
        pack()
    }

    fun showAt(mouseEvent: MouseEvent) {
        contentPane.repaint()
        val screenSize = Toolkit.getDefaultToolkit().screenSize
        var x = mouseEvent.x
        var y = mouseEvent.y - height - 10
        if (x + width > screenSize.width) x = screenSize.width - width - 10
        if (y < 0) y = mouseEvent.y + 10

        location = Point(x, y)
        isVisible = true
        toFront()
        requestFocus()
    }

    fun showAtPointer() {
        val pointer = java.awt.MouseInfo.getPointerInfo()?.location ?: return
        val dummyEvent = MouseEvent(
            this,
            MouseEvent.MOUSE_RELEASED,
            System.currentTimeMillis(),
            0,
            pointer.x,
            pointer.y,
            1,
            false,
            MouseEvent.BUTTON3
        )
        showAt(dummyEvent)
    }

    private var lastHiddenTime: Long = 0

    fun hideMenu() {
        if (isVisible) {
            lastHiddenTime = System.currentTimeMillis()
            isVisible = false
        }
    }

    fun wasRecentlyHidden(): Boolean {
        return System.currentTimeMillis() - lastHiddenTime < 250
    }

    override fun close() {
        runCatching {
            Toolkit.getDefaultToolkit().removeAWTEventListener(globalMouseListener)
        }
        dispose()
    }

    private fun createItem(text: String, iconType: TrayIconType, onClick: () -> Unit): JPanel {
        return object : JPanel() {
            private var isHovered = false

            init {
                layout = null
                isOpaque = false
                alignmentX = Component.LEFT_ALIGNMENT
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                minimumSize = Dimension(itemWidth, itemHeight)
                preferredSize = Dimension(itemWidth, itemHeight)
                maximumSize = Dimension(itemWidth, itemHeight)

                addMouseListener(object : MouseAdapter() {
                    override fun mouseEntered(e: MouseEvent) {
                        isHovered = true
                        repaint()
                    }

                    override fun mouseExited(e: MouseEvent) {
                        isHovered = false
                        repaint()
                    }

                    override fun mouseClicked(e: MouseEvent) {
                        onClick()
                    }
                })
            }

            override fun paintComponent(g: Graphics) {
                val g2 = g.create() as Graphics2D
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

                val isDark = isSystemDarkMode()
                val textColor: Color
                val iconColor: Color

                if (isHovered) {
                    g2.color = Color(0x1a, 0x8a, 0x6e) // Turquoise selection pill
                    g2.fillRoundRect(2, 1, width - 4, height - 2, 8, 8)
                    textColor = Color.WHITE
                    iconColor = Color.WHITE
                } else {
                    textColor = if (isDark) Color(0xf1, 0xf1, 0xf1) else Color(0x33, 0x33, 0x33)
                    iconColor = if (isDark) Color(0x8a, 0x8d, 0xa0) else Color(0x55, 0x55, 0x55)
                }

                drawVectorIcon(g2, iconType, 10, (height - 16) / 2, iconColor)

                g2.font = Font("Segoe UI", Font.PLAIN, 12)
                g2.color = textColor
                val fm = g2.fontMetrics
                val textY = (height - fm.height) / 2 + fm.ascent
                g2.drawString(text, 30, textY)

                g2.dispose()
            }
        }
    }

    private fun drawVectorIcon(g2: Graphics2D, type: TrayIconType, x: Int, y: Int, color: Color) {
        g2.color = color
        g2.stroke = BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)

        when (type) {
            TrayIconType.APP -> {
                g2.drawOval(x + 4, y + 4, 8, 8)
                g2.drawOval(x + 6, y + 6, 4, 4)
                g2.drawLine(x + 8, y + 1, x + 8, y + 3)
                g2.drawLine(x + 8, y + 13, x + 8, y + 15)
                g2.drawLine(x + 1, y + 8, x + 3, y + 8)
                g2.drawLine(x + 13, y + 8, x + 15, y + 8)
            }
            TrayIconType.EXIT -> {
                g2.drawRoundRect(x + 1, y + 2, 8, 11, 2, 2)
                g2.drawLine(x + 6, y + 7, x + 14, y + 7)
                g2.drawLine(x + 11, y + 4, x + 14, y + 7)
                g2.drawLine(x + 11, y + 10, x + 14, y + 7)
            }
        }
    }

    private fun isSystemDarkMode(): Boolean {
        return runCatching {
            val proc = Runtime.getRuntime().exec(
                arrayOf(
                    "reg",
                    "query",
                    "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                    "/v",
                    "AppsUseLightTheme"
                )
            )
            val text = proc.inputStream.bufferedReader().readText()
            proc.waitFor()
            text.contains("0x0")
        }.getOrDefault(true)
    }
}
