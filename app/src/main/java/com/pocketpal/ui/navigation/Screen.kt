package com.pocketpal.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Chat : Screen("chat", "Chat", Icons.Default.ChatBubble)
    object Pals : Screen("pals", "Pals", Icons.Default.People)
    object Models : Screen("models", "Models", Icons.Default.Memory)
    object Benchmark : Screen("benchmark", "Benchmark", Icons.Default.Speed)
    object HandsFreeVoice : Screen("hands_free_voice", "Hands-Free", Icons.Default.RecordVoiceOver)
    object Server : Screen("server", "Local API", Icons.Default.Dns)
    object VoicePacks : Screen("voice_packs", "Voices", Icons.Default.VolumeUp)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}
