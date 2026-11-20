package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldOnline

@Composable
fun HourlyBatteryGaugeCard(
    hourlyDrainPercentage: Float = 3.2f, // -3.2% per hour
    currentBatteryPct: Int = 87,
    modifier: Modifier = Modifier
) {
    val animatedDrain by animateFloatAsState(
        targetValue = (hourlyDrainPercentage / 15f).coerceIn(0f, 1f), // Scale max 15% hourly drain
        animationSpec = tween(durationMillis = 1000),
        label = "GaugeAnimation"
    )

    val gaugeColor = when {
        hourlyDrainPercentage < 4.0f -> EmeraldOnline
        hourlyDrainPercentage < 8.0f -> Color(0xFFF59E0B) // Amber
        else -> Color(0xFFEF4444) // Red
    }

    val trackColor = MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(gaugeColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BatteryAlert,
                            contentDescription = "Battery Gauge",
                            tint = gaugeColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "1-HOUR BATTERY DRAIN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "-${String.format("%.1f", hourlyDrainPercentage)}% / hr",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = gaugeColor
                )

                Text(
                    text = "Current: $currentBatteryPct% • Room DB Telemetry",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Circular Arc Gauge Canvas
            Box(
                modifier = Modifier.size(76.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(70.dp)) {
                    val strokeWidth = 8.dp.toPx()

                    // Background Track Arc
                    drawArc(
                        color = trackColor,
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Active Gauge Arc
                    drawArc(
                        color = gaugeColor,
                        startAngle = 135f,
                        sweepAngle = 270f * animatedDrain,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Text(
                    text = "${hourlyDrainPercentage.toInt()}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
