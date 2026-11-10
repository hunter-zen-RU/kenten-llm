package com.pocketpal.data.repository

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.pocketpal.data.local.ChatDao
import com.pocketpal.data.local.ChatMessageEntity
import com.pocketpal.data.local.ChatSessionEntity
import com.pocketpal.data.model.ChatMessage
import com.pocketpal.data.model.ChatSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class ChatRepository(private val chatDao: ChatDao) {

    private val gson = Gson()

    val allSessions: Flow<List<ChatSession>> = chatDao.getAllSessions().map { entities ->
        entities.map { it.toSession() }
    }

    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessage>> {
        return chatDao.getMessagesForSession(sessionId).map { entities ->
            entities.map { it.toMessage() }
        }
    }

    suspend fun createNewSession(title: String = "New Chat", palId: String? = null, modelId: String? = null): ChatSession {
        val session = ChatSession(
            id = UUID.randomUUID().toString(),
            title = title,
            palId = palId,
            modelId = modelId,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        chatDao.insertSession(session.toEntity())
        return session
    }

    suspend fun saveSession(session: ChatSession) {
        chatDao.insertSession(session.toEntity())
    }

    suspend fun deleteSession(sessionId: String) {
        chatDao.deleteMessagesForSession(sessionId)
        chatDao.deleteSessionById(sessionId)
    }

    suspend fun saveMessage(message: ChatMessage) {
        chatDao.insertMessage(message.toEntity())
        val session = chatDao.getSessionById(message.sessionId)
        if (session != null) {
            val titleUpdate = if (session.title == "New Chat" && message.role == "user") {
                if (message.content.length > 30) message.content.take(30) + "..." else message.content
            } else {
                session.title
            }
            chatDao.insertSession(session.copy(title = titleUpdate, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun clearSessionHistory(sessionId: String) {
        chatDao.deleteMessagesForSession(sessionId)
    }

    // Export Chat History to JSON String
    suspend fun exportChatHistoryJson(): String = withContext(Dispatchers.IO) {
        val sessionsList = chatDao.getAllSessions().first()
        val exportArray = JsonArray()

        for (session in sessionsList) {
            val messagesList = chatDao.getMessagesForSession(session.id).first()
            val sessionObj = JsonObject().apply {
                addProperty("id", session.id)
                addProperty("title", session.title)
                addProperty("palId", session.palId ?: "")
                addProperty("modelId", session.modelId ?: "")
                addProperty("createdAt", session.createdAt)
                addProperty("updatedAt", session.updatedAt)

                val msgArray = JsonArray()
                for (msg in messagesList) {
                    val msgObj = JsonObject().apply {
                        addProperty("id", msg.id)
                        addProperty("sessionId", msg.sessionId)
                        addProperty("role", msg.role)
                        addProperty("content", msg.content)
                        addProperty("timestamp", msg.timestamp)
                        addProperty("speedTps", msg.speedTps)
                        addProperty("thinkingTimeMs", msg.thinkingTimeMs)
                    }
                    msgArray.add(msgObj)
                }
                add("messages", msgArray)
            }
            exportArray.add(sessionObj)
        }

        val root = JsonObject().apply {
            addProperty("version", 1)
            addProperty("exportedAt", System.currentTimeMillis())
            add("sessions", exportArray)
        }

        gson.toJson(root)
    }

    // Import Chat History from JSON String
    suspend fun importChatHistoryJson(jsonString: String): Int = withContext(Dispatchers.IO) {
        var importedCount = 0
        try {
            val root = gson.fromJson(jsonString, JsonObject::class.java)
            val sessionsArray = root.getAsJsonArray("sessions") ?: return@withContext 0

            for (i in 0 until sessionsArray.size()) {
                val sObj = sessionsArray[i].asJsonObject
                val sessionId = sObj.get("id")?.asString ?: UUID.randomUUID().toString()
                val sessionEntity = ChatSessionEntity(
                    id = sessionId,
                    title = sObj.get("title")?.asString ?: "Imported Chat",
                    palId = sObj.get("palId")?.asString,
                    modelId = sObj.get("modelId")?.asString,
                    createdAt = sObj.get("createdAt")?.asLong ?: System.currentTimeMillis(),
                    updatedAt = sObj.get("updatedAt")?.asLong ?: System.currentTimeMillis()
                )
                chatDao.insertSession(sessionEntity)
                importedCount++

                val msgsArray = sObj.getAsJsonArray("messages")
                if (msgsArray != null) {
                    for (j in 0 until msgsArray.size()) {
                        val mObj = msgsArray[j].asJsonObject
                        val msgEntity = ChatMessageEntity(
                            id = mObj.get("id")?.asString ?: UUID.randomUUID().toString(),
                            sessionId = sessionId,
                            role = mObj.get("role")?.asString ?: "user",
                            content = mObj.get("content")?.asString ?: "",
                            timestamp = mObj.get("timestamp")?.asLong ?: System.currentTimeMillis(),
                            imageUri = null,
                            tokenCount = mObj.get("content")?.asString?.length ?: 0,
                            speedTps = mObj.get("speedTps")?.asDouble ?: 22.0,
                            thinkingTimeMs = mObj.get("thinkingTimeMs")?.asLong ?: 1000
                        )
                        chatDao.insertMessage(msgEntity)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        importedCount
    }

    private fun ChatSessionEntity.toSession() = ChatSession(
        id = id,
        title = title,
        palId = palId,
        modelId = modelId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun ChatSession.toEntity() = ChatSessionEntity(
        id = id,
        title = title,
        palId = palId,
        modelId = modelId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun ChatMessageEntity.toMessage() = ChatMessage(
        id = id,
        sessionId = sessionId,
        role = role,
        content = content,
        timestamp = timestamp,
        imageUri = imageUri,
        tokenCount = tokenCount,
        speedTps = speedTps,
        thinkingTimeMs = thinkingTimeMs
    )

    private fun ChatMessage.toEntity() = ChatMessageEntity(
        id = id,
        sessionId = sessionId,
        role = role,
        content = content,
        timestamp = timestamp,
        imageUri = imageUri,
        tokenCount = tokenCount,
        speedTps = speedTps,
        thinkingTimeMs = thinkingTimeMs
    )
}
