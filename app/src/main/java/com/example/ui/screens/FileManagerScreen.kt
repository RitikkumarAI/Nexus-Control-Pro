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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.model.FileNode
import com.example.ui.components.EnterpriseHeader
import com.example.ui.components.PrimaryNexusButton
import com.example.ui.theme.CarbonGlass
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.ElectricCobalt
import com.example.ui.theme.EmeraldSignal
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun FileManagerScreen(
    currentPath: String,
    fileList: List<FileNode>,
    isLoading: Boolean,
    selectedFile: FileNode?,
    transferStatus: String?,
    onSelectFile: (FileNode) -> Unit,
    onDownloadFile: (FileNode) -> Unit,
    onDismissSelected: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid)
            .testTag("file_manager_screen")
    ) {
        EnterpriseHeader(
            title = "FILE EXPLORER",
            subtitle = currentPath,
            onBackClick = onBack,
            badgeText = "SCOPED SAF"
        )

        Column(modifier = Modifier.padding(16.dp)) {
            if (transferStatus != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(EmeraldSignal.copy(alpha = 0.15f))
                        .border(1.dp, EmeraldSignal, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Text(text = transferStatus, fontSize = 12.sp, color = EmeraldSignal, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonCyan)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(fileList, key = { it.path }) { node ->
                        FileRowItem(
                            node = node,
                            onClick = { onSelectFile(node) },
                            onDownload = { onDownloadFile(node) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileRowItem(
    node: FileNode,
    onClick: () -> Unit,
    onDownload: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CarbonGlass)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (node.isDirectory) ElectricCobalt.copy(alpha = 0.2f) else NeonCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (node.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                        contentDescription = node.name,
                        tint = if (node.isDirectory) ElectricCobalt else NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = node.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Text(
                        text = if (node.isDirectory) "Directory" else "${String.format("%.2f KB", node.sizeBytes / 1024.0)} • ${node.mimeType}",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            if (!node.isDirectory) {
                IconButton(onClick = onDownload, modifier = Modifier.testTag("download_file_${node.name}")) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = "Download", tint = NeonCyan)
                }
            }
        }
    }
}
