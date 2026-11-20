package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CallLogRepository
import com.example.model.CallHistoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CommLogViewModel(application: Application) : AndroidViewModel(application) {

    private val callLogRepository = CallLogRepository(application)

    private val _callLogs = MutableStateFlow<List<CallHistoryItem>>(emptyList())
    val callLogs: StateFlow<List<CallHistoryItem>> = _callLogs.asStateFlow()

    private val _filterType = MutableStateFlow("ALL") // ALL, INCOMING, OUTGOING, MISSED
    val filterType: StateFlow<String> = _filterType.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadCallLogs()
    }

    fun loadCallLogs() {
        viewModelScope.launch {
            _isLoading.value = true
            val logs = callLogRepository.getCallLogs()
            _callLogs.value = logs
            _isLoading.value = false
        }
    }

    fun setFilter(filter: String) {
        _filterType.value = filter
    }

    fun getFilteredCallLogs(): List<CallHistoryItem> {
        val filter = _filterType.value
        if (filter == "ALL") return _callLogs.value
        return _callLogs.value.filter { it.callType.equals(filter, ignoreCase = true) }
    }
}
