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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.EnterpriseHeader
import com.example.ui.theme.CarbonGlass
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.ElectricCobalt
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun EnterpriseAdminScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid)
            .testTag("enterprise_admin_screen")
    ) {
        EnterpriseHeader(
            title = "ANDROID ENTERPRISE DPM",
            subtitle = "Device Policy Manager & Provisioning",
            onBackClick = onBack,
            badgeText = "DEVICE OWNER MODE"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CarbonGlass)
                    .border(1.dp, ElectricCobalt, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SupervisorAccount,
                        contentDescription = "Enterprise DPM",
                        tint = ElectricCobalt,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Nexus Device Policy Manager (DPM)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "Device Owner & Profile Owner Management Matrix",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CarbonGlass)
                    .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "ADB PROVISIONING COMMAND FOR DEVICE OWNER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ObsidianVoid)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "adb shell dpm set-device-owner com.aistudio.nexuscontrolpro.vzkq/.receiver.NexusDeviceAdminReceiver",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextWhite
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Note: Must be executed on a freshly factory-reset device prior to adding primary Google Accounts.",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CarbonGlass)
                    .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "CAPABILITY MATRIX BY PRIVILEGE LEVEL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Standard Non-Root App: WebRTC Screen Sharing (MediaProjection), SAF File Explorer, MediaStore Gallery, FusedLocation GPS, Call Log Telemetry.\n• Device Owner Privileges: Silent background app installation, Kiosk LockTask mode, system policy restrictions, camera disable/enable, keyguard bypass.",
                        fontSize = 12.sp,
                        color = TextWhite,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
