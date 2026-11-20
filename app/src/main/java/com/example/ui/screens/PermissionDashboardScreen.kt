package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.EnterpriseHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.CarbonGlass
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.EmeraldSignal
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun PermissionDashboardScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid)
            .testTag("permission_dashboard_screen")
    ) {
        EnterpriseHeader(
            title = "PERMISSIONS AUDIT DASHBOARD",
            subtitle = "Android Runtime & Special Permission Grants",
            onBackClick = onBack,
            badgeText = "AUDIT COMPLIANT",
            badgeColor = EmeraldSignal
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PermissionAuditRow(
                title = "MediaProjection Screen Capture",
                category = "SPECIAL PERMISSION",
                description = "Explicit user confirmation prompt shown on each stream start. Never bypassed.",
                icon = Icons.Default.ScreenShare,
                granted = true
            )

            PermissionAuditRow(
                title = "Scoped Storage & Photo Picker",
                category = "STORAGE ACCESS",
                description = "Strict Scoped Storage SAF & MediaStore picker compliance.",
                icon = Icons.Default.Folder,
                granted = true
            )

            PermissionAuditRow(
                title = "FusedLocation GPS Provider",
                category = "RUNTIME PERMISSION",
                description = "Foreground service location updates with persistent notification.",
                icon = Icons.Default.MyLocation,
                granted = true
            )

            PermissionAuditRow(
                title = "Audio Assistance Microphone",
                category = "RUNTIME PERMISSION",
                description = "Visible recording indicator and foreground microphone service.",
                icon = Icons.Default.Mic,
                granted = true
            )

            PermissionAuditRow(
                title = "Call Log & Contacts Query",
                category = "PRIVACY PERMISSION",
                description = "Authorized call telemetry. Call audio recording strictly disabled.",
                icon = Icons.Default.Phone,
                granted = true
            )
        }
    }
}

@Composable
private fun PermissionAuditRow(
    title: String,
    category: String,
    description: String,
    icon: ImageVector,
    granted: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CarbonGlass)
            .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(EmeraldSignal.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = title, tint = EmeraldSignal, modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    Text(text = category, fontSize = 10.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                    Text(text = description, fontSize = 11.sp, color = TextMuted)
                }
            }

            StatusBadge(text = if (granted) "GRANTED" else "DENIED", color = if (granted) EmeraldSignal else Color.Red)
        }
    }
}
