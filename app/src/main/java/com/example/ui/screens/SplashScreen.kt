package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserRole
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    currentRole: UserRole,
    onNavigateToRoleSelection: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToManaged: () -> Unit
) {
    LaunchedEffect(currentRole) {
        delay(1200)
        when (currentRole) {
            UserRole.ADMIN -> onNavigateToAdmin()
            UserRole.MANAGED -> onNavigateToManaged()
            UserRole.UNSET -> onNavigateToRoleSelection()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid)
            .padding(24.dp)
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Nexus Control Pro Shield",
                tint = NeonCyan,
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "NEXUS CONTROL PRO",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Secure Android Remote Device Management",
                fontSize = 13.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = NeonCyan,
                strokeWidth = 3.dp
            )
        }
    }
}
