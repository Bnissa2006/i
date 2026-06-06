package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "history")
data class History(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class DownloadStatus {
    PENDING, DOWNLOADING, PAUSED, COMPLETED, FAILED
}

@Entity(tableName = "downloads")
data class DownloadItem(
    @PrimaryKey val id: String, // Unique identifier (e.g., UUID-like)
    val title: String,
    val url: String,
    val sizeBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val mimeType: String,
    val localPath: String,
    val progress: Float = 0f, // 0.0 to 1.0f
    val status: DownloadStatus = DownloadStatus.PENDING,
    val resolution: String, // e.g., "1080p", "720p", "320kbps"
    val isAudioOnly: Boolean = false,
    val downloadSpeed: String = "0 KB/s",
    val timestamp: Long = System.currentTimeMillis(),
    val thumbnailUrl: String = ""
)
