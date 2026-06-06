package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DownloadItem
import com.example.data.model.DownloadStatus
import com.example.ui.MainViewModel
import com.example.ui.theme.Translation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    viewModel: MainViewModel,
    lang: String,
    modifier: Modifier = Modifier
) {
    val downloads by viewModel.allDownloads.collectAsState()
    var selectedTabState by remember { mutableIntStateOf(0) } // 0 = Downloading, 1 = Completed

    val activeDownloads = downloads.filter {
        it.status == DownloadStatus.DOWNLOADING ||
                it.status == DownloadStatus.PAUSED ||
                it.status == DownloadStatus.PENDING ||
                it.status == DownloadStatus.FAILED
    }
    val completedDownloads = downloads.filter { it.status == DownloadStatus.COMPLETED }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Tab Headers using Material 3 TabRow
        TabRow(
            selectedTabIndex = selectedTabState,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTabState == 0,
                onClick = { selectedTabState = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(Translation.getString("downloading_header", lang), fontWeight = FontWeight.Bold)
                        if (activeDownloads.isNotEmpty()) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(start = 6.dp)
                            ) {
                                Text(activeDownloads.size.toString())
                            }
                        }
                    }
                },
                modifier = Modifier.testTag("tab_downloading")
            )
            Tab(
                selected = selectedTabState == 1,
                onClick = { selectedTabState = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(Translation.getString("completed_header", lang), fontWeight = FontWeight.Bold)
                        if (completedDownloads.isNotEmpty()) {
                            Badge(
                                containerColor = Color.Gray,
                                contentColor = Color.White,
                                modifier = Modifier.padding(start = 6.dp)
                            ) {
                                Text(completedDownloads.size.toString())
                            }
                        }
                    }
                },
                modifier = Modifier.testTag("tab_completed")
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (selectedTabState == 0) {
                // Active Downloads List
                if (activeDownloads.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CloudQueue,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = Translation.getString("no_active_downloads", lang),
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(activeDownloads, key = { it.id }) { item ->
                            ActiveDownloadItemCard(item, viewModel, lang)
                        }
                    }
                }
            } else {
                // Completed Downloads List
                if (completedDownloads.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = Translation.getString("no_completed", lang),
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(completedDownloads, key = { it.id }) { item ->
                            CompletedDownloadItemCard(item, viewModel, lang)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveDownloadItemCard(
    item: DownloadItem,
    viewModel: MainViewModel,
    lang: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_download_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.isAudioOnly) Icons.Default.MusicNote else Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
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
                    Text(
                        text = "Quality: ${item.resolution}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                // Control Action Buttons
                when (item.status) {
                    DownloadStatus.DOWNLOADING -> {
                        IconButton(onClick = { viewModel.downloadManager.pauseDownload(item.id) }) {
                            Icon(imageVector = Icons.Default.Pause, contentDescription = "Pause", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    DownloadStatus.PAUSED -> {
                        IconButton(onClick = { viewModel.downloadManager.resumeDownload(item.id) }) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Resume", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    DownloadStatus.FAILED -> {
                        IconButton(onClick = { viewModel.downloadManager.retryDownload(item.id) }) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retry", tint = Color.Red)
                        }
                    }
                    else -> {}
                }

                IconButton(onClick = { viewModel.downloadManager.cancelDownload(item.id) }) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Percentage Loader
            val percentVal = (item.progress * 100).toInt()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${formatBytes(item.downloadedBytes)} / ${formatBytes(item.sizeBytes)}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Text(
                    text = "$percentVal%",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { item.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (item.status == DownloadStatus.PAUSED) Color.Gray else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${Translation.getString("speed_lbl", lang)} ${item.downloadSpeed}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = item.status.name,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (item.status) {
                        DownloadStatus.FAILED -> Color.Red
                        DownloadStatus.PAUSED -> Color.Gray
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }
        }
    }
}

@Composable
fun CompletedDownloadItemCard(
    item: DownloadItem,
    viewModel: MainViewModel,
    lang: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.isAudioOnly) Icons.Default.MusicNote else Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
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
                Row {
                    Text(
                        text = item.resolution,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = formatBytes(item.sizeBytes),
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            IconButton(onClick = {
                if (item.isAudioOnly) {
                    viewModel.setPlayingAudio(item)
                } else {
                    viewModel.setPlayingVideo(item)
                }
            }) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = "Play Completed File",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

// Global bytes format utility
fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0.0 B"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.size - 1) {
        value /= 1024
        unitIndex++
    }
    return String.format("%.1f %s", value, units[unitIndex])
}
