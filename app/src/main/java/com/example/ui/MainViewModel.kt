package com.example.ui

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.DownloadItem
import com.example.data.model.DownloadStatus
import com.example.data.model.ExtractedStream
import com.example.data.download.DownloadManager
import com.example.data.download.MediaExtractor
import com.example.data.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "MainViewModel"

    private val db = AppDatabase.getDatabase(application)
    val appRepository = AppRepository(db.downloadDao())
    val downloadManager = DownloadManager(application, appRepository)

    // Language locale state (Arabic "ar" / English "en")
    var language by mutableStateOf("en")
        private set

    // Dark mode state
    var isDarkMode by mutableStateOf(true)
        private set

    // Simple Browser Navigation History
    val browserHistory = mutableStateListOf<String>()

    // Extraction State
    var isExtracting by mutableStateOf(false)
        private set
    var searchUrlInput by mutableStateOf("https://youtu.be/4mT1rpl4w6A?si=XzKBrMA38DdwCGT-")
    var extractedStreams = mutableStateListOf<ExtractedStream>()
        private set
    var extractionErrorMsg by mutableStateOf<String?>(null)
        private set

    // Live observed streams from DB
    val allDownloadsState: StateFlow<List<DownloadItem>> = appRepository.allDownloadsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun changeLanguage(lang: String) {
        language = lang
    }

    fun toggleTheme() {
        isDarkMode = !isDarkMode
    }

    fun clearHistory() {
        browserHistory.clear()
    }

    fun addToHistory(url: String) {
        if (browserHistory.isEmpty() || browserHistory.last() != url) {
            browserHistory.add(url)
        }
    }

    // High fidelity real extraction task triggered when user attempts to grab stream qualities
    fun triggerWebpageStreamExtraction(url: String, onFinished: (Boolean) -> Unit = {}) {
        isExtracting = true
        extractionErrorMsg = null
        extractedStreams.clear()
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val streams = MediaExtractor.extractMedia(url)
                withContext(Dispatchers.Main) {
                    isExtracting = false
                    if (streams.isNotEmpty()) {
                        extractedStreams.addAll(streams)
                        onFinished(true)
                    } else {
                        extractionErrorMsg = "No stream found"
                        onFinished(false)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed stream extraction: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    isExtracting = false
                    extractionErrorMsg = e.localizedMessage ?: "Unknown compilation error"
                    onFinished(false)
                }
            }
        }
    }

    // Queues download on the background pipeline
    fun queueStreamForDownload(stream: ExtractedStream) {
        viewModelScope.launch(Dispatchers.IO) {
            val extension = stream.container
            val uniqueId = UUID.randomUUID().toString()
            val fileName = "${stream.title.filter { it.isLetterOrDigit() || it.isWhitespace() }.take(50)}.$extension"
            
            val storageDir = File(getApplication<Application>().filesDir, "downloads")
            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }
            val destinationFile = File(storageDir, fileName)

            val downloadItem = DownloadItem(
                id = uniqueId,
                url = stream.url,
                title = stream.title,
                thumbnail = stream.thumbnail,
                isAudioOnly = stream.isAudioOnly,
                status = DownloadStatus.PENDING,
                downloadedBytes = 0L,
                sizeBytes = stream.sizeBytes,
                progress = 0f,
                downloadSpeed = "Reconnecting...",
                localPath = destinationFile.absolutePath,
                mimeType = if (stream.isAudioOnly) "audio/$extension" else "video/$extension",
                durationSeconds = stream.durationSeconds
            )

            // Save to DB and kickstart real thread downloader
            appRepository.insertOrUpdate(downloadItem)
            withContext(Dispatchers.Main) {
                downloadManager.startDownload(downloadItem)
            }
        }
    }

    fun removeDownload(item: DownloadItem) {
        viewModelScope.launch(Dispatchers.IO) {
            // Cancel downloading
            downloadManager.cancelDownload(item.id)
            appRepository.deleteById(item.id)
        }
    }
}
