package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NexusDevice
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonStop
import com.example.ui.theme.EmeraldOnline

/**
 * Interactive preview of the Android Home Screen App Widget showing live connection status,
 * battery level, and last known location of selected high-priority managed devices, with
 * one-tap pinning to the device home screen and direct access to the Bulk Fleet Manager.
 */
@Composable
fun PriorityWidgetPreviewCard(
    priorityDevices: List<NexusDevice>,
    onPinWidgetToHomeScreen: () -> Unit,
    onManageFleetClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val widgetGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0E1420),
            Color(0xFF131D31)
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(widgetGradient)
            .border(1.dp, Color(0xFF283858), RoundedCornerShape(20.dp))
            .padding(16.dp)
            .testTag("priority_widget_preview_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Widget Header
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
                            .background(Color(0xFF1E6FFF).copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = "Home Screen Widget",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Home Screen Fleet Widget",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF0F4F8)
                        )
                        Text(
                            text = "${priorityDevices.size}/3 High-Priority Devices Pinned",
                            fontSize = 11.sp,
                            color = Color(0xFF8C9BAE)
                        )
                    }
                }

                StatusBadge(
                    text = "LIVE WIDGET",
                    color = EmeraldOnline
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (priorityDevices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF161F30))
                        .border(1.dp, Color(0xFF283858), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Star up to 3 managed devices to monitor connection, battery & GPS on your Home Screen.",
                        fontSize = 12.sp,
                        color = Color(0xFF8C9BAE)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    priorityDevices.take(3).forEach { device ->
                        WidgetDevicePreviewRow(device = device)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPinWidgetToHomeScreen,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("pin_home_screen_widget_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddToHomeScreen,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Add Widget",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (onManageFleetClick != null) {
                    OutlinedButton(
                        onClick = onManageFleetClick,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("open_managed_fleet_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Devices,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Bulk Fleet",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFF0F4F8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetDevicePreviewRow(device: NexusDevice) {
    val batteryColor = when {
        device.batteryLevel <= 20 -> CrimsonStop
        device.batteryLevel <= 45 -> AmberWarning
        else -> EmeraldOnline
    }

    val statusDotColor = if (device.isOnline) EmeraldOnline else CrimsonStop

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF161F30))
            .border(1.dp, Color(0xFF283858), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusDotColor)
                )
                Spacer(modifier = Modifier.width(7.dp))
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "High Priority",
                    tint = AmberWarning,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = device.deviceName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF0F4F8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (device.isLocked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = CrimsonStop,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Icon(
                    imageVector = if (device.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                    contentDescription = "Battery level",
                    tint = batteryColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "${device.batteryLevel}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = batteryColor
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

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
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Last known location",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = device.lastLocationAddress ?: "Location unavailable",
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = if (device.isOnline) "Online • ${device.networkType}" else "Offline",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (device.isOnline) Color(0xFF38BDF8) else Color(0xFFF87171)
            )
        }
    }
}
