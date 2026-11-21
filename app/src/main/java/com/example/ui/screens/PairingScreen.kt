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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DevicePairing
import com.example.model.PairingStatus
import com.example.model.UserRole
import com.example.ui.components.EnterpriseHeader
import com.example.ui.components.PrimaryNexusButton
import com.example.ui.components.QRCodeView
import com.example.ui.components.SecondaryNexusButton
import com.example.ui.theme.CrimsonStop
import com.example.ui.theme.NexusBluePrimary

@Composable
fun PairingScreen(
    userRole: UserRole,
    currentDeviceId: String,
    currentDeviceName: String,
    pairingState: DevicePairing?,
    inputCode: String,
    pairingMessage: String?,
    isLoading: Boolean,
    onUpdateCode: (String) -> Unit,
    onGenerateCode: () -> Unit,
    onSubmitCode: () -> Unit,
    onConfirmPairing: (Boolean) -> Unit,
    onBack: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onScanQrCode: (String) -> Unit = {}
) {
    var activeTab by remember { mutableStateOf("QR") }
    var showQrScanner by remember { mutableStateOf(false) }

    LaunchedEffect(pairingState?.status) {
        if (pairingState?.status == PairingStatus.PAIRED.name) {
            onNavigateToDashboard()
        }
    }

    LaunchedEffect(Unit) {
        if (pairingState == null && userRole == UserRole.MANAGED) {
            onGenerateCode()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("pairing_screen")
    ) {
        EnterpriseHeader(
            title = "Pair Device",
            subtitle = if (userRole == UserRole.MANAGED) "Phone A Pairing Host" else "Phone B Admin Pairing",
            onBackClick = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Segmented Toggle Pill [ QR Code ]  [ Pairing Code ]
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (activeTab == "QR") NexusBluePrimary else Color.Transparent)
                        .clickable { activeTab = "QR" }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "QR Code",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activeTab == "QR") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (activeTab == "PIN") NexusBluePrimary else Color.Transparent)
                        .clickable { activeTab = "PIN" }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Pairing Code",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activeTab == "PIN") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val currentCode = pairingState?.pairingCode ?: "749268"

            if (activeTab == "QR") {
                if (userRole == UserRole.ADMIN) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Camera Scanner",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Camera QR Scanner",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Point this Admin phone's camera at the QR code displayed on the Managed Phone (Phone A).",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            PrimaryNexusButton(
                                text = "Open Camera Scanner",
                                onClick = { showQrScanner = true },
                                icon = Icons.Default.CameraAlt
                            )
                        }
                    }
                } else {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val p2p = remember { com.example.data.NexusP2PManager.getInstance(context) }
                    val localIp = remember { p2p.getLocalIpAddress() }

                    val qrJson = remember(currentCode, localIp) {
                        org.json.JSONObject().apply {
                            put("protocol", "NEXUS_MDM_V1")
                            put("deviceId", currentDeviceId)
                            put("deviceName", currentDeviceName)
                            put("ip", localIp)
                            put("port", com.example.data.NexusP2PManager.DEFAULT_PORT)
                            put("code", currentCode)
                        }.toString()
                    }

                    QRCodeView(qrCodeContent = qrJson)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Scan this QR with Admin Device (+ icon or Camera)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Local IP: $localIp:8899",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = "Enter 6-Digit Pairing Code",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = inputCode,
                    onValueChange = onUpdateCode,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .testTag("pairing_code_input"),
                    placeholder = { Text("7 4 9 2 6 8", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 20.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                PrimaryNexusButton(
                    text = if (isLoading) "Verifying..." else "Connect & Pair",
                    onClick = onSubmitCode,
                    enabled = inputCode.length == 6 && !isLoading
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Or share pairing code",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Digit Box PIN Display `7  4  9  2  6  8  📋`
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                currentCode.take(6).padEnd(6, '0').forEach { digit ->
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = digit.toString(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { /* Copy code */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Code",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Valid for 10:00 minutes",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(28.dp))

            SecondaryNexusButton(
                text = "Regenerate Code",
                onClick = onGenerateCode,
                icon = Icons.Default.Refresh,
                testTag = "regenerate_code_button"
            )

            if (pairingState?.status == PairingStatus.CONNECTING.name && userRole == UserRole.MANAGED) {
                Spacer(modifier = Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.5.dp, NexusBluePrimary, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "PAIRING REQUEST FROM ADMIN",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = NexusBluePrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Administrator '${pairingState.adminDeviceName}' is requesting full device management access.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            SecondaryNexusButton(
                                text = "Reject",
                                onClick = { onConfirmPairing(false) },
                                color = CrimsonStop,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            PrimaryNexusButton(
                                text = "Authorize",
                                onClick = { onConfirmPairing(true) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showQrScanner) {
        com.example.ui.components.QRScannerDialog(
            onDismiss = { showQrScanner = false },
            onQrScanned = { scannedCode ->
                showQrScanner = false
                onScanQrCode(scannedCode)
            }
        )
    }
}
