package com.pocketpal.data.repository

import com.pocketpal.data.model.ChatMessage
import com.pocketpal.data.model.Pal
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AiEngineService {

    fun generateStreamingResponse(
        prompt: String,
        pal: Pal?,
        modelName: String,
        temperature: Float,
        hasImage: Boolean,
        history: List<ChatMessage>
    ): Flow<String> = flow {
        val isCoding = pal?.category == "Coding" || prompt.lowercase().contains("code") || prompt.lowercase().contains("function")
        val isWriting = pal?.category == "Writing" || prompt.lowercase().contains("story") || prompt.lowercase().contains("write")
        val isMath = pal?.category == "Education" || prompt.lowercase().contains("math") || prompt.lowercase().contains("calculate")

        val fullTextResponse = buildString {
            if (hasImage) {
                append("**Image Analysis:**\n")
                append("Based on visual recognition, here is the detailed analysis of your attached photo:\n\n")
            }

            when {
                isCoding -> {
                    append("Here is the Kotlin solution using modern Jetpack Compose standards:\n\n")
                    append("```kotlin\n")
                    append("@Composable\n")
                    append("fun FeatureCard(\n")
                    append("    title: String,\n")
                    append("    description: String,\n")
                    append("    onClick: () -> Unit\n")
                    append(") {\n")
                    append("    ElevatedCard(\n")
                    append("        modifier = Modifier\n")
                    append("            .fillMaxWidth()\n")
                    append("            .clickable { onClick() },\n")
                    append("        shape = RoundedCornerShape(16.dp)\n")
                    append("    ) {\n")
                    append("        Column(modifier = Modifier.padding(16.dp)) {\n")
                    append("            Text(\n")
                    append("                text = title,\n")
                    append("                style = MaterialTheme.typography.titleMedium\n")
                    append("            )\n")
                    append("            Spacer(modifier = Modifier.height(4.dp))\n")
                    append("            Text(\n")
                    append("                text = description,\n")
                    append("                style = MaterialTheme.typography.bodyMedium\n")
                    append("            )\n")
                    append("        }\n")
                    append("    }\n")
                    append("}\n")
                    append("```\n\n")
                    append("**Key Architectural Highlights:**\n")
                    append("1. Strictly adheres to Material Design 3 guidelines.\n")
                    append("2. Applies standardized 16dp grid padding.\n")
                    append("3. Incorporates visual ripple feedback via clickable modifier.")
                }
                isMath -> {
                    append("Step-by-step mathematical derivation:\n\n")
                    append("1. **Define the variables:** Let x be the input parameter.\n")
                    append("2. **Apply formula:** f(x) = x^2 + 4x + 4 = (x + 2)^2.\n")
                    append("3. **Verify constraints:** Since (x+2)^2 >= 0, the global minimum occurs at x = -2.\n\n")
                    append("Therefore, the exact result is **-2**.")
                }
                isWriting -> {
                    append("The neon rain glistened off the slick asphalt of Sector 7. ")
                    append("Detective Vance pulled up his collar as the digital billboard flickered with glowing blue code. ")
                    append("'Another quiet night,' he muttered to his AI pocket companion, 'or so we thought.'")
                }
                else -> {
                    append("Hello! I am **${pal?.name ?: "PocketPal"}**. ")
                    append("I am operating offline using **$modelName** with a temperature setting of **$temperature**.\n\n")
                    append("Key capabilities available:\n")
                    append("- **Software Engineering:** Kotlin, Jetpack Compose, Python, Rust\n")
                    append("- **Creative Writing & Summarization**\n")
                    append("- **Mathematical Reasoning & Problem Solving**\n")
                    append("- **Hardware Speed & Memory Benchmarking**\n\n")
                    append("How can I assist you today?")
                }
            }
        }

        // Stream word by word with micro delays
        val words = fullTextResponse.split(" ")
        val currentSb = StringBuilder()
        for (i in words.indices) {
            currentSb.append(words[i]).append(" ")
            emit(currentSb.toString())
            delay(40)
        }
    }
}
