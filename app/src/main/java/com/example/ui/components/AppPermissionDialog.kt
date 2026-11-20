package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.AppItem
import com.example.model.SensitivePermissionState
import com.example.ui.theme.EmeraldOnline

@Composable
fun AppPermissionDialog(
    app: AppItem,
    onDismissRequest: () -> Unit,
    onTogglePermission: (permissionKey: String, isGranted: Boolean) -> Unit
) {
    val currentPerms = app.sensitivePermissions.ifEmpty {
        listOf(
            SensitivePermissionState("android.permission.ACCESS_FINE_LOCATION", "Location", false),
            SensitivePermissionState("android.permission.CAMERA", "Camera", false),
            SensitivePermissionState("android.permission.RECORD_AUDIO", "Microphone", false),
            SensitivePermissionState("android.permission.READ_CONTACTS", "Contacts", false)
        )
    }

    val permissionStates = remember(app.packageName) {
        mutableStateMapOf<String, Boolean>().apply {
            currentPerms.forEach { put(it.permissionKey, it.isGranted) }
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
                .padding(20.dp)
                .testTag("app_permission_dialog")
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
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Security",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "App Permissions",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = app.label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("close_permission_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Package: ${app.packageName}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable permissions list
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    currentPerms.forEach { perm ->
                        val isGranted = permissionStates[perm.permissionKey] ?: perm.isGranted
                        PermissionItemRow(
                            permission = perm,
                            isGranted = isGranted,
                            onToggle = { granted ->
                                permissionStates[perm.permissionKey] = granted
                                onTogglePermission(perm.permissionKey, granted)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                PrimaryNexusButton(
                    text = "Done",
                    onClick = onDismissRequest,
                    testTag = "done_permission_dialog_button"
                )
            }
        }
    }
}

@Composable
private fun PermissionItemRow(
    permission: SensitivePermissionState,
    isGranted: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val icon = when {
        permission.permissionKey.contains("LOCATION", ignoreCase = true) -> Icons.Default.MyLocation
        permission.permissionKey.contains("CAMERA", ignoreCase = true) -> Icons.Default.CameraAlt
        permission.permissionKey.contains("AUDIO", ignoreCase = true) ||
                permission.permissionKey.contains("RECORD", ignoreCase = true) -> Icons.Default.Mic
        permission.permissionKey.contains("CONTACT", ignoreCase = true) -> Icons.Default.Contacts
        else -> Icons.Default.Security
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                1.dp,
                if (isGranted) EmeraldOnline.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
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
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isGranted) EmeraldOnline.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = permission.displayName,
                        tint = if (isGranted) EmeraldOnline else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = permission.displayName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = permission.permissionKey.substringAfterLast("."),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isGranted) "GRANTED" else "DENIED",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isGranted) EmeraldOnline else Color(0xFFEF4444)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = isGranted,
                    onCheckedChange = onToggle,
                    modifier = Modifier.testTag("toggle_perm_${permission.displayName.lowercase()}"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = EmeraldOnline,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }
        }
    }
}
