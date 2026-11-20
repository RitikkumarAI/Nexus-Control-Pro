package com.example.data

import android.content.Context
import android.location.Location
import android.util.Log
import com.example.model.GeofencePushAlert
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class GeofenceConfig(
    val zoneName: String = "Primary Safe Zone",
    val centerLatitude: Double = 26.8467, // Default Lucknow center
    val centerLongitude: Double = 80.9462,
    val radiusMeters: Float = 500f, // 500 meter safe zone
    val isGeofenceActive: Boolean = true
)

data class GeofenceBreachAlert(
    val isBreached: Boolean = false,
    val currentDistanceMeters: Float = 0f,
    val maxAllowedRadiusMeters: Float = 500f,
    val alertMessage: String? = null
)

class GeofenceAlertManager(
    private val context: Context,
    private val firestoreRepository: FirestoreRepository = FirestoreRepository(context)
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val notificationHelper = GeofenceNotificationHelper(context)

    private val _geofenceConfig = MutableStateFlow(GeofenceConfig())
    val geofenceConfig: StateFlow<GeofenceConfig> = _geofenceConfig.asStateFlow()

    private val _breachAlert = MutableStateFlow(GeofenceBreachAlert())
    val breachAlert: StateFlow<GeofenceBreachAlert> = _breachAlert.asStateFlow()

    private val _pushAlertsEnabled = MutableStateFlow(true)
    val pushAlertsEnabled: StateFlow<Boolean> = _pushAlertsEnabled.asStateFlow()

    private val _alertHistory = MutableStateFlow<List<GeofencePushAlert>>(emptyList())
    val alertHistory: StateFlow<List<GeofencePushAlert>> = _alertHistory.asStateFlow()

    private val dispatchedNotificationIds = mutableSetOf<String>()
    private var isListeningToFirestore = false

    fun hasNotificationPermission(): Boolean = notificationHelper.hasNotificationPermission()

    fun setPushAlertsEnabled(enabled: Boolean) {
        _pushAlertsEnabled.value = enabled
        if (enabled) {
            notificationHelper.ensureNotificationChannel()
        }
    }

    fun configureGeofence(
        centerLat: Double,
        centerLng: Double,
        radiusMeters: Float,
        zoneName: String = _geofenceConfig.value.zoneName
    ) {
        _geofenceConfig.value = GeofenceConfig(
            zoneName = zoneName,
            centerLatitude = centerLat,
            centerLongitude = centerLng,
            radiusMeters = radiusMeters,
            isGeofenceActive = true
        )
    }

    fun updateBoundaryRadius(radiusMeters: Float) {
        _geofenceConfig.value = _geofenceConfig.value.copy(
            radiusMeters = radiusMeters,
            isGeofenceActive = true
        )
    }

    /**
     * Subscribes the Admin device to Firestore geofence breach events and automatically
     * dispatches high-priority local push notifications for newly detected boundary exits.
     */
    fun startAdminGeofenceListener() {
        if (isListeningToFirestore) return
        isListeningToFirestore = true
        scope.launch {
            firestoreRepository.observeGeofenceAlerts().collect { remoteAlerts ->
                if (remoteAlerts.isNotEmpty()) {
                    _alertHistory.value = remoteAlerts
                    if (_pushAlertsEnabled.value) {
                        remoteAlerts
                            .filter { !it.acknowledged && it.alertId !in dispatchedNotificationIds }
                            .forEach { newAlert ->
                                dispatchedNotificationIds.add(newAlert.alertId)
                                notificationHelper.dispatchGeofenceBreachNotification(newAlert)
                            }
                    }
                }
            }
        }
    }

    /**
     * Checks if current GPS coordinates exceed configured radius, publishes the breach to
     * Firestore, and dispatches a push notification on the admin device.
     */
    fun evaluateLocationForGeofenceBreach(
        currentLat: Double,
        currentLng: Double,
        deviceId: String = "phone_b",
        deviceName: String = "Field Phone A"
    ) {
        val config = _geofenceConfig.value
        if (!config.isGeofenceActive) return

        val results = FloatArray(1)
        Location.distanceBetween(
            config.centerLatitude,
            config.centerLongitude,
            currentLat,
            currentLng,
            results
        )

        val distanceMeters = results[0]
        val isOutsideRadius = distanceMeters > config.radiusMeters

        val alert = if (isOutsideRadius) {
            val msg = "${deviceName} exited ${config.zoneName} (${distanceMeters.toInt()}m from center, limit ${config.radiusMeters.toInt()}m)"
            GeofenceBreachAlert(
                isBreached = true,
                currentDistanceMeters = distanceMeters,
                maxAllowedRadiusMeters = config.radiusMeters,
                alertMessage = msg
            )
        } else {
            GeofenceBreachAlert(
                isBreached = false,
                currentDistanceMeters = distanceMeters,
                maxAllowedRadiusMeters = config.radiusMeters,
                alertMessage = null
            )
        }

        _breachAlert.value = alert

        if (isOutsideRadius) {
            val pushAlert = GeofencePushAlert(
                alertId = "geo_${UUID.randomUUID().toString().take(8)}",
                deviceId = deviceId,
                deviceName = deviceName,
                zoneName = config.zoneName,
                latitude = currentLat,
                longitude = currentLng,
                distanceFromCenterMeters = distanceMeters,
                boundaryRadiusMeters = config.radiusMeters,
                message = alert.alertMessage ?: "Managed device moved outside geofence boundary.",
                timestamp = System.currentTimeMillis(),
                acknowledged = false
            )

            // Update immediate local feed & trigger push notification on admin device
            _alertHistory.value = listOf(pushAlert) + _alertHistory.value
            dispatchedNotificationIds.add(pushAlert.alertId)
            if (_pushAlertsEnabled.value) {
                notificationHelper.dispatchGeofenceBreachNotification(pushAlert)
            }

            scope.launch {
                try {
                    firestoreRepository.publishGeofenceAlert(pushAlert)
                    firestoreRepository.logAuditAction(
                        action = "GEOFENCE_BREACH_ALERT",
                        initiatorRole = "SYSTEM",
                        targetDeviceId = deviceId,
                        details = pushAlert.message
                    )
                } catch (e: Exception) {
                    Log.e("GeofenceAlertManager", "Failed to publish geofence alert: ${e.message}")
                }
            }
        }
    }

    /**
     * Allows the administrator to test the end-to-end geofence breach notification pipeline
     * by evaluating a coordinate just outside the currently established boundary radius.
     */
    fun triggerBoundaryTestCheck(deviceId: String = "phone_b", deviceName: String = "Field Phone A") {
        val config = _geofenceConfig.value
        // Offset latitude by ~1.4x the configured boundary radius (~111,111m per degree)
        val deltaDegrees = ((config.radiusMeters * 1.45) / 111_111.0)
        evaluateLocationForGeofenceBreach(
            currentLat = config.centerLatitude + deltaDegrees,
            currentLng = config.centerLongitude,
            deviceId = deviceId,
            deviceName = deviceName
        )
    }

    fun acknowledgeAlert(alertId: String) {
        _alertHistory.value = _alertHistory.value.map { item ->
            if (item.alertId == alertId) item.copy(acknowledged = true) else item
        }
        val stillHasUnacked = _alertHistory.value.any { !it.acknowledged }
        if (!stillHasUnacked) {
            _breachAlert.value = _breachAlert.value.copy(isBreached = false, alertMessage = null)
        }
        scope.launch {
            firestoreRepository.acknowledgeGeofenceAlert(alertId)
        }
    }
}
