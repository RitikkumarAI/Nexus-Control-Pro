package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LocationRepository
import com.example.model.DeviceLocation
import com.example.service.LocationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val locationRepository = LocationRepository(application)

    private val _locationState = MutableStateFlow<DeviceLocation?>(null)
    val locationState: StateFlow<DeviceLocation?> = _locationState.asStateFlow()

    private val _isContinuousTrackingActive = MutableStateFlow(false)
    val isContinuousTrackingActive: StateFlow<Boolean> = _isContinuousTrackingActive.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        refreshLocation()
    }

    fun refreshLocation() {
        viewModelScope.launch {
            _isLoading.value = true
            val loc = locationRepository.getCurrentLocation()
            _locationState.value = loc
            _isLoading.value = false
        }
    }

    fun toggleContinuousTracking(enable: Boolean) {
        val app = getApplication<Application>()
        val intent = Intent(app, LocationService::class.java).apply {
            action = if (enable) LocationService.ACTION_START else LocationService.ACTION_STOP
        }
        if (enable) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                app.startForegroundService(intent)
            } else {
                app.startService(intent)
            }
        } else {
            app.startService(intent)
        }
        _isContinuousTrackingActive.value = enable
    }
}
