package com.example.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CommandHeatmapAnalyzer
import com.example.data.CommandHeatmapRow
import com.example.model.AuditLogEntry
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonStop
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.NexusBluePrimary

/**
 * Interactive 2D Command Frequency Heatmap widget showing which remote commands
 * are executed most frequently across the managed fleet to highlight common
 * troubleshooting workflows.
 */
@Composable
fun CommandHeatmapCard(
    auditLogs: List<AuditLogEntry>,
    onOpenActivityLogs: (() -> Unit)? = null,
    onOpenBulkFleet: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var windowMode by remember { mutableStateOf("7D") }
    val report = remember(auditLogs, windowMode) {
        CommandHeatmapAnalyzer.buildHeatmap(auditLogs, windowMode)
    }

    var selectedCell by remember(windowMode) {
        mutableStateOf<Pair<String, Int>?>(null)
    }

    val isDarkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val activeInspectorText = remember(selectedCell, report) {
        val selection = selectedCell
        if (selection == null) {
            val lead = report.rows.firstOrNull()
            if (lead != null) {
                "Tap any cell to inspect • Hottest: ${lead.displayLabel} (${lead.totalCount} runs, peak ${lead.peakBucketLabel})"
            } else {
                "Tap any heatmap cell to inspect command volume."
            }
        } else {
            val row = report.rows.firstOrNull { it.commandKey == selection.first }
            val bucketLabel = report.bucketLabels.getOrElse(selection.second) { "" }
            val count = row?.bucketCounts?.getOrElse(selection.second) { 0 } ?: 0
            if (row != null) {
                "${row.displayLabel} (${row.workflowCategory}) • $bucketLabel: $count executions (${row.sharePct.toInt()}% of fleet)"
            } else {
                "Tap any heatmap cell to inspect command volume."
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            .padding(16.dp)
            .testTag("command_frequency_heatmap_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header + Window Toggle
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
                            .background(AmberWarning.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Command Frequency Heatmap",
                            tint = AmberWarning,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Command Frequency Heatmap",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${report.totalExecutions} fleet executions • ${report.liveEventCount} live events",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Compact 7D / 24H Window Selector
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(2.dp)
                ) {
                    HeatmapWindowChip(
                        label = "7D",
                        selected = windowMode == "7D",
                        onClick = { windowMode = "7D" },
                        testTag = "heatmap_range_7d"
                    )
                    HeatmapWindowChip(
                        label = "24H",
                        selected = windowMode == "24H",
                        onClick = { windowMode = "24H" },
                        testTag = "heatmap_range_24h"
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Column Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "COMMAND",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(86.dp)
                )
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    report.bucketLabels.forEach { bucket ->
                        Text(
                            text = bucket,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Text(
                    text = "SUM",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Heatmap Matrix Rows
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                report.rows.forEach { row ->
                    HeatmapMatrixRow(
                        row = row,
                        maxCellCount = report.maxCellCount,
                        isDarkSurface = isDarkSurface,
                        selectedBucketIndex = if (selectedCell?.first == row.commandKey) selectedCell?.second else null,
                        onSelectCell = { bucketIdx ->
                            selectedCell = if (selectedCell == Pair(row.commandKey, bucketIdx)) {
                                null
                            } else {
                                Pair(row.commandKey, bucketIdx)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Intensity Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GridOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Intensity",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Low",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    listOf(0.0f, 0.25f, 0.50f, 0.78f, 1.0f).forEach { level ->
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(heatmapCellColor(level, isDarkSurface))
                        )
                    }
                    Text(
                        text = "Peak",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Interactive Cell Inspector Pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .testTag("heatmap_cell_inspector")
            ) {
                Text(
                    text = activeInspectorText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Troubleshooting Workflow Insight Callout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.09f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Insights,
                    contentDescription = "Workflow insight",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = report.topWorkflowInsight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 16.sp
                )
            }

            if (onOpenActivityLogs != null || onOpenBulkFleet != null) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onOpenBulkFleet != null) {
                        Button(
                            onClick = onOpenBulkFleet,
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("heatmap_open_bulk_fleet_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Devices,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Bulk Actions",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (onOpenActivityLogs != null) {
                        OutlinedButton(
                            onClick = onOpenActivityLogs,
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("heatmap_open_audit_logs_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Audit Logs",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeatmapWindowChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HeatmapMatrixRow(
    row: CommandHeatmapRow,
    maxCellCount: Int,
    isDarkSurface: Boolean,
    selectedBucketIndex: Int?,
    onSelectCell: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = row.displayLabel,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(86.dp)
        )

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            row.bucketCounts.forEachIndexed { idx, count ->
                val normalized = (count.toFloat() / maxCellCount.toFloat()).coerceIn(0f, 1f)
                val cellBg = heatmapCellColor(normalized, isDarkSurface)
                val isSelected = selectedBucketIndex == idx
                val textColor = when {
                    normalized >= 0.55f -> Color.White
                    isDarkSurface -> Color(0xFFE2E8F0)
                    else -> Color(0xFF0F172A)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(cellBg)
                        .border(
                            width = if (isSelected) 2.dp else 0.5.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { onSelectCell(idx) }
                        .testTag("heatmap_cell_${row.commandKey}_$idx"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = count.toString(),
                        fontSize = 10.sp,
                        fontWeight = if (normalized >= 0.6f) FontWeight.Bold else FontWeight.Medium,
                        color = textColor
                    )
                }
            }
        }

        Text(
            text = row.totalCount.toString(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.End,
            modifier = Modifier.width(34.dp)
        )
    }
}

private fun heatmapCellColor(intensity: Float, isDark: Boolean): Color {
    return when {
        intensity <= 0.05f -> if (isDark) Color(0xFF1A2438) else Color(0xFFE2E8F0)
        intensity < 0.30f -> NexusBluePrimary.copy(alpha = if (isDark) 0.30f else 0.22f)
        intensity < 0.55f -> EmeraldOnline.copy(alpha = if (isDark) 0.55f else 0.45f)
        intensity < 0.80f -> AmberWarning.copy(alpha = if (isDark) 0.82f else 0.78f)
        else -> CrimsonStop.copy(alpha = if (isDark) 0.90f else 0.88f)
    }
}
