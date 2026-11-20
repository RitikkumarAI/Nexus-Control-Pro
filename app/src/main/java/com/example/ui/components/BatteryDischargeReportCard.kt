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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingDown
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BatteryDegradationPoint
import com.example.model.BatteryDischargeReport
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonStop
import com.example.ui.theme.EmeraldOnline
import java.util.Locale

@Composable
fun BatteryDischargeReportCard(
    report: BatteryDischargeReport,
    modifier: Modifier = Modifier
) {
    // 0 = Capacity vs Initial Calibration (mAh), 1 = Discharge Rate (%/hr)
    var chartMode by remember { mutableIntStateOf(0) }
    val points = remember(report.timelinePoints) {
        report.timelinePoints.ifEmpty {
            listOf(
                BatteryDegradationPoint("Cal. 0", 5000, 5000, 2.7f, 0),
                BatteryDegradationPoint("M1", 5000, 4960, 2.9f, 22),
                BatteryDegradationPoint("M2", 5000, 4915, 3.1f, 46),
                BatteryDegradationPoint("M3", 5000, 4870, 3.2f, 71),
                BatteryDegradationPoint("M4", 5000, 4820, 3.5f, 98),
                BatteryDegradationPoint("M5", 5000, 4780, 3.6f, 124),
                BatteryDegradationPoint("Now", 5000, 4740, 3.8f, 148)
            )
        }
    }

    var selectedIndex by remember(points.size) { mutableIntStateOf(points.lastIndex.coerceAtLeast(0)) }
    val selectedPoint = points.getOrElse(selectedIndex) { points.last() }

    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant

    val retentionColor = when {
        report.capacityRetainedPct >= 92f -> EmeraldOnline
        report.capacityRetainedPct >= 84f -> AmberWarning
        else -> CrimsonStop
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, outlineVariant, RoundedCornerShape(18.dp))
            .padding(16.dp)
            .testTag("battery_discharge_report_card")
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
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(retentionColor.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = "Battery Discharge & Degradation Report",
                            tint = retentionColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "BATTERY DISCHARGE REPORT",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = onSurface
                        )
                        Text(
                            text = "Current Capacity vs. Initial Calibration",
                            fontSize = 11.sp,
                            color = onSurfaceVariant
                        )
                    }
                }

                StatusBadge(
                    text = "${String.format(Locale.US, "%.1f", report.capacityRetainedPct)}% HEALTH",
                    color = retentionColor
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Comparative KPI Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, outlineVariant, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ReportMetricColumn(
                    label = "INITIAL CAL.",
                    value = "${report.initialCalibratedMah} mAh",
                    accentColor = primaryColor
                )
                ReportMetricColumn(
                    label = "CURRENT CAP.",
                    value = "${report.currentCapacityMah} mAh",
                    accentColor = retentionColor
                )
                ReportMetricColumn(
                    label = "DEGRADATION",
                    value = "-${report.degradationDeltaMah} mAh",
                    accentColor = if (report.degradationDeltaMah > 450) CrimsonStop else AmberWarning
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Chart Mode Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChartModeChip(
                    label = "Capacity Degradation",
                    selected = chartMode == 0,
                    onClick = { chartMode = 0 },
                    testTag = "discharge_mode_capacity",
                    modifier = Modifier.weight(1f)
                )
                ChartModeChip(
                    label = "Discharge Rate (%/hr)",
                    selected = chartMode == 1,
                    onClick = { chartMode = 1 },
                    testTag = "discharge_mode_rate",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (chartMode == 0) {
                    ChartLegendItem(color = primaryColor, label = "Initial Calibrated (${report.initialCalibratedMah} mAh)", dashed = true)
                    ChartLegendItem(color = retentionColor, label = "Measured Capacity", dashed = false)
                } else {
                    ChartLegendItem(color = AmberWarning, label = "Avg Discharge: ${String.format(Locale.US, "%.1f", report.avgDischargeRatePctPerHr)}%/hr", dashed = false)
                    ChartLegendItem(color = EmeraldOnline, label = "Est. SOT: ${String.format(Locale.US, "%.1f", report.estimatedScreenOnHours)}h", dashed = false)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Native Jetpack Compose Canvas Chart
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .testTag("battery_degradation_canvas")
            ) {
                val w = size.width
                val h = size.height
                val topPad = 14.dp.toPx()
                val bottomPad = 12.dp.toPx()
                val chartHeight = (h - topPad - bottomPad).coerceAtLeast(1f)
                val count = points.size
                if (count < 2) return@Canvas

                // Horizontal gridlines
                for (i in 0..3) {
                    val y = topPad + chartHeight * (i / 3f)
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.16f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                if (chartMode == 0) {
                    // Scale Y from 80% of initial capacity up to 102% so degradation delta is clearly legible
                    val maxMah = (report.initialCalibratedMah * 1.02f)
                    val minMah = (report.initialCalibratedMah * 0.80f)
                    val span = (maxMah - minMah).coerceAtLeast(1f)
                    val stepX = w / (count - 1)

                    fun mapY(mah: Int): Float {
                        val norm = ((mah - minMah) / span).coerceIn(0f, 1f)
                        return topPad + chartHeight * (1f - norm)
                    }

                    // Initial Calibrated Baseline (dashed horizontal reference)
                    val baselineY = mapY(report.initialCalibratedMah)
                    drawLine(
                        color = primaryColor.copy(alpha = 0.85f),
                        start = Offset(0f, baselineY),
                        end = Offset(w, baselineY),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )

                    val measuredCoords = points.mapIndexed { idx, pt ->
                        Offset(idx * stepX, mapY(pt.measuredCapacityMah))
                    }

                    // Smooth curve for measured capacity
                    val linePath = Path().apply {
                        moveTo(measuredCoords[0].x, measuredCoords[0].y)
                        for (i in 0 until measuredCoords.size - 1) {
                            val p1 = measuredCoords[i]
                            val p2 = measuredCoords[i + 1]
                            val cx = (p1.x + p2.x) / 2f
                            cubicTo(cx, p1.y, cx, p2.y, p2.x, p2.y)
                        }
                    }

                    // Degradation gap shaded area between calibrated baseline and measured capacity
                    val gapPath = Path().apply {
                        moveTo(measuredCoords[0].x, baselineY)
                        lineTo(measuredCoords.last().x, baselineY)
                        lineTo(measuredCoords.last().x, measuredCoords.last().y)
                        for (i in measuredCoords.size - 1 downTo 1) {
                            val p2 = measuredCoords[i]
                            val p1 = measuredCoords[i - 1]
                            val cx = (p1.x + p2.x) / 2f
                            cubicTo(cx, p2.y, cx, p1.y, p1.x, p1.y)
                        }
                        close()
                    }

                    drawPath(
                        path = gapPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                AmberWarning.copy(alpha = 0.24f),
                                Color.Transparent
                            ),
                            startY = baselineY,
                            endY = topPad + chartHeight
                        )
                    )

                    // Gradient area under measured curve
                    val fillPath = Path().apply {
                        addPath(linePath)
                        lineTo(w, topPad + chartHeight)
                        lineTo(0f, topPad + chartHeight)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                retentionColor.copy(alpha = 0.25f),
                                Color.Transparent
                            ),
                            startY = baselineY,
                            endY = topPad + chartHeight
                        )
                    )

                    drawPath(
                        path = linePath,
                        color = retentionColor,
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Draw checkpoint nodes
                    measuredCoords.forEachIndexed { idx, pt ->
                        val isSelected = idx == selectedIndex
                        if (isSelected) {
                            drawCircle(
                                color = retentionColor.copy(alpha = 0.28f),
                                radius = 9.dp.toPx(),
                                center = pt
                            )
                        }
                        drawCircle(
                            color = retentionColor,
                            radius = if (isSelected) 5.dp.toPx() else 3.5.dp.toPx(),
                            center = pt
                        )
                    }
                } else {
                    // Discharge Rate Bar + Trend Overlay (%/hr)
                    val maxRate = (points.maxOfOrNull { it.dischargeRatePctPerHr } ?: 5f).coerceAtLeast(5f)
                    val slotWidth = w / count
                    val barWidth = (slotWidth * 0.46f).coerceIn(12.dp.toPx(), 28.dp.toPx())

                    points.forEachIndexed { idx, pt ->
                        val norm = (pt.dischargeRatePctPerHr / maxRate).coerceIn(0.05f, 1f)
                        val barH = chartHeight * norm
                        val x = idx * slotWidth + (slotWidth - barWidth) / 2f
                        val y = topPad + chartHeight - barH
                        val barColor = when {
                            idx == selectedIndex -> primaryColor
                            pt.dischargeRatePctPerHr >= 3.6f -> AmberWarning
                            else -> EmeraldOnline
                        }
                        drawRoundRect(
                            color = barColor.copy(alpha = if (idx == selectedIndex) 0.95f else 0.7f),
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barH),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Interactive Checkpoint Period Selector Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                points.forEachIndexed { index, pt ->
                    val isSelected = index == selectedIndex
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) primaryColor.copy(alpha = 0.16f)
                                else Color.Transparent
                            )
                            .clickable { selectedIndex = index }
                            .defaultMinSize(minWidth = 36.dp, minHeight = 32.dp)
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .testTag("degradation_point_$index"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = pt.periodLabel,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) primaryColor else onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Selected Checkpoint Inspector Strip
            val deltaMah = (selectedPoint.calibratedBaselineMah - selectedPoint.measuredCapacityMah).coerceAtLeast(0)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                    .border(1.dp, outlineVariant, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (chartMode == 0) Icons.Default.BatteryStd else Icons.Default.TrendingDown,
                        contentDescription = "Selected Period Details",
                        tint = primaryColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "${selectedPoint.periodLabel} • ${selectedPoint.cycleCount} Cycles",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = onSurface
                        )
                        Text(
                            text = "Measured ${selectedPoint.measuredCapacityMah} mAh vs ${selectedPoint.calibratedBaselineMah} mAh baseline",
                            fontSize = 10.sp,
                            color = onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (deltaMah == 0) "0 mAh" else "-$deltaMah mAh",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (deltaMah == 0) EmeraldOnline else AmberWarning
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.1f", selectedPoint.dischargeRatePctPerHr)}%/hr",
                        fontSize = 10.sp,
                        color = onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportMetricColumn(
    label: String,
    value: String,
    accentColor: Color
) {
    Column {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
    }
}

@Composable
private fun ChartModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) primaryColor.copy(alpha = 0.14f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                1.dp,
                if (selected) primaryColor else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ChartLegendItem(
    color: Color,
    label: String,
    dashed: Boolean
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(width = if (dashed) 12.dp else 8.dp, height = if (dashed) 3.dp else 8.dp)
                .clip(if (dashed) RoundedCornerShape(2.dp) else CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
