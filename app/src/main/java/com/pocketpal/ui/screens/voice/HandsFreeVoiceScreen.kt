package com.pocketpal.ui.screens.voice

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.pocketpal.data.screen.ScreenContextManager
import com.pocketpal.data.screen.ScreenContextResult
import com.pocketpal.data.voice.HandsFreeForegroundService
import com.pocketpal.ui.components.EdgeLightingVisualizer
import com.pocketpal.ui.theme.BackgroundDark
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HandsFreeVoiceScreen(
    viewModel: HandsFreeVoiceViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val screenManager = remember { ScreenContextManager(context) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.initVoiceEngines(context)
            viewModel.startHandsFreeSession()
        } else {
            Toast.makeText(context, "Microphone permission required for Hands-Free mode", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        val hasAudioPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasAudioPermission) {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            viewModel.initVoiceEngines(context)
            viewModel.startHandsFreeSession()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopHandsFreeSession()
        }
    }

    val voiceState by viewModel.voiceState.collectAsState()
    val userTranscript by viewModel.userTranscript.collectAsState()
    val aiResponse by viewModel.aiResponse.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val selectedPal by viewModel.selectedPal.collectAsState()

    var isBackgroundOverlayEnabled by remember { mutableStateOf(false) }

    fun handleReadScreen() {
        scope.launch {
            val activity = context as? Activity
            val view = activity?.window?.decorView
            if (view != null) {
                val result = screenManager.captureScreenContext(view, "local_model", "Local")
                when (result) {
                    is ScreenContextResult.VisionImage -> {
                        Toast.makeText(context, "Captured Screen Image for Vision AI!", Toast.LENGTH_SHORT).show()
                    }
                    is ScreenContextResult.TextOcr -> {
                        viewModel.processUserSpeech("Read screen content: ${result.extractedText}")
                        Toast.makeText(context, "Screen text read via OCR!", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                viewModel.processUserSpeech("Read current screen")
            }
        }
    }

    // Pulse & Spectrum Wave Animations
    val infiniteTransition = rememberInfiniteTransition(label = "audioVisualizer")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    val primaryWaveColor = when (voiceState) {
        VoiceState.LISTENING -> Color(0xFF06B6D4) // Cyan
        VoiceState.PROCESSING -> Color(0xFF6366F1) // Indigo
        VoiceState.SPEAKING -> Color(0xFF14B8A6) // Teal
        VoiceState.IDLE -> Color(0xFF64748B) // Slate
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(24.dp)
    ) {
        // Ambient Edge Lighting Visualizer
        EdgeLightingVisualizer(
            voiceState = voiceState,
            isActive = true
        )

        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Hands-Free Voice Mode",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Talking to ${selectedPal?.name ?: "Kenten"} (Offline)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8)
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFF334155), CircleShape)
                    .testTag("exit_hands_free_button")
            ) {
                Icon(Icons.Default.Close, contentDescription = "Exit Voice Mode", tint = Color.White)
            }
        }

        // Edge Lighting System Overlay Toggle Bar
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 70.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E293B)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FlipToFront,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Edge Lighting Overlay Over All Apps",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
                Switch(
                    checked = isBackgroundOverlayEnabled,
                    onCheckedChange = { enabled ->
                        isBackgroundOverlayEnabled = enabled
                        if (enabled) {
                            val hasAudioPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                            if (!hasAudioPermission) {
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else if (!Settings.canDrawOverlays(context)) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } else {
                                HandsFreeForegroundService.startService(context, true)
                            }
                        } else {
                            HandsFreeForegroundService.stopService(context)
                        }
                    },
                    modifier = Modifier.testTag("toggle_floating_overlay_switch")
                )
            }
        }

        // Professional Audio Spectrum Visualizer
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Multi-Ring & Radial Equalizer Bars
                Canvas(
                    modifier = Modifier
                        .size(240.dp)
                        .scale(if (voiceState == VoiceState.LISTENING || voiceState == VoiceState.SPEAKING) pulseScale else 1.0f)
                ) {
                    val centerPx = size.width / 2
                    val radius = size.width / 2 - 20.dp.toPx()

                    // Ambient Outer Ripple Ring
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(primaryWaveColor.copy(alpha = 0.35f), Color.Transparent),
                            center = Offset(centerPx, centerPx),
                            radius = size.width / 2
                        )
                    )

                    // 32 Circular Equalizer Frequency Spectrum Bars
                    val numBars = 32
                    for (i in 0 until numBars) {
                        val angleDeg = (i * 360f / numBars) + wavePhase
                        val angleRad = Math.toRadians(angleDeg.toDouble())

                        val barLength = when (voiceState) {
                            VoiceState.LISTENING -> (15 + (18 * sin(angleRad * 2 + wavePhase / 20))).dp.toPx()
                            VoiceState.SPEAKING -> (20 + (25 * cos(angleRad * 3 + wavePhase / 15))).dp.toPx()
                            VoiceState.PROCESSING -> (10 + (10 * sin(angleRad * 4))).dp.toPx()
                            VoiceState.IDLE -> 6.dp.toPx()
                        }

                        val startX = centerPx + (radius * cos(angleRad)).toFloat()
                        val startY = centerPx + (radius * sin(angleRad)).toFloat()
                        val endX = centerPx + ((radius + barLength) * cos(angleRad)).toFloat()
                        val endY = centerPx + ((radius + barLength) * sin(angleRad)).toFloat()

                        drawLine(
                            color = primaryWaveColor.copy(alpha = if (voiceState == VoiceState.IDLE) 0.4f else 0.85f),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 4.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    // Inner Boundary Ring
                    drawCircle(
                        color = primaryWaveColor.copy(alpha = 0.6f),
                        radius = radius - 8.dp.toPx(),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // Center Mic / Orb Surface
                Surface(
                    shape = CircleShape,
                    color = primaryWaveColor,
                    shadowElevation = 12.dp,
                    modifier = Modifier.size(110.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val icon = when (voiceState) {
                            VoiceState.SPEAKING -> Icons.Default.GraphicEq
                            VoiceState.PROCESSING -> Icons.Default.Bolt
                            VoiceState.LISTENING -> Icons.Default.Mic
                            VoiceState.IDLE -> Icons.Default.Pause
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
            }

            // Status Text
            Text(
                text = when (voiceState) {
                    VoiceState.LISTENING -> "Listening... Speak naturally"
                    VoiceState.PROCESSING -> "Processing offline..."
                    VoiceState.SPEAKING -> "${selectedPal?.name ?: "PocketPal"} is speaking..."
                    VoiceState.IDLE -> if (isMuted) "Microphone muted" else "Tap microphone to speak"
                },
                style = MaterialTheme.typography.titleMedium,
                color = primaryWaveColor,
                fontWeight = FontWeight.Bold
            )

            // Live Transcript Display
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 110.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1E293B)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (userTranscript.isNotBlank()) {
                        Text(
                            text = "You: $userTranscript",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFF1F5F9)
                        )
                    }
                    if (aiResponse.isNotBlank()) {
                        Text(
                            text = "${selectedPal?.name ?: "AI"}: $aiResponse",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Bottom Controls Bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mute Button
            IconButton(
                onClick = { viewModel.toggleMute() },
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        if (isMuted) MaterialTheme.colorScheme.error else Color(0xFF334155),
                        CircleShape
                    )
                    .testTag("hands_free_mute_button")
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Mute Toggle",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Read Screen Context Action Button
            Button(
                onClick = { handleReadScreen() },
                modifier = Modifier
                    .height(52.dp)
                    .testTag("read_screen_button"),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.Screenshot, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Read Screen", fontWeight = FontWeight.Bold, color = Color.White)
            }

            // Quick Speech Test Button
            Button(
                onClick = {
                    viewModel.processUserSpeech("Explain what hands-free AI voice mode can do.")
                },
                modifier = Modifier
                    .height(52.dp)
                    .testTag("hands_free_speak_test_button"),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Demo Prompt", fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}
