package com.pocketpal.ui.screens.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpal.data.server.LocalhostApiServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

class LocalhostServerViewModel(val server: LocalhostApiServer) : ViewModel() {

    val isRunning = server.isRunning
    val port = server.port
    val logs = server.logs

    private val _testResponse = MutableStateFlow("")
    val testResponse = _testResponse.asStateFlow()

    fun toggleServer() {
        if (isRunning.value) {
            server.stopServer()
        } else {
            server.startServer(port.value)
        }
    }

    fun setPort(newPort: Int) {
        if (!isRunning.value) {
            server.startServer(newPort)
            server.stopServer()
        }
    }

    fun testLocalhostEndpoint() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentPort = port.value
                val socket = Socket("127.0.0.1", currentPort)
                val writer = PrintWriter(socket.getOutputStream(), true)
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

                val payload = "{\"model\":\"llama-3.2-1b\",\"messages\":[{\"role\":\"user\",\"content\":\"Test local server endpoint\"}]}"
                val bytes = payload.toByteArray()

                writer.print("POST /v1/chat/completions HTTP/1.1\r\n")
                writer.print("Host: localhost:$currentPort\r\n")
                writer.print("Content-Type: application/json\r\n")
                writer.print("Content-Length: ${bytes.size}\r\n")
                writer.print("Connection: close\r\n")
                writer.print("\r\n")
                writer.print(payload)
                writer.flush()

                val sb = StringBuilder()
                var line: String?
                var isBody = false
                while (reader.readLine().also { line = it } != null) {
                    if (line.isNullOrEmpty()) {
                        isBody = true
                        continue
                    }
                    if (isBody) {
                        sb.append(line).append("\n")
                    }
                }
                socket.close()

                withContext(Dispatchers.Main) {
                    _testResponse.value = sb.toString()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _testResponse.value = "Test Failed: ${e.message}"
                }
            }
        }
    }

    fun clearLogs() {
        server.clearLogs()
    }
}
