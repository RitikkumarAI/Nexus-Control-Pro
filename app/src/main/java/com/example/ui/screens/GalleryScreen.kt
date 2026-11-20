package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.MediaItemData
import com.example.ui.components.EnterpriseHeader
import com.example.ui.components.SecondaryNexusButton
import com.example.ui.theme.CarbonGlass
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.ElectricCobalt
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun GalleryScreen(
    mediaList: List<MediaItemData>,
    filterType: String,
    isLoading: Boolean,
    selectedMedia: MediaItemData?,
    onFilterSelect: (String) -> Unit,
    onMediaSelect: (MediaItemData?) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid)
            .testTag("gallery_screen")
    ) {
        EnterpriseHeader(
            title = "AUTHORIZED GALLERY",
            subtitle = "MediaStore Photo & Video Explorer",
            onBackClick = onBack,
            badgeText = "${mediaList.size} ITEMS"
        )

        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterTabButton(text = "ALL", active = filterType == "ALL", onClick = { onFilterSelect("ALL") }, modifier = Modifier.weight(1f))
                FilterTabButton(text = "PHOTOS", active = filterType == "IMAGES", onClick = { onFilterSelect("IMAGES") }, modifier = Modifier.weight(1f))
                FilterTabButton(text = "VIDEOS", active = filterType == "VIDEOS", onClick = { onFilterSelect("VIDEOS") }, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonCyan)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(mediaList, key = { it.id }) { item ->
                        GalleryGridTile(item = item, onClick = { onMediaSelect(item) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterTabButton(text: String, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) NeonCyan else CarbonGlass)
            .border(1.dp, if (active) NeonCyan else DarkBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) ObsidianVoid else TextWhite
        )
    }
}

@Composable
private fun GalleryGridTile(item: MediaItemData, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(CarbonGlass)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.contentUri)
                .crossfade(true)
                .build(),
            contentDescription = item.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (item.isVideo) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = "Video",
                    tint = NeonCyan,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
