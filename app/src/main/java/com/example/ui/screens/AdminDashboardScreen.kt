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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
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
import com.example.data.DeviceSignalStabilityProfile
import com.example.data.GeofenceConfig
import com.example.data.local.SyncQueueEntity
import com.example.model.AuditLogEntry
import com.example.model.BatteryDischargeReport
import com.example.model.ComplianceReport
import com.example.model.FleetBroadcastAlert
import com.example.model.GeofencePushAlert
import com.example.model.KioskConfig
import com.example.model.NexusDevice
import com.example.model.StorageOptimizationReport
import com.example.model.WifiProvisioningProfile
import com.example.ui.components.BatteryDischargeReportCard
import com.example.ui.components.CommandHeatmapCard
import com.example.ui.components.CompliancePostureCard
import com.example.ui.components.FleetBroadcastCard
import com.example.ui.components.GeofenceAlertNotificationCard
import com.example.ui.components.KioskManagementCard
import com.example.ui.components.NetworkSignalStabilityCard
import com.example.ui.components.PriorityWidgetPreviewCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.StorageOptimizationCard
import com.example.ui.components.SyncQueueStatusCard
import com.example.ui.components.WifiProvisioningCard
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.LocalThemeController

@Composable
fun AdminDashboardScreen(
    managedDevice: NexusDevice?,
    pairedDeviceId: String?,
    onRefresh: () -> Unit,
    onNavigateToPairing: () -> Unit,
    onNavigateToScreenShare: () -> Unit,
    onNavigateToAppManager: () -> Unit,
    onNavigateToFileManager: () -> Unit,
    onNavigateToGallery: () -> Unit,
    onNavigateToLocation: () -> Unit,
    onNavigateToCommLog: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToEnterprise: () -> Unit,
    onNavigateToAuditLogs: () -> Unit = onNavigateToPermissions,
    onNavigateToManagedDevices: () -> Unit = onNavigateToPairing,
    batteryDischargeReport: BatteryDischargeReport = BatteryDischargeReport(),
    geofenceConfig: GeofenceConfig = GeofenceConfig(),
    geofencePushAlertsEnabled: Boolean = true,
    hasNotificationPermission: Boolean = true,
    geofenceAlerts: List<GeofencePushAlert> = emptyList(),
    onToggleGeofencePushAlerts: (Boolean) -> Unit = {},
    onUpdateGeofenceRadius: (Float) -> Unit = {},
    onTriggerGeofenceBoundaryTest: () -> Unit = {},
    onAcknowledgeGeofenceAlert: (String) -> Unit = {},
    syncQueueItems: List<SyncQueueEntity> = emptyList(),
    pendingSyncCount: Int = 0,
    isOfflineBufferMode: Boolean = false,
    isFlushingSyncQueue: Boolean = false,
    onToggleOfflineBufferMode: (Boolean) -> Unit = {},
    onEnqueueSettingsSync: () -> Unit = {},
    onEnqueueGeofenceSync: () -> Unit = {},
    onFlushSyncQueue: () -> Unit = {},
    onClearCompletedSyncItems: () -> Unit = {},
    priorityWidgetDevices: List<NexusDevice> = emptyList(),
    onPinWidgetToHomeScreen: () -> Unit = {},
    auditLogs: List<AuditLogEntry> = emptyList(),
    signalStabilityProfiles: List<DeviceSignalStabilityProfile> = emptyList(),
    onRefreshSignalStability: () -> Unit = {},
    onNavigateToRemoteLogcat: () -> Unit = {},
    kioskConfig: KioskConfig = KioskConfig(),
    onToggleKiosk: (Boolean, String?) -> Unit = { _, _ -> },
    complianceReport: ComplianceReport = ComplianceReport(),
    onRemediateAllCompliance: () -> Unit = {},
    wifiProfiles: List<WifiProvisioningProfile> = emptyList(),
    onPushWifiProfile: (String, String, Boolean, Boolean) -> Unit = { _, _, _, _ -> },
    onRevokeWifiProfile: (String) -> Unit = {},
    fleetBroadcasts: List<FleetBroadcastAlert> = emptyList(),
    onSendFleetBroadcast: (String, String, String, Boolean) -> Unit = { _, _, _, _ -> },
    onAcknowledgeFleetBroadcast: (String) -> Unit = {},
    storageReport: StorageOptimizationReport = StorageOptimizationReport(),
    isOptimizingStorage: Boolean = false,
    onTriggerStorageCleanup: () -> Unit = {},
    onScanQrPair: (String) -> Unit = {}
) {
    val device = managedDevice ?: NexusDevice(
        deviceName = "Field Phone A",
        manufacturer = "Realme",
        model = "Realme 11 Pro 5G",
        batteryLevel = 87,
        isCharging = true,
        networkType = "4G",
        isOnline = true
    )

    val themeController = LocalThemeController.current
    var currentBottomTab by remember { mutableStateOf("Home") }
    var showAddDeviceModal by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                NavigationBarItem(
                    selected = currentBottomTab == "Home",
                    onClick = { currentBottomTab = "Home" },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    )
                )

                NavigationBarItem(
                    selected = currentBottomTab == "Devices",
                    onClick = { onNavigateToManagedDevices() },
                    icon = { Icon(Icons.Default.PhoneAndroid, contentDescription = "Managed Devices") },
                    label = { Text("Devices", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )

                NavigationBarItem(
                    selected = false,
                    onClick = { showAddDeviceModal = true },
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    },
                    label = { Text("", fontSize = 0.sp) }
                )

                NavigationBarItem(
                    selected = currentBottomTab == "Activity",
                    onClick = { onNavigateToAuditLogs() },
                    icon = { Icon(Icons.Default.Shield, contentDescription = "Activity Logs") },
                    label = { Text("Activity", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )

                NavigationBarItem(
                    selected = currentBottomTab == "Settings",
                    onClick = { onNavigateToSecurity() },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .testTag("admin_dashboard_screen")
        ) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Nexus Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "NEXUS CONTROL PRO",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { themeController.toggleTheme() },
                        modifier = Modifier.testTag("admin_theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (themeController.isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (themeController.isDark) "Switch to Light Mode" else "Switch to Dark Mode",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    val unackedAlerts = geofenceAlerts.count { !it.acknowledged }
                    IconButton(onClick = { onNavigateToLocation() }) {
                        BadgedBox(
                            badge = {
                                if (unackedAlerts > 0) {
                                    Badge { Text(unackedAlerts.toString()) }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Geofence Alerts",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Greeting Header
                Text(
                    text = "Hello, Ritik 👋",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Manage your connected devices",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Target Connected Device Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                        .clickable { onNavigateToEnterprise() }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = device.deviceName,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = device.deviceName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                StatusBadge(
                                    text = if (device.isOnline) "Online" else "Offline",
                                    color = if (device.isOnline) EmeraldOnline else Color.Gray
                                )
                            }

                            Text(
                                text = "${device.manufacturer} ${device.model}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "🔋 ${device.batteryLevel}%",
                                    fontSize = 11.sp,
                                    color = EmeraldOnline,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "📶 ${device.networkType.ifBlank { "Online" }}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Details",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 3x3 Grid Actions Module Grid
                val gridItems = listOf(
                    GridActionItem("Live Screen", Icons.Default.ScreenShare, onNavigateToScreenShare, "nav_screen_share"),
                    GridActionItem("Apps", Icons.Default.Apps, onNavigateToAppManager, "nav_app_manager"),
                    GridActionItem("Files", Icons.Default.Folder, onNavigateToFileManager, "nav_file_manager"),
                    GridActionItem("Gallery", Icons.Default.PhotoLibrary, onNavigateToGallery, "nav_gallery"),
                    GridActionItem("Location", Icons.Default.MyLocation, onNavigateToLocation, "nav_location"),
                    GridActionItem("Call Logs", Icons.Default.Call, onNavigateToCommLog, "nav_comm_log"),
                    GridActionItem("Mic Listen", Icons.Default.Mic, onNavigateToPermissions, "nav_mic"),
                    GridActionItem("Messages", Icons.Default.Sms, onNavigateToCommLog, "nav_messages"),
                    GridActionItem("Camera", Icons.Default.CameraAlt, onNavigateToScreenShare, "nav_camera")
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    items(gridItems.size) { index ->
                        val item = gridItems[index]
                        ActionGridSquareTile(
                            title = item.title,
                            icon = item.icon,
                            onClick = item.onClick,
                            testTag = item.testTag
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "PRIORITY FLEET & HOME WIDGET",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                PriorityWidgetPreviewCard(
                    priorityDevices = priorityWidgetDevices,
                    onPinWidgetToHomeScreen = onPinWidgetToHomeScreen,
                    onManageFleetClick = onNavigateToManagedDevices
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "COMMAND TROUBLESHOOTING HEATMAP",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                CommandHeatmapCard(
                    auditLogs = auditLogs,
                    onOpenActivityLogs = onNavigateToAuditLogs,
                    onOpenBulkFleet = onNavigateToManagedDevices
                )

                if (signalStabilityProfiles.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "NETWORK STABILITY & REMOTE LOGCAT",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    NetworkSignalStabilityCard(
                        profiles = signalStabilityProfiles,
                        onRefreshProfiles = onRefreshSignalStability,
                        onOpenRemoteLogcat = onNavigateToRemoteLogcat
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "BACKGROUND SYNC & ACTIVITY AUDIT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                SyncQueueStatusCard(
                    queueItems = syncQueueItems,
                    pendingCount = pendingSyncCount,
                    isOfflineBufferMode = isOfflineBufferMode,
                    isFlushing = isFlushingSyncQueue,
                    onToggleOfflineBufferMode = onToggleOfflineBufferMode,
                    onEnqueueSettingsSync = onEnqueueSettingsSync,
                    onEnqueueGeofenceSync = onEnqueueGeofenceSync,
                    onFlushQueue = onFlushSyncQueue,
                    onClearCompleted = onClearCompletedSyncItems,
                    onOpenActivityLogs = onNavigateToAuditLogs
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "ENTERPRISE SECURITY & COMPLIANCE POSTURE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                CompliancePostureCard(
                    report = complianceReport,
                    targetDeviceName = device.deviceName,
                    onRemediateAll = onRemediateAllCompliance
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "ENTERPRISE KIOSK & LOCKTASK MODE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                KioskManagementCard(
                    config = kioskConfig,
                    targetDeviceName = device.deviceName,
                    onToggleKiosk = { enabled -> onToggleKiosk(enabled, null) },
                    onSetSingleAppMode = { pkg -> onToggleKiosk(true, pkg) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "FLEET BROADCAST & EMERGENCY ALERTS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                FleetBroadcastCard(
                    broadcasts = fleetBroadcasts,
                    onSendBroadcast = onSendFleetBroadcast,
                    onAcknowledge = onAcknowledgeFleetBroadcast
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "ENTERPRISE WI-FI PROVISIONING",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                WifiProvisioningCard(
                    profiles = wifiProfiles,
                    onPushProfile = onPushWifiProfile,
                    onRevokeProfile = onRevokeWifiProfile
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "STORAGE & CACHE BLOAT OPTIMIZER",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                StorageOptimizationCard(
                    report = storageReport,
                    targetDeviceName = device.deviceName,
                    isOptimizing = isOptimizingStorage,
                    onTriggerCleanup = onTriggerStorageCleanup
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "GEOFENCE & BATTERY ANALYTICS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                GeofenceAlertNotificationCard(
                    geofenceConfig = geofenceConfig,
                    pushAlertsEnabled = geofencePushAlertsEnabled,
                    hasNotificationPermission = hasNotificationPermission,
                    alerts = geofenceAlerts,
                    onTogglePushAlerts = onToggleGeofencePushAlerts,
                    onUpdateBoundaryRadius = onUpdateGeofenceRadius,
                    onTriggerBoundaryTest = onTriggerGeofenceBoundaryTest,
                    onAcknowledgeAlert = onAcknowledgeGeofenceAlert
                )

                Spacer(modifier = Modifier.height(14.dp))

                BatteryDischargeReportCard(
                    report = batteryDischargeReport
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showAddDeviceModal) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAddDeviceModal = false },
            title = {
                Text(
                    text = "Add Managed Device",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Connect and pair with the target managed Android device:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    androidx.compose.material3.Button(
                        onClick = {
                            showAddDeviceModal = false
                            showQrScanner = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scan QR Code (Camera)")
                    }

                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            showAddDeviceModal = false
                            onNavigateToPairing()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enter 6-Digit PIN Code")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showAddDeviceModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showQrScanner) {
        com.example.ui.components.QRScannerDialog(
            onDismiss = { showQrScanner = false },
            onQrScanned = { scannedCode ->
                showQrScanner = false
                onScanQrPair(scannedCode)
            }
        )
    }
}

private data class GridActionItem(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val testTag: String
)

@Composable
private fun ActionGridSquareTile(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag(testTag)
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
