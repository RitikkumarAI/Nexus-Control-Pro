package com.example.model

enum class UserRole {
    UNSET,
    ADMIN,
    MANAGED
}

enum class PairingStatus {
    IDLE,
    GENERATED,
    CONNECTING,
    PAIRED,
    REVOKED,
    EXPIRED
}

data class NexusDevice(
    val deviceId: String = "",
    val deviceName: String = "Android Device",
    val manufacturer: String = "Generic",
    val model: String = "Model",
    val androidVersion: String = "Android 14",
    val batteryLevel: Int = 100,
    val isCharging: Boolean = false,
    val networkType: String = "Wi-Fi",
    val isOnline: Boolean = true,
    val totalStorageGb: Double = 128.0,
    val usedStorageGb: Double = 45.2,
    val totalRamGb: Double = 8.0,
    val usedRamGb: Double = 3.5,
    val uptimeHours: Long = 12,
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val role: String = UserRole.MANAGED.name,
    val pairedAdminId: String? = null,
    val lastLatitude: Double? = null,
    val lastLongitude: Double? = null,
    val lastLocationAddress: String? = null,
    val lastLocationTime: Long? = null,
    val reboot_command: Boolean = false,
    val focusModeActive: Boolean = false,
    val targetFocusMode: Boolean? = null,
    val ringerVolume: Int = 80,
    val mediaVolume: Int = 70,
    val alarmVolume: Int = 85,
    val targetRingerVolume: Int? = null,
    val targetMediaVolume: Int? = null,
    val targetAlarmVolume: Int? = null,
    val volumeCommandTimestamp: Long? = null,
    val targetPermissionPackage: String? = null,
    val targetPermissionName: String? = null,
    val targetPermissionGrant: Boolean? = null,
    val permissionCommandTimestamp: Long? = null,
    val isHighPriority: Boolean = false,
    val isLocked: Boolean = false,
    val kioskModeActive: Boolean = false,
    val kioskSingleAppPackage: String? = null,
    val ipAddress: String? = null,
    val p2pPort: Int = 8899
)

data class SensitivePermissionState(
    val permissionKey: String = "",
    val displayName: String = "",
    val isGranted: Boolean = false,
    val iconType: String = "LOCATION"
)

data class BatteryOptimizationItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val estimatedSavingHours: Float = 0.5f,
    val appPackage: String? = null,
    val actionLabel: String = "Restrict App",
    val category: String = "APP_RESTRICTION"
)

data class AiBatteryInsight(
    val executiveSummary: String = "",
    val healthStatus: String = "ELEVATED DRAIN",
    val drainSlopePerHour: Float = 3.8f,
    val projectedLifespanHours: Float = 14.2f,
    val primaryDrainSources: List<String> = emptyList(),
    val actionableSuggestions: List<BatteryOptimizationItem> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

data class DevicePairing(
    val pairingId: String = "",
    val managedDeviceId: String = "",
    val adminDeviceId: String = "",
    val pairingCode: String = "",
    val status: String = PairingStatus.GENERATED.name,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (10 * 60 * 1000), // 10 minutes
    val managedDeviceName: String = "",
    val adminDeviceName: String = "",
    val managedIp: String? = null,
    val managedPort: Int = 8899
)

data class SignalingSession(
    val sessionId: String = "",
    val pairingId: String = "",
    val managedDeviceId: String = "",
    val adminDeviceId: String = "",
    val sessionType: String = "SCREEN_SHARE", // SCREEN_SHARE or AUDIO_ASSIST
    val status: String = "OFFERING", // OFFERING, CONNECTED, ENDED
    val sdpOffer: String = "",
    val sdpAnswer: String = "",
    val iceCandidates: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

data class AuditLogEntry(
    val id: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val action: String = "",
    val initiatorRole: String = "",
    val targetDeviceId: String = "",
    val details: String = "",
    val success: Boolean = true,
    val adminUser: String = "Ritik Kumar (Admin)",
    val outcome: String = if (success) "SUCCESS" else "FAILED",
    val previousHash: String = "GENESIS",
    val entryHash: String = ""
)

data class BatteryCalibrationReport(
    val estimatedCapacityMah: Int = 4780,
    val designCapacityMah: Int = 5000,
    val healthPercentage: Float = 95.6f,
    val cycleCount: Int = 142,
    val chargeEfficiencyPercent: Float = 94.2f,
    val temperatureCelsius: Float = 31.4f,
    val voltageMv: Int = 4120,
    val calibrationStatus: String = "CALIBRATED", // CALIBRATING, CALIBRATED, DEGRADED
    val batteryTechnology: String = "Li-poly",
    val recommendations: List<String> = listOf(
        "Maintain battery charge between 20% and 80% to maximize cycle life",
        "Avoid high-temperature fast charging above 40°C",
        "Battery degradation rate is normal (-0.4% / month)"
    ),
    val timestamp: Long = System.currentTimeMillis()
)

data class WifiAccessPointInfo(
    val ssid: String = "",
    val bssid: String = "",
    val rssiDbm: Int = -60,
    val signalLevelPct: Int = 80,
    val frequencyMhz: Int = 5180,
    val band: String = "5 GHz",
    val securityType: String = "WPA2/WPA3",
    val channel: Int = 36,
    val isCurrentConnection: Boolean = false
)

data class NetworkDiagnosticsReport(
    val connectedSsid: String? = null,
    val linkSpeedMbps: Int = 433,
    val frequencyMhz: Int = 5180,
    val rssiDbm: Int = -55,
    val ipAddress: String = "192.168.1.145",
    val gatewayAddress: String = "192.168.1.1",
    val accessPoints: List<WifiAccessPointInfo> = emptyList(),
    val channelCongestionRating: String = "LOW",
    val diagnosticTimestamp: Long = System.currentTimeMillis()
)

data class BatteryDegradationPoint(
    val periodLabel: String = "",
    val calibratedBaselineMah: Int = 5000,
    val measuredCapacityMah: Int = 4780,
    val dischargeRatePctPerHr: Float = 3.2f,
    val cycleCount: Int = 0
)

data class BatteryDischargeReport(
    val initialCalibratedMah: Int = 5000,
    val currentCapacityMah: Int = 4740,
    val capacityRetainedPct: Float = 94.8f,
    val degradationDeltaMah: Int = 260,
    val avgDischargeRatePctPerHr: Float = 3.4f,
    val estimatedScreenOnHours: Float = 11.4f,
    val degradationStatus: String = "HEALTHY",
    val timelinePoints: List<BatteryDegradationPoint> = emptyList(),
    val generatedAt: Long = System.currentTimeMillis()
)

data class GeofencePushAlert(
    val alertId: String = "",
    val deviceId: String = "",
    val deviceName: String = "Managed Device",
    val zoneName: String = "Primary Safe Zone",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val distanceFromCenterMeters: Float = 0f,
    val boundaryRadiusMeters: Float = 500f,
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val acknowledged: Boolean = false
)

data class KioskConfig(
    val enabled: Boolean = false,
    val allowedPackages: List<String> = listOf("com.example", "com.google.android.apps.maps", "com.android.calculator2"),
    val singleAppPackage: String? = null,
    val exitPasscode: String = "1234",
    val blockStatusBar: Boolean = true,
    val blockNavBar: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class ComplianceCheckItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val severity: String = "CRITICAL", // CRITICAL, HIGH, MEDIUM, LOW
    val passed: Boolean = true,
    val remediationAction: String = ""
)

data class ComplianceReport(
    val deviceId: String = "phone_a",
    val overallScore: Int = 82, // 0 - 100
    val status: String = "WARNING", // COMPLIANT, WARNING, NON_COMPLIANT
    val checks: List<ComplianceCheckItem> = emptyList(),
    val nonCompliantCount: Int = 0,
    val lastAuditTimestamp: Long = System.currentTimeMillis()
)

data class WifiProvisioningProfile(
    val id: String = "",
    val ssid: String = "",
    val securityType: String = "WPA2/WPA3 Enterprise",
    val isHidden: Boolean = false,
    val autoConnect: Boolean = true,
    val proxyHost: String? = null,
    val status: String = "ACTIVE", // ACTIVE, REVOKED
    val targetScope: String = "All Fleet",
    val pushedTimestamp: Long = System.currentTimeMillis()
)

data class FleetBroadcastAlert(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val priority: String = "CRITICAL", // CRITICAL, URGENT, INFO
    val audioChimeOverride: Boolean = true,
    val targetScope: String = "All Fleet (5 devices)",
    val targetDeviceCount: Int = 5,
    val broadcastTimestamp: Long = System.currentTimeMillis(),
    val acknowledgedDeviceIds: List<String> = emptyList()
)

data class AppCacheItem(
    val packageName: String = "",
    val appName: String = "",
    val cacheSizeMb: Long = 0,
    val isStale: Boolean = true
)

data class StorageOptimizationReport(
    val deviceId: String = "phone_a",
    val totalReclaimableMb: Long = 2480,
    val appCacheItems: List<AppCacheItem> = emptyList(),
    val tempFilesMb: Long = 620,
    val orphanDownloadsMb: Long = 480,
    val lastOptimizedTimestamp: Long = System.currentTimeMillis()
)


