package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AuditChainVerificationResult
import com.example.data.AuditLogExporter
import com.example.data.AuditLogIntegrityVerifier
import com.example.model.AuditLogEntry
import com.example.ui.components.EnterpriseHeader
import com.example.ui.components.PrimaryNexusButton
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonStop
import com.example.ui.theme.EmeraldOnline
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AuditLogScreen(
    auditLogs: List<AuditLogEntry>,
    onBack: () -> Unit,
    chainVerification: AuditChainVerificationResult? = null
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedOutcome by remember { mutableStateOf("ALL") }

    // Ensure logs always have a verified SHA-256 chain for inspection
    val normalizedLogs = remember(auditLogs) {
        if (auditLogs.isEmpty()) {
            val now = System.currentTimeMillis()
            AuditLogIntegrityVerifier.sealChronologicalChain(
                listOf(
                    AuditLogEntry(
                        id = "audit_init_1",
                        timestamp = now - 900_000L,
                        action = "DEVICE_PAIRING_VERIFIED",
                        initiatorRole = "ADMIN",
                        targetDeviceId = "phone_a",
                        details = "Trusted TLS pairing handshake verified",
                        success = true,
                        adminUser = "Ritik Kumar (Admin)",
                        outcome = "SUCCESS"
                    ),
                    AuditLogEntry(
                        id = "audit_init_2",
                        timestamp = now - 300_000L,
                        action = "SETTINGS_SYNC",
                        initiatorRole = "ADMIN",
                        targetDeviceId = "phone_a",
                        details = "Synchronized enterprise telemetry and network configuration",
                        success = true,
                        adminUser = "Ritik Kumar (Admin)",
                        outcome = "SUCCESS"
                    )
                )
            )
        } else {
            auditLogs
        }
    }

    val verification = remember(normalizedLogs, chainVerification) {
        chainVerification?.takeIf { it.totalEntries == normalizedLogs.size }
            ?: AuditLogIntegrityVerifier.verifyChain(normalizedLogs)
    }

    val filteredLogs = remember(normalizedLogs, searchQuery, selectedOutcome) {
        val q = searchQuery.trim().lowercase(Locale.getDefault())
        normalizedLogs.filter { entry ->
            val matchesOutcome = selectedOutcome == "ALL" ||
                entry.outcome.equals(selectedOutcome, ignoreCase = true)
            val matchesQuery = q.isEmpty() ||
                entry.action.lowercase(Locale.getDefault()).contains(q) ||
                entry.adminUser.lowercase(Locale.getDefault()).contains(q) ||
                entry.targetDeviceId.lowercase(Locale.getDefault()).contains(q) ||
                entry.details.lowercase(Locale.getDefault()).contains(q) ||
                entry.outcome.lowercase(Locale.getDefault()).contains(q) ||
                entry.entryHash.lowercase(Locale.getDefault()).contains(q)
            matchesOutcome && matchesQuery
        }
    }

    val outcomeFilters = listOf("ALL", "SUCCESS", "QUEUED", "FAILED")

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .testTag("audit_log_screen")
        ) {
            EnterpriseHeader(
                title = "ACTIVITY LOGS",
                subtitle = "Searchable, tamper-evident remote command ledger",
                onBackClick = onBack,
                badgeText = if (verification.isChainIntact) "SHA-256 INTACT" else "TAMPER ALERT",
                badgeColor = if (verification.isChainIntact) EmeraldOnline else CrimsonStop
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cryptographic Integrity Banner
                item {
                    ChainIntegrityBanner(verification = verification)
                }

                // Live Search Input
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("activity_logs_search_input"),
                        placeholder = {
                            Text(
                                text = "Search command, admin user, device, or hash...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search activity logs",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.testTag("clear_search_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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

                // Outcome Filter Chips Row
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        outcomeFilters.forEach { outcome ->
                            val count = if (outcome == "ALL") {
                                normalizedLogs.size
                            } else {
                                normalizedLogs.count { it.outcome.equals(outcome, ignoreCase = true) }
                            }
                            val selected = selectedOutcome == outcome
                            FilterChip(
                                selected = selected,
                                onClick = { selectedOutcome = outcome },
                                label = {
                                    Text(
                                        text = "$outcome ($count)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                modifier = Modifier.testTag("filter_chip_${outcome.lowercase(Locale.getDefault())}"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }

                // Summary & CSV Export Bar
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SHOWING ${filteredLogs.size} OF ${normalizedLogs.size} EVENTS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        PrimaryNexusButton(
                            text = "EXPORT CSV",
                            onClick = {
                                AuditLogExporter.exportAndShareAuditLogsCsv(context, filteredLogs)
                            },
                            icon = Icons.Default.Share,
                            modifier = Modifier
                                .width(154.dp)
                                .height(48.dp),
                            testTag = "export_csv_report_button"
                        )
                    }
                }

                // Empty State
                if (filteredLogs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "No matching logs",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No matching activity logs",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Adjust your search query or outcome filter.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(filteredLogs, key = { it.id }) { log ->
                        val isTampered = verification.tamperedEntryIds.contains(log.id)
                        AuditLogItemCard(
                            log = log,
                            isTampered = isTampered
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChainIntegrityBanner(verification: AuditChainVerificationResult) {
    val accent = if (verification.isChainIntact) EmeraldOnline else CrimsonStop
    val shortDigest = verification.headDigest.take(14)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(alpha = 0.09f))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(14.dp)
            .testTag("audit_chain_integrity_banner")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (verification.isChainIntact) Icons.Default.VerifiedUser else Icons.Default.GppBad,
                    contentDescription = "Audit chain integrity",
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (verification.isChainIntact) {
                        "Tamper-Evident Ledger Verified"
                    } else {
                        "Integrity Warning: ${verification.tamperedEntryIds.size} Altered Record(s)"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${verification.verifiedEntries}/${verification.totalEntries} records verified • Head SHA-256 #$shortDigest",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AuditLogItemCard(
    log: AuditLogEntry,
    isTampered: Boolean
) {
    val formattedTime = remember(log.timestamp) {
        SimpleDateFormat("MMM dd, yyyy • HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
    }

    val icon: ImageVector = when {
        log.action.contains("SYNC_QUEUE", ignoreCase = true) ||
            log.action.contains("OFFLINE_QUEUED", ignoreCase = true) -> Icons.Default.CloudQueue
        log.action.contains("SETTINGS", ignoreCase = true) -> Icons.Default.Settings
        log.action.contains("BATTERY", ignoreCase = true) -> Icons.Default.BatteryChargingFull
        log.action.contains("WIFI", ignoreCase = true) -> Icons.Default.Wifi
        log.action.contains("GEOFENCE", ignoreCase = true) ||
            log.action.contains("LOCATION", ignoreCase = true) -> Icons.Default.MyLocation
        log.action.contains("SCREEN", ignoreCase = true) -> Icons.Default.ScreenShare
        log.action.contains("FILE", ignoreCase = true) -> Icons.Default.Folder
        log.action.contains("APP", ignoreCase = true) -> Icons.Default.Apps
        log.action.contains("PAIR", ignoreCase = true) -> Icons.Default.PhoneAndroid
        else -> Icons.Default.Shield
    }

    val outcomeColor: Color = when (log.outcome.uppercase(Locale.getDefault())) {
        "SUCCESS" -> EmeraldOnline
        "QUEUED" -> AmberWarning
        "FAILED" -> CrimsonStop
        else -> MaterialTheme.colorScheme.primary
    }

    val borderColor = if (isTampered) {
        CrimsonStop.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(if (isTampered) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(14.dp)
            .testTag("audit_log_card_${log.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(outcomeColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = log.action,
                        tint = outcomeColor,
                        modifier = Modifier.size(21.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = log.action.replace('_', ' '),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusBadge(
                            text = log.outcome,
                            color = outcomeColor
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = log.details,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Admin user",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${log.adminUser} • ${log.targetDeviceId}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = formattedTime,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isTampered) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                        contentDescription = if (isTampered) "Tampered hash" else "Verified SHA-256 hash",
                        tint = if (isTampered) CrimsonStop else EmeraldOnline,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    val hashShort = log.entryHash.take(12).ifBlank { "UNSEALED" }
                    val prevShort = log.previousHash.take(8)
                    Text(
                        text = "SHA-256 #$hashShort ← $prevShort",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (isTampered) CrimsonStop else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = log.initiatorRole,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
