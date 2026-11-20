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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppItem
import com.example.ui.components.EnterpriseHeader
import com.example.ui.theme.CarbonGlass
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.ElectricCobalt
import com.example.ui.theme.EmeraldSignal
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun AppManagerScreen(
    appList: List<AppItem>,
    searchQuery: String,
    isLoading: Boolean,
    hasUsageAccess: Boolean,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid)
            .testTag("app_manager_screen")
    ) {
        EnterpriseHeader(
            title = "APPLICATION MANAGER",
            subtitle = "Package Visibility & Usage Analytics",
            onBackClick = onBack,
            badgeText = "${appList.size} APPS LISTED"
        )

        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_search_input"),
                placeholder = { Text("Search installed applications...", color = TextMuted) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = NeonCyan) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = DarkBorder,
                    focusedContainerColor = CarbonGlass,
                    unfocusedContainerColor = CarbonGlass
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonCyan)
                }
            } else if (appList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "No matching applications found", color = TextMuted)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(appList, key = { it.packageName }) { app ->
                        AppRowItem(app = app)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRowItem(app: AppItem) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CarbonGlass)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (app.isSystemApp) ElectricCobalt.copy(alpha = 0.2f) else NeonCyan.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (app.isSystemApp) Icons.Default.Android else Icons.Default.Apps,
                    contentDescription = app.label,
                    tint = if (app.isSystemApp) ElectricCobalt else NeonCyan,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.label,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    if (app.isSystemApp) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(DarkBorder)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(text = "SYSTEM", fontSize = 9.sp, color = TextMuted)
                        }
                    }
                }

                Text(
                    text = app.packageName,
                    fontSize = 11.sp,
                    color = TextMuted
                )

                if (app.usageTimeMillisMs > 0) {
                    Text(
                        text = "24h Usage: ${app.usageTimeMillisMs / (1000 * 60)} mins",
                        fontSize = 11.sp,
                        color = EmeraldSignal,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                text = "v${app.versionName}",
                fontSize = 12.sp,
                color = TextMuted
            )
        }
    }
}
