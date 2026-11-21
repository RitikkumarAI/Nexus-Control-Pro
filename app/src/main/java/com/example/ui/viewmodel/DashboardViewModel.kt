package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BatteryCalibrationManager
import com.example.data.EnterpriseSuiteManager
import com.example.data.FirestoreRepository
import com.example.data.GeofenceAlertManager
import com.example.data.GeofenceConfig
import com.example.data.DeviceSignalStabilityProfile
import com.example.data.LocalDeviceManager
import com.example.data.NetworkScannerManager
import com.example.data.NetworkSignalStabilityManager
import com.example.data.OfflineSyncQueueManager
import com.example.data.PerformanceRepository
import com.example.data.local.PerformanceMetricEntity
import com.example.data.local.SyncQueueEntity
import com.example.model.AuditLogEntry
import com.example.model.BatteryCalibrationReport
import com.example.model.BatteryDischargeReport
import com.example.model.ComplianceReport
import com.example.model.FleetBroadcastAlert
import com.example.model.GeofencePushAlert
import com.example.model.KioskConfig
import com.example.model.NetworkDiagnosticsReport
import com.example.model.NexusDevice
import com.example.model.StorageOptimizationReport
import com.example.model.WifiProvisioningProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BatteryMetricState(
    val level: Int = 100,
    val isCharging: Boolean = false
)

data class StorageMetricState(
    val usedGb: Double = 0.0,
    val totalGb: Double = 128.0,
    val percentageUsed: Float = 0f
)

data class NetworkMetricState(
    val type: String = "Wi-Fi",
    val isOnline: Boolean = true
)

data class RamMetricState(
    val usedGb: Double = 5.2,
    val totalGb: Double = 8.0,
    val percentageUsed: Float = 0.65f
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val firestoreRepository = FirestoreRepository(application)
    private val localDeviceManager = LocalDeviceManager(application)
    private val performanceRepository = PerformanceRepository(application)
    private val batteryCalibrationManager = BatteryCalibrationManager(application)
    private val networkScannerManager = NetworkScannerManager(application)
    private val networkSignalStabilityManager = NetworkSignalStabilityManager(application)
    private val geofenceAlertManager = GeofenceAlertManager(application, firestoreRepository)
    private val offlineSyncQueueManager = OfflineSyncQueueManager(application, firestoreRepository)
    private val enterpriseSuiteManager = EnterpriseSuiteManager(application, firestoreRepository)

    private val _managedDeviceState = MutableStateFlow<NexusDevice?>(null)
    val managedDeviceState: StateFlow<NexusDevice?> = _managedDeviceState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val performanceMetricsState: StateFlow<List<PerformanceMetricEntity>> = performanceRepository.recentMetrics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val batteryCalibrationReport: StateFlow<BatteryCalibrationReport> = batteryCalibrationManager.calibrationReport
    val batteryDischargeReport: StateFlow<BatteryDischargeReport> = batteryCalibrationManager.dischargeReport
    val isCalibratingBattery: StateFlow<Boolean> = batteryCalibrationManager.isCalibrating
    val calibrationProgress: StateFlow<Float> = batteryCalibrationManager.calibrationProgress

    val networkDiagnosticsReport: StateFlow<NetworkDiagnosticsReport> = networkScannerManager.diagnosticsReport
    val isScanningWifi: StateFlow<Boolean> = networkScannerManager.isScanning

    private val _signalStabilityProfiles = MutableStateFlow(networkSignalStabilityManager.getDeviceProfiles())
    val signalStabilityProfiles: StateFlow<List<DeviceSignalStabilityProfile>> = _signalStabilityProfiles.asStateFlow()

    val geofenceConfig: StateFlow<GeofenceConfig> = geofenceAlertManager.geofenceConfig
    val geofencePushAlertsEnabled: StateFlow<Boolean> = geofenceAlertManager.pushAlertsEnabled
    val geofenceAlerts: StateFlow<List<GeofencePushAlert>> = geofenceAlertManager.alertHistory

    val syncQueueItems: StateFlow<List<SyncQueueEntity>> = offlineSyncQueueManager.queueItems
    val pendingSyncCount: StateFlow<Int> = offlineSyncQueueManager.pendingCount
    val isOfflineBufferMode: StateFlow<Boolean> = offlineSyncQueueManager.isOfflineBufferMode
    val isFlushingSyncQueue: StateFlow<Boolean> = offlineSyncQueueManager.isFlushing
    val auditLogs: StateFlow<List<AuditLogEntry>> = firestoreRepository.observeAuditLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val kioskConfig: StateFlow<KioskConfig> = enterpriseSuiteManager.kioskConfig
    val complianceReport: StateFlow<ComplianceReport> = enterpriseSuiteManager.complianceReport
    val wifiProfiles: StateFlow<List<WifiProvisioningProfile>> = enterpriseSuiteManager.wifiProfiles
    val fleetBroadcasts: StateFlow<List<FleetBroadcastAlert>> = enterpriseSuiteManager.fleetBroadcasts
    val storageReport: StateFlow<StorageOptimizationReport> = enterpriseSuiteManager.storageReport
    val isOptimizingStorage: StateFlow<Boolean> = enterpriseSuiteManager.isOptimizingStorage

    init {
        geofenceAlertManager.startAdminGeofenceListener()
    }

    fun hasNotificationPermission(): Boolean = geofenceAlertManager.hasNotificationPermission()

    // Derived StateFlows for specific system metrics observed from Firestore
    val batteryState: StateFlow<BatteryMetricState> = _managedDeviceState
        .map { device ->
            BatteryMetricState(
                level = device?.batteryLevel ?: 87,
                isCharging = device?.isCharging ?: true
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BatteryMetricState(87, true))

    val storageState: StateFlow<StorageMetricState> = _managedDeviceState
        .map { device ->
            val used = device?.usedStorageGb ?: 76.4
            val total = device?.totalStorageGb ?: 128.0
            val pct = if (total > 0) (used / total).toFloat() else 0.60f
            StorageMetricState(usedGb = used, totalGb = total, percentageUsed = pct)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StorageMetricState(76.4, 128.0, 0.60f))

    val networkState: StateFlow<NetworkMetricState> = _managedDeviceState
        .map { device ->
            NetworkMetricState(
                type = device?.networkType ?: "Jio 4G",
                isOnline = device?.isOnline ?: true
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NetworkMetricState("Jio 4G", true))

    val ramState: StateFlow<RamMetricState> = _managedDeviceState
        .map { device ->
            val used = device?.usedRamGb ?: 5.2
            val total = device?.totalRamGb ?: 8.0
            val pct = if (total > 0) (used / total).toFloat() else 0.65f
            RamMetricState(usedGb = used, totalGb = total, percentageUsed = pct)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RamMetricState())

    fun observeRemoteDevice(targetDeviceId: String) {
        viewModelScope.launch {
            firestoreRepository.observeDevice(targetDeviceId).collect { device ->
                if (device != null) {
                    _managedDeviceState.value = device
                    // Automatically evaluate geofence boundary when managed device reports GPS coordinates
                    val lat = device.lastLatitude
                    val lng = device.lastLongitude
                    if (lat != null && lng != null) {
                        geofenceAlertManager.evaluateLocationForGeofenceBreach(
                            currentLat = lat,
                            currentLng = lng,
                            deviceId = device.deviceId,
                            deviceName = device.deviceName
                        )
                    }
                } else {
                    _managedDeviceState.value = NexusDevice(
                        deviceId = targetDeviceId,
                        deviceName = "Field Phone A",
                        manufacturer = "Realme",
                        model = "Realme 11 Pro 5G",
                        batteryLevel = 87,
                        isCharging = true,
                        networkType = "Jio 4G",
                        totalStorageGb = 128.0,
                        usedStorageGb = 76.4,
                        isOnline = true
                    )
                }
            }
        }
    }

    fun refreshTelemetry(deviceId: String, deviceName: String, role: String) {
        viewModelScope.launch {
            _isRefreshing.value = true
            val remoteIp = _managedDeviceState.value?.ipAddress
            if (!remoteIp.isNullOrBlank()) {
                val p2p = com.example.data.NexusP2PManager.getInstance(getApplication())
                val result = p2p.fetchRemoteTelemetry(remoteIp)
                if (result.isSuccess) {
                    _managedDeviceState.value = result.getOrThrow()
                    com.example.data.LiveAlertManager.postAlert(
                        title = "Telemetry Synchronized",
                        message = "Live data synced directly from ${_managedDeviceState.value?.deviceName ?: "managed device"}",
                        type = com.example.data.LiveAlertType.SUCCESS
                    )
                    _isRefreshing.value = false
                    return@launch
                }
            }

            val updated = localDeviceManager.getLocalDeviceTelemetry(deviceId, deviceName, role)
            _managedDeviceState.value = updated
            firestoreRepository.updateDeviceTelemetry(updated)
            performanceRepository.recordCurrentMetrics()
            _isRefreshing.value = false
        }
    }

    fun recalibrateBattery() {
        viewModelScope.launch {
            batteryCalibrationManager.runAutomatedCalibration()
            firestoreRepository.logAuditAction(
                action = "BATTERY_CALIBRATION",
                initiatorRole = "ADMIN",
                targetDeviceId = _managedDeviceState.value?.deviceId ?: "phone_b",
                details = "Executed automated Coulomb-counter calibration analysis. Capacity: ${batteryCalibrationReport.value.estimatedCapacityMah} mAh (${batteryCalibrationReport.value.healthPercentage}%)"
            )
        }
    }

    fun scanNearbyWifiNetworks() {
        viewModelScope.launch {
            val report = networkScannerManager.refreshScan()
            firestoreRepository.logAuditAction(
                action = "WIFI_NETWORK_SCAN",
                initiatorRole = "ADMIN",
                targetDeviceId = _managedDeviceState.value?.deviceId ?: "phone_b",
                details = "Network diagnostic scan completed. Detected ${report.accessPoints.size} access points. Connected: ${report.connectedSsid}"
            )
        }
    }

    fun toggleGeofencePushAlerts(enabled: Boolean) {
        geofenceAlertManager.setPushAlertsEnabled(enabled)
    }

    fun updateGeofenceRadius(radiusMeters: Float) {
        geofenceAlertManager.updateBoundaryRadius(radiusMeters)
    }

    fun triggerGeofenceBoundaryTest() {
        val device = _managedDeviceState.value
        geofenceAlertManager.triggerBoundaryTestCheck(
            deviceId = device?.deviceId ?: "phone_a",
            deviceName = device?.deviceName ?: "Field Phone A"
        )
    }

    fun acknowledgeGeofenceAlert(alertId: String) {
        geofenceAlertManager.acknowledgeAlert(alertId)
    }

    fun recordPerformanceSample() {
        viewModelScope.launch {
            performanceRepository.recordCurrentMetrics()
        }
    }

    fun toggleRemoteFocusMode(deviceId: String, enable: Boolean) {
        if (isOfflineBufferMode.value || !offlineSyncQueueManager.checkNetworkConnected()) {
            offlineSyncQueueManager.enqueueOrExecute(
                operationType = "FOCUS_MODE_UPDATE",
                targetDeviceId = deviceId,
                payloadSummary = if (enable) "Enable Focus Mode (DND)" else "Disable Focus Mode",
                payloadData = enable.toString()
            )
            return
        }
        viewModelScope.launch {
            firestoreRepository.sendFocusModeCommand(deviceId, enable)
        }
    }

    fun setRemoteVolume(deviceId: String, ringer: Int?, media: Int?, alarm: Int?) {
        if (isOfflineBufferMode.value || !offlineSyncQueueManager.checkNetworkConnected()) {
            offlineSyncQueueManager.enqueueOrExecute(
                operationType = "AUDIO_POLICY_UPDATE",
                targetDeviceId = deviceId,
                payloadSummary = "Audio Policy (Ring=${ringer ?: "-"}%, Media=${media ?: "-"}%)",
                payloadData = "${ringer ?: ""},${media ?: ""},${alarm ?: ""}"
            )
            return
        }
        viewModelScope.launch {
            firestoreRepository.sendVolumeCommand(deviceId, ringer, media, alarm)
        }
    }

    fun setOfflineBufferMode(offline: Boolean) {
        offlineSyncQueueManager.setOfflineBufferMode(offline)
    }

    fun enqueueSettingsSync() {
        val deviceId = _managedDeviceState.value?.deviceId ?: "phone_a"
        offlineSyncQueueManager.enqueueOrExecute(
            operationType = "SETTINGS_SYNC",
            targetDeviceId = deviceId,
            payloadSummary = "Telemetry cadence (15m) & Wi-Fi scan policy",
            payloadData = "telemetryIntervalMin=15;wifiScanEnabled=true"
        )
    }

    fun enqueueGeofencePolicySync() {
        val deviceId = _managedDeviceState.value?.deviceId ?: "phone_a"
        val radius = geofenceConfig.value.radiusMeters.toInt()
        offlineSyncQueueManager.enqueueOrExecute(
            operationType = "GEOFENCE_UPDATE",
            targetDeviceId = deviceId,
            payloadSummary = "Geofence perimeter (${radius}m radius) & push alerts",
            payloadData = "radiusMeters=$radius;alertsEnabled=${geofencePushAlertsEnabled.value}"
        )
    }

    fun flushSyncQueue() {
        offlineSyncQueueManager.flushPendingQueue()
    }

    fun clearCompletedSyncItems() {
        offlineSyncQueueManager.clearCompleted()
    }

    fun refreshSignalStabilityProfiles() {
        _signalStabilityProfiles.value = networkSignalStabilityManager.getDeviceProfiles()
    }

    // --- ENTERPRISE SUITE ACTIONS ---
    fun toggleKioskMode(deviceId: String, enabled: Boolean, singleApp: String? = null) {
        enterpriseSuiteManager.toggleKioskMode(deviceId, enabled, singleApp)
    }

    fun remediateAllCompliance(deviceId: String) {
        enterpriseSuiteManager.remediateAllCompliance(deviceId)
    }

    fun pushWifiProfile(ssid: String, securityType: String, isHidden: Boolean, autoConnect: Boolean) {
        enterpriseSuiteManager.pushNewWifiProfile(ssid, securityType, isHidden, autoConnect)
    }

    fun revokeWifiProfile(profileId: String) {
        enterpriseSuiteManager.revokeWifiProfile(profileId)
    }

    fun sendFleetBroadcast(title: String, message: String, priority: String, audioChime: Boolean) {
        enterpriseSuiteManager.sendBroadcast(title, message, priority, audioChime)
    }

    fun acknowledgeFleetBroadcast(alertId: String, deviceId: String) {
        enterpriseSuiteManager.acknowledgeBroadcast(alertId, deviceId)
    }

    fun triggerStorageCleanup(deviceId: String) {
        enterpriseSuiteManager.triggerStorageCleanup(deviceId)
    }
}
