package com.pocketpal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val palId: String?,
    val modelId: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val role: String,
    val content: String,
    val timestamp: Long,
    val imageUri: String?,
    val tokenCount: Int,
    val speedTps: Double,
    val thinkingTimeMs: Long
)

@Entity(tableName = "pals")
data class PalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val avatar: String,
    val description: String,
    val systemPrompt: String,
    val category: String,
    val isCustom: Boolean,
    val temperature: Float,
    val topP: Float,
    val topK: Int,
    val iconName: String
)

@Entity(tableName = "benchmark_results")
data class BenchmarkResultEntity(
    @PrimaryKey val id: String,
    val modelName: String,
    val tokensPerSec: Double,
    val ttftMs: Long,
    val ramUsageMb: Int,
    val timestamp: Long,
    val hardwareCpu: String,
    val hardwareRam: String
)
