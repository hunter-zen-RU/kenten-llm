package com.pocketpal.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.pocketpal.ui.screens.voice.VoiceState

@Composable
fun EdgeLightingVisualizer(
    voiceState: VoiceState,
    isActive: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (!isActive) return

    val infiniteTransition = rememberInfiniteTransition(label = "edgeLighting")
    val glowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPhase"
    )

    val primaryColor = when (voiceState) {
        VoiceState.LISTENING -> Color(0xFF06B6D4) // Electric Cyan
        VoiceState.PROCESSING -> Color(0xFF6366F1) // Indigo
        VoiceState.SPEAKING -> Color(0xFF14B8A6) // Teal
        VoiceState.IDLE -> Color(0xFF38BDF8)
    }

    val secondaryColor = Color(0xFF818CF8)

    Canvas(modifier = modifier.fillMaxSize()) {
        val strokeWidth = (6 + (4 * glowPhase)).dp.toPx()

        val brush = Brush.linearGradient(
            colors = listOf(
                primaryColor,
                secondaryColor,
                primaryColor.copy(alpha = 0.5f),
                secondaryColor
            ),
            start = Offset(x = 0f, y = 0f),
            end = Offset(x = size.width * glowPhase, y = size.height)
        )

        // Outer Neon Edge Glow
        drawRect(
            brush = brush,
            topLeft = Offset.Zero,
            size = Size(size.width, size.height),
            style = Stroke(width = strokeWidth)
        )

        // Soft Outer Shadow Glow
        drawRect(
            color = primaryColor.copy(alpha = 0.25f * glowPhase),
            topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
            size = Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx()),
            style = Stroke(width = (strokeWidth * 1.5f))
        )
    }
}
