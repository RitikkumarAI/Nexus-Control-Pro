package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NexusDevice
import com.example.ui.components.EnterpriseHeader
import com.example.ui.components.HourlyBatteryGaugeCard
import com.example.ui.components.MetricCard
import com.example.ui.components.PrimaryNexusButton
import com.example.ui.components.SecondaryNexusButton
import com.example.ui.theme.CrimsonStop
import com.example.ui.theme.EmeraldOnline

@Composable
fun ManagedDashboardScreen(
    currentDevice: NexusDevice?,
    pairedAdminId: String?,
    onRefresh: () -> Unit,
    onNavigateToPairing: () -> Unit,
    onEmergencyRevoke: () -> Unit,
    onNavigateToSecurity: () -> Unit
) {
    val device = currentDevice ?: NexusDevice(
        deviceName = "Phone A (Managed)",
        batteryLevel = 94,
        isCharging = false,
        networkType = "Wi-Fi 6"
    )

    var allowScreenSharing by remember { mutableStateOf(true) }
    var allowFileAccess by remember { mutableStateOf(true) }
    var allowLocationSharing by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("managed_dashboard_screen")
    ) {
        EnterpriseHeader(
            title = "PHONE A: MANAGED DEVICE",
            subtitle = "Active Telemetry & Authorization Control",
            badgeText = "MANAGED NODE",
            badgeColor = EmeraldOnline
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Pairing Status Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        1.5.dp,
                        if (pairedAdminId != null) EmeraldOnline else MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Status",
                            tint = if (pairedAdminId != null) EmeraldOnline else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (pairedAdminId != null) "Connected to Administrator" else "Ready for Administrator Pairing",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (pairedAdminId != null) "Admin Device ID: ${pairedAdminId.take(12)}..." else "Tap below to pair with Phone B",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onRefresh, modifier = Modifier.testTag("refresh_managed_button")) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (pairedAdminId == null) {
                PrimaryNexusButton(
                    text = "Pair Phone A with Phone B Admin",
                    onClick = onNavigateToPairing,
                    icon = Icons.Default.VpnKey,
                    testTag = "managed_pair_button"
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = "LOCAL DEVICE METRICS BROADCAST",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                MetricCard(
                    title = "Battery Level",
                    value = "${device.batteryLevel}%",
                    subtitle = if (device.isCharging) "Charging" else "On Battery",
                    icon = Icons.Default.BatteryChargingFull,
                    progress = device.batteryLevel / 100f,
                    accentColor = EmeraldOnline,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                MetricCard(
                    title = "Network Connection",
                    value = device.networkType,
                    subtitle = "Broadcasting Telemetry",
                    icon = Icons.Default.Wifi,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            HourlyBatteryGaugeCard(
                hourlyDrainPercentage = 2.8f,
                currentBatteryPct = device.batteryLevel,
                modifier = Modifier.testTag("managed_hourly_battery_gauge_card")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Focus Mode (DND) & Remote Volume Status on Managed Node
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        1.dp,
                        if (device.focusModeActive) EmeraldOnline.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(14.dp)
                    .testTag("managed_focus_mode_card")
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (device.focusModeActive) Icons.Default.Shield else Icons.Default.Wifi,
                                contentDescription = "Focus",
                                tint = if (device.focusModeActive) EmeraldOnline else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (device.focusModeActive) "FOCUS MODE (DND): SILENCED" else "FOCUS MODE (DND): NORMAL",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (device.focusModeActive) EmeraldOnline else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Admin Synced",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "🔊 Ring: ${device.ringerVolume}% • Media: ${device.mediaVolume}% • Alarm: ${device.alarmVolume}%",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "GRANULAR PERMISSION AUTHORIZATION",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            PermissionToggleRow(
                title = "Remote Screen Share Stream",
                subtitle = "Allow Phone B administrator to request MediaProjection screen capture",
                checked = allowScreenSharing,
                onCheckedChange = { allowScreenSharing = it }
            )

            Spacer(modifier = Modifier.height(8.dp))

            PermissionToggleRow(
                title = "Scoped Storage & File Explorer",
                subtitle = "Allow Phone B administrator to browse permitted folder files & MediaStore",
                checked = allowFileAccess,
                onCheckedChange = { allowFileAccess = it }
            )

            Spacer(modifier = Modifier.height(8.dp))

            PermissionToggleRow(
                title = "GPS Location Telemetry",
                subtitle = "Allow Phone B administrator to view location updates",
                checked = allowLocationSharing,
                onCheckedChange = { allowLocationSharing = it }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Emergency Kill Switch
            SecondaryNexusButton(
                text = "EMERGENCY REVOKE ALL ADMIN ACCESS",
                onClick = onEmergencyRevoke,
                icon = Icons.Default.Block,
                color = CrimsonStop,
                testTag = "emergency_revoke_button"
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Tapping Emergency Revoke immediately terminates all active WebRTC screen streams, location updates, and pairing tokens.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun PermissionToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }
    }
}
