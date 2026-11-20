package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.local.SessionHistoryEntity
import com.example.model.AuditLogEntry
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AuditLogExporter {

    /**
     * Converts Room database session log entities into standard CSV format string
     */
    fun convertLogsToCsv(logs: List<SessionHistoryEntity>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val sb = StringBuilder()

        // CSV Header
        sb.append("LogID,Timestamp,FormattedDate,InteractionType,DurationSeconds,TargetDevice,AdminDevice,Details\n")

        for (log in logs) {
            val dateStr = dateFormat.format(Date(log.timestamp))
            val safeDetails = log.details.replace(",", ";").replace("\n", " ")

            sb.append("\"${log.id}\",")
                .append("${log.timestamp},")
                .append("\"$dateStr\",")
                .append("\"${log.interactionType}\",")
                .append("${log.durationSeconds},")
                .append("\"${log.targetDeviceId}\",")
                .append("\"${log.adminDeviceId}\",")
                .append("\"$safeDetails\"\n")
        }

        return sb.toString()
    }

    /**
     * Writes CSV string to cache directory and launches Android share intent
     */
    fun exportAndShareCsv(context: Context, logs: List<SessionHistoryEntity>) {
        try {
            val csvData = convertLogsToCsv(logs)
            val exportFile = File(context.cacheDir, "nexus_audit_compliance_report.csv")
            exportFile.writeText(csvData)

            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                exportFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "NEXUS CONTROL PRO - Audit Compliance Report")
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Export Audit Log CSV"))
        } catch (e: Exception) {
            Log.e("AuditLogExporter", "Error exporting CSV: ${e.message}")
        }
    }

    /**
     * Converts AuditLogEntry items into standard CSV format string
     */
    fun convertAuditLogsToCsv(logs: List<AuditLogEntry>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val sb = StringBuilder()

        sb.append("LogID,Timestamp,FormattedDate,Action,AdminUser,InitiatorRole,TargetDevice,Outcome,Details,PreviousHash,EntryHash\n")

        for (log in logs) {
            val dateStr = dateFormat.format(Date(log.timestamp))
            val safeDetails = log.details.replace(",", ";").replace("\n", " ")

            sb.append("\"${log.id}\",")
                .append("${log.timestamp},")
                .append("\"$dateStr\",")
                .append("\"${log.action}\",")
                .append("\"${log.adminUser}\",")
                .append("\"${log.initiatorRole}\",")
                .append("\"${log.targetDeviceId}\",")
                .append("\"${log.outcome}\",")
                .append("\"$safeDetails\",")
                .append("\"${log.previousHash}\",")
                .append("\"${log.entryHash}\"\n")
        }

        return sb.toString()
    }

    /**
     * Writes AuditLogEntry CSV string to cache directory and launches Android share intent
     */
    fun exportAndShareAuditLogsCsv(context: Context, logs: List<AuditLogEntry>) {
        try {
            val csvData = convertAuditLogsToCsv(logs)
            val exportFile = File(context.cacheDir, "nexus_audit_compliance_report.csv")
            exportFile.writeText(csvData)

            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                exportFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "NEXUS CONTROL PRO - Audit Compliance Report")
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Export Audit Log CSV"))
        } catch (e: Exception) {
            Log.e("AuditLogExporter", "Error exporting CSV: ${e.message}")
        }
    }
}
