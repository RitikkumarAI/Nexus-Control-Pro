package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.PerformanceMetricEntity
import com.example.ui.theme.EmeraldOnline
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SystemPerformanceCard(
    metrics: List<PerformanceMetricEntity>,
    onRecordSample: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Both, 1 = CPU, 2 = RAM

    val displayMetrics = remember(metrics) {
        if (metrics.isEmpty()) {
            listOf(
                PerformanceMetricEntity(cpuUsagePercent = 28f, ramUsagePercent = 48f, ramUsedGb = 3.8, ramTotalGb = 8.0),
                PerformanceMetricEntity(cpuUsagePercent = 42f, ramUsagePercent = 52f, ramUsedGb = 4.1, ramTotalGb = 8.0),
                PerformanceMetricEntity(cpuUsagePercent = 88f, ramUsagePercent = 75f, ramUsedGb = 6.0, ramTotalGb = 8.0, isSpike = true),
                PerformanceMetricEntity(cpuUsagePercent = 45f, ramUsagePercent = 54f, ramUsedGb = 4.3, ramTotalGb = 8.0),
                PerformanceMetricEntity(cpuUsagePercent = 92f, ramUsagePercent = 82f, ramUsedGb = 6.5, ramTotalGb = 8.0, isSpike = true),
                PerformanceMetricEntity(cpuUsagePercent = 38f, ramUsagePercent = 50f, ramUsedGb = 4.0, ramTotalGb = 8.0)
            )
        } else {
            metrics.reversed()
        }
    }

    val latestMetric = displayMetrics.lastOrNull()
    val currentCpu = latestMetric?.cpuUsagePercent ?: 38f
    val currentRam = latestMetric?.ramUsagePercent ?: 50f

    val peakCpu = displayMetrics.maxOfOrNull { it.cpuUsagePercent } ?: currentCpu
    val peakRam = displayMetrics.maxOfOrNull { it.ramUsagePercent } ?: currentRam
    val spikeCount = displayMetrics.count { it.isSpike }

    val cpuColor = MaterialTheme.colorScheme.primary
    val ramColor = EmeraldOnline
    val spikeColor = Color(0xFFEF4444)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            .padding(16.dp)
            .testTag("system_performance_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "System Performance",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "SYSTEM PERFORMANCE",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Room DB Historical Spikes & Telemetry",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (spikeCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(spikeColor.copy(alpha = 0.15f))
                            .border(1.dp, spikeColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = "Spikes",
                                tint = spikeColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$spikeCount Spikes",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = spikeColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Current Stats Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(cpuColor))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "CPU Load", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = "${currentCpu.toInt()}%",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(text = "Peak: ${peakCpu.toInt()}%", fontSize = 10.sp, color = cpuColor)
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ramColor))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "RAM Usage", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = "${currentRam.toInt()}%",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${String.format("%.1f", latestMetric?.ramUsedGb ?: 4.0)} / 8.0 GB",
                        fontSize = 10.sp,
                        color = ramColor
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.TrendingUp, contentDescription = null, tint = cpuColor, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Sampling", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = "Real-time",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(text = "Room Synced", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Chart Series Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SeriesChip(label = "Combined", active = selectedTab == 0, onClick = { selectedTab = 0 })
                SeriesChip(label = "CPU Spikes", active = selectedTab == 1, onClick = { selectedTab = 1 })
                SeriesChip(label = "Memory RAM", active = selectedTab == 2, onClick = { selectedTab = 2 })
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Performance Multi-Line Trend Chart
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .testTag("performance_trend_canvas")
            ) {
                val width = size.width
                val height = size.height
                val count = displayMetrics.size
                if (count < 2) return@Canvas

                val stepX = width / (count - 1)

                // Draw Grid Guidelines
                val gridLines = 4
                for (i in 0..gridLines) {
                    val y = height * (i / gridLines.toFloat())
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.15f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Draw CPU Trend Line
                if (selectedTab == 0 || selectedTab == 1) {
                    val cpuPoints = displayMetrics.mapIndexed { i, m ->
                        Offset(i * stepX, height - (height * (m.cpuUsagePercent / 100f)))
                    }

                    val cpuPath = Path().apply {
                        moveTo(cpuPoints[0].x, cpuPoints[0].y)
                        for (i in 0 until cpuPoints.size - 1) {
                            val p1 = cpuPoints[i]
                            val p2 = cpuPoints[i + 1]
                            val cx = (p1.x + p2.x) / 2f
                            cubicTo(cx, p1.y, cx, p2.y, p2.x, p2.y)
                        }
                    }

                    val cpuArea = Path().apply {
                        addPath(cpuPath)
                        lineTo(width, height)
                        lineTo(0f, height)
                        close()
                    }

                    drawPath(
                        path = cpuArea,
                        brush = Brush.verticalGradient(
                            colors = listOf(cpuColor.copy(alpha = 0.25f), Color.Transparent)
                        )
                    )

                    drawPath(
                        path = cpuPath,
                        color = cpuColor,
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Data Point Circles & Spike Rings
                    cpuPoints.forEachIndexed { i, pt ->
                        val isSpike = displayMetrics[i].cpuUsagePercent > 80f
                        val pointColor = if (isSpike) spikeColor else cpuColor
                        if (isSpike) {
                            drawCircle(color = spikeColor.copy(alpha = 0.35f), radius = 8.dp.toPx(), center = pt)
                        }
                        drawCircle(color = pointColor, radius = 4.dp.toPx(), center = pt)
                    }
                }

                // Draw RAM Trend Line
                if (selectedTab == 0 || selectedTab == 2) {
                    val ramPoints = displayMetrics.mapIndexed { i, m ->
                        Offset(i * stepX, height - (height * (m.ramUsagePercent / 100f)))
                    }

                    val ramPath = Path().apply {
                        moveTo(ramPoints[0].x, ramPoints[0].y)
                        for (i in 0 until ramPoints.size - 1) {
                            val p1 = ramPoints[i]
                            val p2 = ramPoints[i + 1]
                            val cx = (p1.x + p2.x) / 2f
                            cubicTo(cx, p1.y, cx, p2.y, p2.x, p2.y)
                        }
                    }

                    drawPath(
                        path = ramPath,
                        color = ramColor,
                        style = Stroke(width = 2.5.dp.toPx())
                    )

                    ramPoints.forEachIndexed { i, pt ->
                        val isSpike = displayMetrics[i].ramUsagePercent > 80f
                        val pointColor = if (isSpike) spikeColor else ramColor
                        drawCircle(color = pointColor, radius = 3.dp.toPx(), center = pt)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // X-Axis Timeline
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val firstTime = timeFormat.format(Date(displayMetrics.first().timestamp))
                val midTime = timeFormat.format(Date(displayMetrics[displayMetrics.size / 2].timestamp))
                val lastTime = timeFormat.format(Date(displayMetrics.last().timestamp))

                Text(text = firstTime, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = midTime, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "$lastTime (Now)", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun SeriesChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                1.dp,
                if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
