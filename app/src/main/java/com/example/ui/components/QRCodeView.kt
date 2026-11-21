package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun QRCodeView(
    qrCodeContent: String,
    modifier: Modifier = Modifier,
    sizeDp: Int = 220
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    val qrBitmapState = produceState<Bitmap?>(initialValue = null, key1 = qrCodeContent) {
        value = withContext(Dispatchers.Default) {
            generateQrBitmap(
                content = qrCodeContent,
                sizePx = sizeDp * 2,
                foregroundColor = primaryColor.toArgb(),
                backgroundColor = android.graphics.Color.WHITE
            )
        }
    }

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = qrBitmapState.value
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Nexus Pairing QR Code",
                modifier = Modifier.size((sizeDp - 28).dp)
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun generateQrBitmap(
    content: String,
    sizePx: Int,
    foregroundColor: Int,
    backgroundColor: Int
): Bitmap? {
    return try {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1
        )
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) foregroundColor else backgroundColor
            }
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        bitmap
    } catch (e: Exception) {
        null
    }
}
