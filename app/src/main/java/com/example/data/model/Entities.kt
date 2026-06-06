package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELED
}

@Serializable
@Entity(tableName = "downloads")
data class DownloadItem(
    @PrimaryKey val id: String,
    val url: String,
    val title: String,
    val thumbnail: String,
    val isAudioOnly: Boolean,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val downloadedBytes: Long = 0L,
    val sizeBytes: Long = 0L,
    val progress: Float = 0f, // 0.0 to 1.0
    val downloadSpeed: String = "0 KB/s",
    val localPath: String,
    val mimeType: String,
    val durationSeconds: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class ExtractedStream(
    val url: String,
    val quality: String,
    val isAudioOnly: Boolean,
    val sizeBytes: Long,
    val title: String,
    val thumbnail: String,
    val durationSeconds: Int,
    val container: String
)
