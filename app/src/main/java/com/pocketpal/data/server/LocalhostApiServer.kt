package com.pocketpal.data.server

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.pocketpal.data.model.ApiRequestLog
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID

class LocalhostApiServer {

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val gson = Gson()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _port = MutableStateFlow(8080)
    val port: StateFlow<Int> = _port.asStateFlow()

    private val _logs = MutableStateFlow<List<ApiRequestLog>>(emptyList())
    val logs: StateFlow<List<ApiRequestLog>> = _logs.asStateFlow()

    fun startServer(port: Int = 8080) {
        if (_isRunning.value) return
        _port.value = port
        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(port)
                _isRunning.value = true
                addLog("SYSTEM", "/v1/health", 200, "Server started on http://localhost:$port")

                while (isActive) {
                    val clientSocket = serverSocket?.accept() ?: break
                    launch { handleClient(clientSocket) }
                }
            } catch (e: Exception) {
                if (isActive) {
                    addLog("ERROR", "/v1/server", 500, e.localizedMessage ?: "Server error")
                }
            } finally {
                _isRunning.value = false
            }
        }
    }

    fun stopServer() {
        try {
            serverSocket?.close()
            serverSocket = null
            serverJob?.cancel()
            _isRunning.value = false
            addLog("SYSTEM", "/v1/server", 200, "Server stopped")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleClient(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = PrintWriter(socket.getOutputStream(), true)

            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return
            val method = parts[0]
            val endpoint = parts[1]

            // Read headers to find Content-Length
            var contentLength = 0
            var line: String?
            while (reader.readLine().also { line = it } != null && !line.isNullOrBlank()) {
                if (line!!.startsWith("Content-Length:", ignoreCase = true)) {
                    contentLength = line!!.substringAfter(":").trim().toIntOrDefault(0)
                }
            }

            // Read body if POST
            val bodyBuilder = StringBuilder()
            if (contentLength > 0) {
                val bodyChars = CharArray(contentLength)
                reader.read(bodyChars, 0, contentLength)
                bodyBuilder.append(bodyChars)
            }
            val requestBody = bodyBuilder.toString()

            val (status, responseBody) = when {
                endpoint == "/v1/models" && method == "GET" -> {
                    200 to buildModelsJsonResponse()
                }
                endpoint == "/v1/chat/completions" && method == "POST" -> {
                    200 to buildChatCompletionsJsonResponse(requestBody)
                }
                endpoint == "/v1/health" || endpoint == "/" -> {
                    200 to "{\"status\":\"ok\",\"engine\":\"PocketPal Local AI\",\"mode\":\"offline\"}"
                }
                else -> {
                    404 to "{\"error\":\"Endpoint not found. Use POST /v1/chat/completions or GET /v1/models\"}"
                }
            }

            // Write HTTP Header & Body
            writer.print("HTTP/1.1 $status ${if (status == 200) "OK" else "Not Found"}\r\n")
            writer.print("Content-Type: application/json; charset=utf-8\r\n")
            writer.print("Access-Control-Allow-Origin: *\r\n")
            writer.print("Content-Length: ${responseBody.toByteArray().size}\r\n")
            writer.print("Connection: close\r\n")
            writer.print("\r\n")
            writer.print(responseBody)
            writer.flush()

            addLog(method, endpoint, status, requestBody.take(100), responseBody.take(150))
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun buildModelsJsonResponse(): String {
        return """
        {
          "object": "list",
          "data": [
            {
              "id": "llama-3.2-1b",
              "object": "model",
              "created": 1700000000,
              "owned_by": "pocketpal-local"
            },
            {
              "id": "qwen-2.5-1.5b",
              "object": "model",
              "created": 1700000000,
              "owned_by": "pocketpal-local"
            }
          ]
        }
        """.trimIndent()
    }

    private fun buildChatCompletionsJsonResponse(requestJson: String): String {
        val userPrompt = try {
            val obj = gson.fromJson(requestJson, JsonObject::class.java)
            val messages = obj.getAsJsonArray("messages")
            messages.last().asJsonObject.get("content").asString
        } catch (e: Exception) {
            "Hello from API client!"
        }

        val aiContent = "PocketPal Local Server Response:\nProcessed offline on your device for prompt: '$userPrompt'."
        val escapedContent = gson.toJson(aiContent)

        return """
        {
          "id": "chatcmpl-${UUID.randomUUID()}",
          "object": "chat.completion",
          "created": ${System.currentTimeMillis() / 1000},
          "model": "llama-3.2-1b",
          "choices": [
            {
              "index": 0,
              "message": {
                "role": "assistant",
                "content": $escapedContent
              },
              "finish_reason": "stop"
            }
          ],
          "usage": {
            "prompt_tokens": 14,
            "completion_tokens": 28,
            "total_tokens": 42
          }
        }
        """.trimIndent()
    }

    private fun addLog(method: String, endpoint: String, status: Int, reqSnippet: String = "", resSnippet: String = "") {
        val log = ApiRequestLog(
            id = UUID.randomUUID().toString(),
            method = method,
            endpoint = endpoint,
            status = status,
            timestamp = System.currentTimeMillis(),
            requestBodySnippet = reqSnippet,
            responseBodySnippet = resSnippet
        )
        _logs.update { (listOf(log) + it).take(50) }
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    private fun String.toIntOrDefault(default: Int): Int {
        return try { this.toInt() } catch (e: Exception) { default }
    }
}
