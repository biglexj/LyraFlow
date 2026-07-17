package com.biglexj.lyraflow.feature.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun DictationVisualizer(
    listening: Boolean,
    processing: Boolean,
    level: Float,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "audio-wave")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Restart),
        label = "audio-phase",
    ).value
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val idle = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier) {
        val bars = 17
        val gap = size.width / (bars * 2f)
        repeat(bars) { index ->
            val pulse = ((sin(phase + index * .72f) + 1f) / 2f)
            val energy = when {
                listening -> .12f + level.coerceAtLeast(.08f) * .88f * pulse
                processing -> .24f + .68f * pulse
                else -> .18f + .08f * sin(index.toFloat()).coerceAtLeast(0f)
            }
            val color = when {
                listening -> primary
                processing -> lerp(primary, secondary, pulse)
                else -> idle
            }
            val height = size.height * energy
            val x = gap * (index * 2 + 1)
            drawLine(
                color = color,
                start = Offset(x, (size.height - height) / 2f),
                end = Offset(x, (size.height + height) / 2f),
                strokeWidth = gap * .72f,
                cap = StrokeCap.Round,
            )
        }
    }
}
