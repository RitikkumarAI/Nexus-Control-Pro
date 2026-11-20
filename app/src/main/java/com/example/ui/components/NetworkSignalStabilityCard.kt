package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DeviceSignalStabilityProfile
import com.example.data.IntermittentZoneAlert
import com.example.data.NetworkSignalStabilityManager
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonStop
import com.example.ui.theme.EmeraldOnline
import java.util.Locale

/**
 * Interactive 24-hour Network Signal Stability chart mapping average signal strength (dBm)
 * fluctuations and min-max jitter per managed device to pinpoint intermittent connectivity zones.
 */
@Composable
fun NetworkSignalStabilityCard(
    profiles: List<DeviceSignalStabilityProfile>,
    onRefreshProfiles: () -> Unit,
    onOpenRemoteLogcat: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (profiles.isEmpty()) return

    var selectedDeviceIndex by remember(profiles.size) { mutableIntStateOf(0) }
    val activeProfile = profiles.getOrElse(selectedDeviceIndex) { profiles.first() }
    val points = activeProfile.hourlyPoints

    // Default inspection point: worst dropout hour if any, else latest bucket
    var selectedPointIndex by remember(activeProfile.deviceId) {
        val dropIdx = points.indexOfFirst { it.isIntermittentDrop }
        mutableIntStateOf(if (dropIdx >= 0) dropIdx else (points.lastIndex).coerceAtLeast(0))
    }
    val selectedPoint = points.getOrElse(selectedPointIndex) { points.first() }

    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val stabilityColor = when {
        activeProfile.stabilityScorePct >= 88 -> EmeraldOnline
        activeProfile.stabilityScorePct >= 72 -> AmberWarning
        else -> CrimsonStop
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, outlineVariant, RoundedCornerShape(18.dp))
            .padding(16.dp)
            .testTag("network_signal_stability_card")
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
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(primaryColor.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SignalCellularAlt,
                            contentDescription = "Network Signal Stability",
                            tint = primaryColor,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "24h Network Signal Stability",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${activeProfile.networkType} • Fluctuation & dead-zone map",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onRefreshProfiles,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("refresh_signal_stability_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh signal stability",
                        tint = primaryColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Device Switcher Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                profiles.forEachIndexed { index, profile ->
                    FilterChip(
                        selected = index == selectedDeviceIndex,
                        onClick = { selectedDeviceIndex = index },
                        label = {
                            Text(
                                text = profile.deviceName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        modifier = Modifier.testTag("signal_device_chip_${profile.deviceId}"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = primaryColor.copy(alpha = 0.16f),
                            selectedLabelColor = primaryColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // KPI Summary Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SignalKpiTile(
                    label = "24H AVG",
                    value = "${activeProfile.average24hRssiDbm} dBm",
                    accent = primaryColor,
                    modifier = Modifier.weight(1f)
                )
                SignalKpiTile(
                    label = "STABILITY",
                    value = "${activeProfile.stabilityScorePct}%",
                    accent = stabilityColor,
                    modifier = Modifier.weight(1f)
                )
                SignalKpiTile(
                    label = "DROP ZONES",
                    value = "${activeProfile.dropoutCount}",
                    accent = if (activeProfile.dropoutCount > 0) CrimsonStop else EmeraldOnline,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive 24-Hour Signal Strength Area + Range Chart
            val minDbm = -96f
            val maxDbm = -40f
            val thresholdDbm = NetworkSignalStabilityManager.INSTABILITY_THRESHOLD_DBM.toFloat()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
                    .padding(start = 10.dp, end = 12.dp, top = 12.dp, bottom = 8.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "-40 dBm (Strong)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldOnline
                        )
                        Text(
                            text = "Tap graph to inspect hour",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(146.dp)
                            .testTag("signal_stability_chart_canvas")
                            .pointerInput(points) {
                                detectTapGestures { offset ->
                                    if (points.isNotEmpty() && size.width > 0) {
                                        val stepX = size.width.toFloat() / (points.size - 1).coerceAtLeast(1)
                                        val tappedIdx = ((offset.x + stepX / 2f) / stepX)
                                            .toInt()
                                            .coerceIn(0, points.lastIndex)
                                        selectedPointIndex = tappedIdx
                                    }
                                }
                            }
                    ) {
                        val w = size.width
                        val h = size.height
                        if (points.size < 2 || w <= 0f || h <= 0f) return@Canvas

                        fun dbmToY(dbm: Float): Float {
                            val clamped = dbm.coerceIn(minDbm, maxDbm)
                            val ratio = (clamped - minDbm) / (maxDbm - minDbm)
                            return h - (ratio * h)
                        }

                        val stepX = w / (points.size - 1).toFloat()

                        // Horizontal reference grid lines (-50, -65, -78 threshold, -90)
                        listOf(-50f, -65f, -90f).forEach { refDbm ->
                            val y = dbmToY(refDbm)
                            drawLine(
                                color = outlineVariant.copy(alpha = 0.55f),
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 1f
                            )
                        }

                        // Shaded Intermittent Connectivity Drop Zone below -78 dBm
                        val thresholdY = dbmToY(thresholdDbm)
                        drawRect(
                            color = CrimsonStop.copy(alpha = 0.11f),
                            topLeft = Offset(0f, thresholdY),
                            size = Size(w, (h - thresholdY).coerceAtLeast(0f))
                        )
                        drawLine(
                            color = CrimsonStop.copy(alpha = 0.7f),
                            start = Offset(0f, thresholdY),
                            end = Offset(w, thresholdY),
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                        )

                        // Min-Max Fluctuation Ribbon
                        val rangePath = Path().apply {
                            points.forEachIndexed { idx, pt ->
                                val x = idx * stepX
                                val y = dbmToY(pt.maxRssiDbm.toFloat())
                                if (idx == 0) moveTo(x, y) else lineTo(x, y)
                            }
                            for (idx in points.lastIndex downTo 0) {
                                val x = idx * stepX
                                val y = dbmToY(points[idx].minRssiDbm.toFloat())
                                lineTo(x, y)
                            }
                            close()
                        }
                        drawPath(
                            path = rangePath,
                            color = primaryColor.copy(alpha = 0.16f)
                        )

                        // Smooth Average RSSI Area Fill
                        val areaPath = Path().apply {
                            moveTo(0f, h)
                            points.forEachIndexed { idx, pt ->
                                lineTo(idx * stepX, dbmToY(pt.avgRssiDbm.toFloat()))
                            }
                            lineTo(w, h)
                            close()
                        }
                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.28f),
                                    primaryColor.copy(alpha = 0.02f)
                                )
                            )
                        )

                        // Average RSSI Line
                        val linePath = Path().apply {
                            points.forEachIndexed { idx, pt ->
                                val x = idx * stepX
                                val y = dbmToY(pt.avgRssiDbm.toFloat())
                                if (idx == 0) moveTo(x, y) else lineTo(x, y)
                            }
                        }
                        drawPath(
                            path = linePath,
                            color = primaryColor,
                            style = Stroke(width = 5f, cap = StrokeCap.Round)
                        )

                        // Selected Crosshair Line
                        val selX = selectedPointIndex * stepX
                        drawLine(
                            color = primaryColor.copy(alpha = 0.45f),
                            start = Offset(selX, 0f),
                            end = Offset(selX, h),
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )

                        // Data Point Dots
                        points.forEachIndexed { idx, pt ->
                            val x = idx * stepX
                            val y = dbmToY(pt.avgRssiDbm.toFloat())
                            val isSelected = idx == selectedPointIndex
                            val dotColor = if (pt.isIntermittentDrop) CrimsonStop else primaryColor
                            if (isSelected) {
                                drawCircle(
                                    color = dotColor.copy(alpha = 0.28f),
                                    radius = 14f,
                                    center = Offset(x, y)
                                )
                            }
                            drawCircle(
                                color = dotColor,
                                radius = if (isSelected) 7.5f else if (pt.isIntermittentDrop) 5.5f else 4f,
                                center = Offset(x, y)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // X-axis 24h Labels (every 2nd label for clean spacing)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("00h", "04h", "08h", "12h", "16h", "20h", "22h").forEach { lbl ->
                            Text(
                                text = lbl,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "--- -78 dBm Intermittent Instability Threshold",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CrimsonStop
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Selected Time Bucket Tooltip Readout
            val pointAccent = if (selectedPoint.isIntermittentDrop) CrimsonStop else EmeraldOnline
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(pointAccent.copy(alpha = 0.10f))
                    .border(1.dp, pointAccent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(10.dp)
                    .testTag("signal_selected_point_tooltip"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(pointAccent)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${selectedPoint.hourLabel} • Avg ${selectedPoint.avgRssiDbm} dBm (Range ${selectedPoint.minRssiDbm}..${selectedPoint.maxRssiDbm} dBm)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Zone: ${selectedPoint.zoneLabel} • Packet Loss: ${String.format(Locale.getDefault(), "%.1f", selectedPoint.packetLossPct)}%",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Pinpointed Intermittent Connectivity Zones
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "INTERMITTENT CONNECTIVITY ZONES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (activeProfile.intermittentZones.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(EmeraldOnline.copy(alpha = 0.10f))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SignalCellularAlt,
                        contentDescription = null,
                        tint = EmeraldOnline,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "No intermittent connectivity drop zones detected in the last 24h.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    activeProfile.intermittentZones.take(3).forEach { zone ->
                        IntermittentZoneRow(zone = zone)
                    }
                }
            }

            if (onOpenRemoteLogcat != null) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = outlineVariant.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onOpenRemoteLogcat,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("open_remote_logcat_from_signal_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open Remote Logcat (WebRTC Stream)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SignalKpiTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.09f))
            .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun IntermittentZoneRow(zone: IntermittentZoneAlert) {
    val badgeColor = if (zone.severity == "SEVERE") CrimsonStop else AmberWarning
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (zone.severity == "SEVERE") Icons.Default.WarningAmber else Icons.Default.LocationOn,
                contentDescription = zone.severity,
                tint = badgeColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = zone.zoneName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${zone.timeWindow} • Avg ${zone.avgRssiDbm} dBm • Loss ${String.format(Locale.getDefault(), "%.1f", zone.packetLossPct)}%",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        StatusBadge(text = zone.severity, color = badgeColor)
    }
}
