package com.pocketpal.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.ui.screens.voice.VoiceState
import kotlin.math.sin

@Composable
fun FloatingVoiceOverlayWidget(
    voiceState: VoiceState,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onClickOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "miniVisualizer")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "miniPulse"
    )

    val activeColor = when (voiceState) {
        VoiceState.LISTENING -> Color(0xFF06B6D4)
        VoiceState.PROCESSING -> Color(0xFF6366F1)
        VoiceState.SPEAKING -> Color(0xFF14B8A6)
        VoiceState.IDLE -> Color(0xFF64748B)
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClickOpen() },
        color = Color(0xFF0F172A).copy(alpha = 0.95f),
        shadowElevation = 10.dp,
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (voiceState == VoiceState.LISTENING || voiceState == VoiceState.SPEAKING) {
                    Canvas(modifier = Modifier.size(36.dp).scale(pulseScale)) {
                        drawCircle(color = activeColor.copy(alpha = 0.3f))
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = activeColor,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (voiceState == VoiceState.SPEAKING) Icons.Default.GraphicEq else Icons.Default.Mic,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Column {
                Text(
                    text = "Hands-Free Voice",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
                Text(
                    text = when (voiceState) {
                        VoiceState.LISTENING -> "Listening..."
                        VoiceState.PROCESSING -> "Thinking..."
                        VoiceState.SPEAKING -> "Speaking..."
                        VoiceState.IDLE -> "Offline Voice"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = activeColor
                )
            }

            IconButton(
                onClick = onToggleMute,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("floating_overlay_mute_button")
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Mute",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
