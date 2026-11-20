package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FirestoreRepository
import com.example.data.OfflineSyncQueueManager
import com.example.data.PriorityWidgetStore
import com.example.model.NexusDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ManagedDevicesViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val firestoreRepository = FirestoreRepository(application)
    private val offlineSyncQueueManager = OfflineSyncQueueManager(application, firestoreRepository)

    private val _managedDevices = MutableStateFlow(createInitialFleet())
    val managedDevices: StateFlow<List<NexusDevice>> = _managedDevices.asStateFlow()

    val highPriorityDevices: StateFlow<List<NexusDevice>> = _managedDevices
        .map { list -> list.filter { it.isHighPriority }.take(3) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedDeviceIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedDeviceIds: StateFlow<Set<String>> = _selectedDeviceIds.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterMode = MutableStateFlow("ALL") // ALL, PRIORITY, ONLINE, OFFLINE
    val filterMode: StateFlow<String> = _filterMode.asStateFlow()

    private val _isExecutingBulkAction = MutableStateFlow(false)
    val isExecutingBulkAction: StateFlow<Boolean> = _isExecutingBulkAction.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    init {
        PriorityWidgetStore.savePriorityDevices(appContext, _managedDevices.value)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateFilterMode(mode: String) {
        _filterMode.value = mode
    }

    fun toggleDeviceSelection(deviceId: String) {
        val current = _selectedDeviceIds.value.toMutableSet()
        if (current.contains(deviceId)) {
            current.remove(deviceId)
        } else {
            current.add(deviceId)
        }
        _selectedDeviceIds.value = current
    }

    fun selectAll(deviceIds: List<String>) {
        _selectedDeviceIds.value = deviceIds.toSet()
    }

    fun clearSelection() {
        _selectedDeviceIds.value = emptySet()
    }

    fun toggleHighPriority(deviceId: String) {
        val updated = _managedDevices.value.map { device ->
            if (device.deviceId == deviceId) {
                device.copy(isHighPriority = !device.isHighPriority)
            } else {
                device
            }
        }
        _managedDevices.value = updated
        PriorityWidgetStore.savePriorityDevices(appContext, updated)

        val target = updated.firstOrNull { it.deviceId == deviceId }
        val priorityCount = updated.count { it.isHighPriority }
        _statusBanner.value = if (target?.isHighPriority == true) {
            if (priorityCount > 3) {
                "${target.deviceName} marked High-Priority (Widget shows top 3 of $priorityCount)"
            } else {
                "Pinned ${target.deviceName} to Home Screen Widget"
            }
        } else {
            "Removed ${target?.deviceName ?: "device"} from Home Screen Widget"
        }
    }

    fun executeBulkLock() {
        val selectedIds = _selectedDeviceIds.value
        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            _isExecutingBulkAction.value = true
            val selectedDevices = _managedDevices.value.filter { it.deviceId in selectedIds }
            firestoreRepository.dispatchBulkDeviceCommand(
                deviceIds = selectedDevices.map { it.deviceId },
                deviceNames = selectedDevices.map { it.deviceName },
                commandType = "LOCK"
            )

            val updated = _managedDevices.value.map { device ->
                if (device.deviceId in selectedIds) {
                    device.copy(isLocked = true)
                } else {
                    device
                }
            }
            _managedDevices.value = updated
            PriorityWidgetStore.savePriorityDevices(appContext, updated)
            _selectedDeviceIds.value = emptySet()
            _statusBanner.value = "Locked ${selectedDevices.size} managed device(s) simultaneously"
            _isExecutingBulkAction.value = false
        }
    }

    fun executeBulkUnlock() {
        val selectedIds = _selectedDeviceIds.value
        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            _isExecutingBulkAction.value = true
            val selectedDevices = _managedDevices.value.filter { it.deviceId in selectedIds }
            firestoreRepository.dispatchBulkDeviceCommand(
                deviceIds = selectedDevices.map { it.deviceId },
                deviceNames = selectedDevices.map { it.deviceName },
                commandType = "UNLOCK"
            )

            val updated = _managedDevices.value.map { device ->
                if (device.deviceId in selectedIds) {
                    device.copy(isLocked = false)
                } else {
                    device
                }
            }
            _managedDevices.value = updated
            PriorityWidgetStore.savePriorityDevices(appContext, updated)
            _selectedDeviceIds.value = emptySet()
            _statusBanner.value = "Unlocked ${selectedDevices.size} managed device(s) simultaneously"
            _isExecutingBulkAction.value = false
        }
    }

    fun executeBulkReboot() {
        val selectedIds = _selectedDeviceIds.value
        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            _isExecutingBulkAction.value = true
            val selectedDevices = _managedDevices.value.filter { it.deviceId in selectedIds }
            firestoreRepository.dispatchBulkDeviceCommand(
                deviceIds = selectedDevices.map { it.deviceId },
                deviceNames = selectedDevices.map { it.deviceName },
                commandType = "REBOOT"
            )

            val updated = _managedDevices.value.map { device ->
                if (device.deviceId in selectedIds) {
                    device.copy(reboot_command = true, role = "REBOOT_REQUESTED")
                } else {
                    device
                }
            }
            _managedDevices.value = updated
            PriorityWidgetStore.savePriorityDevices(appContext, updated)
            _selectedDeviceIds.value = emptySet()
            _statusBanner.value = "Dispatched Reboot to ${selectedDevices.size} managed device(s)"
            _isExecutingBulkAction.value = false
        }
    }

    fun executeBulkSync() {
        val selectedIds = _selectedDeviceIds.value
        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            _isExecutingBulkAction.value = true
            val selectedDevices = _managedDevices.value.filter { it.deviceId in selectedIds }
            for (device in selectedDevices) {
                offlineSyncQueueManager.enqueueOrExecute(
                    operationType = "TELEMETRY_REFRESH",
                    targetDeviceId = device.deviceId,
                    payloadSummary = "Bulk fleet telemetry & location refresh for ${device.deviceName}",
                    payloadData = "source=bulk_toolbar"
                )
            }

            val now = System.currentTimeMillis()
            val updated = _managedDevices.value.map { device ->
                if (device.deviceId in selectedIds) {
                    device.copy(lastSeenTimestamp = now, reboot_command = false)
                } else {
                    device
                }
            }
            _managedDevices.value = updated
            PriorityWidgetStore.savePriorityDevices(appContext, updated)
            _selectedDeviceIds.value = emptySet()
            _statusBanner.value = "Synced telemetry & widget snapshot for ${selectedDevices.size} device(s)"
            _isExecutingBulkAction.value = false
        }
    }

    fun executeBulkPinToWidget() {
        val selectedIds = _selectedDeviceIds.value
        if (selectedIds.isEmpty()) return

        val updated = _managedDevices.value.map { device ->
            if (device.deviceId in selectedIds) {
                device.copy(isHighPriority = true)
            } else {
                device
            }
        }
        _managedDevices.value = updated
        PriorityWidgetStore.savePriorityDevices(appContext, updated)
        _selectedDeviceIds.value = emptySet()
        _statusBanner.value = "Updated Home Screen Widget priority selection (${selectedIds.size} device(s) pinned)"
    }

    fun requestPinHomeScreenWidget() {
        PriorityWidgetStore.savePriorityDevices(appContext, _managedDevices.value)
        val launched = PriorityWidgetStore.requestPinWidget(appContext)
        _statusBanner.value = if (launched) {
            "Confirm 'Add' on your home screen to pin the Nexus Priority Fleet widget."
        } else {
            "Widget updated! Long-press your home screen → Widgets → Nexus Priority Fleet to place it."
        }
    }

    fun dismissStatusBanner() {
        _statusBanner.value = null
    }

    private fun createInitialFleet(): List<NexusDevice> {
        val now = System.currentTimeMillis()
        return listOf(
            NexusDevice(
                deviceId = "phone_a",
                deviceName = "Field Phone A",
                manufacturer = "Realme",
                model = "11 Pro 5G",
                androidVersion = "Android 14",
                batteryLevel = 87,
                isCharging = true,
                networkType = "5G",
                isOnline = true,
                lastLatitude = 12.9352,
                lastLongitude = 77.6245,
                lastLocationAddress = "Koramangala Tech Park, Bengaluru",
                lastSeenTimestamp = now - 35_000L,
                isHighPriority = true,
                isLocked = false
            ),
            NexusDevice(
                deviceId = "tablet_ops_02",
                deviceName = "Warehouse Tablet B2",
                manufacturer = "Samsung",
                model = "Galaxy Tab Active5",
                androidVersion = "Android 14",
                batteryLevel = 64,
                isCharging = false,
                networkType = "Wi-Fi",
                isOnline = true,
                lastLatitude = 12.9698,
                lastLongitude = 77.7500,
                lastLocationAddress = "Whitefield Logistics Hub, Sector 4",
                lastSeenTimestamp = now - 110_000L,
                isHighPriority = true,
                isLocked = false
            ),
            NexusDevice(
                deviceId = "dispatch_unit_03",
                deviceName = "Dispatch Pixel C3",
                manufacturer = "Google",
                model = "Pixel 8 Pro",
                androidVersion = "Android 15",
                batteryLevel = 23,
                isCharging = false,
                networkType = "Offline",
                isOnline = false,
                lastLatitude = 12.9756,
                lastLongitude = 77.6066,
                lastLocationAddress = "MG Road Depot • 14m ago",
                lastSeenTimestamp = now - 840_000L,
                isHighPriority = true,
                isLocked = true
            ),
            NexusDevice(
                deviceId = "kiosk_lobby_04",
                deviceName = "Lobby Kiosk K4",
                manufacturer = "Lenovo",
                model = "Tab P12 Enterprise",
                androidVersion = "Android 13",
                batteryLevel = 98,
                isCharging = true,
                networkType = "Wi-Fi",
                isOnline = true,
                lastLatitude = 12.9716,
                lastLongitude = 77.5946,
                lastLocationAddress = "HQ Main Reception, Floor 1",
                lastSeenTimestamp = now - 25_000L,
                isHighPriority = false,
                isLocked = false
            ),
            NexusDevice(
                deviceId = "courier_handheld_05",
                deviceName = "Courier Scanner D5",
                manufacturer = "Zebra",
                model = "TC58 Rugged",
                androidVersion = "Android 13",
                batteryLevel = 49,
                isCharging = false,
                networkType = "4G LTE",
                isOnline = true,
                lastLatitude = 12.9141,
                lastLongitude = 77.6412,
                lastLocationAddress = "HSR Layout Route 7, Bengaluru",
                lastSeenTimestamp = now - 210_000L,
                isHighPriority = false,
                isLocked = false
            ),
            NexusDevice(
                deviceId = "field_backup_06",
                deviceName = "Backup Unit E6",
                manufacturer = "Motorola",
                model = "ThinkPhone",
                androidVersion = "Android 14",
                batteryLevel = 12,
                isCharging = false,
                networkType = "Offline",
                isOnline = false,
                lastLatitude = 13.0358,
                lastLongitude = 77.5970,
                lastLocationAddress = "Hebbal Service Center • 2h ago",
                lastSeenTimestamp = now - 7_200_000L,
                isHighPriority = false,
                isLocked = false
            )
        )
    }
}
