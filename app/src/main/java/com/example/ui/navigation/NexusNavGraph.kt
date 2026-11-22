package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.model.NexusDevice
import com.example.model.UserRole
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AppManagerScreen
import com.example.ui.screens.CommLogScreen
import com.example.ui.screens.DeviceDetailsScreen
import com.example.ui.screens.EnterpriseAdminScreen
import com.example.ui.screens.FileManagerScreen
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.LocationScreen
import com.example.ui.screens.ManagedDashboardScreen
import com.example.ui.screens.ManagedDevicesScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PairingScreen
import com.example.ui.screens.PermissionDashboardScreen
import com.example.ui.screens.RemoteLogcatScreen
import com.example.ui.screens.RoleSelectionScreen
import com.example.ui.screens.ScreenShareScreen
import com.example.ui.screens.SecuritySettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.viewmodel.AppManagerViewModel
import com.example.ui.viewmodel.CommLogViewModel
import com.example.ui.viewmodel.DashboardViewModel
import com.example.ui.viewmodel.FileManagerViewModel
import com.example.ui.viewmodel.GalleryViewModel
import com.example.ui.viewmodel.LocationViewModel
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ManagedDevicesViewModel
import com.example.ui.viewmodel.PairingViewModel
import com.example.ui.viewmodel.RemoteLogcatViewModel
import com.example.ui.viewmodel.ScreenShareViewModel
import com.example.ui.viewmodel.SecurityViewModel

@Composable
fun NexusNavGraph(
    navController: NavHostController = rememberNavController(),
    mainViewModel: MainViewModel = viewModel(),
    pairingViewModel: PairingViewModel = viewModel(),
    dashboardViewModel: DashboardViewModel = viewModel(),
    screenShareViewModel: ScreenShareViewModel = viewModel(),
    appManagerViewModel: AppManagerViewModel = viewModel(),
    fileManagerViewModel: FileManagerViewModel = viewModel(),
    galleryViewModel: GalleryViewModel = viewModel(),
    locationViewModel: LocationViewModel = viewModel(),
    commLogViewModel: CommLogViewModel = viewModel(),
    securityViewModel: SecurityViewModel = viewModel(),
    managedDevicesViewModel: ManagedDevicesViewModel = viewModel()
) {
    val role by mainViewModel.roleState.collectAsState()
    val themeMode by mainViewModel.themeModeState.collectAsState()
    val currentDeviceId by mainViewModel.currentDeviceId.collectAsState()
    val currentDeviceName by mainViewModel.currentDeviceName.collectAsState()
    val pairedDeviceId by mainViewModel.pairedDeviceIdState.collectAsState()
    val managedDevice by dashboardViewModel.managedDeviceState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                currentRole = role,
                onNavigateToRoleSelection = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToAdmin = {
                    dashboardViewModel.observeRemoteDevice(pairedDeviceId ?: "phone_a")
                    navController.navigate(Screen.AdminDashboard.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToManaged = {
                    mainViewModel.updateLocalTelemetry()
                    navController.navigate(Screen.ManagedDashboard.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onSkip = {
                    navController.navigate(Screen.RoleSelection.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                onGetStarted = {
                    navController.navigate(Screen.RoleSelection.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.RoleSelection.route) {
            RoleSelectionScreen(
                onSelectRole = { selected ->
                    mainViewModel.selectRole(selected)
                    if (selected == UserRole.ADMIN) {
                        navController.navigate(Screen.AdminDashboard.route) {
                            popUpTo(Screen.RoleSelection.route) { inclusive = true }
                        }
                    } else {
                        mainViewModel.updateLocalTelemetry()
                        navController.navigate(Screen.ManagedDashboard.route) {
                            popUpTo(Screen.RoleSelection.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.Pairing.route) {
            val pairingState by pairingViewModel.pairingState.collectAsState()
            val inputCode by pairingViewModel.inputCode.collectAsState()
            val message by pairingViewModel.pairingMessage.collectAsState()
            val isLoading by pairingViewModel.isLoading.collectAsState()

            PairingScreen(
                userRole = role,
                currentDeviceId = currentDeviceId,
                currentDeviceName = currentDeviceName,
                pairingState = pairingState,
                inputCode = inputCode,
                pairingMessage = message,
                isLoading = isLoading,
                onUpdateCode = pairingViewModel::updateInputCode,
                onGenerateCode = {
                    pairingViewModel.generateManagedPairingCode(currentDeviceId, currentDeviceName)
                },
                onSubmitCode = {
                    pairingViewModel.submitPairingCodeFromAdmin(currentDeviceId, currentDeviceName)
                },
                onConfirmPairing = pairingViewModel::confirmPairingOnManaged,
                onBack = { navController.popBackStack() },
                onScanQrCode = { qrData ->
                    pairingViewModel.pairFromScannedQr(
                        qrData = qrData,
                        adminDeviceId = currentDeviceId,
                        adminDeviceName = currentDeviceName
                    ) { newDev ->
                        managedDevicesViewModel.addOrUpdateDevice(newDev)
                        dashboardViewModel.observeRemoteDevice(newDev.deviceId)
                        navController.navigate(Screen.AdminDashboard.route) {
                            popUpTo(Screen.Pairing.route) { inclusive = true }
                        }
                    }
                },
                onNavigateToDashboard = {
                    if (role == UserRole.ADMIN) {
                        navController.navigate(Screen.AdminDashboard.route) {
                            popUpTo(Screen.Pairing.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.ManagedDashboard.route) {
                            popUpTo(Screen.Pairing.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.AdminDashboard.route) {
            val dischargeReport by dashboardViewModel.batteryDischargeReport.collectAsState()
            val geofenceConfig by dashboardViewModel.geofenceConfig.collectAsState()
            val geofencePushEnabled by dashboardViewModel.geofencePushAlertsEnabled.collectAsState()
            val geofenceAlerts by dashboardViewModel.geofenceAlerts.collectAsState()
            val syncQueueItems by dashboardViewModel.syncQueueItems.collectAsState()
            val pendingSyncCount by dashboardViewModel.pendingSyncCount.collectAsState()
            val isOfflineBufferMode by dashboardViewModel.isOfflineBufferMode.collectAsState()
            val isFlushingSyncQueue by dashboardViewModel.isFlushingSyncQueue.collectAsState()
            val priorityWidgetDevices by managedDevicesViewModel.highPriorityDevices.collectAsState()
            val auditLogs by securityViewModel.auditLogs.collectAsState()
            val signalStabilityProfiles by dashboardViewModel.signalStabilityProfiles.collectAsState()
            val kioskConfig by dashboardViewModel.kioskConfig.collectAsState()
            val complianceReport by dashboardViewModel.complianceReport.collectAsState()
            val wifiProfiles by dashboardViewModel.wifiProfiles.collectAsState()
            val fleetBroadcasts by dashboardViewModel.fleetBroadcasts.collectAsState()
            val storageReport by dashboardViewModel.storageReport.collectAsState()
            val isOptimizingStorage by dashboardViewModel.isOptimizingStorage.collectAsState()

            LaunchedEffect(pairedDeviceId) {
                if (!pairedDeviceId.isNullOrBlank()) {
                    dashboardViewModel.observeRemoteDevice(pairedDeviceId!!)
                }
            }

            AdminDashboardScreen(
                managedDevice = managedDevice,
                pairedDeviceId = pairedDeviceId,
                onRefresh = {
                    dashboardViewModel.refreshTelemetry(currentDeviceId, currentDeviceName, role.name)
                },
                onNavigateToPairing = { navController.navigate(Screen.Pairing.route) },
                onNavigateToScreenShare = { navController.navigate(Screen.ScreenShare.route) },
                onNavigateToAppManager = { navController.navigate(Screen.AppManager.route) },
                onNavigateToFileManager = { navController.navigate(Screen.FileManager.route) },
                onNavigateToGallery = { navController.navigate(Screen.Gallery.route) },
                onNavigateToLocation = { navController.navigate(Screen.Location.route) },
                onNavigateToCommLog = { navController.navigate(Screen.CommLog.route) },
                onNavigateToPermissions = { navController.navigate(Screen.PermissionDashboard.route) },
                onNavigateToSecurity = { navController.navigate(Screen.SecuritySettings.route) },
                onNavigateToEnterprise = { navController.navigate(Screen.DeviceDetails.route) },
                onNavigateToAuditLogs = { navController.navigate(Screen.AuditLog.route) },
                onNavigateToManagedDevices = { navController.navigate(Screen.ManagedDevices.route) },
                batteryDischargeReport = dischargeReport,
                geofenceConfig = geofenceConfig,
                geofencePushAlertsEnabled = geofencePushEnabled,
                hasNotificationPermission = dashboardViewModel.hasNotificationPermission(),
                geofenceAlerts = geofenceAlerts,
                onToggleGeofencePushAlerts = dashboardViewModel::toggleGeofencePushAlerts,
                onUpdateGeofenceRadius = dashboardViewModel::updateGeofenceRadius,
                onTriggerGeofenceBoundaryTest = dashboardViewModel::triggerGeofenceBoundaryTest,
                onAcknowledgeGeofenceAlert = dashboardViewModel::acknowledgeGeofenceAlert,
                syncQueueItems = syncQueueItems,
                pendingSyncCount = pendingSyncCount,
                isOfflineBufferMode = isOfflineBufferMode,
                isFlushingSyncQueue = isFlushingSyncQueue,
                onToggleOfflineBufferMode = dashboardViewModel::setOfflineBufferMode,
                onEnqueueSettingsSync = dashboardViewModel::enqueueSettingsSync,
                onEnqueueGeofenceSync = dashboardViewModel::enqueueGeofencePolicySync,
                onFlushSyncQueue = dashboardViewModel::flushSyncQueue,
                onClearCompletedSyncItems = dashboardViewModel::clearCompletedSyncItems,
                priorityWidgetDevices = priorityWidgetDevices,
                onPinWidgetToHomeScreen = managedDevicesViewModel::requestPinHomeScreenWidget,
                auditLogs = auditLogs,
                signalStabilityProfiles = signalStabilityProfiles,
                onRefreshSignalStability = dashboardViewModel::refreshSignalStabilityProfiles,
                onNavigateToRemoteLogcat = { navController.navigate(Screen.RemoteLogcat.route) },
                kioskConfig = kioskConfig,
                onToggleKiosk = { enabled, singleApp ->
                    dashboardViewModel.toggleKioskMode(managedDevice?.deviceId ?: "phone_a", enabled, singleApp)
                },
                complianceReport = complianceReport,
                onRemediateAllCompliance = {
                    dashboardViewModel.remediateAllCompliance(managedDevice?.deviceId ?: "phone_a")
                },
                wifiProfiles = wifiProfiles,
                onPushWifiProfile = dashboardViewModel::pushWifiProfile,
                onRevokeWifiProfile = dashboardViewModel::revokeWifiProfile,
                fleetBroadcasts = fleetBroadcasts,
                onSendFleetBroadcast = dashboardViewModel::sendFleetBroadcast,
                onAcknowledgeFleetBroadcast = { alertId ->
                    dashboardViewModel.acknowledgeFleetBroadcast(alertId, managedDevice?.deviceId ?: "phone_a")
                },
                storageReport = storageReport,
                isOptimizingStorage = isOptimizingStorage,
                onTriggerStorageCleanup = {
                    dashboardViewModel.triggerStorageCleanup(managedDevice?.deviceId ?: "phone_a")
                },
                onScanQrPair = { qrData ->
                    pairingViewModel.pairFromScannedQr(
                        qrData = qrData,
                        adminDeviceId = currentDeviceId,
                        adminDeviceName = currentDeviceName
                    ) { newDev ->
                        managedDevicesViewModel.addOrUpdateDevice(newDev)
                        dashboardViewModel.observeRemoteDevice(newDev.deviceId)
                    }
                }
            )
        }

        composable(Screen.DeviceDetails.route) {
            DeviceDetailsScreen(
                device = managedDevice,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ManagedDashboard.route) {
            val telemetry by mainViewModel.currentDeviceTelemetry.collectAsState()
            ManagedDashboardScreen(
                currentDevice = telemetry,
                pairedAdminId = pairedDeviceId,
                onRefresh = { mainViewModel.updateLocalTelemetry() },
                onNavigateToPairing = { navController.navigate(Screen.Pairing.route) },
                onEmergencyRevoke = { mainViewModel.revokeAllAccess() },
                onNavigateToSecurity = { navController.navigate(Screen.SecuritySettings.route) }
            )
        }

        composable(Screen.ScreenShare.route) {
            val isSharingActive by screenShareViewModel.isSharingActive.collectAsState()
            val resolution by screenShareViewModel.streamResolution.collectAsState()
            val latency by screenShareViewModel.connectionLatencyMs.collectAsState()
            val focusModeActive by screenShareViewModel.focusModeActive.collectAsState()
            val ringerVolume by screenShareViewModel.ringerVolume.collectAsState()
            val mediaVolume by screenShareViewModel.mediaVolume.collectAsState()
            val alarmVolume by screenShareViewModel.alarmVolume.collectAsState()

            LaunchedEffect(pairedDeviceId) {
                screenShareViewModel.observeDeviceAudioAndFocus(pairedDeviceId ?: "phone_b")
            }

            ScreenShareScreen(
                isSharingActive = isSharingActive,
                streamResolution = resolution,
                connectionLatencyMs = latency,
                isAdmin = role == UserRole.ADMIN,
                focusModeActive = focusModeActive,
                ringerVolume = ringerVolume,
                mediaVolume = mediaVolume,
                alarmVolume = alarmVolume,
                onToggleFocusMode = { enable ->
                    screenShareViewModel.toggleFocusMode(pairedDeviceId ?: "phone_b", enable)
                },
                onUpdateVolumes = { ring, media, alarm ->
                    screenShareViewModel.setRemoteVolume(pairedDeviceId ?: "phone_b", ring, media, alarm)
                },
                onStartStream = {
                    if (role == UserRole.ADMIN) {
                        screenShareViewModel.requestRemoteScreenStream(
                            targetDeviceId = pairedDeviceId ?: "phone_a",
                            targetIp = managedDevice?.ipAddress
                        )
                    } else {
                        screenShareViewModel.startManagedScreenCapture(pairedDeviceId ?: "admin_device")
                    }
                },
                onStopStream = {
                    if (role == UserRole.ADMIN) {
                        screenShareViewModel.stopRemoteViewing(pairedDeviceId ?: "phone_a")
                    } else {
                        screenShareViewModel.stopManagedScreenCapture()
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AppManager.route) {
            val query by appManagerViewModel.searchQuery.collectAsState()
            val isLoading by appManagerViewModel.isLoading.collectAsState()
            val hasUsageAccess by appManagerViewModel.hasUsageAccess.collectAsState()

            LaunchedEffect(pairedDeviceId, managedDevice?.ipAddress) {
                appManagerViewModel.loadApplications(pairedDeviceId, managedDevice?.ipAddress)
            }

            AppManagerScreen(
                appList = appManagerViewModel.getFilteredApps(),
                searchQuery = query,
                isLoading = isLoading,
                hasUsageAccess = hasUsageAccess,
                onQueryChange = appManagerViewModel::updateSearchQuery,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.FileManager.route) {
            val currentPath by fileManagerViewModel.currentPath.collectAsState()
            val fileList by fileManagerViewModel.fileList.collectAsState()
            val isLoading by fileManagerViewModel.isLoading.collectAsState()
            val selectedFile by fileManagerViewModel.selectedFile.collectAsState()
            val transferStatus by fileManagerViewModel.transferStatus.collectAsState()

            FileManagerScreen(
                currentPath = currentPath,
                fileList = fileList,
                isLoading = isLoading,
                selectedFile = selectedFile,
                transferStatus = transferStatus,
                onSelectFile = fileManagerViewModel::selectFile,
                onDownloadFile = fileManagerViewModel::downloadFile,
                onDismissSelected = fileManagerViewModel::dismissSelectedFile,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Gallery.route) {
            val mediaList = galleryViewModel.getFilteredMedia()
            val filterType by galleryViewModel.filterType.collectAsState()
            val isLoading by galleryViewModel.isLoading.collectAsState()
            val selectedMedia by galleryViewModel.selectedMedia.collectAsState()

            GalleryScreen(
                mediaList = mediaList,
                filterType = filterType,
                isLoading = isLoading,
                selectedMedia = selectedMedia,
                onFilterSelect = galleryViewModel::setFilter,
                onMediaSelect = galleryViewModel::selectMedia,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Location.route) {
            val locationState by locationViewModel.locationState.collectAsState()
            val trackingActive by locationViewModel.isContinuousTrackingActive.collectAsState()
            val isLoading by locationViewModel.isLoading.collectAsState()

            LaunchedEffect(pairedDeviceId, managedDevice?.ipAddress) {
                locationViewModel.refreshLocation(pairedDeviceId, managedDevice?.ipAddress)
            }

            LocationScreen(
                locationState = locationState,
                isContinuousTrackingActive = trackingActive,
                isLoading = isLoading,
                onRefreshLocation = { locationViewModel.refreshLocation(pairedDeviceId, managedDevice?.ipAddress) },
                onToggleTracking = locationViewModel::toggleContinuousTracking,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CommLog.route) {
            val logs = commLogViewModel.getFilteredCallLogs()
            val filterType by commLogViewModel.filterType.collectAsState()
            val isLoading by commLogViewModel.isLoading.collectAsState()

            LaunchedEffect(pairedDeviceId) {
                commLogViewModel.loadCallLogs(pairedDeviceId)
            }

            CommLogScreen(
                callLogs = logs,
                filterType = filterType,
                isLoading = isLoading,
                onFilterSelect = commLogViewModel::setFilter,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PermissionDashboard.route) {
            PermissionDashboardScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.SecuritySettings.route) {
            val auditLogs by securityViewModel.auditLogs.collectAsState()
            SecuritySettingsScreen(
                auditLogs = auditLogs,
                currentThemeMode = themeMode,
                onSetThemeMode = mainViewModel::setThemeMode,
                onEmergencyRevoke = { mainViewModel.revokeAllAccess() },
                onRoleReset = {
                    mainViewModel.selectRole(UserRole.UNSET)
                    navController.navigate(Screen.RoleSelection.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onRemoteReboot = {
                    securityViewModel.triggerRemoteReboot(pairedDeviceId ?: "phone_b")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.EnterpriseAdmin.route) {
            EnterpriseAdminScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.AuditLog.route) {
            val auditLogs by securityViewModel.auditLogs.collectAsState()
            val chainVerification by securityViewModel.chainVerification.collectAsState()
            com.example.ui.screens.AuditLogScreen(
                auditLogs = auditLogs,
                onBack = { navController.popBackStack() },
                chainVerification = chainVerification
            )
        }

        composable(Screen.RemoteTerminal.route) {
            val terminalViewModel: com.example.ui.viewmodel.TerminalViewModel = viewModel()
            com.example.ui.screens.RemoteTerminalScreen(
                viewModel = terminalViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ManagedDevices.route) {
            ManagedDevicesScreen(
                viewModel = managedDevicesViewModel,
                onBack = { navController.popBackStack() },
                onNavigateToPairing = { navController.navigate(Screen.Pairing.route) },
                onScanQrPair = { qrData ->
                    pairingViewModel.pairFromScannedQr(
                        qrData = qrData,
                        adminDeviceId = currentDeviceId,
                        adminDeviceName = currentDeviceName
                    ) { newDev ->
                        managedDevicesViewModel.addOrUpdateDevice(newDev)
                        dashboardViewModel.observeRemoteDevice(newDev.deviceId)
                    }
                }
            )
        }

        composable(Screen.RemoteLogcat.route) {
            val remoteLogcatViewModel: RemoteLogcatViewModel = viewModel()
            RemoteLogcatScreen(
                viewModel = remoteLogcatViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
