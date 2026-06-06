package com.example.ui.screens

import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.MainViewModel
import com.example.ui.WebTab
import com.example.ui.theme.Translation

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BrowserScreen(
    viewModel: MainViewModel,
    lang: String,
    modifier: Modifier = Modifier
) {
    val tabs by viewModel.webTabs.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()
    val activeTab = tabs.find { it.id == activeTabId } ?: tabs.first()

    val bookmarks by viewModel.bookmarks.collectAsState()
    val history by viewModel.history.collectAsState()

    val hasDetectedMedia by viewModel.hasDetectedMedia.collectAsState()
    val detectedTitle by viewModel.detectedMediaTitle.collectAsState()
    val detectedUrl by viewModel.detectedMediaUrl.collectAsState()

    var showDownloadSheet by remember { mutableStateOf(false) }
    var urlInput by remember(activeTab.url) { mutableStateOf(activeTab.url) }

    // Check bookmarks status dynamically
    var isCurrentPageBookmarked by remember { mutableStateOf(false) }
    LaunchedEffect(activeTab.url, bookmarks) {
        isCurrentPageBookmarked = bookmarks.any { it.url == activeTab.url }
    }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Multi-Tab management header
        Surface(
            tonalElevation = 4.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                // Tab slider row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = Translation.getString("tabs_title", lang),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(tabs) { tab ->
                            val isActive = tab.id == activeTabId
                            InputChip(
                                selected = isActive,
                                onClick = { viewModel.selectTab(tab.id) },
                                label = {
                                    Text(
                                        text = tab.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 100.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (tabs.size > 1) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close tab",
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { viewModel.removeTab(tab.id) }
                                        )
                                    }
                                },
                                colors = InputChipDefaults.inputChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.addNewTab() },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("add_tab_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Tab",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Address loading search bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    TextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        placeholder = { Text(Translation.getString("search_placeholder", lang), fontSize = 14.sp) },
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
                            .testTag("address_bar"),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock, 
                                contentDescription = "Secure Status Icon",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (urlInput.isNotBlank()) {
                                IconButton(onClick = { urlInput = "" }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    FilledIconButton(
                        onClick = {
                            val target = urlInput.trim()
                            if (target.isNotBlank()) {
                                val destination = if (target.startsWith("http://") || target.startsWith("https://")) {
                                    target
                                } else if (target.contains(".") && !target.contains(" ")) {
                                    "https://$target"
                                } else {
                                    "https://www.google.com/search?q=$target"
                                }
                                viewModel.updateCurrentTabUrl(destination, destination)
                                webViewInstance?.loadUrl(destination)
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("search_go_button")
                    ) {
                        Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Go")
                    }
                }

                // Web control bar (Back, Forward, Refresh, Bookmark status, Home page)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (webViewInstance?.canGoBack() == true) {
                                webViewInstance?.goBack()
                            }
                        },
                        enabled = webViewInstance?.canGoBack() == true
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }

                    IconButton(
                        onClick = {
                            if (webViewInstance?.canGoForward() == true) {
                                webViewInstance?.goForward()
                            }
                        },
                        enabled = webViewInstance?.canGoForward() == true
                    ) {
                        Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Forward")
                    }

                    IconButton(onClick = { webViewInstance?.reload() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
                    }

                    IconButton(onClick = {
                        viewModel.toggleBookmark(activeTab.title, activeTab.url)
                    }) {
                        Icon(
                            imageVector = if (isCurrentPageBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Bookmark Toggle",
                            tint = if (isCurrentPageBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = {
                        viewModel.updateCurrentTabUrl("about:blank", "Home")
                        urlInput = ""
                        viewModel.clearDetectedMedia()
                        webViewInstance?.loadUrl("about:blank")
                    }) {
                        Icon(imageVector = Icons.Default.Home, contentDescription = "Home Page")
                    }
                }

                // Inline mini loading progress
                if (activeTab.progress > 0 && activeTab.progress < 100) {
                    LinearProgressIndicator(
                        progress = { activeTab.progress / 100f },
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (activeTab.url == "about:blank" || activeTab.url == "about:") {
                // Customized Beautiful Browser Home with bookmark tiles and supported popular shortcuts
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        // Branding card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = Translation.getString("app_title", lang),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 24.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Secure Web Downloader Companion",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    // Popular Supported Shortcuts
                    item {
                        Text(
                            text = Translation.getString("popular_sites", lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val sites = listOf(
                                Triple("YouTube", "https://www.youtube.com", Icons.Default.PlayCircle),
                                Triple("Vimeo", "https://www.vimeo.com", Icons.Default.VideoLibrary),
                                Triple("SoundCloud", "https://www.soundcloud.com", Icons.Default.MusicNote),
                                Triple("Google", "https://www.google.com", Icons.Default.Search)
                            )
                            sites.forEach { (name, url, icon) ->
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 4.dp)
                                        .clickable {
                                            viewModel.updateCurrentTabUrl(url, name)
                                            webViewInstance?.loadUrl(url)
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = name,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = name,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bookmarks List Section
                    item {
                        Text(
                            text = Translation.getString("bookmarks", lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (bookmarks.isEmpty()) {
                            Text(
                                text = Translation.getString("no_bookmarks", lang),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    }

                    items(bookmarks) { bookmark ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    viewModel.updateCurrentTabUrl(bookmark.url, bookmark.title)
                                    webViewInstance?.loadUrl(bookmark.url)
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = bookmark.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = bookmark.url,
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(onClick = { viewModel.toggleBookmark(bookmark.title, bookmark.url) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete bookmark")
                                }
                            }
                        }
                    }

                    // Browsing History Section
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = Translation.getString("history", lang),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (history.isNotEmpty()) {
                                TextButton(onClick = { viewModel.clearAllHistory() }) {
                                    Text("Clear All", color = MaterialTheme.colorScheme.tertiary)
                                }
                            }
                        }
                        if (history.isEmpty()) {
                            Text(
                                text = Translation.getString("no_history", lang),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    }

                    items(history.take(10)) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .combinedClickable(
                                    onClick = {
                                        viewModel.updateCurrentTabUrl(item.url, item.title)
                                        webViewInstance?.loadUrl(item.url)
                                    },
                                    onLongClick = { viewModel.deleteHistoryItem(item.id) }
                                ),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.url,
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Custom webview wrapper
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                databaseEnabled = true
                                mediaPlaybackRequiresUserGesture = false
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    url?.let {
                                        viewModel.updateCurrentTabUrl(it, view?.title ?: it)
                                    }
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    super.onProgressChanged(view, newProgress)
                                    viewModel.updateTabProgress(newProgress)
                                }

                                override fun onReceivedTitle(view: WebView?, webTitle: String?) {
                                    super.onReceivedTitle(view, webTitle)
                                    webTitle?.let {
                                        viewModel.updateCurrentTabUrl(view?.url ?: "", it)
                                    }
                                }
                            }

                            webViewInstance = this
                            loadUrl(activeTab.url)
                        }
                    },
                    update = { view ->
                        if (view.url != activeTab.url && activeTab.url != "about:blank") {
                            view.loadUrl(activeTab.url)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // High priority floating download button which flashes beautifully when media is autodetected!
            if (hasDetectedMedia) {
                FloatingActionButton(
                    onClick = { showDownloadSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .size(64.dp)
                        .testTag("floating_download_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = "Extract Media",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }

    // Modal BottomSheet for Video Resolutions & Audio format selection and download
    if (showDownloadSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDownloadSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = Translation.getString("quality_dialog_title", lang),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (detectedTitle.isBlank()) "Media detected from active site" else detectedTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                var selectedOptionItem by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

                // Group choices elegantly (Video, Audio)
                Text(
                    text = Translation.getString("video_category", lang),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                )

                val videoResolutions = listOf(
                    Triple("1080p Full HD", "1080p", 45),
                    Triple("720p HD", "720p", 22),
                    Triple("480p Medium", "480p", 12),
                    Triple("360p Compact", "360p", 6)
                )

                videoResolutions.forEach { (label, res, size) ->
                    val option = res to false
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedOptionItem = option }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedOptionItem == option,
                            onClick = { selectedOptionItem = option }
                        )
                        Text(text = label, modifier = Modifier.weight(1f))
                        Text(text = "~$size MB", color = Color.Gray, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = Translation.getString("audio_category", lang),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                )

                val audioFormats = listOf(
                    Triple("MP3 High Quality", "MP3 320kbps", 9),
                    Triple("MP3 Standard", "MP3 128kbps", 4),
                    Triple("AAC Audio", "AAC", 3)
                )

                audioFormats.forEach { (label, res, size) ->
                    val option = res to true
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedOptionItem = option }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedOptionItem == option,
                            onClick = { selectedOptionItem = option }
                        )
                        Text(text = label, modifier = Modifier.weight(1f))
                        Text(text = "~$size MB", color = Color.Gray, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        selectedOptionItem?.let { (res, isAudio) ->
                            viewModel.downloadManager.startDownload(
                                title = detectedTitle,
                                url = detectedUrl,
                                resolution = res,
                                isAudio = isAudio
                            )
                            showDownloadSheet = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("quality_sheet_download_confirm"),
                    enabled = selectedOptionItem != null
                ) {
                    Text(Translation.getString("btn_download", lang), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
