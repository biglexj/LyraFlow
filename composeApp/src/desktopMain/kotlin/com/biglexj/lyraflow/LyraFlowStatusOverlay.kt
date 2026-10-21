package com.biglexj.lyraflow

import com.biglexj.lyraflow.domain.dictation.DictationState
import com.sun.jna.Native
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinUser
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GraphicsEnvironment
import java.awt.RenderingHints
import java.awt.Window
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JComponent
import javax.swing.JWindow
import javax.swing.SwingUtilities
import javax.swing.Timer
import kotlin.math.sin

/**
 * A non-focusable, always-visible recording status light for the desktop.
 *
 * Implementa inicializacion perezosa (Lazy) de JWindow para evitar colisiones
 * y bloqueos en el Desktop Window Manager (DWM) de Windows 11 antes de que la
 * ventana principal de Compose termine su mapeo inicial (conforme a Core-Docs).
 *
 * El gestor de ventanas puede ocultar o degradar una ventana *utility* sin aviso,
 * y como el overlay solo se re-mostraba cuando cambiaba el estado o el nivel de
 * audio, en reposo desaparecia y no volvia. Por eso el componente de dibujo
 * mantiene ademas un vigilante periodico que lo reactiva y devuelve al frente.
 */
class LyraFlowStatusOverlay : AutoCloseable {
    private var window: JWindow? = null
    private val display = StatusDisplay(onWatchdog = ::restoreWindow)

    fun update(state: DictationState, level: Float = 0f) {
        display.status = when (state) {
            DictationState.Idle, is DictationState.Completed, is DictationState.Failed -> OverlayStatus.Idle
            DictationState.Listening -> OverlayStatus.Listening
            is DictationState.Transcribing -> OverlayStatus.Transcribing
            is DictationState.AttemptFailed -> OverlayStatus.AttemptFailed
        }
        display.audioLevel = level.coerceIn(0f, 1f)
        ensureWindow()
    }

    private fun ensureWindow() {
        SwingUtilities.invokeLater {
            val existing = window
            if (existing != null) {
                if (!existing.isVisible) {
                    existing.isVisible = true
                    positionAtBottom(existing)
                }
                return@invokeLater
            }

            val win = JWindow().apply {
                type = Window.Type.UTILITY
                background = Color(0, 0, 0, 0)
                isAlwaysOnTop = true
                focusableWindowState = false
                isAutoRequestFocus = false
                size = Dimension(68, 30)
                contentPane = display
                addComponentListener(object : ComponentAdapter() {
                    override fun componentShown(event: ComponentEvent) = positionAtBottom(this@apply)
                })
            }
            positionAtBottom(win)
            window = win
            win.isVisible = true
        }
    }

    /** Reactiva el overlay si el gestor de ventanas lo oculto o lo envio atras. */
    private fun restoreWindow() {
        val win = window ?: return
        if (!win.isDisplayable) return

        if (!win.isVisible) {
            win.isVisible = true
            positionAtBottom(win)
            return
        }
        reassertTopmost(win)
    }

    /** Reforza el HWND_TOPMOST por nativo: setAlwaysOnTop de AWT no basta si el DWM ya lo degrado. */
    private fun reassertTopmost(win: JWindow) {
        if (!System.getProperty("os.name").contains("windows", ignoreCase = true)) return
        runCatching {
            val hwnd = HWND(Native.getWindowPointer(win))
            if (hwnd.pointer == null || hwnd.pointer == com.sun.jna.Pointer.NULL) return
            User32.INSTANCE.SetWindowPos(
                hwnd,
                HWND(com.sun.jna.Pointer(-1L)),
                0, 0, 0, 0,
                WinUser.SWP_NOMOVE or WinUser.SWP_NOSIZE or
                    WinUser.SWP_NOACTIVATE,
            )
        }
    }

    private fun positionAtBottom(win: JWindow) {
        val area = GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
        win.setLocation(area.x + (area.width - win.width) / 2, area.y + area.height - win.height - 18)
    }

    override fun close() {
        SwingUtilities.invokeLater {
            window?.dispose()
            window = null
        }
    }
}

private enum class OverlayStatus(val color: Color) {
    Idle(Color(0x7C, 0x85, 0x8B)),
    Listening(Color(0x00, 0xA8, 0x96)),
    Transcribing(Color(0x7F, 0x52, 0xFF)),
    AttemptFailed(Color(0xE5, 0x53, 0x53)),
}

private class StatusDisplay(
    private val onWatchdog: () -> Unit = {},
) : JComponent() {
    var status: OverlayStatus = OverlayStatus.Idle
    var audioLevel: Float = 0f
    private var smoothedLevel: Float = 0f
    private var tick = 0

    init {
        isOpaque = false
        Timer(48) {
            tick++
            if (tick % WATCHDOG_EVERY_TICKS == 0) onWatchdog()
            repaint()
        }.start()
    }

    override fun paintComponent(graphics: Graphics) {
        val canvas = graphics.create() as Graphics2D
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val centerY = height / 2
        smoothedLevel += (audioLevel - smoothedLevel) * 0.35f

        for (index in 0 until 9) {
            val moving = status != OverlayStatus.Idle
            val wave = if (moving) ((sin(tick * .33 + index * .72) + 1) * .5).toFloat() else .15f
            val barHeight = when (status) {
                OverlayStatus.Idle -> 6
                OverlayStatus.Listening -> {
                    val baseHeight = 5 + (wave * 3).toInt()
                    val dynamicRange = (smoothedLevel * 16f * (0.6f + wave * 0.8f)).toInt()
                    (baseHeight + dynamicRange).coerceAtMost(24)
                }
                OverlayStatus.Transcribing, OverlayStatus.AttemptFailed -> 7 + (wave * 9).toInt()
            }
            val x = 6 + index * 6
            canvas.color = status.color
            canvas.fillRoundRect(x, centerY - barHeight / 2, 4, barHeight, 4, 4)
        }
        canvas.dispose()
    }

    private companion object {
        /** ~1.9 s a 48 ms por tick. */
        const val WATCHDOG_EVERY_TICKS = 40
    }
}
