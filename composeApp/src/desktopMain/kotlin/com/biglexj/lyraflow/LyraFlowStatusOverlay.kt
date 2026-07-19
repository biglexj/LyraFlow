package com.biglexj.lyraflow

import com.biglexj.lyraflow.domain.dictation.DictationState
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
import javax.swing.Timer
import kotlin.math.sin

/** A non-focusable, always-visible recording status light for the desktop. */
class LyraFlowStatusOverlay : JWindow() {
    private val display = StatusDisplay()

    init {
        type = Window.Type.UTILITY
        background = Color(0, 0, 0, 0)
        isAlwaysOnTop = true
        focusableWindowState = false
        size = Dimension(68, 30)
        contentPane = display
        addComponentListener(object : ComponentAdapter() {
            override fun componentShown(event: ComponentEvent) = positionAtBottom()
        })
        positionAtBottom()
        isVisible = true
    }

    fun update(state: DictationState, level: Float = 0f) {
        display.status = when (state) {
            DictationState.Idle, is DictationState.Completed, is DictationState.Failed -> OverlayStatus.Idle
            DictationState.Listening -> OverlayStatus.Listening
            is DictationState.Transcribing -> OverlayStatus.Transcribing
        }
        display.audioLevel = level.coerceIn(0f, 1f)
        display.repaint()
    }

    private fun positionAtBottom() {
        val area = GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
        setLocation(area.x + (area.width - width) / 2, area.y + area.height - height - 18)
    }
}

private enum class OverlayStatus(val color: Color) {
    Idle(Color(0x7C, 0x85, 0x8B)),
    Listening(Color(0x00, 0xB8, 0x8A)),
    Transcribing(Color(0x70, 0x8C, 0xFF)),
}

private class StatusDisplay : JComponent() {
    var status: OverlayStatus = OverlayStatus.Idle
    var audioLevel: Float = 0f
    private var smoothedLevel: Float = 0f
    private var tick = 0

    init {
        isOpaque = false
        Timer(48) { tick++; repaint() }.start()
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
                OverlayStatus.Transcribing -> 7 + (wave * 9).toInt()
            }
            val x = 6 + index * 6
            canvas.color = status.color
            canvas.fillRoundRect(x, centerY - barHeight / 2, 4, barHeight, 4, 4)
        }
        canvas.dispose()
    }
}
