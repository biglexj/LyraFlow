package com.biglexj.lyraflow.feature.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

enum class LyraIconType { Home, Settings, Mic, Copy, Clear, Sparkle }

@Composable
fun LyraIcon(type: LyraIconType, modifier: Modifier = Modifier) {
    val color = LocalContentColor.current
    Canvas(modifier) {
        val stroke = 1.8.dp.toPx()
        when (type) {
            LyraIconType.Home -> {
                drawLine(color, Offset(size.width * .18f, size.height * .48f), Offset(size.width * .5f, size.height * .2f), stroke, StrokeCap.Round)
                drawLine(color, Offset(size.width * .5f, size.height * .2f), Offset(size.width * .82f, size.height * .48f), stroke, StrokeCap.Round)
                drawRoundRect(color, Offset(size.width * .27f, size.height * .43f), Size(size.width * .46f, size.height * .38f), CornerRadius(size.minDimension * .05f), style = Stroke(stroke))
            }
            LyraIconType.Settings -> {
                drawCircle(color, size.minDimension * .16f, center, style = Stroke(stroke))
                repeat(8) { index ->
                    val angle = index * Math.PI / 4
                    val start = Offset(center.x + kotlin.math.cos(angle).toFloat() * size.width * .28f, center.y + kotlin.math.sin(angle).toFloat() * size.height * .28f)
                    val end = Offset(center.x + kotlin.math.cos(angle).toFloat() * size.width * .39f, center.y + kotlin.math.sin(angle).toFloat() * size.height * .39f)
                    drawLine(color, start, end, stroke, StrokeCap.Round)
                }
            }
            LyraIconType.Mic -> {
                drawRoundRect(color, Offset(size.width * .36f, size.height * .12f), Size(size.width * .28f, size.height * .48f), CornerRadius(size.minDimension * .14f), style = Stroke(stroke))
                drawArc(color, 0f, 180f, false, Offset(size.width * .25f, size.height * .35f), Size(size.width * .5f, size.height * .4f), style = Stroke(stroke, cap = StrokeCap.Round))
                drawLine(color, Offset(size.width * .5f, size.height * .75f), Offset(size.width * .5f, size.height * .88f), stroke, StrokeCap.Round)
            }
            LyraIconType.Copy -> {
                drawRoundRect(color, Offset(size.width * .3f, size.height * .18f), Size(size.width * .5f, size.height * .54f), CornerRadius(size.minDimension * .06f), style = Stroke(stroke))
                drawRoundRect(color, Offset(size.width * .18f, size.height * .3f), Size(size.width * .5f, size.height * .54f), CornerRadius(size.minDimension * .06f), style = Stroke(stroke))
            }
            LyraIconType.Clear -> {
                drawLine(color, Offset(size.width * .25f, size.height * .25f), Offset(size.width * .75f, size.height * .75f), stroke, StrokeCap.Round)
                drawLine(color, Offset(size.width * .75f, size.height * .25f), Offset(size.width * .25f, size.height * .75f), stroke, StrokeCap.Round)
            }
            LyraIconType.Sparkle -> {
                drawLine(color, Offset(size.width * .5f, size.height * .12f), Offset(size.width * .5f, size.height * .88f), stroke, StrokeCap.Round)
                drawLine(color, Offset(size.width * .12f, size.height * .5f), Offset(size.width * .88f, size.height * .5f), stroke, StrokeCap.Round)
                drawLine(color, Offset(size.width * .25f, size.height * .25f), Offset(size.width * .75f, size.height * .75f), stroke, StrokeCap.Round)
                drawLine(color, Offset(size.width * .75f, size.height * .25f), Offset(size.width * .25f, size.height * .75f), stroke, StrokeCap.Round)
            }
        }
    }
}
