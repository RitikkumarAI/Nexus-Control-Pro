package com.example.data

import android.content.Context
import com.example.model.AppCacheItem
import com.example.model.ComplianceCheckItem
import com.example.model.ComplianceReport
import com.example.model.FleetBroadcastAlert
import com.example.model.KioskConfig
import com.example.model.StorageOptimizationReport
import com.example.model.WifiProvisioningProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Enterprise MDM Suite coordinator managing:
 * 1. Kiosk & Single-App Lockdown
 * 2. Compliance & Security Posture Audit
 * 3. Enterprise Wi-Fi & Network Provisioning
 * 4. Fleet Broadcast & Emergency Alerts
 * 5. Storage & Cache Bloat Optimization
 */
class EnterpriseSuiteManager(
    private val context: Context,
    private val firestoreRepository: FirestoreRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 1. Kiosk State
    private val _kioskConfig = MutableStateFlow(
        KioskConfig(
            enabled = false,
            allowedPackages = listOf("com.google.android.apps.maps", "com.android.calculator2", "com.example"),
            singleAppPackage = null,
            exitPasscode = "1234",
            blockStatusBar = true,
            blockNavBar = true
        )
    )
    val kioskConfig: StateFlow<KioskConfig> = _kioskConfig.asStateFlow()

    // 2. Compliance State
    private val _complianceReport = MutableStateFlow(createDefaultComplianceReport("phone_a"))
    val complianceReport: StateFlow<ComplianceReport> = _complianceReport.asStateFlow()

    // 3. Wi-Fi Provisioning Profiles
    private val _wifiProfiles = MutableStateFlow(createDefaultWifiProfiles())
    val wifiProfiles: StateFlow<List<WifiProvisioningProfile>> = _wifiProfiles.asStateFlow()

    // 4. Fleet Broadcast Alerts
    private val _fleetBroadcasts = MutableStateFlow(createDefaultFleetBroadcasts())
    val fleetBroadcasts: StateFlow<List<FleetBroadcastAlert>> = _fleetBroadcasts.asStateFlow()

    // 5. Storage Optimization Report
    private val _storageReport = MutableStateFlow(createDefaultStorageReport("phone_a"))
    val storageReport: StateFlow<StorageOptimizationReport> = _storageReport.asStateFlow()

    private val _isOptimizingStorage = MutableStateFlow(false)
    val isOptimizingStorage: StateFlow<Boolean> = _isOptimizingStorage.asStateFlow()

    // --- 1. KIOSK CONTROLS ---
    fun toggleKioskMode(deviceId: String, enabled: Boolean, singleApp: String? = null) {
        val current = _kioskConfig.value
        val updated = current.copy(
            enabled = enabled,
            singleAppPackage = singleApp,
            lastUpdated = System.currentTimeMillis()
        )
        _kioskConfig.value = updated
        scope.launch {
            firestoreRepository.updateKioskConfig(deviceId, updated)
        }
    }

    fun updateKioskAllowedPackages(deviceId: String, packages: List<String>) {
        val updated = _kioskConfig.value.copy(
            allowedPackages = packages,
            lastUpdated = System.currentTimeMillis()
        )
        _kioskConfig.value = updated
        scope.launch {
            firestoreRepository.updateKioskConfig(deviceId, updated)
        }
    }

    // --- 2. COMPLIANCE CONTROLS ---
    fun remediateAllCompliance(deviceId: String) {
        val current = _complianceReport.value
        val remediatedChecks = current.checks.map { it.copy(passed = true) }
        val fixed = current.copy(
            overallScore = 100,
            status = "COMPLIANT",
            nonCompliantCount = 0,
            checks = remediatedChecks,
            lastAuditTimestamp = System.currentTimeMillis()
        )
        _complianceReport.value = fixed
        scope.launch {
            firestoreRepository.remediateCompliance(deviceId)
        }
    }

    // --- 3. WI-FI PROVISIONING CONTROLS ---
    fun pushNewWifiProfile(ssid: String, securityType: String, isHidden: Boolean, autoConnect: Boolean) {
        val profile = WifiProvisioningProfile(
            id = "wifi_prof_${UUID.randomUUID().toString().take(6)}",
            ssid = ssid,
            securityType = securityType,
            isHidden = isHidden,
            autoConnect = autoConnect,
            status = "ACTIVE",
            targetScope = "All Fleet",
            pushedTimestamp = System.currentTimeMillis()
        )
        _wifiProfiles.value = listOf(profile) + _wifiProfiles.value
        scope.launch {
            firestoreRepository.pushWifiProfile(profile)
        }
    }

    fun revokeWifiProfile(profileId: String) {
        _wifiProfiles.value = _wifiProfiles.value.map {
            if (it.id == profileId) it.copy(status = "REVOKED") else it
        }
        scope.launch {
            firestoreRepository.revokeWifiProfile(profileId)
        }
    }

    // --- 4. FLEET BROADCAST CONTROLS ---
    fun sendBroadcast(title: String, message: String, priority: String, audioChime: Boolean) {
        val alert = FleetBroadcastAlert(
            id = "alert_${UUID.randomUUID().toString().take(6)}",
            title = title,
            message = message,
            priority = priority,
            audioChimeOverride = audioChime,
            targetScope = "All Fleet (5 devices)",
            targetDeviceCount = 5,
            broadcastTimestamp = System.currentTimeMillis(),
            acknowledgedDeviceIds = listOf("phone_a") // Local device auto-acknowledges upon transmission
        )
        _fleetBroadcasts.value = listOf(alert) + _fleetBroadcasts.value
        scope.launch {
            firestoreRepository.publishFleetBroadcast(alert)
        }
    }

    fun acknowledgeBroadcast(alertId: String, deviceId: String) {
        _fleetBroadcasts.value = _fleetBroadcasts.value.map { b ->
            if (b.id == alertId && !b.acknowledgedDeviceIds.contains(deviceId)) {
                b.copy(acknowledgedDeviceIds = b.acknowledgedDeviceIds + deviceId)
            } else {
                b
            }
        }
        scope.launch {
            firestoreRepository.acknowledgeFleetBroadcast(alertId, deviceId)
        }
    }

    // --- 5. STORAGE & CACHE OPTIMIZATION CONTROLS ---
    fun triggerStorageCleanup(deviceId: String) {
        scope.launch {
            _isOptimizingStorage.value = true
            val current = _storageReport.value
            val cleaned = current.copy(
                totalReclaimableMb = 120,
                tempFilesMb = 25,
                orphanDownloadsMb = 45,
                appCacheItems = current.appCacheItems.map { it.copy(cacheSizeMb = 15, isStale = false) },
                lastOptimizedTimestamp = System.currentTimeMillis()
            )
            _storageReport.value = cleaned
            firestoreRepository.triggerRemoteCacheCleanup(deviceId)
            _isOptimizingStorage.value = false
        }
    }

    private fun createDefaultComplianceReport(deviceId: String): ComplianceReport {
        return ComplianceReport(
            deviceId = deviceId,
            overallScore = 83,
            status = "WARNING",
            nonCompliantCount = 1,
            checks = listOf(
                ComplianceCheckItem(
                    id = "check_usb_debug",
                    title = "USB Debugging Restricted",
                    description = "ADB USB debugging is currently enabled on device (security risk).",
                    severity = "CRITICAL",
                    passed = false,
                    remediationAction = "Disable Developer Options USB Debug"
                ),
                ComplianceCheckItem(
                    id = "check_root_integrity",
                    title = "Hardware Root & Play Integrity",
                    description = "Kernel integrity verified. No su binaries or Magisk detected.",
                    severity = "CRITICAL",
                    passed = true,
                    remediationAction = "Verified"
                ),
                ComplianceCheckItem(
                    id = "check_screen_lock",
                    title = "Enterprise PIN/Biometric Enforced",
                    description = "6-digit secure keyguard passcode active.",
                    severity = "HIGH",
                    passed = true,
                    remediationAction = "Enforce PIN policy"
                ),
                ComplianceCheckItem(
                    id = "check_unknown_sources",
                    title = "Side-loaded APK Installation Blocked",
                    description = "Non-managed unknown source installation prevented via policy.",
                    severity = "HIGH",
                    passed = true,
                    remediationAction = "Block unknown sources"
                ),
                ComplianceCheckItem(
                    id = "check_storage_encryption",
                    title = "Hardware Storage Encryption",
                    description = "FBE (File-Based Encryption) active with TEE hardware key.",
                    severity = "HIGH",
                    passed = true,
                    remediationAction = "Verify keystore"
                ),
                ComplianceCheckItem(
                    id = "check_patch_currency",
                    title = "OS Security Patch Level",
                    description = "Security patch is within acceptable 60-day enterprise window.",
                    severity = "MEDIUM",
                    passed = true,
                    remediationAction = "Check system updates"
                )
            ),
            lastAuditTimestamp = System.currentTimeMillis() - 180_000L
        )
    }

    private fun createDefaultWifiProfiles(): List<WifiProvisioningProfile> {
        val now = System.currentTimeMillis()
        return listOf(
            WifiProvisioningProfile(
                id = "wifi_prof_01",
                ssid = "Nexus-Corp-Enterprise",
                securityType = "WPA2/WPA3 Enterprise",
                isHidden = false,
                autoConnect = true,
                status = "ACTIVE",
                targetScope = "All Fleet",
                pushedTimestamp = now - 3_600_000L
            ),
            WifiProvisioningProfile(
                id = "wifi_prof_02",
                ssid = "Nexus-Warehouse-IoT",
                securityType = "WPA2/WPA3 Personal",
                isHidden = true,
                autoConnect = true,
                status = "ACTIVE",
                targetScope = "Warehouse Fleet",
                pushedTimestamp = now - 86_400_000L
            ),
            WifiProvisioningProfile(
                id = "wifi_prof_03",
                ssid = "Nexus-Guest-Restricted",
                securityType = "Open (Proxy Filtered)",
                isHidden = false,
                autoConnect = false,
                status = "REVOKED",
                targetScope = "Visitor Fleet",
                pushedTimestamp = now - 172_800_000L
            )
        )
    }

    private fun createDefaultFleetBroadcasts(): List<FleetBroadcastAlert> {
        val now = System.currentTimeMillis()
        return listOf(
            FleetBroadcastAlert(
                id = "alert_01",
                title = "⚡ Severe Weather Alert: Whitefield Sector",
                message = "High wind speeds and storm alert in Whitefield logistics route. Return all delivery vehicles to central depot immediately.",
                priority = "CRITICAL",
                audioChimeOverride = true,
                targetScope = "All Fleet (5 devices)",
                targetDeviceCount = 5,
                broadcastTimestamp = now - 1_200_000L,
                acknowledgedDeviceIds = listOf("phone_a", "tablet_ops_02", "dispatch_unit_03", "kiosk_lobby_04")
            ),
            FleetBroadcastAlert(
                id = "alert_02",
                title = "Scheduled System Maintenance at 02:00",
                message = "Fleet telemetry sync will pause for 15 minutes tonight for server-side maintenance. No action required.",
                priority = "INFO",
                audioChimeOverride = false,
                targetScope = "All Fleet (5 devices)",
                targetDeviceCount = 5,
                broadcastTimestamp = now - 14_400_000L,
                acknowledgedDeviceIds = listOf("phone_a", "tablet_ops_02", "dispatch_unit_03", "kiosk_lobby_04", "courier_handheld_05")
            )
        )
    }

    private fun createDefaultStorageReport(deviceId: String): StorageOptimizationReport {
        return StorageOptimizationReport(
            deviceId = deviceId,
            totalReclaimableMb = 2480,
            tempFilesMb = 620,
            orphanDownloadsMb = 480,
            appCacheItems = listOf(
                AppCacheItem("com.google.android.youtube", "YouTube App", 980, isStale = true),
                AppCacheItem("com.android.chrome", "Chrome Browser", 560, isStale = true),
                AppCacheItem("com.google.android.apps.maps", "Google Maps Tiles", 380, isStale = true),
                AppCacheItem("com.whatsapp", "WhatsApp Media Cache", 240, isStale = false)
            ),
            lastOptimizedTimestamp = System.currentTimeMillis() - 48_000_000L
        )
    }
}
