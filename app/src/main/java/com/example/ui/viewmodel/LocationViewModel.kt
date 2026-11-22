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

    private val firestoreRepository = com.example.data.FirestoreRepository(application)
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

    fun refreshLocation(targetDeviceId: String? = null, targetIp: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            // 1. Try Local LAN P2P
            if (!targetIp.isNullOrBlank()) {
                val p2p = com.example.data.NexusP2PManager.getInstance(getApplication())
                val result = p2p.fetchRemoteLocation(targetIp)
                if (result.isSuccess) {
                    _locationState.value = result.getOrThrow()
                    _isLoading.value = false
                    return@launch
                }
            }

            // 2. Fetch remote device's cloud GPS from Firestore
            if (!targetDeviceId.isNullOrBlank()) {
                val dev = firestoreRepository.getDevice(targetDeviceId)
                if (dev != null && dev.lastLatitude != null && dev.lastLongitude != null) {
                    _locationState.value = DeviceLocation(
                        latitude = dev.lastLatitude,
                        longitude = dev.lastLongitude,
                        accuracyMeters = 8.0f,
                        timestamp = dev.lastLocationTime ?: System.currentTimeMillis(),
                        addressLabel = dev.lastLocationAddress ?: "${dev.deviceName} • Real-time GPS Location"
                    )
                    _isLoading.value = false
                    return@launch
                }
            }

            // 3. Fallback to local GPS
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
