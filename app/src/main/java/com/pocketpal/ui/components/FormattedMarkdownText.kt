package com.pocketpal.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpal.ui.theme.CodeBackground

@Composable
fun FormattedMarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val context = LocalContext.current
    val parts = parseCodeBlocks(text)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (part in parts) {
            when (part) {
                is TextPart.Normal -> {
                    Text(
                        text = buildAnnotatedStringFromMarkdown(part.content, textColor),
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor
                    )
                }
                is TextPart.CodeBlock -> {
                    CodeBlockCard(
                        language = part.language,
                        code = part.code,
                        onCopy = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Code", part.code)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CodeBlockCard(
    language: String,
    code: String,
    onCopy: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp)),
        color = CodeBackground
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.ifBlank { "code" }.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF94A3B8)
                )
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("copy_code_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = code,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = Color(0xFFF1F5F9)
                )
            )
        }
    }
}

sealed class TextPart {
    data class Normal(val content: String) : TextPart()
    data class CodeBlock(val language: String, val code: String) : TextPart()
}

private fun parseCodeBlocks(text: String): List<TextPart> {
    val result = mutableListOf<TextPart>()
    val codeBlockRegex = Regex("```(\\w*)\\n?([\\s\\S]*?)```")
    var currentIndex = 0

    codeBlockRegex.findAll(text).forEach { match ->
        if (match.range.first > currentIndex) {
            val normalText = text.substring(currentIndex, match.range.first)
            if (normalText.isNotBlank()) {
                result.add(TextPart.Normal(normalText))
            }
        }
        val lang = match.groupValues[1]
        val code = match.groupValues[2].trimEnd()
        result.add(TextPart.CodeBlock(lang, code))
        currentIndex = match.range.last + 1
    }

    if (currentIndex < text.length) {
        val remaining = text.substring(currentIndex)
        if (remaining.isNotBlank()) {
            result.add(TextPart.Normal(remaining))
        }
    }

    if (result.isEmpty()) {
        result.add(TextPart.Normal(text))
    }

    return result
}

private fun buildAnnotatedStringFromMarkdown(text: String, defaultColor: Color) = buildAnnotatedString {
    val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
    var currentIndex = 0

    boldRegex.findAll(text).forEach { match ->
        if (match.range.first > currentIndex) {
            append(text.substring(currentIndex, match.range.first))
        }
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = defaultColor)) {
            append(match.groupValues[1])
        }
        currentIndex = match.range.last + 1
    }

    if (currentIndex < text.length) {
        append(text.substring(currentIndex))
    }
}
