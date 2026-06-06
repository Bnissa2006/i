package com.example.ui.screens

import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.model.DownloadItem
import com.example.data.model.DownloadStatus
import com.example.ui.MainViewModel
import com.example.ui.theme.Translation
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    viewModel: MainViewModel,
    lang: String,
    modifier: Modifier = Modifier
) {
    val downloads by viewModel.allDownloads.collectAsState()
    val searchQuery by viewModel.fileSearchQuery.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeCategoryFilter by remember { mutableStateOf("All") } // "All", "Videos", "Audios"
    var showRenameDialogItem by remember { mutableStateOf<DownloadItem?>(null) }
    var renameInputVal by remember { mutableStateOf("") }

    // Filter list based on type selection and active search queries
    val completedFilesList = downloads.filter {
        it.status == DownloadStatus.COMPLETED &&
                (activeCategoryFilter == "All" ||
                        (activeCategoryFilter == "Videos" && !it.isAudioOnly) ||
                        (activeCategoryFilter == "Audios" && it.isAudioOnly)) &&
                it.title.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { viewModel.updateFileSearchQuery(it) },
                placeholder = { Text(Translation.getString("search_files", lang), fontSize = 14.sp) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = CircleShape,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(max = 48.dp)
                    .testTag("file_search_bar"),
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search Files")
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.updateFileSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                }
            )
        }

        // Category Filter Chips Line
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Videos", "Audios").forEach { category ->
                val localizedLabel = when (category) {
                    "Videos" -> Translation.getString("video_category", lang)
                    "Audios" -> Translation.getString("audio_category", lang)
                    else -> if (lang == "Arabic") "الكل" else if (lang == "French") "Tout" else "All"
                }
                val isSelected = activeCategoryFilter == category
                FilterChip(
                    selected = isSelected,
                    onClick = { activeCategoryFilter = category },
                    label = { Text(localizedLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("filter_chip_$category")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Files container listing
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (completedFilesList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = Translation.getString("no_files", lang),
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(completedFilesList, key = { it.id }) { fileItem ->
                        var isExpandedOptionsRow by remember { mutableStateOf(false) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isExpandedOptionsRow = !isExpandedOptionsRow },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column {
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
                                            imageVector = if (fileItem.isAudioOnly) Icons.Default.AudioFile else Icons.Default.VideoFile,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = fileItem.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row {
                                            Text(
                                                text = fileItem.resolution,
                                                fontSize = 11.sp,
                                                color = Color.Gray,
                                                modifier = Modifier.padding(end = 8.dp)
                                            )
                                            Text(
                                                text = formatBytes(fileItem.sizeBytes),
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }

                                    IconButton(onClick = { isExpandedOptionsRow = !isExpandedOptionsRow }) {
                                        Icon(
                                            imageVector = if (isExpandedOptionsRow) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = "Show operations options"
                                        )
                                    }
                                }

                                // Interactive functional action drawer drawer row
                                if (isExpandedOptionsRow) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceAround
                                    ) {
                                        // Play action
                                        TextButton(
                                            onClick = {
                                                if (fileItem.isAudioOnly) {
                                                    viewModel.setPlayingAudio(fileItem)
                                                } else {
                                                    viewModel.setPlayingVideo(fileItem)
                                                }
                                            }
                                        ) {
                                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(Translation.getString("play_text", lang), fontSize = 12.sp)
                                        }

                                        // Rename action
                                        TextButton(
                                            onClick = {
                                                showRenameDialogItem = fileItem
                                                renameInputVal = fileItem.title
                                            }
                                        ) {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Rename", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(Translation.getString("rename_text", lang), fontSize = 12.sp)
                                        }

                                        // Share action
                                        TextButton(
                                            onClick = { shareDownloadedFile(context, fileItem) }
                                        ) {
                                            Icon(imageVector = Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(Translation.getString("share_text", lang), fontSize = 12.sp)
                                        }

                                        // Delete action
                                        TextButton(
                                            onClick = {
                                                scope.launch {
                                                    viewModel.downloadManager.cancelDownload(fileItem.id)
                                                }
                                            }
                                        ) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(Translation.getString("delete_text", lang), color = Color.Red, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Rename Dialog implementation
    if (showRenameDialogItem != null) {
        val targetItem = showRenameDialogItem!!
        AlertDialog(
            onDismissRequest = { showRenameDialogItem = null },
            title = { Text(Translation.getString("rename_file_title", lang), fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                OutlinedTextField(
                    value = renameInputVal,
                    onValueChange = { renameInputVal = it },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("rename_input_field")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newTitle = renameInputVal.trim()
                        if (newTitle.isNotBlank()) {
                            scope.launch {
                                val oldFile = File(targetItem.localPath)
                                val extension = if (targetItem.isAudioOnly) "mp3" else "mp4"
                                val newFileName = "${newTitle.replace("[^a-zA-Z0-9]".toRegex(), "_")}_${targetItem.id.take(4)}.$extension"
                                val newFile = File(oldFile.parentFile, newFileName)

                                if (oldFile.exists()) {
                                    oldFile.renameTo(newFile)
                                }

                                viewModel.updateDownload(
                                    targetItem.copy(
                                        title = newTitle,
                                        localPath = newFile.absolutePath
                                    )
                                )
                                showRenameDialogItem = null
                            }
                        }
                    },
                    modifier = Modifier.testTag("rename_confirm_button")
                ) {
                    Text(Translation.getString("btn_save", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialogItem = null }) {
                    Text(Translation.getString("btn_cancel", lang))
                }
            }
        )
    }
}

// Share downloaded media safely utilizing standard FileProvider structure
fun shareDownloadedFile(context: Context, item: DownloadItem) {
    try {
        val file = File(item.localPath)
        if (!file.exists()) return

        // We use a safe try-catch wrapper in case our authority isn't fully registered in standard tests
        val fileUri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            android.net.Uri.fromFile(file)
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = item.mimeType
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_TITLE, item.title)
            putExtra(Intent.EXTRA_SUBJECT, item.title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Media File"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
