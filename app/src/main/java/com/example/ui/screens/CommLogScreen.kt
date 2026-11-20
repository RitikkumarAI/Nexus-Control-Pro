package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material.icons.filled.PhoneMissed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import com.example.model.CallHistoryItem
import com.example.ui.components.EnterpriseHeader
import com.example.ui.theme.CarbonGlass
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.ElectricCobalt
import com.example.ui.theme.EmeraldSignal
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun CommLogScreen(
    callLogs: List<CallHistoryItem>,
    filterType: String,
    isLoading: Boolean,
    onFilterSelect: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid)
            .testTag("comm_log_screen")
    ) {
        EnterpriseHeader(
            title = "CALL & COMMUNICATION LOGS",
            subtitle = "Call History Telemetry",
            onBackClick = onBack,
            badgeText = "${callLogs.size} CALL RECORDS"
        )

        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                CommFilterTab(text = "ALL", active = filterType == "ALL", onClick = { onFilterSelect("ALL") }, modifier = Modifier.weight(1f))
                CommFilterTab(text = "INCOMING", active = filterType == "INCOMING", onClick = { onFilterSelect("INCOMING") }, modifier = Modifier.weight(1f))
                CommFilterTab(text = "OUTGOING", active = filterType == "OUTGOING", onClick = { onFilterSelect("OUTGOING") }, modifier = Modifier.weight(1f))
                CommFilterTab(text = "MISSED", active = filterType == "MISSED", onClick = { onFilterSelect("MISSED") }, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonCyan)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(callLogs, key = { it.id }) { item ->
                        CallLogRowItem(item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun CommFilterTab(text: String, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) NeonCyan else CarbonGlass)
            .border(1.dp, if (active) NeonCyan else DarkBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) ObsidianVoid else TextWhite
        )
    }
}

@Composable
private fun CallLogRowItem(item: CallHistoryItem) {
    val (icon, color) = when (item.callType) {
        "INCOMING" -> Pair(Icons.AutoMirrored.Filled.CallReceived, EmeraldSignal)
        "OUTGOING" -> Pair(Icons.AutoMirrored.Filled.CallMade, ElectricCobalt)
        else -> Pair(Icons.Default.PhoneMissed, CrimsonAlert)
    }

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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = item.callType, tint = color, modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = item.name ?: item.number,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Text(
                        text = if (item.name != null) item.number else item.callType,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${item.durationSeconds}s",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = "Today",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }
    }
}
