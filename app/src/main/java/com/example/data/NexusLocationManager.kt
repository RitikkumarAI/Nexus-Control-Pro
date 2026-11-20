package com.example.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.model.DeviceLocation
import com.example.model.NexusDevice
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class NexusLocationManager(
    private val context: Context,
    private val firestoreRepository: FirestoreRepository = FirestoreRepository(context)
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _currentLocation = MutableStateFlow<DeviceLocation?>(null)
    val currentLocation: StateFlow<DeviceLocation?> = _currentLocation.asStateFlow()

    private var isTracking = false
    private var targetDeviceId = "phone_b"

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            val geocoder = Geocoder(context, Locale.getDefault())
            var addressText = "Lat: ${location.latitude}, Lng: ${location.longitude}"

            try {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    addressText = "${addr.subLocality ?: addr.locality ?: ""}, ${addr.adminArea ?: ""}"
                }
            } catch (e: Exception) {
                Log.d("NexusLocationManager", "Geocoder fallback: ${e.message}")
            }

            val deviceLocation = DeviceLocation(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracyMeters = location.accuracy,
                timestamp = System.currentTimeMillis(),
                addressLabel = addressText
            )

            _currentLocation.value = deviceLocation

            // Post location to Firestore for Location Management phase
            scope.launch {
                try {
                    firestoreRepository.updateDeviceTelemetry(
                        NexusDevice(
                            deviceId = targetDeviceId,
                            lastLatitude = location.latitude,
                            lastLongitude = location.longitude,
                            lastLocationAddress = addressText,
                            lastLocationTime = System.currentTimeMillis()
                        )
                    )
                } catch (e: Exception) {
                    Log.e("NexusLocationManager", "Firestore location update error: ${e.message}")
                }
            }
        }
    }

    fun hasLocationPermissions(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }

    fun startLocationUpdates(deviceId: String = "phone_b", intervalMs: Long = 10000L) {
        if (!hasLocationPermissions() || isTracking) return

        try {
            this.targetDeviceId = deviceId
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
                .setMinUpdateIntervalMillis(5000L)
                .build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            isTracking = true
            Log.d("NexusLocationManager", "Started location tracking updates for $deviceId")
        } catch (e: SecurityException) {
            Log.e("NexusLocationManager", "SecurityException requesting location: ${e.message}")
        }
    }

    fun stopLocationUpdates() {
        if (!isTracking) return
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            isTracking = false
            Log.d("NexusLocationManager", "Stopped location tracking updates.")
        } catch (e: Exception) {
            Log.e("NexusLocationManager", "Error stopping location updates: ${e.message}")
        }
    }
}
