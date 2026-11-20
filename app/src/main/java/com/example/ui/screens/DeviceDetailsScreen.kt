package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NexusDevice
import com.example.ui.components.EnterpriseHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.NexusBluePrimary
import com.example.ui.theme.NexusCyanAccent

@Composable
fun DeviceDetailsScreen(
    device: NexusDevice?,
    onBack: () -> Unit
) {
    val dev = device ?: NexusDevice(
        deviceName = "Field Phone A",
        manufacturer = "Realme",
        model = "Realme 11 Pro 5G",
        androidVersion = "Android 14",
        batteryLevel = 87,
        isCharging = true,
        networkType = "Jio 4G",
        totalStorageGb = 128.0,
        usedStorageGb = 76.4,
        totalRamGb = 8.0,
        usedRamGb = 5.2,
        isOnline = true
    )

    var activeTab by remember { mutableStateOf("Overview") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("device_details_screen")
    ) {
        EnterpriseHeader(
            title = "Device Details",
            subtitle = dev.deviceName,
            onBackClick = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Device Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(NexusBluePrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = dev.deviceName,
                            tint = NexusBluePrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = dev.deviceName,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            StatusBadge(
                                text = if (dev.isOnline) "Online" else "Offline",
                                color = if (dev.isOnline) EmeraldOnline else Color.Gray
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "${dev.manufacturer} ${dev.model}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "IMEI: 86349XXXXXXXXX",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Segmented Tabs [ Overview ] [ Usage ] [ Permissions ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                SegmentedTabButton(text = "Overview", active = activeTab == "Overview", onClick = { activeTab = "Overview" }, modifier = Modifier.weight(1f))
                SegmentedTabButton(text = "Usage", active = activeTab == "Usage", onClick = { activeTab = "Usage" }, modifier = Modifier.weight(1f))
                SegmentedTabButton(text = "Permissions", active = activeTab == "Permissions", onClick = { activeTab = "Permissions" }, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (activeTab == "Overview") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DetailRowItem(
                        title = "Android 14",
                        subtitle = "Version",
                        icon = Icons.Default.Android,
                        accentColor = EmeraldOnline
                    )

                    DetailRowItem(
                        title = "Battery",
                        subtitle = "${dev.batteryLevel}% (${if (dev.isCharging) "Charging" else "Discharging"})",
                        icon = Icons.Default.BatteryChargingFull,
                        accentColor = EmeraldOnline
                    )

                    DetailRowItem(
                        title = "Storage",
                        subtitle = "${dev.usedStorageGb} GB / ${dev.totalStorageGb} GB",
                        icon = Icons.Default.Storage,
                        accentColor = NexusBluePrimary
                    )

                    DetailRowItem(
                        title = "RAM",
                        subtitle = "${dev.usedRamGb} GB / ${dev.totalRamGb} GB",
                        icon = Icons.Default.Memory,
                        accentColor = NexusBluePrimary
                    )

                    DetailRowItem(
                        title = "Network",
                        subtitle = dev.networkType,
                        icon = Icons.Default.SignalCellular4Bar,
                        accentColor = NexusBluePrimary
                    )

                    DetailRowItem(
                        title = "Wi-Fi",
                        subtitle = "Office_WiFi (Connected)",
                        icon = Icons.Default.Wifi,
                        accentColor = EmeraldOnline
                    )
                }
            } else if (activeTab == "Usage") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    Column {
                        Text(text = "APPLICATION USAGE DURATION (LAST 24H)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "• WhatsApp: 2h 45m\n• YouTube: 1h 20m\n• Chrome: 48m\n• Instagram: 32m", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 20.sp)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    Column {
                        Text(text = "AUTHORIZED MANAGEMENT PERMISSIONS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "✓ Screen Share (MediaProjection)\n✓ Scoped Storage File Access\n✓ Location GPS Updates\n✓ Call History Log Read", fontSize = 13.sp, color = EmeraldOnline, lineHeight = 20.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentedTabButton(text: String, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) NexusBluePrimary else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DetailRowItem(title: String, subtitle: String, icon: ImageVector, accentColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "More", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
