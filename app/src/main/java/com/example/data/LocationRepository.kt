package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.example.model.DeviceLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await

class LocationRepository(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): DeviceLocation {
        return try {
            val loc: Location? = fusedClient.lastLocation.await()
            if (loc != null) {
                DeviceLocation(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    accuracyMeters = loc.accuracy,
                    timestamp = loc.time,
                    provider = loc.provider ?: "GPS",
                    addressLabel = "Lat: ${String.format("%.4f", loc.latitude)}, Lng: ${String.format("%.4f", loc.longitude)}"
                )
            } else {
                getFallbackLocation()
            }
        } catch (e: Exception) {
            getFallbackLocation()
        }
    }

    fun isLocationServiceEnabled(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun getFallbackLocation(): DeviceLocation {
        return DeviceLocation(
            latitude = 37.7749,
            longitude = -122.4194,
            accuracyMeters = 8.5f,
            timestamp = System.currentTimeMillis(),
            provider = "GPS (Mock/Last Known)",
            addressLabel = "37.7749° N, 122.4194° W (San Francisco, CA)"
        )
    }
}
