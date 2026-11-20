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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.BatteryDischargeReportCard
import com.example.ui.components.BatteryHealthCalibrationCard
import com.example.ui.components.BatteryTrendChart
import com.example.ui.components.CommandHeatmapCard
import com.example.ui.components.CompliancePostureCard
import com.example.ui.components.DeviceHealthCard
import com.example.ui.components.FleetBroadcastCard
import com.example.ui.components.GeofenceAlertNotificationCard
import com.example.ui.components.HourlyBatteryGaugeCard
import com.example.ui.components.KioskManagementCard
import com.example.ui.components.MetricCard
import com.example.model.NexusDevice
import com.example.ui.components.NetworkScannerCard
import com.example.ui.components.NetworkSignalStabilityCard
import com.example.ui.components.PriorityWidgetPreviewCard
import com.example.ui.components.RemoteAudioFocusCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.StorageOptimizationCard
import com.example.ui.components.SyncQueueStatusCard
import com.example.ui.components.SystemPerformanceCard
import com.example.ui.components.TopAppUsageSummary
import com.example.ui.components.WifiProvisioningCard
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.LocalThemeController
import com.example.ui.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    pairedDeviceId: String?,
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
    onNavigateToRemoteLogcat: () -> Unit = {},
    priorityWidgetDevices: List<NexusDevice> = emptyList(),
    onPinWidgetToHomeScreen: () -> Unit = {}
) {
    val managedDevice by viewModel.managedDeviceState.collectAsStateWithLifecycle()
    val batteryState by viewModel.batteryState.collectAsStateWithLifecycle()
    val storageState by viewModel.storageState.collectAsStateWithLifecycle()
    val networkState by viewModel.networkState.collectAsStateWithLifecycle()
    val ramState by viewModel.ramState.collectAsStateWithLifecycle()
    val performanceMetrics by viewModel.performanceMetricsState.collectAsStateWithLifecycle()
    val batteryCalibrationReport by viewModel.batteryCalibrationReport.collectAsStateWithLifecycle()
    val batteryDischargeReport by viewModel.batteryDischargeReport.collectAsStateWithLifecycle()
    val isCalibratingBattery by viewModel.isCalibratingBattery.collectAsStateWithLifecycle()
    val calibrationProgress by viewModel.calibrationProgress.collectAsStateWithLifecycle()
    val networkDiagnosticsReport by viewModel.networkDiagnosticsReport.collectAsStateWithLifecycle()
    val isScanningWifi by viewModel.isScanningWifi.collectAsStateWithLifecycle()
    val geofenceConfig by viewModel.geofenceConfig.collectAsStateWithLifecycle()
    val geofencePushAlertsEnabled by viewModel.geofencePushAlertsEnabled.collectAsStateWithLifecycle()
    val geofenceAlerts by viewModel.geofenceAlerts.collectAsStateWithLifecycle()
    val syncQueueItems by viewModel.syncQueueItems.collectAsStateWithLifecycle()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsStateWithLifecycle()
    val isOfflineBufferMode by viewModel.isOfflineBufferMode.collectAsStateWithLifecycle()
    val isFlushingSyncQueue by viewModel.isFlushingSyncQueue.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
    val signalStabilityProfiles by viewModel.signalStabilityProfiles.collectAsStateWithLifecycle()
    val kioskConfig by viewModel.kioskConfig.collectAsStateWithLifecycle()
    val complianceReport by viewModel.complianceReport.collectAsStateWithLifecycle()
    val wifiProfiles by viewModel.wifiProfiles.collectAsStateWithLifecycle()
    val fleetBroadcasts by viewModel.fleetBroadcasts.collectAsStateWithLifecycle()
    val storageReport by viewModel.storageReport.collectAsStateWithLifecycle()
    val isOptimizingStorage by viewModel.isOptimizingStorage.collectAsStateWithLifecycle()
    val unacknowledgedAlertCount = geofenceAlerts.count { !it.acknowledged }

    val themeController = LocalThemeController.current

    LaunchedEffect(pairedDeviceId) {
        if (!pairedDeviceId.isNullOrEmpty()) {
            viewModel.observeRemoteDevice(pairedDeviceId)
        }
    }

    var currentBottomTab by remember { mutableStateOf("Home") }

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
                    onClick = { onNavigateToPairing() },
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
                .testTag("nexus_dashboard_screen")
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
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "NEXUS CONTROL PRO",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Quick Light / Dark mode toggle button
                    IconButton(
                        onClick = { themeController.toggleTheme() },
                        modifier = Modifier.testTag("dashboard_theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (themeController.isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (themeController.isDark) "Switch to Light Mode" else "Switch to Dark Mode",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    IconButton(onClick = { onNavigateToLocation() }) {
                        BadgedBox(
                            badge = {
                                if (unacknowledgedAlertCount > 0) {
                                    Badge { Text(unacknowledgedAlertCount.toString()) }
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

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Greeting & Title Banner
                item(span = { GridItemSpan(2) }) {
                    Column {
                        Text(
                            text = "Hello, Admin 👋",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Real-time telemetry and management portal",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Target Connected Device Banner
                item(span = { GridItemSpan(2) }) {
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
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = "Device",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
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
                                        text = managedDevice?.deviceName ?: "Field Phone A",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    StatusBadge(
                                        text = if (managedDevice?.isOnline != false) "Online" else "Offline",
                                        color = if (managedDevice?.isOnline != false) EmeraldOnline else Color.Gray
                                    )
                                }

                                Text(
                                    text = "${managedDevice?.manufacturer ?: "Realme"} ${managedDevice?.model ?: "11 Pro 5G"}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "🔋 ${batteryState.level}%",
                                        fontSize = 11.sp,
                                        color = EmeraldOnline,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "📶 ${networkState.type}",
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
                }

                // Priority Fleet & Home Screen Widget Preview
                item(span = { GridItemSpan(2) }) {
                    Text(
                        text = "PRIORITY FLEET & HOME WIDGET",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                item(span = { GridItemSpan(2) }) {
                    PriorityWidgetPreviewCard(
                        priorityDevices = priorityWidgetDevices,
                        onPinWidgetToHomeScreen = onPinWidgetToHomeScreen,
                        onManageFleetClick = onNavigateToManagedDevices
                    )
                }

                // Visual Command Frequency Heatmap Widget
                item(span = { GridItemSpan(2) }) {
                    Text(
                        text = "COMMAND TROUBLESHOOTING HEATMAP",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                item(span = { GridItemSpan(2) }) {
                    CommandHeatmapCard(
                        auditLogs = auditLogs,
                        onOpenActivityLogs = onNavigateToAuditLogs,
                        onOpenBulkFleet = onNavigateToManagedDevices
                    )
                }

                // 24h Network Signal Stability & Remote Logcat Entry
                item(span = { GridItemSpan(2) }) {
                    Text(
                        text = "NETWORK STABILITY & REMOTE LOGCAT",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                item(span = { GridItemSpan(2) }) {
                    NetworkSignalStabilityCard(
                        profiles = signalStabilityProfiles,
                        onRefreshProfiles = viewModel::refreshSignalStabilityProfiles,
                        onOpenRemoteLogcat = onNavigateToRemoteLogcat
                    )
                }

                // Section Header: Device Health & Performance
                item(span = { GridItemSpan(2) }) {
                    Text(
                        text = "BACKGROUND SYNC & ACTIVITY AUDIT",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                // WorkManager Offline Sync Queue & Activity Logs Card
                item(span = { GridItemSpan(2) }) {
                    SyncQueueStatusCard(
                        queueItems = syncQueueItems,
                        pendingCount = pendingSyncCount,
                        isOfflineBufferMode = isOfflineBufferMode,
                        isFlushing = isFlushingSyncQueue,
                        onToggleOfflineBufferMode = viewModel::setOfflineBufferMode,
                        onEnqueueSettingsSync = viewModel::enqueueSettingsSync,
                        onEnqueueGeofenceSync = viewModel::enqueueGeofencePolicySync,
                        onFlushQueue = viewModel::flushSyncQueue,
                        onClearCompleted = viewModel::clearCompletedSyncItems,
                        onOpenActivityLogs = onNavigateToAuditLogs
                    )
                }

                // Enterprise Security & Compliance Posture
                item(span = { GridItemSpan(2) }) {
                    Text(
                        text = "ENTERPRISE SECURITY & COMPLIANCE POSTURE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                item(span = { GridItemSpan(2) }) {
                    CompliancePostureCard(
                        report = complianceReport,
                        targetDeviceName = managedDevice?.deviceName ?: "Field Phone A",
                        onRemediateAll = {
                            viewModel.remediateAllCompliance(managedDevice?.deviceId ?: "phone_a")
                        }
                    )
                }

                // Enterprise Kiosk Lockdown
                item(span = { GridItemSpan(2) }) {
                    Text(
                        text = "ENTERPRISE KIOSK & LOCKTASK MODE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                item(span = { GridItemSpan(2) }) {
                    KioskManagementCard(
                        config = kioskConfig,
                        targetDeviceName = managedDevice?.deviceName ?: "Field Phone A",
                        onToggleKiosk = { enabled ->
                            viewModel.toggleKioskMode(managedDevice?.deviceId ?: "phone_a", enabled, null)
                        },
                        onSetSingleAppMode = { pkg ->
                            viewModel.toggleKioskMode(managedDevice?.deviceId ?: "phone_a", true, pkg)
                        }
                    )
                }

                // Fleet Broadcast
                item(span = { GridItemSpan(2) }) {
                    Text(
                        text = "FLEET BROADCAST & EMERGENCY ALERTS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                item(span = { GridItemSpan(2) }) {
                    FleetBroadcastCard(
                        broadcasts = fleetBroadcasts,
                        onSendBroadcast = viewModel::sendFleetBroadcast,
                        onAcknowledge = { alertId ->
                            viewModel.acknowledgeFleetBroadcast(alertId, managedDevice?.deviceId ?: "phone_a")
                        }
                    )
                }

                // Enterprise Wi-Fi Provisioning
                item(span = { GridItemSpan(2) }) {
                    Text(
                        text = "ENTERPRISE WI-FI PROVISIONING",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                item(span = { GridItemSpan(2) }) {
                    WifiProvisioningCard(
                        profiles = wifiProfiles,
                        onPushProfile = viewModel::pushWifiProfile,
                        onRevokeProfile = viewModel::revokeWifiProfile
                    )
                }

                // Storage & Cache Optimizer
                item(span = { GridItemSpan(2) }) {
                    Text(
                        text = "STORAGE & CACHE BLOAT OPTIMIZER",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                item(span = { GridItemSpan(2) }) {
                    StorageOptimizationCard(
                        report = storageReport,
                        targetDeviceName = managedDevice?.deviceName ?: "Field Phone A",
                        isOptimizing = isOptimizingStorage,
                        onTriggerCleanup = {
                            viewModel.triggerStorageCleanup(managedDevice?.deviceId ?: "phone_a")
                        }
                    )
                }

                // Section Header: Device Health & Performance
                item(span = { GridItemSpan(2) }) {
                    Text(
                        text = "HEALTH & ANALYTICS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                // System Performance Component (CPU & RAM Spikes from Room DB)
                item(span = { GridItemSpan(2) }) {
                    SystemPerformanceCard(
                        metrics = performanceMetrics,
                        onRecordSample = viewModel::recordPerformanceSample
                    )
                }

                // Device Health Summary Card
                item(span = { GridItemSpan(2) }) {
                    DeviceHealthCard(
                        networkLatencyMs = 24,
                        topApps = listOf(
                            TopAppUsageSummary("WhatsApp", 0.35f, 142),
                            TopAppUsageSummary("Chrome", 0.25f, 98),
                            TopAppUsageSummary("YouTube", 0.20f, 76)
                        )
                    )
                }

                // 7-Day Battery Historical Trend Chart
                item(span = { GridItemSpan(2) }) {
                    BatteryTrendChart()
                }

                // 1-Hour Dynamic Battery Gauge
                item(span = { GridItemSpan(2) }) {
                    HourlyBatteryGaugeCard(
                        hourlyDrainPercentage = 3.2f,
                        currentBatteryPct = batteryState.level,
                        modifier = Modifier.testTag("hourly_battery_gauge_card")
                    )
                }

                // Battery Health Calibration Report
                item(span = { GridItemSpan(2) }) {
                    BatteryHealthCalibrationCard(
                        report = batteryCalibrationReport,
                        isCalibrating = isCalibratingBattery,
                        calibrationProgress = calibrationProgress,
                        onStartCalibration = viewModel::recalibrateBattery
                    )
                }

                // Detailed Battery Discharge & Degradation Comparison Report
                item(span = { GridItemSpan(2) }) {
                    BatteryDischargeReportCard(
                        report = batteryDischargeReport
                    )
                }

                // Geofence Boundary Exit Push Notification Center
                item(span = { GridItemSpan(2) }) {
                    GeofenceAlertNotificationCard(
                        geofenceConfig = geofenceConfig,
                        pushAlertsEnabled = geofencePushAlertsEnabled,
                        hasNotificationPermission = viewModel.hasNotificationPermission(),
                        alerts = geofenceAlerts,
                        onTogglePushAlerts = viewModel::toggleGeofencePushAlerts,
                        onUpdateBoundaryRadius = viewModel::updateGeofenceRadius,
                        onTriggerBoundaryTest = viewModel::triggerGeofenceBoundaryTest,
                        onAcknowledgeAlert = viewModel::acknowledgeGeofenceAlert
                    )
                }

                // Wi-Fi Access Points & Network Scanner
                item(span = { GridItemSpan(2) }) {
                    NetworkScannerCard(
                        report = networkDiagnosticsReport,
                        isScanning = isScanningWifi,
                        onTriggerScan = viewModel::scanNearbyWifiNetworks
                    )
                }

                // Focus Mode (DND) & Remote Volume Management Card
                item(span = { GridItemSpan(2) }) {
                    RemoteAudioFocusCard(
                        focusModeActive = managedDevice?.focusModeActive ?: false,
                        ringerVolume = managedDevice?.ringerVolume ?: 80,
                        mediaVolume = managedDevice?.mediaVolume ?: 70,
                        alarmVolume = managedDevice?.alarmVolume ?: 85,
                        onToggleFocusMode = { enable ->
                            viewModel.toggleRemoteFocusMode(pairedDeviceId ?: "phone_b", enable)
                        },
                        onUpdateVolumes = { ringer, media, alarm ->
                            viewModel.setRemoteVolume(pairedDeviceId ?: "phone_b", ringer, media, alarm)
                        }
                    )
                }

                // Section Header: Telemetry Metrics
                item(span = { GridItemSpan(2) }) {
                    Text(
                        text = "SYSTEM METRICS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                // Metric Card 1: Battery
                item {
                    MetricCard(
                        title = "Battery",
                        value = "${batteryState.level}%",
                        subtitle = if (batteryState.isCharging) "Charging" else "Discharging",
                        icon = Icons.Default.BatteryChargingFull,
                        progress = batteryState.level / 100f,
                        accentColor = EmeraldOnline,
                        modifier = Modifier.testTag("metric_battery")
                    )
                }

                // Metric Card 2: Storage
                item {
                    MetricCard(
                        title = "Storage",
                        value = "${storageState.usedGb} GB",
                        subtitle = "Total: ${storageState.totalGb.toInt()} GB",
                        icon = Icons.Default.Storage,
                        progress = storageState.percentageUsed,
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("metric_storage")
                    )
                }

                // Metric Card 3: RAM Memory
                item {
                    MetricCard(
                        title = "RAM",
                        value = "${ramState.usedGb} GB",
                        subtitle = "Total: ${ramState.totalGb.toInt()} GB",
                        icon = Icons.Default.Memory,
                        progress = ramState.percentageUsed,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.testTag("metric_ram")
                    )
                }

                // Metric Card 4: Network
                item {
                    MetricCard(
                        title = "Network",
                        value = networkState.type,
                        subtitle = if (networkState.isOnline) "Connected (Live)" else "Disconnected",
                        icon = Icons.Default.Wifi,
                        progress = if (networkState.isOnline) 0.95f else 0.0f,
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("metric_network")
                    )
                }

                // Section Header: Remote Actions
                item(span = { GridItemSpan(2) }) {
                    Text(
                        text = "QUICK ACTIONS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }

                // 3-column sub-grid items for remote actions
                val quickActions = listOf(
                    DashboardActionItem("Live Screen", Icons.Default.ScreenShare, onNavigateToScreenShare, "action_live_screen"),
                    DashboardActionItem("Apps", Icons.Default.Apps, onNavigateToAppManager, "action_apps"),
                    DashboardActionItem("Files", Icons.Default.Folder, onNavigateToFileManager, "action_files"),
                    DashboardActionItem("Gallery", Icons.Default.PhotoLibrary, onNavigateToGallery, "action_gallery"),
                    DashboardActionItem("Location", Icons.Default.MyLocation, onNavigateToLocation, "action_location"),
                    DashboardActionItem("Call Logs", Icons.Default.Call, onNavigateToCommLog, "action_call_logs"),
                    DashboardActionItem("Mic Listen", Icons.Default.Mic, onNavigateToPermissions, "action_mic"),
                    DashboardActionItem("Messages", Icons.Default.Sms, onNavigateToCommLog, "action_messages"),
                    DashboardActionItem("Camera", Icons.Default.CameraAlt, onNavigateToScreenShare, "action_camera")
                )

                items(quickActions.size) { index ->
                    val action = quickActions[index]
                    DashboardActionCard(
                        title = action.title,
                        icon = action.icon,
                        onClick = action.onClick,
                        testTag = action.testTag
                    )
                }

                item(span = { GridItemSpan(2) }) {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

private data class DashboardActionItem(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val testTag: String
)

@Composable
private fun DashboardActionCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag(testTag)
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
