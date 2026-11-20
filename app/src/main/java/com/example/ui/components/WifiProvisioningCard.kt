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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.WifiProvisioningProfile
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonStop
import com.example.ui.theme.EmeraldOnline

@Composable
fun WifiProvisioningCard(
    profiles: List<WifiProvisioningProfile>,
    onPushProfile: (ssid: String, securityType: String, isHidden: Boolean, autoConnect: Boolean) -> Unit,
    onRevokeProfile: (profileId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var inputSsid by remember { mutableStateOf("") }
    var selectedSecurity by remember { mutableStateOf("WPA2/WPA3 Enterprise") }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(text = "Push Enterprise Wi-Fi Profile", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Dispatches secure network credentials to all managed fleet devices without exposing passwords to end users.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = inputSsid,
                        onValueChange = { inputSsid = it },
                        label = { Text("Network SSID") },
                        placeholder = { Text("e.g. Nexus-Field-5G") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("push_wifi_ssid_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputSsid.isNotBlank()) {
                            onPushProfile(inputSsid.trim(), selectedSecurity, false, true)
                            inputSsid = ""
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_push_wifi_button")
                ) {
                    Text("Provision Profile")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val activeCount = profiles.count { it.status == "ACTIVE" }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            .padding(16.dp)
            .testTag("wifi_provisioning_card")
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
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "Wi-Fi Provisioning",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Enterprise Wi-Fi Provisioning",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$activeCount active network profiles pushed to fleet",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                StatusBadge(
                    text = "$activeCount PROFILES",
                    color = EmeraldOnline
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Profiles List
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                profiles.forEach { profile ->
                    WifiProfileRow(
                        profile = profile,
                        onRevoke = { onRevokeProfile(profile.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("open_push_wifi_dialog_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Push New Enterprise Wi-Fi Profile",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun WifiProfileRow(
    profile: WifiProvisioningProfile,
    onRevoke: () -> Unit
) {
    val isActive = profile.status == "ACTIVE"
    val statusColor = if (isActive) EmeraldOnline else CrimsonStop

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
                imageVector = if (isActive) Icons.Default.Wifi else Icons.Default.WifiOff,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profile.ssid,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (profile.autoConnect) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("AUTO", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Text(
                    text = "${profile.securityType} • Target: ${profile.targetScope}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (isActive) {
            IconButton(
                onClick = onRevoke,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("revoke_wifi_${profile.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = "Revoke network credentials",
                    tint = CrimsonStop,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            StatusBadge(text = "REVOKED", color = CrimsonStop)
        }
    }
}
