package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.model.AuditLogEntry
import com.example.ui.components.EnterpriseHeader
import com.example.ui.components.SecondaryNexusButton
import com.example.ui.theme.CrimsonStop
import com.example.ui.theme.NexusBluePrimary

@Composable
fun SecuritySettingsScreen(
    auditLogs: List<AuditLogEntry>,
    currentThemeMode: String = "DARK",
    onSetThemeMode: (String) -> Unit = {},
    onEmergencyRevoke: () -> Unit,
    onRoleReset: () -> Unit,
    onRemoteReboot: () -> Unit = {},
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("security_settings_screen")
    ) {
        EnterpriseHeader(
            title = "Settings",
            subtitle = "Preferences & Security Configuration",
            onBackClick = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // User Profile Card `RK` Avatar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(NexusBluePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "RK",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ritik Kumar",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Admin Account",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Profile Details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Appearance Toggle Section `[ Light ]  [ Dark ]  [ System ]`
            Column {
                Text(
                    text = "Appearance",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp)
                ) {
                    ThemeSegmentButton(text = "Light", active = currentThemeMode == "LIGHT", onClick = { onSetThemeMode("LIGHT") }, modifier = Modifier.weight(1f))
                    ThemeSegmentButton(text = "Dark", active = currentThemeMode == "DARK", onClick = { onSetThemeMode("DARK") }, modifier = Modifier.weight(1f))
                    ThemeSegmentButton(text = "System", active = currentThemeMode == "SYSTEM", onClick = { onSetThemeMode("SYSTEM") }, modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Settings List Options
            SettingsOptionRow(title = "Security & Privacy", subtitle = "Encryption keys & PIN pairing", icon = Icons.Default.Lock)
            SettingsOptionRow(title = "Discreet Admin Mode & 24/7 Background Persistence", subtitle = "Runs 24/7 background service via BOOT_COMPLETED", icon = Icons.Default.PhoneAndroid)
            SettingsOptionRow(title = "Screen Dark Ambient Overlay", subtitle = "Keeps Managed Phone screen dark during active remote session", icon = Icons.Default.Info)
            SettingsOptionRow(title = "Alerts & Notifications", subtitle = "Session disconnect notifications", icon = Icons.Default.Notifications)
            SettingsOptionRow(title = "Data & Storage", subtitle = "Logs & cache cleanup", icon = Icons.Default.Storage)
            SettingsOptionRow(title = "Backup & Export", subtitle = "Export session audit logs", icon = Icons.Default.CloudDownload)

            val ctx = androidx.compose.ui.platform.LocalContext.current
            val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
            SettingsOptionRow(
                title = "Check for Updates (OTA)",
                subtitle = "Check GitHub for new updates • Current: v${com.example.BuildConfig.VERSION_NAME}",
                icon = Icons.Default.CloudDownload,
                onClick = {
                    coroutineScope.launch {
                        val manager = com.example.data.AppUpdateManager.getInstance(ctx)
                        val update = manager.checkForUpdates()
                        if (update == null) {
                            com.example.data.LiveAlertManager.postAlert(
                                title = "App Up-to-Date",
                                message = "Nexus Control Pro is up-to-date (v${com.example.BuildConfig.VERSION_NAME}).",
                                type = com.example.data.LiveAlertType.SUCCESS
                            )
                        }
                    }
                }
            )

            SettingsOptionRow(title = "About", subtitle = "NEXUS CONTROL PRO v${com.example.BuildConfig.VERSION_NAME} (Build ${com.example.BuildConfig.VERSION_CODE})", icon = Icons.Default.Info)

            Spacer(modifier = Modifier.height(8.dp))

            SecondaryNexusButton(
                text = "Remote Restart Device ('svc power reboot')",
                onClick = onRemoteReboot,
                color = com.example.ui.theme.NeonCyan,
                testTag = "remote_reboot_button"
            )

            SecondaryNexusButton(
                text = "Revoke All Pairings (Emergency)",
                onClick = onEmergencyRevoke,
                color = CrimsonStop,
                testTag = "kill_switch_button"
            )

            SecondaryNexusButton(
                text = "Reset Operating Role",
                onClick = onRoleReset,
                color = NexusBluePrimary,
                testTag = "reset_role_button"
            )
        }
    }
}

@Composable
private fun ThemeSegmentButton(text: String, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) NexusBluePrimary else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsOptionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NexusBluePrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = title, tint = NexusBluePrimary, modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Open", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
