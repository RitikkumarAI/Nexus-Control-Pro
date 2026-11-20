package com.example.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
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
import com.example.data.GeofenceConfig
import com.example.model.GeofencePushAlert
import com.example.ui.theme.CrimsonStop
import com.example.ui.theme.EmeraldOnline
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GeofenceAlertNotificationCard(
    geofenceConfig: GeofenceConfig,
    pushAlertsEnabled: Boolean,
    hasNotificationPermission: Boolean,
    alerts: List<GeofencePushAlert>,
    onTogglePushAlerts: (Boolean) -> Unit,
    onUpdateBoundaryRadius: (Float) -> Unit,
    onTriggerBoundaryTest: () -> Unit,
    onAcknowledgeAlert: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var permissionGrantedLocal by remember(hasNotificationPermission) {
        mutableStateOf(hasNotificationPermission)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGrantedLocal = granted
        if (granted) {
            onTogglePushAlerts(true)
        }
    }

    val unacknowledgedAlerts = remember(alerts) { alerts.filter { !it.acknowledged } }
    val hasActiveBreach = unacknowledgedAlerts.isNotEmpty()
    val statusColor = when {
        hasActiveBreach -> CrimsonStop
        pushAlertsEnabled -> EmeraldOnline
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (hasActiveBreach) 1.5.dp else 1.dp,
                color = if (hasActiveBreach) CrimsonStop.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(18.dp)
            )
            .padding(16.dp)
            .testTag("geofence_push_notification_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(statusColor.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (hasActiveBreach) Icons.Default.WarningAmber
                            else if (pushAlertsEnabled) Icons.Default.NotificationsActive
                            else Icons.Default.NotificationsOff,
                            contentDescription = "Geofence Push Notifications",
                            tint = statusColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "GEOFENCE PUSH ALERTS",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Admin Boundary Exit Notifications",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                StatusBadge(
                    text = when {
                        hasActiveBreach -> "${unacknowledgedAlerts.size} BREACH"
                        pushAlertsEnabled -> "ARMED"
                        else -> "PAUSED"
                    },
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Push Notification Toggle + Permission Guard
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Boundary Exit Push Notifications",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (!permissionGrantedLocal && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            "Tap toggle to grant Android notification permission"
                        } else {
                            "Alerts this admin device immediately on boundary exit"
                        },
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = pushAlertsEnabled && permissionGrantedLocal,
                    onCheckedChange = { enable ->
                        if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permissionGrantedLocal) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onTogglePushAlerts(enable)
                        }
                    },
                    modifier = Modifier.testTag("geofence_push_alerts_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = EmeraldOnline,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Active Boundary Info & Quick Radius Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = "Geofence Zone",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${geofenceConfig.zoneName} • ${geofenceConfig.radiusMeters.toInt()}m Radius",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val radiusOptions = listOf(250f, 500f, 1000f, 2000f)
                radiusOptions.forEach { radius ->
                    val isSelected = geofenceConfig.radiusMeters == radius
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onUpdateBoundaryRadius(radius) }
                            .padding(vertical = 8.dp)
                            .testTag("geofence_radius_${radius.toInt()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (radius >= 1000f) "${(radius / 1000f).toInt()} km" else "${radius.toInt()}m",
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Active Breach Feed or Safe Zone Confirmation
            if (unacknowledgedAlerts.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    unacknowledgedAlerts.take(2).forEach { alert ->
                        GeofenceBreachItemRow(
                            alert = alert,
                            onAcknowledge = { onAcknowledgeAlert(alert.alertId) }
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(EmeraldOnline.copy(alpha = 0.08f))
                        .border(1.dp, EmeraldOnline.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Inside Boundary",
                        tint = EmeraldOnline,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Managed device is within the established geofence boundary.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            SecondaryNexusButton(
                text = "Simulate & Test Boundary Alert",
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permissionGrantedLocal) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    onTriggerBoundaryTest()
                },
                icon = Icons.Default.NotificationsActive,
                color = if (hasActiveBreach) CrimsonStop else MaterialTheme.colorScheme.primary,
                testTag = "test_geofence_push_button"
            )
        }
    }
}

@Composable
private fun GeofenceBreachItemRow(
    alert: GeofencePushAlert,
    onAcknowledge: () -> Unit
) {
    val timeText = remember(alert.timestamp) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(alert.timestamp))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CrimsonStop.copy(alpha = 0.10f))
            .border(1.dp, CrimsonStop.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${alert.deviceName} • Outside ${alert.zoneName}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CrimsonStop
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${alert.distanceFromCenterMeters.toInt()}m away (Limit: ${alert.boundaryRadiusMeters.toInt()}m) • $timeText",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CrimsonStop)
                    .clickable { onAcknowledge() }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .testTag("ack_geofence_alert_${alert.alertId}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Ack",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
