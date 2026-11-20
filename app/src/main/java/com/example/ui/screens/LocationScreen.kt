package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DeviceLocation
import com.example.ui.components.EnterpriseHeader
import com.example.ui.components.MetricCard
import com.example.ui.theme.CarbonGlass
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.EmeraldSignal
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun LocationScreen(
    locationState: DeviceLocation?,
    isContinuousTrackingActive: Boolean,
    isLoading: Boolean,
    onRefreshLocation: () -> Unit,
    onToggleTracking: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val loc = locationState ?: DeviceLocation(
        latitude = 37.7749,
        longitude = -122.4194,
        accuracyMeters = 5.2f,
        timestamp = System.currentTimeMillis(),
        provider = "GPS Hardware"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid)
            .testTag("location_screen")
    ) {
        EnterpriseHeader(
            title = "GPS LOCATION TELEMETRY",
            subtitle = "FusedLocationProvider & Map Rendering",
            onBackClick = onBack,
            badgeText = if (isContinuousTrackingActive) "TRACKING ON" else "STANDBY",
            badgeColor = if (isContinuousTrackingActive) EmeraldSignal else NeonCyan
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Simulated Map Box Viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(CarbonGlass)
                    .border(1.5.dp, NeonCyan, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                SimulatedMapGridCanvas()

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianVoid.copy(alpha = 0.85f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "ACCURACY: ±${loc.accuracyMeters} m • ${loc.provider}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldSignal
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Location Metrics Grid
            Row(modifier = Modifier.fillMaxWidth()) {
                MetricCard(
                    title = "Latitude",
                    value = String.format("%.5f°", loc.latitude),
                    subtitle = "North",
                    icon = Icons.Default.MyLocation,
                    accentColor = NeonCyan,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                MetricCard(
                    title = "Longitude",
                    value = String.format("%.5f°", loc.longitude),
                    subtitle = "West",
                    icon = Icons.Default.MyLocation,
                    accentColor = EmeraldSignal,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Continuous Tracking Foreground Service Toggle
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Continuous Foreground Service Tracking",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "Maintains location sync in background with persistent status notification",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }

                    Switch(
                        checked = isContinuousTrackingActive,
                        onCheckedChange = onToggleTracking,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianVoid,
                            checkedTrackColor = NeonCyan,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkBorder
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SimulatedMapGridCanvas() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        drawRect(color = Color(0xFF0F172A))

        // Grid lines
        for (i in 1..8) {
            val x = i * (width / 8)
            drawLine(color = Color(0xFF1E293B), start = Offset(x, 0f), end = Offset(x, height), strokeWidth = 1f)
        }
        for (j in 1..6) {
            val y = j * (height / 6)
            drawLine(color = Color(0xFF1E293B), start = Offset(0f, y), end = Offset(width, y), strokeWidth = 1f)
        }

        // Radar target pulse
        drawCircle(color = NeonCyan.copy(alpha = 0.2f), radius = 60f, center = Offset(width / 2, height / 2))
        drawCircle(color = NeonCyan.copy(alpha = 0.4f), radius = 35f, center = Offset(width / 2, height / 2))
        drawCircle(color = NeonCyan, radius = 10f, center = Offset(width / 2, height / 2))
    }
}
