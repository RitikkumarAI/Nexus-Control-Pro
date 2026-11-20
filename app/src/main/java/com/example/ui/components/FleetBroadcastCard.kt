package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import com.example.model.FleetBroadcastAlert
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonStop
import com.example.ui.theme.EmeraldOnline

@Composable
fun FleetBroadcastCard(
    broadcasts: List<FleetBroadcastAlert>,
    onSendBroadcast: (title: String, message: String, priority: String, audioChime: Boolean) -> Unit,
    onAcknowledge: (alertId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSendDialog by remember { mutableStateOf(false) }
    var inputTitle by remember { mutableStateOf("") }
    var inputMessage by remember { mutableStateOf("") }
    var inputPriority by remember { mutableStateOf("CRITICAL") }
    var chimeOverride by remember { mutableStateOf(true) }

    if (showSendDialog) {
        AlertDialog(
            onDismissRequest = { showSendDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = null,
                    tint = CrimsonStop
                )
            },
            title = {
                Text(text = "Broadcast Emergency Alert to Fleet", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Dispatches a high-priority announcement to all active fleet devices with real-time acknowledgment receipts.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = inputTitle,
                        onValueChange = { inputTitle = it },
                        label = { Text("Alert Title") },
                        placeholder = { Text("e.g. Severe Weather Warning") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("broadcast_title_input")
                    )
                    OutlinedTextField(
                        value = inputMessage,
                        onValueChange = { inputMessage = it },
                        label = { Text("Message Body") },
                        placeholder = { Text("Details or action instructions...") },
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("broadcast_message_input")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Audio Chime Override (Bypass Silent)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = chimeOverride,
                            onCheckedChange = { chimeOverride = it },
                            modifier = Modifier.testTag("broadcast_chime_switch")
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputTitle.isNotBlank() && inputMessage.isNotBlank()) {
                            onSendBroadcast(inputTitle.trim(), inputMessage.trim(), inputPriority, chimeOverride)
                            inputTitle = ""
                            inputMessage = ""
                            showSendDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonStop),
                    modifier = Modifier.testTag("confirm_send_broadcast_button")
                ) {
                    Text("Transmit Alert", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSendDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val criticalCount = broadcasts.count { it.priority == "CRITICAL" }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (criticalCount > 0) 1.5.dp else 1.dp,
                color = if (criticalCount > 0) CrimsonStop.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(18.dp)
            )
            .padding(16.dp)
            .testTag("fleet_broadcast_card")
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
                            .background(CrimsonStop.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = "Fleet Broadcast",
                            tint = CrimsonStop,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Fleet Broadcast & Emergency Alerts",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${broadcasts.size} alerts dispatched • Real-time read receipts",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                StatusBadge(
                    text = if (criticalCount > 0) "$criticalCount CRITICAL" else "BROADCAST",
                    color = if (criticalCount > 0) CrimsonStop else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Broadcasts List
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                broadcasts.take(3).forEach { alert ->
                    FleetBroadcastItemRow(
                        alert = alert,
                        onAcknowledge = { onAcknowledge(alert.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { showSendDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CrimsonStop,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("open_send_broadcast_dialog_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Broadcast Emergency Alert to Fleet",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FleetBroadcastItemRow(
    alert: FleetBroadcastAlert,
    onAcknowledge: () -> Unit
) {
    val priorityColor = when (alert.priority) {
        "CRITICAL" -> CrimsonStop
        "URGENT" -> AmberWarning
        else -> MaterialTheme.colorScheme.primary
    }

    val ackRatio = "${alert.acknowledgedDeviceIds.size}/${alert.targetDeviceCount}"
    val allAck = alert.acknowledgedDeviceIds.size >= alert.targetDeviceCount

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(10.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (alert.priority == "CRITICAL") Icons.Default.WarningAmber else Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = priorityColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = alert.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                StatusBadge(text = alert.priority, color = priorityColor)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = alert.message,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (alert.audioChimeOverride) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Chime Override Active",
                            tint = CrimsonStop,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = "Receipts: $ackRatio acknowledged",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (allAck) EmeraldOnline else AmberWarning
                    )
                }

                if (!allAck) {
                    TextButton(
                        onClick = onAcknowledge,
                        modifier = Modifier.defaultMinSize(minHeight = 32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Acknowledge", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
