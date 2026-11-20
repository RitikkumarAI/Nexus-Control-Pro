package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import kotlin.random.Random

@Composable
fun QRCodeView(
    qrCodeContent: String,
    modifier: Modifier = Modifier,
    sizeDp: Int = 220
) {
    val seed = qrCodeContent.hashCode().toLong()
    val random = Random(seed)

    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(surfaceColor)
            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size((sizeDp - 32).dp)) {
            val width = size.width
            val height = size.height
            val matrixSize = 21
            val cellSize = width / matrixSize

            // Outer positioning targets (top-left, top-right, bottom-left)
            drawCornerTarget(0, 0, cellSize, primaryColor, surfaceColor)
            drawCornerTarget(matrixSize - 7, 0, cellSize, primaryColor, surfaceColor)
            drawCornerTarget(0, matrixSize - 7, cellSize, primaryColor, surfaceColor)

            // Data matrix fill
            for (row in 0 until matrixSize) {
                for (col in 0 until matrixSize) {
                    val isCornerTarget = (row < 7 && col < 7) ||
                            (row < 7 && col >= matrixSize - 7) ||
                            (row >= matrixSize - 7 && col < 7)
                    if (isCornerTarget) continue

                    val isFilled = random.nextBoolean()
                    if (isFilled) {
                        drawRoundRect(
                            color = primaryColor,
                            topLeft = Offset(col * cellSize + 1f, row * cellSize + 1f),
                            size = Size(cellSize - 2f, cellSize - 2f),
                            cornerRadius = CornerRadius(2f, 2f)
                        )
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCornerTarget(
    startCol: Int,
    startRow: Int,
    cellSize: Float,
    primaryColor: androidx.compose.ui.graphics.Color,
    surfaceColor: androidx.compose.ui.graphics.Color
) {
    val x = startCol * cellSize
    val y = startRow * cellSize
    val sizePx = 7 * cellSize

    // Outer box
    drawRoundRect(
        color = primaryColor,
        topLeft = Offset(x, y),
        size = Size(sizePx, sizePx),
        cornerRadius = CornerRadius(6f, 6f)
    )

    // Inner cutout
    val cutMargin = cellSize
    drawRoundRect(
        color = surfaceColor,
        topLeft = Offset(x + cutMargin, y + cutMargin),
        size = Size(sizePx - 2 * cutMargin, sizePx - 2 * cutMargin),
        cornerRadius = CornerRadius(4f, 4f)
    )

    // Center solid block
    val centerMargin = 2 * cellSize
    drawRoundRect(
        color = primaryColor,
        topLeft = Offset(x + centerMargin, y + centerMargin),
        size = Size(sizePx - 2 * centerMargin, sizePx - 2 * centerMargin),
        cornerRadius = CornerRadius(3f, 3f)
    )
}
