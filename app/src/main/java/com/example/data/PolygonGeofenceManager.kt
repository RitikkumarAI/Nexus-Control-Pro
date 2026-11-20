package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GeoLatLng(
    val latitude: Double,
    val longitude: Double
)

data class PolygonZone(
    val zoneId: String,
    val zoneLabel: String,
    val vertices: List<GeoLatLng>
)

data class PolygonZoneAlert(
    val zoneId: String,
    val zoneLabel: String,
    val eventType: String, // "ENTERED" or "EXITED"
    val timestamp: Long = System.currentTimeMillis()
)

class PolygonGeofenceManager(
    private val context: Context,
    private val firestoreRepository: FirestoreRepository = FirestoreRepository(context)
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _polygonZones = MutableStateFlow<List<PolygonZone>>(
        listOf(
            PolygonZone(
                zoneId = "z1",
                zoneLabel = "Main Campus Zone",
                vertices = listOf(
                    GeoLatLng(26.8500, 80.9400),
                    GeoLatLng(26.8500, 80.9600),
                    GeoLatLng(26.8300, 80.9600),
                    GeoLatLng(26.8300, 80.9400)
                )
            )
        )
    )
    val polygonZones: StateFlow<List<PolygonZone>> = _polygonZones.asStateFlow()

    private val activeZoneStates = mutableMapOf<String, Boolean>() // zoneId -> isInside

    fun addPolygonZone(zone: PolygonZone) {
        _polygonZones.value = _polygonZones.value + zone
    }

    /**
     * Evaluates current GPS location against all defined custom polygon zones using Ray-Casting Algorithm
     */
    fun evaluatePolygonGeofence(currentLat: Double, currentLng: Double, deviceId: String = "phone_b") {
        val currentPoint = GeoLatLng(currentLat, currentLng)

        for (zone in _polygonZones.value) {
            val isInsideNow = isPointInPolygon(currentPoint, zone.vertices)
            val wasInsideBefore = activeZoneStates[zone.zoneId] ?: false

            if (isInsideNow && !wasInsideBefore) {
                // Zone Entered Alert
                activeZoneStates[zone.zoneId] = true
                triggerZoneAlert(zone, "ENTERED", deviceId)
            } else if (!isInsideNow && wasInsideBefore) {
                // Zone Exited Alert
                activeZoneStates[zone.zoneId] = false
                triggerZoneAlert(zone, "EXITED", deviceId)
            }
        }
    }

    /**
     * Ray-Casting Algorithm to determine point-in-polygon containment
     */
    private fun isPointInPolygon(point: GeoLatLng, polygon: List<GeoLatLng>): Boolean {
        if (polygon.size < 3) return false
        var isInside = false
        var j = polygon.size - 1

        for (i in polygon.indices) {
            val pi = polygon[i]
            val pj = polygon[j]

            val intersect = ((pi.longitude > point.longitude) != (pj.longitude > point.longitude)) &&
                    (point.latitude < (pj.latitude - pi.latitude) * (point.longitude - pi.longitude) / (pj.longitude - pi.longitude) + pi.latitude)

            if (intersect) {
                isInside = !isInside
            }
            j = i
        }
        return isInside
    }

    private fun triggerZoneAlert(zone: PolygonZone, eventType: String, deviceId: String) {
        val msg = "POLYGON GEOFENCE ALERT: Device $eventType '${zone.zoneLabel}'"
        Log.w("PolygonGeofenceManager", msg)

        scope.launch {
            try {
                firestoreRepository.logAuditAction(
                    action = "POLYGON_ZONE_${eventType}",
                    initiatorRole = "SYSTEM",
                    targetDeviceId = deviceId,
                    details = msg
                )
            } catch (e: Exception) {
                Log.e("PolygonGeofenceManager", "Error logging polygon alert: ${e.message}")
            }
        }
    }
}
