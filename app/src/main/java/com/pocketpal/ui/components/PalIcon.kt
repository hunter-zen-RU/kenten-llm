package com.pocketpal.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PalIcon(
    iconKey: String,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    val vector: ImageVector = when (iconKey.lowercase()) {
        "code", "coder", "💻" -> Icons.Default.Code
        "writer", "writing", "✍️" -> Icons.Default.EditNote
        "tutor", "education", "math", "📐" -> Icons.Default.Calculate
        "roleplay", "detective", "🕵️" -> Icons.Default.Psychology
        "smart", "chat", "bot", "assistant", "🤖", "🌟" -> Icons.Default.SmartToy
        "creative" -> Icons.Default.AutoAwesome
        else -> Icons.Default.SmartToy
    }

    Icon(
        imageVector = vector,
        contentDescription = iconKey,
        tint = tint,
        modifier = modifier.size(size)
    )
}
