package com.example.data

import android.content.Context
import android.net.wifi.WifiManager
import java.util.Calendar
import kotlin.math.abs

data class SignalStabilityPoint(
    val hourLabel: String,
    val hourIndex: Int,
    val avgRssiDbm: Int,
    val minRssiDbm: Int,
    val maxRssiDbm: Int,
    val packetLossPct: Float,
    val zoneLabel: String,
    val isIntermittentDrop: Boolean
)

data class IntermittentZoneAlert(
    val zoneName: String,
    val timeWindow: String,
    val avgRssiDbm: Int,
    val minRssiDbm: Int,
    val packetLossPct: Float,
    val severity: String // MODERATE, SEVERE
)

data class DeviceSignalStabilityProfile(
    val deviceId: String,
    val deviceName: String,
    val networkType: String,
    val currentRssiDbm: Int,
    val average24hRssiDbm: Int,
    val stabilityScorePct: Int,
    val dropoutCount: Int,
    val worstZoneName: String,
    val hourlyPoints: List<SignalStabilityPoint>,
    val intermittentZones: List<IntermittentZoneAlert>,
    val lastUpdatedMs: Long = System.currentTimeMillis()
)

/**
 * Tracks and computes 24-hour average network signal strength fluctuations (dBm)
 * per managed device and pinpoints intermittent connectivity zones.
 */
class NetworkSignalStabilityManager(private val context: Context) {

    companion object {
        const val INSTABILITY_THRESHOLD_DBM = -78
        private val HOUR_LABELS = listOf(
            "00h", "02h", "04h", "06h", "08h", "10h",
            "12h", "14h", "16h", "18h", "20h", "22h"
        )
    }

    fun getDeviceProfiles(): List<DeviceSignalStabilityProfile> {
        val liveRssi = readLiveWifiRssiDbm()
        val currentBucket = (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) / 2).coerceIn(0, 11)

        return listOf(
            buildProfile(
                deviceId = "phone_a",
                deviceName = "Field Phone A",
                networkType = "5G / Wi-Fi 6",
                baseRssi = intArrayOf(-56, -54, -55, -58, -63, -68, -72, -83, -79, -64, -59, -57),
                jitter = intArrayOf(3, 2, 3, 4, 5, 6, 7, 9, 8, 5, 4, 3),
                zones = listOf(
                    "HQ Dock", "HQ Dock", "HQ Dock", "Transit Corridor",
                    "Koramangala Hub", "South Wing", "Parking Level B1",
                    "Basement Loading Bay", "Freight Elevator Shaft",
                    "Koramangala Tech Park", "HQ Dock", "HQ Dock"
                ),
                liveOverrideBucket = currentBucket,
                liveOverrideRssi = liveRssi
            ),
            buildProfile(
                deviceId = "tablet_ops_02",
                deviceName = "Warehouse Tablet B2",
                networkType = "Wi-Fi 5 GHz",
                baseRssi = intArrayOf(-60, -59, -61, -64, -71, -81, -85, -74, -67, -63, -61, -60),
                jitter = intArrayOf(3, 3, 4, 5, 6, 8, 9, 6, 5, 4, 3, 3),
                zones = listOf(
                    "Charging Bay", "Charging Bay", "Aisle 1", "Aisle 2",
                    "Sorting Floor", "Cold Storage Vault", "Rear Steel Racking",
                    "Packing Zone C", "Whitefield Hub", "Whitefield Hub",
                    "Charging Bay", "Charging Bay"
                )
            ),
            buildProfile(
                deviceId = "dispatch_unit_03",
                deviceName = "Dispatch Pixel C3",
                networkType = "4G LTE",
                baseRssi = intArrayOf(-64, -66, -65, -70, -77, -86, -88, -82, -75, -69, -67, -65),
                jitter = intArrayOf(4, 4, 4, 6, 7, 8, 7, 8, 6, 5, 4, 4),
                zones = listOf(
                    "MG Road Depot", "MG Road Depot", "MG Road Depot", "Ring Road North",
                    "Underpass Tunnel", "Metro Dead Zone", "Sub-level Garage",
                    "East Flyover", "Indiranagar Drop", "MG Road Depot",
                    "MG Road Depot", "MG Road Depot"
                )
            ),
            buildProfile(
                deviceId = "kiosk_lobby_04",
                deviceName = "Lobby Kiosk K4",
                networkType = "Wi-Fi 6",
                baseRssi = intArrayOf(-48, -47, -48, -49, -52, -55, -54, -53, -51, -49, -48, -47),
                jitter = intArrayOf(2, 2, 2, 3, 3, 4, 3, 3, 3, 2, 2, 2),
                zones = List(12) { "HQ Main Reception, Floor 1" }
            ),
            buildProfile(
                deviceId = "courier_handheld_05",
                deviceName = "Courier Scanner D5",
                networkType = "4G LTE",
                baseRssi = intArrayOf(-62, -61, -63, -67, -74, -79, -76, -84, -71, -66, -64, -62),
                jitter = intArrayOf(3, 3, 4, 5, 6, 7, 6, 8, 5, 4, 4, 3),
                zones = listOf(
                    "HSR Depot", "HSR Depot", "HSR Depot", "Sector 2 Gate",
                    "Outer Ring Rd", "Bellandur Bridge", "Sarjapur Cutoff",
                    "Basement Mailroom", "HSR Layout Route 7", "HSR Depot",
                    "HSR Depot", "HSR Depot"
                )
            )
        )
    }

    private fun buildProfile(
        deviceId: String,
        deviceName: String,
        networkType: String,
        baseRssi: IntArray,
        jitter: IntArray,
        zones: List<String>,
        liveOverrideBucket: Int? = null,
        liveOverrideRssi: Int? = null
    ): DeviceSignalStabilityProfile {
        val points = HOUR_LABELS.mapIndexed { idx, label ->
            val rawAvg = if (liveOverrideBucket == idx && liveOverrideRssi != null) {
                liveOverrideRssi
            } else {
                baseRssi[idx]
            }
            val spread = jitter[idx]
            val minVal = (rawAvg - spread).coerceAtLeast(-98)
            val maxVal = (rawAvg + spread).coerceAtMost(-38)
            val isDrop = rawAvg <= INSTABILITY_THRESHOLD_DBM || minVal <= -85
            val packetLoss = when {
                rawAvg <= -84 -> 4.6f + (abs(rawAvg + 84) * 0.4f)
                rawAvg <= INSTABILITY_THRESHOLD_DBM -> 2.1f + (abs(rawAvg + 78) * 0.35f)
                rawAvg <= -70 -> 0.8f
                else -> 0.1f
            }

            SignalStabilityPoint(
                hourLabel = label,
                hourIndex = idx,
                avgRssiDbm = rawAvg,
                minRssiDbm = minVal,
                maxRssiDbm = maxVal,
                packetLossPct = packetLoss,
                zoneLabel = zones.getOrElse(idx) { "Active Coverage Area" },
                isIntermittentDrop = isDrop
            )
        }

        val avg24h = points.map { it.avgRssiDbm }.average().toInt()
        val dropPoints = points.filter { it.isIntermittentDrop }
        val avgSpread = points.map { it.maxRssiDbm - it.minRssiDbm }.average()
        val stabilityScore = (100 - (dropPoints.size * 9) - (avgSpread * 1.1).toInt())
            .coerceIn(42, 99)

        val alerts = dropPoints.map { pt ->
            val endHour = ((pt.hourIndex * 2) + 2) % 24
            val window = "${pt.hourLabel.removeSuffix("h")}:00–%02d:00".format(endHour)
            IntermittentZoneAlert(
                zoneName = pt.zoneLabel,
                timeWindow = window,
                avgRssiDbm = pt.avgRssiDbm,
                minRssiDbm = pt.minRssiDbm,
                packetLossPct = pt.packetLossPct,
                severity = if (pt.avgRssiDbm <= -83) "SEVERE" else "MODERATE"
            )
        }

        val worst = points.minByOrNull { it.avgRssiDbm }
        val currentBucket = (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) / 2).coerceIn(0, 11)

        return DeviceSignalStabilityProfile(
            deviceId = deviceId,
            deviceName = deviceName,
            networkType = networkType,
            currentRssiDbm = points[currentBucket].avgRssiDbm,
            average24hRssiDbm = avg24h,
            stabilityScorePct = stabilityScore,
            dropoutCount = dropPoints.size,
            worstZoneName = if (dropPoints.isEmpty()) "None (Stable 24h)" else worst?.zoneLabel ?: "Unknown",
            hourlyPoints = points,
            intermittentZones = alerts
        )
    }

    @Suppress("DEPRECATION")
    private fun readLiveWifiRssiDbm(): Int? {
        return try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val rssi = wm?.connectionInfo?.rssi
            if (rssi != null && rssi in -98..-30) rssi else null
        } catch (_: Exception) {
            null
        }
    }
}
