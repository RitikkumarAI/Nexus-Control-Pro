package com.example.ui.components

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
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldOnline

data class DailyBatteryPoint(
    val dayLabel: String,
    val batteryPct: Int // 0 to 100
)

@Composable
fun BatteryTrendChart(
    dailyData: List<DailyBatteryPoint> = listOf(
        DailyBatteryPoint("Mon", 92),
        DailyBatteryPoint("Tue", 85),
        DailyBatteryPoint("Wed", 78),
        DailyBatteryPoint("Thu", 88),
        DailyBatteryPoint("Fri", 70),
        DailyBatteryPoint("Sat", 82),
        DailyBatteryPoint("Sun", 87)
    ),
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column {
            // Chart Title
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
                            .background(EmeraldOnline.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BatteryChargingFull,
                            contentDescription = "Battery Trend",
                            tint = EmeraldOnline,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "7-DAY BATTERY TRENDS",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = onSurfaceColor
                        )
                        Text(
                            text = "Historical drain curve (Room DB)",
                            fontSize = 11.sp,
                            color = onSurfaceVariantColor
                        )
                    }
                }

                Text(
                    text = "Avg: 82%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldOnline
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Native Jetpack Compose Canvas Line Chart
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val width = size.width
                val height = size.height
                val pointsCount = dailyData.size
                if (pointsCount < 2) return@Canvas

                val stepX = width / (pointsCount - 1)

                val points = dailyData.mapIndexed { index, data ->
                    val x = index * stepX
                    val y = height - (height * (data.batteryPct / 100f))
                    Offset(x, y)
                }

                // Smooth Path Creation
                val strokePath = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    for (i in 0 until points.size - 1) {
                        val p1 = points[i]
                        val p2 = points[i + 1]
                        val controlPoint1 = Offset((p1.x + p2.x) / 2f, p1.y)
                        val controlPoint2 = Offset((p1.x + p2.x) / 2f, p2.y)
                        cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p2.x, p2.y)
                    }
                }

                // Gradient Fill Path
                val fillPath = Path().apply {
                    addPath(strokePath)
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }

                // Draw Area Gradient
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )

                // Draw Curve Line
                drawPath(
                    path = strokePath,
                    color = primaryColor,
                    style = Stroke(width = 3.dp.toPx())
                )

                // Draw Data Point Circles
                points.forEach { pt ->
                    drawCircle(
                        color = primaryColor,
                        radius = 4.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = onSurfaceColor,
                        radius = 2.dp.toPx(),
                        center = pt
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Axis X Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dailyData.forEach { data ->
                    Text(
                        text = data.dayLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = onSurfaceVariantColor
                    )
                }
            }
        }
    }
}
