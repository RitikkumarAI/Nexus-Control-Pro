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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.EnterpriseHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.viewmodel.TerminalConsoleLog
import com.example.ui.viewmodel.TerminalViewModel

@Composable
fun RemoteTerminalScreen(
    viewModel: TerminalViewModel,
    onBack: () -> Unit
) {
    val consoleLogs by viewModel.consoleLogs.collectAsStateWithLifecycle()
    val commandInput by viewModel.commandInput.collectAsStateWithLifecycle()
    val isExecuting by viewModel.isExecuting.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid)
            .testTag("remote_terminal_screen")
    ) {
        EnterpriseHeader(
            title = "REMOTE TERMINAL CONSOLE",
            subtitle = "Encrypted Shell & ADB Administrative CLI",
            onBackClick = onBack,
            badgeText = "E2EE ENCRYPTED",
            badgeColor = EmeraldOnline
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Terminal Console Window
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0A0E17))
                    .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(consoleLogs) { log ->
                        TerminalLogItem(log = log)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Command Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commandInput,
                    onValueChange = viewModel::updateCommandInput,
                    placeholder = {
                        Text(
                            text = "Enter shell command (e.g. getprop, ps, uptime)...",
                            fontSize = 12.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("terminal_command_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        viewModel.sendCommand()
                    }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { viewModel.sendCommand() },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonCyan)
                        .testTag("terminal_send_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Execute",
                        tint = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun TerminalLogItem(log: TerminalConsoleLog) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "nexus@android:~# ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = log.command,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    fontFamily = FontFamily.Monospace
                )
            }

            StatusBadge(
                text = log.timestampText,
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = log.output,
            fontSize = 11.sp,
            color = if (log.isError) Color(0xFFFF5252) else Color(0xFFC0CAF5),
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}
