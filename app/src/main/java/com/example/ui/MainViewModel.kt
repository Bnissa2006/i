package com.example.ui

import android.app.Application
import android.os.Environment
import android.os.StatFs
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.download.DownloadManager
import com.example.data.local.AppDatabase
import com.example.data.model.Bookmark
import com.example.data.model.DownloadItem
import com.example.data.model.DownloadStatus
import com.example.data.model.History
import com.example.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class WebTab(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Google",
    val url: String = "https://www.google.com",
    val progress: Int = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = AppRepository(
        database.bookmarkDao(),
        database.historyDao(),
        database.downloadDao(),
        application
    )

    val downloadManager = DownloadManager(application, repository)

    // Dynamic UI states powered by Room Flows
    val bookmarks: StateFlow<List<Bookmark>> = repository.allBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<History>> = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDownloads: StateFlow<List<DownloadItem>> = repository.allDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Settings States
    val themeMode: StateFlow<String> = repository.themeMode
    val language: StateFlow<String> = repository.language
    val downloadLocation: StateFlow<String> = repository.downloadLocation

    // Browser Multi-Tabs Configuration
    private val _webTabs = MutableStateFlow(listOf(WebTab()))
    val webTabs = _webTabs.asStateFlow()

    private val _activeTabId = MutableStateFlow(_webTabs.value.first().id)
    val activeTabId = _activeTabId.asStateFlow()

    // File Manager Search and filter
    private val _fileSearchQuery = MutableStateFlow("")
    val fileSearchQuery = _fileSearchQuery.asStateFlow()

    // Current player properties
    private val _playingVideo = MutableStateFlow<DownloadItem?>(null)
    val playingVideo = _playingVideo.asStateFlow()

    private val _playingAudio = MutableStateFlow<DownloadItem?>(null)
    val playingAudio = _playingAudio.asStateFlow()

    // Autodetected downloadable media on active web page
    private val _detectedMediaTitle = MutableStateFlow("")
    val detectedMediaTitle = _detectedMediaTitle.asStateFlow()

    private val _detectedMediaUrl = MutableStateFlow("")
    val detectedMediaUrl = _detectedMediaUrl.asStateFlow()

    private val _detectedMediaThumbnail = MutableStateFlow("")
    val detectedMediaThumbnail = _detectedMediaThumbnail.asStateFlow()

    private val _hasDetectedMedia = MutableStateFlow(false)
    val hasDetectedMedia = _hasDetectedMedia.asStateFlow()

    // Tab Operations
    fun addNewTab(url: String = "https://www.google.com") {
        val newTab = WebTab(url = url, title = getDomainOfUrl(url))
        _webTabs.value = _webTabs.value + newTab
        _activeTabId.value = newTab.id
    }

    fun removeTab(tabId: String) {
        if (_webTabs.value.size <= 1) return // Keep at least one tab
        val tabs = _webTabs.value.filter { it.id != tabId }
        _webTabs.value = tabs
        if (_activeTabId.value == tabId) {
            _activeTabId.value = tabs.first().id
        }
    }

    fun selectTab(tabId: String) {
        _activeTabId.value = tabId
    }

    fun updateCurrentTabUrl(url: String, title: String) {
        val updatedTabs = _webTabs.value.map {
            if (it.id == _activeTabId.value) {
                it.copy(url = url, title = title)
            } else {
                it
            }
        }
        _webTabs.value = updatedTabs

        // Auto add to web browsing history
        viewModelScope.launch {
            repository.addHistory(title, url)
        }

        // Trigger realistic media detection dynamically when moving to popular web URLs
        detectMediaOnUrl(url, title)
    }

    fun updateTabProgress(progress: Int) {
        val updatedTabs = _webTabs.value.map {
            if (it.id == _activeTabId.value) {
                it.copy(progress = progress)
            } else {
                it
            }
        }
        _webTabs.value = updatedTabs
    }

    // Media Detection Engine
    private fun detectMediaOnUrl(url: String, title: String) {
        val lowerUrl = url.lowercase()
        val extensionMatches = lowerUrl.endsWith(".mp4") || lowerUrl.endsWith(".mp3") || lowerUrl.endsWith(".mkv")
        val socialMatches = lowerUrl.contains("youtube.com") || lowerUrl.contains("youtu.be") ||
                lowerUrl.contains("vimeo.com") || lowerUrl.contains("soundcloud.com") ||
                lowerUrl.contains("facebook.com") || lowerUrl.contains("dailymotion.com") ||
                lowerUrl.contains("tiktok.com") || lowerUrl.contains("twitch.tv")

        if (extensionMatches || socialMatches) {
            _detectedMediaTitle.value = title.ifBlank { "Media content from " + getDomainOfUrl(url) }
            _detectedMediaUrl.value = url
            _detectedMediaThumbnail.value = if (lowerUrl.contains("soundcloud")) "music" else "video"
            _hasDetectedMedia.value = true
        } else {
            _hasDetectedMedia.value = false
        }
    }

    fun clearDetectedMedia() {
        _hasDetectedMedia.value = false
    }

    // Bookmarks Toggle
    fun toggleBookmark(title: String, url: String) {
        viewModelScope.launch {
            if (repository.isBookmarked(url)) {
                repository.removeBookmark(url)
            } else {
                repository.addBookmark(title, url)
            }
        }
    }

    // History cleaning
    fun deleteHistoryItem(id: Int) {
        viewModelScope.launch {
            repository.removeHistory(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // Preferences Settings
    fun changeThemeMode(theme: String) {
        repository.setThemeMode(theme)
    }

    fun changeLanguage(lang: String) {
        repository.setLanguage(lang)
    }

    fun changeDownloadLocation(loc: String) {
        repository.setDownloadLocation(loc)
    }

    // Playback state managers
    fun setPlayingVideo(item: DownloadItem?) {
        _playingVideo.value = item
    }

    fun setPlayingAudio(item: DownloadItem?) {
        _playingAudio.value = item
    }

    fun updateDownload(item: DownloadItem) {
        viewModelScope.launch {
            repository.updateDownload(item)
        }
    }

    fun updateFileSearchQuery(query: String) {
        _fileSearchQuery.value = query
    }

    // Get storage details card data
    fun getStorageMetrics(): StorageInfo {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val availableBlocks = stat.availableBlocksLong
            val totalBlocks = stat.blockCountLong

            val freeBytes = availableBlocks * blockSize
            val totalBytes = totalBlocks * blockSize

            val downloadsSize = _webTabs.value.run {
                allDownloads.value
                    .filter { it.status == DownloadStatus.COMPLETED }
                    .sumOf { it.sizeBytes }
            }

            StorageInfo(
                usedByAppBytes = downloadsSize,
                systemFreeBytes = freeBytes,
                systemTotalBytes = totalBytes
            )
        } catch (e: Exception) {
            StorageInfo(0, 50 * 1024L * 1024L * 1024L, 128 * 1024L * 1024L * 1024L)
        }
    }

    // Small helpers
    private fun getDomainOfUrl(url: String): String {
        return try {
            val uri = java.net.URI(url)
            var domain = uri.host ?: ""
            if (domain.startsWith("www.")) {
                domain = domain.substring(4)
            }
            domain.ifBlank { "Google" }
        } catch (e: Exception) {
            "Google"
        }
    }
}

data class StorageInfo(
    val usedByAppBytes: Long,
    val systemFreeBytes: Long,
    val systemTotalBytes: Long
)
