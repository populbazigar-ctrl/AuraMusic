package com.aura.music

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

@Composable
fun AuraVisualizerBar(
    isPlaying: Boolean,
    progressMs: Long,
    cyanAccent: Color,
    violetAccent: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val barCount = 20
        val spacing = size.width / (barCount * 2f)
        val barWidth = (size.width - (spacing * (barCount - 1))) / barCount
        val maxBarHeight = size.height

        for (i in 0 until barCount) {
            val ratio = if (isPlaying) {
                // Simple dynamic height calculation based on time and index
                val factor = ((i + (progressMs / 120)) % 10) / 10f
                0.3f + (0.7f * factor)
            } else {
                0.2f
            }

            val barHeight = maxBarHeight * ratio
            val x = i * (barWidth + spacing)
            val y = (maxBarHeight - barHeight) / 2f

            drawRoundRect(
                color = if (i % 2 == 0) cyanAccent else violetAccent,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
