package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.BookmarkDao
import com.example.data.local.DownloadDao
import com.example.data.local.HistoryDao
import com.example.data.model.Bookmark
import com.example.data.model.DownloadItem
import com.example.data.model.DownloadStatus
import com.example.data.model.History
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppRepository(
    private val bookmarkDao: BookmarkDao,
    private val historyDao: HistoryDao,
    private val downloadDao: DownloadDao,
    context: Context
) {
    // Shared Preferences for local settings persistence
    private val prefs: SharedPreferences = context.getSharedPreferences("snaptube_settings", Context.MODE_PRIVATE)

    // Flows for database components
    val allBookmarks: Flow<List<Bookmark>> = bookmarkDao.getAllBookmarks()
    val allHistory: Flow<List<History>> = historyDao.getAllHistory()
    val allDownloads: Flow<List<DownloadItem>> = downloadDao.getAllDownloads()

    // Preferences configuration backing state
    private val _themeMode = MutableStateFlow(prefs.getString("theme", "System") ?: "System")
    val themeMode = _themeMode.asStateFlow()

    private val _language = MutableStateFlow(prefs.getString("language", "English") ?: "English")
    val language = _language.asStateFlow()

    private val _downloadLocation = MutableStateFlow(prefs.getString("location", "Default (App Storage)") ?: "Default (App Storage)")
    val downloadLocation = _downloadLocation.asStateFlow()

    // Bookmark management
    suspend fun addBookmark(title: String, url: String) {
        bookmarkDao.insertBookmark(Bookmark(title = title, url = url))
    }

    suspend fun removeBookmark(url: String) {
        bookmarkDao.deleteBookmarkByUrl(url)
    }

    suspend fun isBookmarked(url: String): Boolean {
        return bookmarkDao.isBookmarked(url)
    }

    // History management
    suspend fun addHistory(title: String, url: String) {
        if (url.isNotBlank() && !url.startsWith("about:")) {
            historyDao.insertHistory(History(title = title, url = url))
        }
    }

    suspend fun removeHistory(id: Int) {
        historyDao.deleteHistory(id)
    }

    suspend fun clearHistory() {
        historyDao.clearHistory()
    }

    // Downloads management
    suspend fun getDownload(id: String): DownloadItem? {
        return downloadDao.getDownloadById(id)
    }

    suspend fun addDownload(download: DownloadItem) {
        downloadDao.insertDownload(download)
    }

    suspend fun updateDownload(download: DownloadItem) {
        downloadDao.updateDownload(download)
    }

    suspend fun deleteDownload(id: String) {
        downloadDao.deleteDownloadById(id)
    }

    // Settings actions
    fun setThemeMode(theme: String) {
        prefs.edit().putString("theme", theme).apply()
        _themeMode.value = theme
    }

    fun setLanguage(lang: String) {
        prefs.edit().putString("language", lang).apply()
        _language.value = lang
    }

    fun setDownloadLocation(loc: String) {
        prefs.edit().putString("location", loc).apply()
        _downloadLocation.value = loc
    }
}
