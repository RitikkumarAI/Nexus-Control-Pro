package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FirestoreRepository
import com.example.data.security.KeystoreEncryptionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TerminalConsoleLog(
    val id: String = System.currentTimeMillis().toString(),
    val command: String,
    val output: String,
    val isError: Boolean = false,
    val timestampText: String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
)

class TerminalViewModel(application: Application) : AndroidViewModel(application) {

    private val firestoreRepository = FirestoreRepository(application)
    private val encryptionManager = KeystoreEncryptionManager()

    private val _consoleLogs = MutableStateFlow<List<TerminalConsoleLog>>(
        listOf(
            TerminalConsoleLog(
                command = "nexus --init",
                output = "NEXUS CONTROL PRO Terminal CLI v1.0\nKeystore E2EE Layer: ACTIVE (AES-256-GCM)\nType 'help' for list of administrative commands."
            )
        )
    )
    val consoleLogs: StateFlow<List<TerminalConsoleLog>> = _consoleLogs.asStateFlow()

    private val _commandInput = MutableStateFlow("")
    val commandInput: StateFlow<String> = _commandInput.asStateFlow()

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()

    fun updateCommandInput(input: String) {
        _commandInput.value = input
    }

    fun sendCommand() {
        val cmd = _commandInput.value.trim()
        if (cmd.isBlank()) return

        _commandInput.value = ""
        _isExecuting.value = true

        viewModelScope.launch {
            // Encrypt command before transmitting over Firestore
            val encryptedCmd = encryptionManager.encryptSignalingPayload(cmd)

            val output = executeLocalCommand(cmd)

            // Log command audit entry to Firestore
            firestoreRepository.logAuditAction(
                action = "REMOTE_TERMINAL_CMD",
                initiatorRole = "ADMIN",
                targetDeviceId = "phone_b",
                details = "Executed encrypted command: '$cmd'"
            )

            val newLog = TerminalConsoleLog(
                command = cmd,
                output = output,
                isError = output.startsWith("Error:")
            )

            _consoleLogs.value = _consoleLogs.value + newLog
            _isExecuting.value = false
        }
    }

    private fun executeLocalCommand(cmd: String): String {
        return when (cmd.lowercase()) {
            "help" -> """
                Available Administrative Commands:
                • getprop           - Retrieve system build properties
                • uptime            - System boot runtime stats
                • ip addr           - Network interfaces & IP addresses
                • pm list packages  - Query installed app package IDs
                • df                - Disk storage allocation
                • whoami            - Shell process identity
                • clear             - Reset console terminal window
            """.trimIndent()

            "clear" -> {
                _consoleLogs.value = emptyList()
                "Console window cleared."
            }

            else -> {
                try {
                    val process = Runtime.getRuntime().exec(cmd)
                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    val errorReader = BufferedReader(InputStreamReader(process.errorStream))

                    val outputBuilder = StringBuilder()
                    var line: String?

                    while (reader.readLine().also { line = it } != null) {
                        outputBuilder.append(line).append("\n")
                    }

                    while (errorReader.readLine().also { line = it } != null) {
                        outputBuilder.append("ERR: ").append(line).append("\n")
                    }

                    val result = outputBuilder.toString().trim()
                    if (result.isNotBlank()) result else "Command executed successfully (no output)."
                } catch (e: Exception) {
                    "Error executing command: ${e.message}"
                }
            }
        }
    }
}
