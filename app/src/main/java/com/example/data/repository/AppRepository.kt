package com.example.data.repository

import com.example.data.local.DownloadDao
import com.example.data.model.DownloadItem
import kotlinx.coroutines.flow.Flow

class AppRepository(private val downloadDao: DownloadDao) {
    val allDownloadsFlow: Flow<List<DownloadItem>> = downloadDao.getAllDownloadsFlow()

    suspend fun getDownload(id: String): DownloadItem? {
        return downloadDao.getDownloadById(id)
    }

    suspend fun insertOrUpdate(item: DownloadItem) {
        downloadDao.insertOrUpdateDownload(item)
    }

    suspend fun delete(item: DownloadItem) {
        downloadDao.deleteDownload(item)
    }

    suspend fun deleteById(id: String) {
        downloadDao.deleteDownloadById(id)
    }
}
