package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.NexusDevice
import com.example.ui.components.EnterpriseHeader
import com.example.ui.components.PriorityWidgetPreviewCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonStop
import com.example.ui.theme.EmeraldOnline
import com.example.ui.viewmodel.ManagedDevicesViewModel
import java.util.Locale

@Composable
fun ManagedDevicesScreen(
    viewModel: ManagedDevicesViewModel,
    onBack: () -> Unit,
    onNavigateToPairing: () -> Unit
) {
    val devices by viewModel.managedDevices.collectAsStateWithLifecycle()
    val priorityDevices by viewModel.highPriorityDevices.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedDeviceIds.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterMode by viewModel.filterMode.collectAsStateWithLifecycle()
    val isExecutingBulk by viewModel.isExecutingBulkAction.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    var pendingBulkConfirmAction by remember { mutableStateOf<String?>(null) } // "LOCK" or "REBOOT"

    val filteredDevices = remember(devices, searchQuery, filterMode) {
        val q = searchQuery.trim().lowercase(Locale.getDefault())
        devices.filter { device ->
            val matchesFilter = when (filterMode) {
                "PRIORITY" -> device.isHighPriority
                "ONLINE" -> device.isOnline
                "OFFLINE" -> !device.isOnline
                else -> true
            }
            val matchesQuery = q.isEmpty() ||
                device.deviceName.lowercase(Locale.getDefault()).contains(q) ||
                device.model.lowercase(Locale.getDefault()).contains(q) ||
                device.manufacturer.lowercase(Locale.getDefault()).contains(q) ||
                (device.lastLocationAddress ?: "").lowercase(Locale.getDefault()).contains(q)
            matchesFilter && matchesQuery
        }
    }

    val onlineCount = remember(devices) { devices.count { it.isOnline } }
    val selectedDevices = remember(devices, selectedIds) {
        devices.filter { it.deviceId in selectedIds }
    }

    // Confirmation dialog for destructive / high-impact bulk actions (Lock / Reboot)
    if (pendingBulkConfirmAction != null && selectedDevices.isNotEmpty()) {
        val isLock = pendingBulkConfirmAction == "LOCK"
        AlertDialog(
            onDismissRequest = { pendingBulkConfirmAction = null },
            icon = {
                Icon(
                    imageVector = if (isLock) Icons.Default.Lock else Icons.Default.RestartAlt,
                    contentDescription = null,
                    tint = if (isLock) AmberWarning else CrimsonStop
                )
            },
            title = {
                Text(
                    text = if (isLock) {
                        "Lock ${selectedDevices.size} Managed Device(s)?"
                    } else {
                        "Reboot ${selectedDevices.size} Managed Device(s)?"
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isLock) {
                            "This will simultaneously lock the screen on the following selected devices and log an audit entry:"
                        } else {
                            "This will simultaneously send a remote reboot command to the following selected devices and log an audit entry:"
                        },
                        fontSize = 13.sp
                    )
                    selectedDevices.forEach { dev ->
                        Text(
                            text = "• ${dev.deviceName} (${dev.model})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isLock) viewModel.executeBulkLock() else viewModel.executeBulkReboot()
                        pendingBulkConfirmAction = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLock) AmberWarning else CrimsonStop,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_bulk_action_button")
                ) {
                    Text(if (isLock) "Confirm Lock" else "Confirm Reboot")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingBulkConfirmAction = null },
                    modifier = Modifier.testTag("cancel_bulk_action_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .testTag("managed_devices_screen")
        ) {
            EnterpriseHeader(
                title = "MANAGED DEVICES",
                subtitle = "$onlineCount/${devices.size} Online • Multi-select for bulk actions",
                onBackClick = onBack,
                badgeText = "${priorityDevices.size} WIDGET",
                badgeColor = MaterialTheme.colorScheme.primary
            )

            // Contextual Bulk Action Toolbar (animated spring reveal when 1+ devices selected)
            val hasSelectedLockedDevices = selectedDevices.any { it.isLocked }
            AnimatedVisibility(
                visible = selectedIds.isNotEmpty(),
                enter = expandVertically(
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                BulkActionToolbar(
                    selectedCount = selectedIds.size,
                    allVisibleSelected = filteredDevices.isNotEmpty() &&
                        filteredDevices.all { it.deviceId in selectedIds },
                    hasLockedSelected = hasSelectedLockedDevices,
                    isExecuting = isExecutingBulk,
                    onSelectAll = {
                        if (filteredDevices.all { it.deviceId in selectedIds }) {
                            viewModel.clearSelection()
                        } else {
                            viewModel.selectAll(filteredDevices.map { it.deviceId })
                        }
                    },
                    onClearSelection = viewModel::clearSelection,
                    onBulkLock = { pendingBulkConfirmAction = "LOCK" },
                    onBulkUnlock = viewModel::executeBulkUnlock,
                    onBulkReboot = { pendingBulkConfirmAction = "REBOOT" },
                    onBulkSync = viewModel::executeBulkSync,
                    onBulkPinWidget = viewModel::executeBulkPinToWidget
                )
            }

            // Helpful prompt when zero devices are selected so bulk toolbar discovery is instant
            AnimatedVisibility(visible = selectedIds.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("bulk_toolbar_hint_bar"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select devices to bulk Lock, Reboot, or Sync simultaneously",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { viewModel.selectAll(filteredDevices.map { it.deviceId }) },
                        modifier = Modifier.testTag("bulk_hint_select_all_button")
                    ) {
                        Text("Select All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Live status feedback banner
            AnimatedVisibility(visible = statusBanner != null) {
                statusBanner?.let { message ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(EmeraldOnline.copy(alpha = 0.12f))
                            .border(1.dp, EmeraldOnline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("fleet_status_banner"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Status update",
                            tint = EmeraldOnline,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = message,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = viewModel::dismissStatusBanner,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss message",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Live Home Screen Widget Preview Card
                item {
                    PriorityWidgetPreviewCard(
                        priorityDevices = priorityDevices,
                        onPinWidgetToHomeScreen = viewModel::requestPinHomeScreenWidget
                    )
                }

                // Search input
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = viewModel::updateSearchQuery,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("managed_devices_search_input"),
                        placeholder = {
                            Text(
                                text = "Search device, model, or location...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search devices",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search"
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }

                // Filter chips + Quick Select All button
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("ALL", "PRIORITY", "ONLINE", "OFFLINE").forEach { mode ->
                                FilterChip(
                                    selected = filterMode == mode,
                                    onClick = { viewModel.updateFilterMode(mode) },
                                    label = {
                                        Text(
                                            text = mode,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    },
                                    modifier = Modifier.testTag("fleet_filter_${mode.lowercase(Locale.getDefault())}"),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                        selectedLabelColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        TextButton(
                            onClick = {
                                if (selectedIds.size == filteredDevices.size && filteredDevices.isNotEmpty()) {
                                    viewModel.clearSelection()
                                } else {
                                    viewModel.selectAll(filteredDevices.map { it.deviceId })
                                }
                            },
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("fleet_select_all_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (selectedIds.size == filteredDevices.size && filteredDevices.isNotEmpty()) "None" else "All",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Managed Device Cards
                items(filteredDevices, key = { it.deviceId }) { device ->
                    ManagedDeviceSelectableCard(
                        device = device,
                        isSelected = device.deviceId in selectedIds,
                        onToggleSelect = { viewModel.toggleDeviceSelection(device.deviceId) },
                        onTogglePriority = { viewModel.toggleHighPriority(device.deviceId) }
                    )
                }

                // Pair Another Device CTA
                item {
                    OutlinedButton(
                        onClick = onNavigateToPairing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("pair_new_device_cta"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pair Another Managed Device",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BulkActionToolbar(
    selectedCount: Int,
    allVisibleSelected: Boolean,
    hasLockedSelected: Boolean,
    isExecuting: Boolean,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onBulkLock: () -> Unit,
    onBulkUnlock: () -> Unit,
    onBulkReboot: () -> Unit,
    onBulkSync: () -> Unit,
    onBulkPinWidget: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("bulk_action_toolbar")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = selectedCount.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Selected • Bulk Command Toolbar",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onSelectAll,
                    modifier = Modifier.defaultMinSize(minHeight = 40.dp)
                ) {
                    Text(
                        text = if (allVisibleSelected) "Deselect All" else "Select All",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(
                    onClick = onClearSelection,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("bulk_toolbar_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear selection",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onBulkLock,
                enabled = !isExecuting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmberWarning,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("bulk_action_lock_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Lock ($selectedCount)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            if (hasLockedSelected) {
                OutlinedButton(
                    onClick = onBulkUnlock,
                    enabled = !isExecuting,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("bulk_action_unlock_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Unlock", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Button(
                onClick = onBulkReboot,
                enabled = !isExecuting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CrimsonStop,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("bulk_action_reboot_button")
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reboot ($selectedCount)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onBulkSync,
                enabled = !isExecuting,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("bulk_action_sync_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sync", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = onBulkPinWidget,
                enabled = !isExecuting,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("bulk_action_pin_widget_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Pin Widget", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ManagedDeviceSelectableCard(
    device: NexusDevice,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onTogglePriority: () -> Unit
) {
    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        device.isHighPriority -> AmberWarning.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    val containerBg = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val batteryColor = when {
        device.batteryLevel <= 20 -> CrimsonStop
        device.batteryLevel <= 45 -> AmberWarning
        else -> EmeraldOnline
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(containerBg)
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onToggleSelect() }
            .padding(12.dp)
            .testTag("managed_device_card_${device.deviceId}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() },
                modifier = Modifier.testTag("select_device_checkbox_${device.deviceId}"),
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.width(4.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = device.deviceName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${device.manufacturer} ${device.model} • ${device.androidVersion}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onTogglePriority,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("toggle_priority_star_${device.deviceId}")
                    ) {
                        Icon(
                            imageVector = if (device.isHighPriority) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (device.isHighPriority) {
                                "Remove from High-Priority Widget"
                            } else {
                                "Pin to High-Priority Widget"
                            },
                            tint = if (device.isHighPriority) AmberWarning else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Telemetry Badges Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBadge(
                        text = if (device.isOnline) "Online • ${device.networkType}" else "Offline",
                        color = if (device.isOnline) EmeraldOnline else CrimsonStop
                    )

                    if (device.isLocked) {
                        StatusBadge(text = "LOCKED", color = AmberWarning)
                    }

                    if (device.reboot_command) {
                        StatusBadge(text = "REBOOTING", color = CrimsonStop)
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (device.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                            contentDescription = "Battery",
                            tint = batteryColor,
                            modifier = Modifier.size(16.dp)
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

                Spacer(modifier = Modifier.height(8.dp))

                // Last Known Location Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Last known location",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = device.lastLocationAddress ?: "Location unavailable",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
