package com.example.data.download

import android.content.Context
import android.util.Log
import com.example.data.model.DownloadItem
import com.example.data.model.DownloadStatus
import com.example.data.repository.AppRepository
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedInputStream
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class DownloadManager(
    private val context: Context,
    private val repository: AppRepository
) {
    private val TAG = "DownloadManager"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = ConcurrentHashMap<String, Job>()

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun startDownload(item: DownloadItem) {
        Log.d(TAG, "Queueing download: ${item.title} with ID: ${item.id}")
        
        // Cancel if already active
        cancelJobQuietly(item.id)
        
        scope.launch {
            repository.insertOrUpdate(item.copy(status = DownloadStatus.DOWNLOADING))
            runDownloadPipeline(item.id)
        }
    }

    fun pauseDownload(id: String) {
        Log.d(TAG, "Pausing download ID: $id")
        cancelJobQuietly(id)
        scope.launch {
            val item = repository.getDownload(id)
            if (item != null && item.status == DownloadStatus.DOWNLOADING) {
                repository.insertOrUpdate(
                    item.copy(
                        status = DownloadStatus.PAUSED,
                        downloadSpeed = "Paused"
                    )
                )
            }
        }
    }

    fun resumeDownload(id: String) {
        Log.d(TAG, "Resuming download ID: $id")
        // Check if job already active
        if (activeJobs.containsKey(id)) return
        
        scope.launch {
            val item = repository.getDownload(id)
            if (item != null) {
                repository.insertOrUpdate(item.copy(status = DownloadStatus.DOWNLOADING))
                runDownloadPipeline(id)
            }
        }
    }

    fun cancelDownload(id: String) {
        Log.d(TAG, "Cancelling download ID: $id")
        cancelJobQuietly(id)
        scope.launch {
            val item = repository.getDownload(id)
            if (item != null) {
                // Delete local file
                try {
                    val file = File(item.localPath)
                    if (file.exists()) file.delete()
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting cancelled file: ${e.message}")
                }
                
                repository.insertOrUpdate(
                    item.copy(
                        status = DownloadStatus.CANCELED,
                        downloadedBytes = 0L,
                        progress = 0f,
                        downloadSpeed = "Canceled"
                    )
                )
            }
        }
    }

    fun retryDownload(id: String) {
        Log.d(TAG, "Retrying download ID: $id")
        scope.launch {
            val item = repository.getDownload(id)
            if (item != null) {
                // Redownload form scratch
                try {
                    val file = File(item.localPath)
                    if (file.exists()) file.delete()
                } catch (e: Exception) {}
                
                val restartedItem = item.copy(
                    status = DownloadStatus.DOWNLOADING,
                    downloadedBytes = 0L,
                    progress = 0f,
                    downloadSpeed = "Reconnecting..."
                )
                repository.insertOrUpdate(restartedItem)
                runDownloadPipeline(id)
            }
        }
    }

    private fun cancelJobQuietly(id: String) {
        activeJobs.remove(id)?.cancel()
    }

    // Handles the actual byte stream downloading with support for range requests (Pause/Resume)
    private fun runDownloadPipeline(id: String) {
        val job = scope.launch(Dispatchers.IO) {
            try {
                var item = repository.getDownload(id) ?: return@launch
                
                val file = File(item.localPath)
                if (!file.parentFile.exists()) {
                    file.parentFile.mkdirs()
                }

                val existingLength = if (file.exists()) file.length() else 0L
                Log.d(TAG, "Starting download pipeline for id: $id. Range start byte: $existingLength")

                // Build range request if file partially exists
                val requestBuilder = Request.Builder()
                    .url(item.url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                
                if (existingLength > 0) {
                    requestBuilder.header("Range", "bytes=$existingLength-")
                }

                val request = requestBuilder.build()
                client.newCall(request).execute().use { response ->
                    val code = response.code
                    val isRangeAccepted = (code == 206)
                    val body = response.body
                    if (body == null || !response.isSuccessful) {
                        Log.e(TAG, "HTTP error: code $code. Download failed.")
                        markDownloadAsFailed(id)
                        return@launch
                    }

                    val responseLength = body.contentLength()
                    val totalLength = if (isRangeAccepted) {
                        existingLength + responseLength
                    } else {
                        // Server doesn't support resuming, overwrite file
                        if (existingLength > 0) {
                            file.delete()
                        }
                        responseLength
                    }

                    if (totalLength != item.sizeBytes && totalLength > 0) {
                        item = item.copy(sizeBytes = totalLength)
                        repository.insertOrUpdate(item)
                    }

                    // Open file output accessor (writable mode)
                    val targetFileAccess = RandomAccessFile(file, "rw")
                    if (isRangeAccepted) {
                        targetFileAccess.seek(existingLength)
                    } else {
                        targetFileAccess.seek(0)
                    }

                    val inputStream = BufferedInputStream(body.byteStream())
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalWritten = if (isRangeAccepted) existingLength else 0L
                    var lastUpdateTime = System.currentTimeMillis()
                    var bytesWrittenSegment = 0L

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        if (!isActive) {
                            targetFileAccess.close()
                            inputStream.close()
                            return@launch
                        }

                        targetFileAccess.write(buffer, 0, bytesRead)
                        totalWritten += bytesRead
                        bytesWrittenSegment += bytesRead

                        val now = System.currentTimeMillis()
                        val diff = now - lastUpdateTime
                        if (diff >= 500) { // Stream state notifications twice per second
                            val progress = if (totalLength > 0) {
                                totalWritten.toFloat() / totalLength.toFloat()
                            } else {
                                0.5f
                            }

                            val speedBytesPerSec = if (diff > 0) {
                                (bytesWrittenSegment * 1000) / diff
                            } else {
                                0L
                            }

                            val speedFormatted = if (speedBytesPerSec > 1024 * 1024) {
                                String.format("%.1f MB/s", speedBytesPerSec.toFloat() / (1024 * 1024))
                            } else {
                                String.format("%d KB/s", speedBytesPerSec / 1024)
                            }

                            val currentItem = repository.getDownload(id)
                            if (currentItem == null || currentItem.status != DownloadStatus.DOWNLOADING) {
                                break
                            }

                            repository.insertOrUpdate(
                                currentItem.copy(
                                    downloadedBytes = totalWritten,
                                    progress = progress.coerceIn(0f, 1f),
                                    downloadSpeed = speedFormatted
                                )
                            )

                            lastUpdateTime = now
                            bytesWrittenSegment = 0
                        }
                    }

                    targetFileAccess.close()
                    inputStream.close()

                    // Ensure final completion state is fully updated
                    val finalItem = repository.getDownload(id)
                    if (finalItem != null && finalItem.status == DownloadStatus.DOWNLOADING) {
                        Log.d(TAG, "Completed download pipeline successfully for id: $id")
                        repository.insertOrUpdate(
                            finalItem.copy(
                                downloadedBytes = totalWritten,
                                progress = 1.0f,
                                status = DownloadStatus.COMPLETED,
                                downloadSpeed = "Completed",
                                sizeBytes = totalWritten
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in download thread: ${e.message}", e)
                markDownloadAsFailed(id)
            } finally {
                activeJobs.remove(id)
            }
        }
        activeJobs[id] = job
    }

    private suspend fun markDownloadAsFailed(id: String) {
        val item = repository.getDownload(id)
        if (item != null) {
            repository.insertOrUpdate(
                item.copy(
                    status = DownloadStatus.FAILED,
                    downloadSpeed = "Failed"
                )
            )
        }
    }
}
