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
    val transition = rememberInfiniteTransition(label = "aura_visualizer_smooth")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283185f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase_smooth"
    )

    val multiColors = listOf(
        Color(0xFF00E5FF), // Cyan
        Color(0xFF7B2CBF), // Deep Violet
        Color(0xFFFF007F)  // Neon Pink
    )

    Canvas(modifier = modifier) {
        val barCount = 28
        val spacing = size.width / (barCount * 1.8f)
        val barWidth = (size.width - (spacing * (barCount - 1))) / barCount
        val maxBarHeight = size.height

        for (i in 0 until barCount) {
            val seed = (i * 0.35f) + (progressMs % 1000L) * 0.0025f
            val ratio = if (isPlaying) {
                val wave1 = abs(sin(phase + seed))
                val wave2 = abs(sin((phase * 1.4f) - i * 0.25f))
                (0.2f + 0.8f * ((wave1 * 0.6f) + (wave2 * 0.4f))).coerceIn(0.15f, 1f)
            } else {
                0.15f
            }

            val barHeight = maxBarHeight * ratio
            val x = i * (barWidth + spacing)
            val y = (maxBarHeight - barHeight) / 2f

            drawRoundRect(
                brush = Brush.verticalGradient(colors = multiColors),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
