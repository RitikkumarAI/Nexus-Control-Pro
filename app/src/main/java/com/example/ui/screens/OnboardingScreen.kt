package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PrimaryNexusButton
import com.example.ui.theme.NexusBluePrimary
import com.example.ui.theme.NexusCyanAccent

@Composable
fun OnboardingScreen(
    onSkip: () -> Unit,
    onGetStarted: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .testTag("onboarding_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Skip Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onSkip, modifier = Modifier.testTag("skip_button")) {
                Text(
                    text = "Skip",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Center Illustration
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                DualPhoneIllustration()
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Monitor & Manage\nYour Devices",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                lineHeight = 30.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Remote access, device monitoring, file management and more — all in one secure app.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Page Indicator Dots
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(NexusBluePrimary)
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outline)
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outline)
                )
            }
        }

        // Bottom Button
        PrimaryNexusButton(
            text = "Get Started",
            onClick = onGetStarted,
            icon = Icons.AutoMirrored.Filled.ArrowForward,
            testTag = "get_started_button"
        )
    }
}

@Composable
private fun DualPhoneIllustration() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Left Phone
        drawRoundRect(
            color = Color(0xFF1E293B),
            topLeft = Offset(width * 0.18f, height * 0.22f),
            size = Size(width * 0.34f, height * 0.58f),
            cornerRadius = CornerRadius(16f, 16f)
        )
        drawRoundRect(
            color = Color(0xFF0F172A),
            topLeft = Offset(width * 0.20f, height * 0.25f),
            size = Size(width * 0.30f, height * 0.52f),
            cornerRadius = CornerRadius(12f, 12f)
        )

        // Right Phone
        drawRoundRect(
            color = Color(0xFF1E293B),
            topLeft = Offset(width * 0.48f, height * 0.18f),
            size = Size(width * 0.34f, height * 0.58f),
            cornerRadius = CornerRadius(16f, 16f)
        )
        drawRoundRect(
            color = Color(0xFF0F172A),
            topLeft = Offset(width * 0.50f, height * 0.21f),
            size = Size(width * 0.30f, height * 0.52f),
            cornerRadius = CornerRadius(12f, 12f)
        )

        // Center Link Circle
        drawCircle(
            color = NexusBluePrimary,
            radius = 32f,
            center = Offset(width * 0.50f, height * 0.48f)
        )
        drawCircle(
            color = NexusCyanAccent,
            radius = 20f,
            center = Offset(width * 0.50f, height * 0.48f)
        )
    }
}
