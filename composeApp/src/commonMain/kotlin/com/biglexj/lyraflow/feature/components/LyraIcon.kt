package com.biglexj.lyraflow.feature.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

enum class LyraIconType { Home, Settings, Mic, Copy, Clear, System, Sun, Moon }

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
            LyraIconType.System -> {
                drawRoundRect(color, Offset(size.width * .16f, size.height * .2f), Size(size.width * .68f, size.height * .46f), CornerRadius(size.minDimension * .07f), style = Stroke(stroke))
                drawLine(color, Offset(size.width * .5f, size.height * .66f), Offset(size.width * .5f, size.height * .78f), stroke, StrokeCap.Round)
                drawLine(color, Offset(size.width * .32f, size.height * .8f), Offset(size.width * .68f, size.height * .8f), stroke, StrokeCap.Round)
            }
            LyraIconType.Sun -> {
                drawCircle(color, size.minDimension * .2f, center, style = Stroke(stroke))
                repeat(8) { index ->
                    val angle = index * Math.PI / 4
                    val start = Offset(center.x + kotlin.math.cos(angle).toFloat() * size.width * .32f, center.y + kotlin.math.sin(angle).toFloat() * size.height * .32f)
                    val end = Offset(center.x + kotlin.math.cos(angle).toFloat() * size.width * .43f, center.y + kotlin.math.sin(angle).toFloat() * size.height * .43f)
                    drawLine(color, start, end, stroke, StrokeCap.Round)
                }
            }
            LyraIconType.Moon -> {
                val crescent = Path().apply {
                    moveTo(size.width * .62f, size.height * .1f)
                    cubicTo(size.width * .38f, size.height * .18f, size.width * .23f, size.height * .39f, size.width * .27f, size.height * .62f)
                    cubicTo(size.width * .31f, size.height * .84f, size.width * .53f, size.height * .96f, size.width * .74f, size.height * .84f)
                    cubicTo(size.width * .49f, size.height * .78f, size.width * .42f, size.height * .55f, size.width * .49f, size.height * .36f)
                    cubicTo(size.width * .53f, size.height * .24f, size.width * .58f, size.height * .15f, size.width * .62f, size.height * .1f)
                    close()
                }
                drawPath(crescent, color)
            }
        }
    }
}
