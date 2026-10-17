package com.aura.music

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun AuraVisualizerBar(
    isPlaying: Boolean,
    progressMs: Long,
    cyanAccent: Color,
    violetAccent: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "aura_visualizer")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283185f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Canvas(modifier = modifier) {
        val barCount = 28
        val spacing = size.width / (barCount * 1.6f)
        val barWidth = (size.width - (spacing * (barCount - 1))) / barCount
        val maxBarHeight = size.height

        for (i in 0 until barCount) {
            val seed = (i * 0.45f) + (progressMs % 1000L) * 0.002f
            val ratio = if (isPlaying) {
                val wave1 = abs(sin(phase + seed))
                val wave2 = abs(sin((phase * 1.7f) - i * 0.3f))
                (0.18f + 0.82f * ((wave1 * 0.65f) + (wave2 * 0.35f))).coerceIn(0.15f, 1f)
            } else {
                0.14f
            }

            val barHeight = maxBarHeight * ratio
            val x = i * (barWidth + spacing)
            val y = (maxBarHeight - barHeight) / 2f

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(cyanAccent, violetAccent)
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
