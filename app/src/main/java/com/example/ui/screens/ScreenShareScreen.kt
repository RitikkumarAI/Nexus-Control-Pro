package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.EnterpriseHeader
import com.example.ui.components.PrimaryNexusButton
import com.example.ui.components.RemoteAudioControlDialog
import com.example.ui.components.SecondaryNexusButton
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.CrimsonStop
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NexusBluePrimary
import com.example.ui.theme.NexusCyanAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun ScreenShareScreen(
    isSharingActive: Boolean,
    streamResolution: String,
    connectionLatencyMs: Int,
    isAdmin: Boolean = true,
    focusModeActive: Boolean = false,
    ringerVolume: Int = 80,
    mediaVolume: Int = 70,
    alarmVolume: Int = 85,
    onToggleFocusMode: (Boolean) -> Unit = {},
    onUpdateVolumes: (ringer: Int, media: Int, alarm: Int) -> Unit = { _, _, _ -> },
    onStartStream: () -> Unit,
    onStopStream: () -> Unit,
    onBack: () -> Unit
) {
    var showAudioDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("screen_share_screen")
    ) {
        EnterpriseHeader(
            title = if (isAdmin) "Remote Screen Viewer" else "Screen Share Transmitter",
            subtitle = if (isAdmin) "Viewing Live Screen from Managed Device" else "Broadcasting Device Screen via WebRTC",
            onBackClick = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Live Status Header Bar with Focus Mode (DND) Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(EmeraldOnline.copy(alpha = 0.15f))
                        .border(1.dp, EmeraldOnline, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isSharingActive) "● Live (60 FPS • ${connectionLatencyMs}ms)" else "● Standby",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldOnline
                    )
                }

                // Focus Mode (DND) quick indicator badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (focusModeActive) EmeraldOnline.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f))
                        .border(1.dp, if (focusModeActive) EmeraldOnline else DarkBorder, RoundedCornerShape(8.dp))
                        .clickable { onToggleFocusMode(!focusModeActive) }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("focus_mode_badge")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (focusModeActive) Icons.Default.DoNotDisturbOn else Icons.Default.NotificationsActive,
                            contentDescription = "DND Status",
                            tint = if (focusModeActive) EmeraldOnline else TextMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (focusModeActive) "FOCUS: DND ACTIVE" else "DND OFF",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (focusModeActive) EmeraldOnline else TextMuted
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = streamResolution,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Screen Mirror Container
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0F172A))
                    .border(2.dp, if (isSharingActive) NexusBluePrimary else MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isSharingActive) {
                    SimulatedLivePhoneScreen()

                    // Right Vertical Tool Overlay (Fullscreen, Audio, Record, Lock)
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OverlayIconButton(icon = Icons.Default.Fullscreen, label = "Fullscreen")
                        OverlayIconButton(
                            icon = Icons.Default.VolumeUp,
                            label = "Audio",
                            onClick = { showAudioDialog = true }
                        )
                        OverlayIconButton(icon = Icons.Default.Videocam, label = "Record")
                        OverlayIconButton(icon = Icons.Default.Lock, label = "Lock")
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.ScreenShare, contentDescription = "Standby", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Stream Standby", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Text(text = "Tap Start Session to launch display stream", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Focus Mode (Do-Not-Disturb) session banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (focusModeActive) EmeraldOnline.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface)
                    .border(
                        1.dp,
                        if (focusModeActive) EmeraldOnline.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("session_focus_mode_banner")
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
                        Icon(
                            imageVector = if (focusModeActive) Icons.Default.DoNotDisturbOn else Icons.Default.NotificationsOff,
                            contentDescription = "Focus Mode",
                            tint = if (focusModeActive) EmeraldOnline else NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Silence Notifications (Focus Mode)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (focusModeActive) EmeraldOnline else TextWhite
                            )
                            Text(
                                text = if (focusModeActive) "Active: Popups & alerts suppressed during session" else "Tap switch to silence remote notifications",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }

                    Switch(
                        checked = focusModeActive,
                        onCheckedChange = { onToggleFocusMode(it) },
                        modifier = Modifier.testTag("screen_share_focus_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmeraldOnline,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkBorder
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Remote Control Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ControlActionTile(
                    icon = Icons.Default.VolumeUp,
                    label = "Volumes",
                    onClick = { showAudioDialog = true },
                    testTag = "action_volume_tile"
                )
                ControlActionTile(
                    icon = if (focusModeActive) Icons.Default.DoNotDisturbOn else Icons.Default.NotificationsActive,
                    label = if (focusModeActive) "DND On" else "DND Off",
                    onClick = { onToggleFocusMode(!focusModeActive) },
                    testTag = "action_dnd_tile"
                )
                ControlActionTile(
                    icon = Icons.Default.TouchApp,
                    label = "Control",
                    testTag = "action_control_tile"
                )
                ControlActionTile(
                    icon = Icons.Default.Screenshot,
                    label = "Screenshot",
                    testTag = "action_screenshot_tile"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!isSharingActive) {
                PrimaryNexusButton(
                    text = if (isAdmin) "Request Live Screen Stream" else "Start Sharing Screen",
                    onClick = onStartStream,
                    icon = Icons.Default.PlayArrow,
                    testTag = "start_stream_button"
                )
            } else {
                SecondaryNexusButton(
                    text = if (isAdmin) "Disconnect Remote Screen" else "End Broadcast",
                    onClick = onStopStream,
                    icon = Icons.Default.Block,
                    color = CrimsonStop,
                    testTag = "stop_stream_button"
                )
            }
        }
    }

    if (showAudioDialog) {
        RemoteAudioControlDialog(
            initialFocusMode = focusModeActive,
            initialRingerVolume = ringerVolume,
            initialMediaVolume = mediaVolume,
            initialAlarmVolume = alarmVolume,
            onDismissRequest = { showAudioDialog = false },
            onApplyChanges = { newFocus, newRing, newMedia, newAlarm ->
                if (newFocus != focusModeActive) {
                    onToggleFocusMode(newFocus)
                }
                onUpdateVolumes(newRing, newMedia, newAlarm)
            }
        )
    }
}

@Composable
private fun OverlayIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ControlActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit = {},
    testTag: String = ""
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(NexusBluePrimary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = NexusBluePrimary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SimulatedLivePhoneScreen() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        drawRect(color = Color(0xFF0F172A))
        drawRect(color = Color(0xFF1E293B), topLeft = Offset(0f, 0f), size = Size(width, 40f))

        val iconSize = width / 5
        val padding = width / 8
        for (row in 0..3) {
            for (col in 0..2) {
                val x = padding + col * (iconSize + 20f)
                val y = 100f + row * (iconSize + 30f)
                drawRoundRect(
                    color = when ((row + col) % 3) {
                        0 -> NexusBluePrimary
                        1 -> NexusCyanAccent
                        else -> EmeraldOnline
                    },
                    topLeft = Offset(x, y),
                    size = Size(iconSize, iconSize),
                    cornerRadius = CornerRadius(16f, 16f)
                )
            }
        }
    }
}
