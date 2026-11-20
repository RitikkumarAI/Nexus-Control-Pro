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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.EmeraldOnline

@Composable
fun RemoteAudioControlDialog(
    initialFocusMode: Boolean,
    initialRingerVolume: Int,
    initialMediaVolume: Int,
    initialAlarmVolume: Int,
    onDismissRequest: () -> Unit,
    onApplyChanges: (focusMode: Boolean, ringer: Int, media: Int, alarm: Int) -> Unit
) {
    var focusMode by remember { mutableStateOf(initialFocusMode) }
    var ringerVolume by remember { mutableFloatStateOf(initialRingerVolume.toFloat()) }
    var mediaVolume by remember { mutableFloatStateOf(initialMediaVolume.toFloat()) }
    var alarmVolume by remember { mutableFloatStateOf(initialAlarmVolume.toFloat()) }

    Dialog(onDismissRequest = onDismissRequest) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
                .padding(20.dp)
                .testTag("remote_audio_control_dialog")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Audio Control",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "AUDIO & FOCUS MODE",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Remote DND & AudioManager Sync",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(32.dp).testTag("close_audio_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Focus Mode (Do-Not-Disturb) Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (focusMode) EmeraldOnline.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            1.dp,
                            if (focusMode) EmeraldOnline.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(14.dp)
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
                                imageVector = if (focusMode) Icons.Default.DoNotDisturbOn else Icons.Default.NotificationsActive,
                                contentDescription = "DND Status",
                                tint = if (focusMode) EmeraldOnline else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (focusMode) "Focus Mode Active (DND)" else "Focus Mode Off",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (focusMode) EmeraldOnline else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Silences notifications via NotificationManager",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = focusMode,
                            onCheckedChange = { focusMode = it },
                            modifier = Modifier.testTag("focus_mode_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldOnline,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "REMOTE STREAM VOLUMES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Ringer Volume Slider
                VolumeSliderRow(
                    label = "Ringer & Notifications",
                    value = ringerVolume,
                    icon = Icons.Default.NotificationsActive,
                    onValueChange = { ringerVolume = it },
                    testTag = "ringer_volume_slider"
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Media Volume Slider
                VolumeSliderRow(
                    label = "Media & Audio Playback",
                    value = mediaVolume,
                    icon = Icons.Default.VolumeUp,
                    onValueChange = { mediaVolume = it },
                    testTag = "media_volume_slider"
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Alarm Volume Slider
                VolumeSliderRow(
                    label = "Alarm & Timers",
                    value = alarmVolume,
                    icon = Icons.Default.Alarm,
                    onValueChange = { alarmVolume = it },
                    testTag = "alarm_volume_slider"
                )

                Spacer(modifier = Modifier.height(20.dp))

                PrimaryNexusButton(
                    text = "Apply Audio Configuration",
                    onClick = {
                        onApplyChanges(
                            focusMode,
                            ringerVolume.toInt(),
                            mediaVolume.toInt(),
                            alarmVolume.toInt()
                        )
                        onDismissRequest()
                    },
                    icon = Icons.Default.VolumeUp,
                    testTag = "apply_audio_settings_button"
                )
            }
        }
    }
}

@Composable
private fun VolumeSliderRow(
    label: String,
    value: Float,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onValueChange: (Float) -> Unit,
    testTag: String
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = primaryColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = onSurfaceColor
                )
            }

            Text(
                text = "${value.toInt()}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (value.toInt() == 0) Color(0xFFEF4444) else primaryColor
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..100f,
            modifier = Modifier.fillMaxWidth().testTag(testTag),
            colors = SliderDefaults.colors(
                thumbColor = primaryColor,
                activeTrackColor = primaryColor,
                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
    }
}
