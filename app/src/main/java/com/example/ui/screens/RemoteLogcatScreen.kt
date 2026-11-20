package com.example.ui.screens

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.LogcatEntry
import com.example.data.PeerConnectionState
import com.example.ui.components.EnterpriseHeader
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonStop
import com.example.ui.theme.EmeraldOnline
import com.example.ui.viewmodel.RemoteLogcatViewModel
import java.util.Locale

@Composable
fun RemoteLogcatScreen(
    viewModel: RemoteLogcatViewModel,
    onBack: () -> Unit
) {
    val selectedDevice by viewModel.selectedDevice.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val dataChannelState by viewModel.dataChannelState.collectAsStateWithLifecycle()
    val latencyMs by viewModel.latencyMs.collectAsStateWithLifecycle()
    val isPaused by viewModel.isPaused.collectAsStateWithLifecycle()
    val autoScroll by viewModel.autoScrollEnabled.collectAsStateWithLifecycle()
    val levelFilter by viewModel.levelFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val allLogs by viewModel.logs.collectAsStateWithLifecycle()

    var expandedLogId by remember { mutableStateOf<Long?>(null) }
    val listState = rememberLazyListState()

    val filteredLogs = remember(allLogs, levelFilter, searchQuery) {
        val q = searchQuery.trim().lowercase(Locale.getDefault())
        allLogs.filter { entry ->
            val matchesLevel = when (levelFilter) {
                "E" -> entry.level == "E" || entry.level == "F" || entry.level == "A"
                "W" -> entry.level == "W"
                "I" -> entry.level == "I"
                "D" -> entry.level == "D" || entry.level == "V"
                else -> true
            }
            val matchesQuery = q.isEmpty() ||
                entry.tag.lowercase(Locale.getDefault()).contains(q) ||
                entry.message.lowercase(Locale.getDefault()).contains(q) ||
                entry.pid.contains(q)
            matchesLevel && matchesQuery
        }
    }

    val errorCount = remember(allLogs) { allLogs.count { it.level == "E" || it.level == "F" } }
    val warnCount = remember(allLogs) { allLogs.count { it.level == "W" } }

    LaunchedEffect(filteredLogs.size, autoScroll) {
        if (autoScroll && filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.lastIndex)
        }
    }

    val isConnected = connectionState == PeerConnectionState.CONNECTED
    val badgeColor = when (dataChannelState) {
        "OPEN" -> EmeraldOnline
        "PAUSED", "CONNECTING" -> AmberWarning
        else -> CrimsonStop
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
                .testTag("remote_logcat_screen")
        ) {
            EnterpriseHeader(
                title = "REMOTE LOGCAT",
                subtitle = "Live WebRTC DataChannel runtime diagnostics",
                onBackClick = onBack,
                badgeText = "WEBRTC $dataChannelState",
                badgeColor = badgeColor
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Target Device Selector Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    viewModel.availableDevices.forEach { device ->
                        FilterChip(
                            selected = device.deviceId == selectedDevice.deviceId,
                            onClick = { viewModel.selectDevice(device) },
                            label = {
                                Text(
                                    text = device.deviceName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            modifier = Modifier.testTag("logcat_device_chip_${device.deviceId}"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                // WebRTC DataChannel Telemetry & Action Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(badgeColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "dc:nexus-logcat-v1 • ${if (isConnected) "${latencyMs}ms ICE" else "Disconnected"}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "E:$errorCount",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CrimsonStop
                            )
                            Text(
                                text = "W:$warnCount",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberWarning
                            )
                            Text(
                                text = "${filteredLogs.size} lines",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (isConnected) viewModel.disconnectStream() else viewModel.connectStream()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isConnected) CrimsonStop else EmeraldOnline,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("logcat_connect_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isConnected) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isConnected) "Stop" else "Connect",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = viewModel::togglePause,
                            enabled = isConnected,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("logcat_pause_button")
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isPaused) "Resume" else "Pause", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = viewModel::emitDiagnosticProbe,
                            enabled = isConnected,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("logcat_probe_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Probe", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = viewModel::clearLogs,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("logcat_clear_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Clear log buffer",
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        OutlinedButton(
                            onClick = { viewModel.shareLogSnapshot(filteredLogs) },
                            enabled = filteredLogs.isNotEmpty(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("logcat_share_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Export logcat snapshot",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Search & Level Filters
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("logcat_search_input"),
                    placeholder = {
                        Text(
                            text = "Filter tag, Exception, Service, PID...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search logs",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "ALL" to "All",
                            "E" to "Errors",
                            "W" to "Warn",
                            "I" to "Info",
                            "D" to "Debug"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = levelFilter == key,
                                onClick = { viewModel.setLevelFilter(key) },
                                label = {
                                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                },
                                modifier = Modifier.testTag("logcat_level_${key.lowercase(Locale.getDefault())}")
                            )
                        }
                    }

                    IconButton(
                        onClick = viewModel::toggleAutoScroll,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("logcat_autoscroll_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerticalAlignBottom,
                            contentDescription = if (autoScroll) "Disable auto-scroll" else "Enable auto-scroll",
                            tint = if (autoScroll) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // High-Contrast Monospace Logcat Terminal
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF070B12))
                        .border(1.dp, Color(0xFF1E2D4A), RoundedCornerShape(14.dp))
                        .testTag("logcat_console_output")
                ) {
                    if (filteredLogs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isConnected) {
                                    "Listening on WebRTC DataChannel... Tap 'Probe' to capture diagnostic events."
                                } else {
                                    "WebRTC Logcat stream disconnected. Tap 'Connect' to start streaming."
                                },
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF8C9BAE)
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredLogs, key = { it.id }) { entry ->
                                LogcatConsoleLine(
                                    entry = entry,
                                    isExpanded = expandedLogId == entry.id,
                                    onClick = {
                                        expandedLogId = if (expandedLogId == entry.id) null else entry.id
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogcatConsoleLine(
    entry: LogcatEntry,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val levelColor = when (entry.level) {
        "E", "F", "A" -> Color(0xFFEF4444)
        "W" -> Color(0xFFF59E0B)
        "I" -> Color(0xFF22C55E)
        "D" -> Color(0xFF38BDF8)
        else -> Color(0xFF94A3B8)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (entry.level == "E" || entry.level == "F") {
                    Color(0xFF2A1218)
                } else {
                    Color(0xFF0E1522)
                }
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(levelColor.copy(alpha = 0.22f))
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(
                    text = entry.level,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = levelColor
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = entry.tag,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF38BDF8),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = entry.timestamp.substringAfter(" "),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF64748B)
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = entry.message,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFFE2E8F0),
            maxLines = if (isExpanded) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 15.sp
        )

        if (isExpanded) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "PID ${entry.pid} • Device: ${entry.deviceId} • Full Time: ${entry.timestamp}",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF94A3B8)
            )
        }
    }
}
