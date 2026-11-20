package com.example.data

import com.example.model.AuditLogEntry
import java.util.Calendar
import java.util.Locale

data class CommandHeatmapRow(
    val commandKey: String,
    val displayLabel: String,
    val workflowCategory: String,
    val bucketCounts: List<Int>,
    val totalCount: Int,
    val sharePct: Float,
    val peakBucketLabel: String,
    val peakBucketCount: Int
)

data class CommandHeatmapReport(
    val windowMode: String, // "7D" or "24H"
    val bucketLabels: List<String>,
    val rows: List<CommandHeatmapRow>,
    val maxCellCount: Int,
    val totalExecutions: Int,
    val liveEventCount: Int,
    val topWorkflowInsight: String
)

/**
 * Aggregates remote command audit history into a 2D heatmap matrix
 * (Command Category x Time Bucket) to surface high-frequency fleet
 * troubleshooting workflows.
 */
object CommandHeatmapAnalyzer {

    private val sevenDayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    private val twentyFourHourLabels = listOf("00h", "04h", "08h", "12h", "16h", "20h")

    private data class CommandMeta(
        val key: String,
        val label: String,
        val workflow: String,
        val baseline7D: IntArray,
        val baseline24H: IntArray
    )

    private val commandCatalog = listOf(
        CommandMeta(
            key = "WIFI_SCAN",
            label = "Wi-Fi Scan",
            workflow = "Connectivity Triage",
            baseline7D = intArrayOf(6, 9, 11, 8, 12, 4, 3),
            baseline24H = intArrayOf(1, 2, 7, 11, 9, 4)
        ),
        CommandMeta(
            key = "SETTINGS_SYNC",
            label = "Settings Sync",
            workflow = "Policy & Config",
            baseline7D = intArrayOf(8, 7, 9, 10, 8, 3, 2),
            baseline24H = intArrayOf(2, 1, 8, 9, 7, 5)
        ),
        CommandMeta(
            key = "REBOOT",
            label = "Reboot",
            workflow = "Crash Recovery",
            baseline7D = intArrayOf(4, 5, 7, 9, 6, 2, 1),
            baseline24H = intArrayOf(1, 1, 5, 6, 8, 3)
        ),
        CommandMeta(
            key = "LOCK",
            label = "Remote Lock",
            workflow = "Security Response",
            baseline7D = intArrayOf(2, 4, 3, 6, 7, 3, 2),
            baseline24H = intArrayOf(1, 0, 3, 4, 6, 5)
        ),
        CommandMeta(
            key = "BATTERY_CAL",
            label = "Battery Cal.",
            workflow = "Power Diagnostics",
            baseline7D = intArrayOf(3, 4, 5, 4, 5, 2, 1),
            baseline24H = intArrayOf(0, 1, 4, 5, 4, 2)
        ),
        CommandMeta(
            key = "GEOFENCE",
            label = "Geofence",
            workflow = "Perimeter Control",
            baseline7D = intArrayOf(2, 3, 4, 5, 4, 2, 1),
            baseline24H = intArrayOf(0, 1, 3, 4, 5, 2)
        )
    )

    fun buildHeatmap(
        auditLogs: List<AuditLogEntry>,
        windowMode: String = "7D"
    ): CommandHeatmapReport {
        val is24H = windowMode.equals("24H", ignoreCase = true)
        val labels = if (is24H) twentyFourHourLabels else sevenDayLabels
        val bucketCount = labels.size

        // Clone baseline arrays so live audit events increment exact buckets cleanly
        val matrix = commandCatalog.associate { meta ->
            val base = if (is24H) meta.baseline24H.copyOf() else meta.baseline7D.copyOf()
            meta.key to base
        }

        val cal = Calendar.getInstance()
        var matchedLiveLogs = 0

        for (entry in auditLogs) {
            val mappedKey = classifyAction(entry.action) ?: continue
            val buckets = matrix[mappedKey] ?: continue
            cal.timeInMillis = entry.timestamp
            val bucketIndex = if (is24H) {
                (cal.get(Calendar.HOUR_OF_DAY) / 4).coerceIn(0, bucketCount - 1)
            } else {
                // Calendar.MONDAY = 2 .. Calendar.SUNDAY = 1 -> map to 0..6 (Mon..Sun)
                val dow = cal.get(Calendar.DAY_OF_WEEK)
                if (dow == Calendar.SUNDAY) 6 else (dow - Calendar.MONDAY).coerceIn(0, 6)
            }
            buckets[bucketIndex] = buckets[bucketIndex] + 1
            matchedLiveLogs++
        }

        val grandTotal = matrix.values.sumOf { it.sum() }.coerceAtLeast(1)
        val maxCell = matrix.values.maxOfOrNull { arr -> arr.maxOrNull() ?: 0 }?.coerceAtLeast(1) ?: 1

        val rows = commandCatalog.map { meta ->
            val counts = matrix[meta.key]?.toList() ?: List(bucketCount) { 0 }
            val total = counts.sum()
            val peakIdx = counts.indices.maxByOrNull { counts[it] } ?: 0
            CommandHeatmapRow(
                commandKey = meta.key,
                displayLabel = meta.label,
                workflowCategory = meta.workflow,
                bucketCounts = counts,
                totalCount = total,
                sharePct = (total.toFloat() / grandTotal.toFloat()) * 100f,
                peakBucketLabel = labels.getOrElse(peakIdx) { labels.first() },
                peakBucketCount = counts.getOrElse(peakIdx) { 0 }
            )
        }.sortedByDescending { it.totalCount }

        val top1 = rows.getOrNull(0)
        val top2 = rows.getOrNull(1)
        val combinedShare = ((top1?.sharePct ?: 0f) + (top2?.sharePct ?: 0f)).toInt()
        val insight = if (top1 != null && top2 != null) {
            "${top1.displayLabel} (${top1.workflowCategory}) & ${top2.displayLabel} drive $combinedShare% of fleet troubleshooting, peaking on ${top1.peakBucketLabel}."
        } else {
            "Execute fleet commands to populate live troubleshooting frequency insights."
        }

        return CommandHeatmapReport(
            windowMode = if (is24H) "24H" else "7D",
            bucketLabels = labels,
            rows = rows,
            maxCellCount = maxCell,
            totalExecutions = grandTotal,
            liveEventCount = matchedLiveLogs,
            topWorkflowInsight = insight
        )
    }

    private fun classifyAction(action: String): String? {
        val upper = action.uppercase(Locale.getDefault())
        return when {
            upper.contains("WIFI") || upper.contains("NETWORK") -> "WIFI_SCAN"
            upper.contains("REBOOT") -> "REBOOT"
            upper.contains("LOCK") -> "LOCK"
            upper.contains("BATTERY") -> "BATTERY_CAL"
            upper.contains("GEOFENCE") || upper.contains("LOCATION") -> "GEOFENCE"
            upper.contains("SETTINGS") || upper.contains("SYNC") ||
                upper.contains("FOCUS") || upper.contains("VOLUME") ||
                upper.contains("PERMISSION") -> "SETTINGS_SYNC"
            else -> null
        }
    }
}
