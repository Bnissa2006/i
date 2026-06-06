package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.DownloadItem
import com.example.data.model.DownloadStatus
import com.example.ui.MainViewModel
import com.example.ui.theme.Translation
import java.io.File

@Composable
fun FilesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val downloads by viewModel.allDownloadsState.collectAsState()
    val completedFiles = downloads.filter { it.status == DownloadStatus.COMPLETED && File(it.localPath).exists() }
    val lang = viewModel.language

    var selectedTab by remember { mutableIntStateOf(0) } // 0: All, 1: Videos, 2: Audios
    var activePlaybackMovie by remember { mutableStateOf<DownloadItem?>(null) }

    val filteredFiles = remember(completedFiles, selectedTab) {
        when (selectedTab) {
            1 -> completedFiles.filter { !it.isAudioOnly }
            2 -> completedFiles.filter { it.isAudioOnly }
            else -> completedFiles
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Elegant category tabs header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TabChip(
                    text = Translation.getString("tab_all", lang),
                    isSelected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                TabChip(
                    text = Translation.getString("tab_video", lang),
                    isSelected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                TabChip(
                    text = Translation.getString("tab_audio", lang),
                    isSelected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
            }

            if (filteredFiles.isEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "",
                        modifier = Modifier.size(72.dp),
                        tint = Color.Gray.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No saved files found",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    items(filteredFiles, key = { it.id }) { item ->
                        SavedFileRowItem(
                            item = item,
                            onClick = { activePlaybackMovie = item },
                            onDelete = { viewModel.removeDownload(item) }
                        )
                    }
                }
            }
        }

        // Active Player Layer Full-screen animation overlays
        if (activePlaybackMovie != null) {
            MediaPlayerOverlay(
                item = activePlaybackMovie!!,
                onClose = { activePlaybackMovie = null }
            )
        }
    }
}

@Composable
fun TabChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .height(36.dp)
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(18.dp)),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
fun SavedFileRowItem(
    item: DownloadItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val file = File(item.localPath)
    val sizeMB = if (file.exists()) {
        String.format("%.1f MB", file.length().toFloat() / (1024 * 1024))
    } else {
        "0 MB"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon / Thumbnail representation
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (item.thumbnail.isNotBlank()) {
                    AsyncImage(
                        model = item.thumbnail,
                        contentDescription = "Thumb",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = if (item.isAudioOnly) Icons.Default.MusicNote else Icons.Default.Movie,
                        contentDescription = "",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (item.isAudioOnly) "Audio | $sizeMB" else "Video | $sizeMB",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Delete completed file button
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.Red.copy(alpha = 0.65f)
                )
            }
        }
    }
}
