package com.example.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class LiveAlertType {
    SUCCESS,
    INFO,
    WARNING,
    ERROR
}

data class LiveAlert(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val type: LiveAlertType = LiveAlertType.INFO,
    val timestamp: Long = System.currentTimeMillis()
)

object LiveAlertManager {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val _currentAlert = MutableStateFlow<LiveAlert?>(null)
    val currentAlert: StateFlow<LiveAlert?> = _currentAlert.asStateFlow()

    fun postAlert(title: String, message: String, type: LiveAlertType = LiveAlertType.INFO, durationMs: Long = 4000L) {
        val alert = LiveAlert(title = title, message = message, type = type)
        _currentAlert.value = alert
        scope.launch {
            delay(durationMs)
            if (_currentAlert.value?.id == alert.id) {
                _currentAlert.value = null
            }
        }
    }

    fun dismiss() {
        _currentAlert.value = null
    }
}
