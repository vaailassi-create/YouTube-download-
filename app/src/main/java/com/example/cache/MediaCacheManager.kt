package com.example.cache

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.example.data.CacheStatus
import com.example.data.MediaCacheEntity
import com.example.data.MediaDao
import com.example.network.RetrofitClient
import com.example.service.MediaDownloadService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

data class DownloadProgress(
    val mediaId: Long,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speedBytesPerSec: Long = 0L,
    val percent: Int = 0
)

/**
 * High-level manager coordinating Retrofit streaming downloads,
 * local internal sandbox storage, and disk stats.
 */
class MediaCacheManager(
    private val context: Context,
    private val mediaDao: MediaDao,
    private val scope: CoroutineScope
) {
    private val downloadApi = RetrofitClient.downloadApi
    private val activeJobs = ConcurrentHashMap<Long, Job>()

    private val _downloadProgressMap = MutableStateFlow<Map<Long, DownloadProgress>>(emptyMap())
    val downloadProgressMap: StateFlow<Map<Long, DownloadProgress>> = _downloadProgressMap.asStateFlow()

    val cacheDirectory: File
        get() = MediaDownloadService.getCacheDirectory(context)

    fun startDownload(media: MediaCacheEntity) {
        if (activeJobs.containsKey(media.id)) return

        // Also trigger background Android Service so downloads persist across app components
        try {
            MediaDownloadService.startDownloadIntent(context, media)
        } catch (e: Exception) {
            // Fallback to in-process coroutine
        }

        val job = scope.launch(Dispatchers.IO) {
            var tempFile: File? = null
            var finalFile: File? = null
            try {
                mediaDao.updateDownloadStatus(
                    id = media.id,
                    downloadedBytes = 0L,
                    totalBytes = media.totalBytes,
                    status = CacheStatus.DOWNLOADING.name,
                    filePath = null,
                    error = null
                )

                // Execute streaming Retrofit call
                val response = downloadApi.downloadMedia(media.sourceUrl)
                if (!response.isSuccessful) {
                    throw Exception("HTTP error ${response.code()}: ${response.message()}")
                }

                val body = response.body() ?: throw Exception("Empty response body from media server")
                val contentLength = body.contentLength().let { if (it > 0) it else media.totalBytes }

                val extension = getExtensionFromMime(media.mimeType, media.sourceUrl)
                finalFile = File(cacheDirectory, "media_${media.id}$extension")
                tempFile = File(cacheDirectory, "media_${media.id}.tmp")

                var downloaded = 0L
                var lastUpdateTime = System.currentTimeMillis()
                var bytesSinceLastUpdate = 0L
                var currentSpeed = 0L

                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(32 * 1024)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloaded += bytesRead
                            bytesSinceLastUpdate += bytesRead

                            val now = System.currentTimeMillis()
                            val timeDiff = now - lastUpdateTime
                            if (timeDiff >= 300) {
                                currentSpeed = (bytesSinceLastUpdate * 1000) / timeDiff
                                lastUpdateTime = now
                                bytesSinceLastUpdate = 0L

                                val progressPercent = if (contentLength > 0) {
                                    ((downloaded * 100) / contentLength).toInt().coerceIn(0, 100)
                                } else 0

                                val progress = DownloadProgress(
                                    mediaId = media.id,
                                    downloadedBytes = downloaded,
                                    totalBytes = contentLength,
                                    speedBytesPerSec = currentSpeed,
                                    percent = progressPercent
                                )
                                updateProgressState(media.id, progress)

                                mediaDao.updateDownloadStatus(
                                    id = media.id,
                                    downloadedBytes = downloaded,
                                    totalBytes = contentLength,
                                    status = CacheStatus.DOWNLOADING.name,
                                    filePath = null
                                )
                            }
                        }
                    }
                }

                if (tempFile.exists()) {
                    if (finalFile.exists()) finalFile.delete()
                    tempFile.renameTo(finalFile)
                }

                // Finalize DB
                mediaDao.updateDownloadStatus(
                    id = media.id,
                    downloadedBytes = downloaded,
                    totalBytes = downloaded,
                    status = CacheStatus.CACHED.name,
                    filePath = finalFile.absolutePath,
                    error = null
                )

                removeProgressState(media.id)

            } catch (e: CancellationException) {
                tempFile?.delete()
                removeProgressState(media.id)
                mediaDao.updateDownloadStatus(
                    id = media.id,
                    downloadedBytes = 0L,
                    totalBytes = media.totalBytes,
                    status = CacheStatus.ONLINE_ONLY.name,
                    filePath = null
                )
            } catch (e: Exception) {
                tempFile?.delete()
                removeProgressState(media.id)
                mediaDao.updateDownloadStatus(
                    id = media.id,
                    downloadedBytes = 0L,
                    totalBytes = media.totalBytes,
                    status = CacheStatus.FAILED.name,
                    filePath = null,
                    error = e.localizedMessage ?: "Download failed"
                )
            } finally {
                activeJobs.remove(media.id)
            }
        }
        activeJobs[media.id] = job
    }

    fun cancelDownload(mediaId: Long) {
        activeJobs[mediaId]?.cancel()
        activeJobs.remove(mediaId)
        removeProgressState(mediaId)
        try {
            MediaDownloadService.cancelDownloadIntent(context, mediaId)
        } catch (e: Exception) {
            // Ignored
        }
    }

    suspend fun clearCacheForItem(media: MediaCacheEntity) {
        cancelDownload(media.id)
        withContext(Dispatchers.IO) {
            media.localFilePath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            }
            val ext = getExtensionFromMime(media.mimeType, media.sourceUrl)
            val fallbackFile = File(cacheDirectory, "media_${media.id}$ext")
            if (fallbackFile.exists()) {
                fallbackFile.delete()
            }
            mediaDao.clearCacheForItem(media.id)
        }
    }

    suspend fun clearAllCache() {
        activeJobs.forEach { (_, job) -> job.cancel() }
        activeJobs.clear()
        _downloadProgressMap.value = emptyMap()

        withContext(Dispatchers.IO) {
            cacheDirectory.listFiles()?.forEach { it.delete() }
            mediaDao.clearAllCache()
        }
    }

    fun getDiskStats(): DiskStats {
        val totalCachedBytes = getDirectorySize(cacheDirectory)
        val stat = StatFs(Environment.getDataDirectory().path)
        val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
        val totalStorageBytes = stat.blockCountLong * stat.blockSizeLong

        return DiskStats(
            cachedBytes = totalCachedBytes,
            availableBytes = availableBytes,
            totalDeviceBytes = totalStorageBytes
        )
    }

    private fun getDirectorySize(dir: File): Long {
        var size = 0L
        val files = dir.listFiles() ?: return 0L
        for (f in files) {
            size += if (f.isDirectory) getDirectorySize(f) else f.length()
        }
        return size
    }

    private fun updateProgressState(mediaId: Long, progress: DownloadProgress) {
        val current = _downloadProgressMap.value.toMutableMap()
        current[mediaId] = progress
        _downloadProgressMap.value = current
    }

    private fun removeProgressState(mediaId: Long) {
        val current = _downloadProgressMap.value.toMutableMap()
        current.remove(mediaId)
        _downloadProgressMap.value = current
    }

    private fun getExtensionFromMime(mimeType: String, url: String): String {
        return when {
            mimeType.contains("mp4", ignoreCase = true) || url.endsWith(".mp4", ignoreCase = true) -> ".mp4"
            mimeType.contains("mpeg", ignoreCase = true) || url.endsWith(".mp3", ignoreCase = true) -> ".mp3"
            mimeType.contains("ogg", ignoreCase = true) || url.endsWith(".ogg", ignoreCase = true) -> ".ogg"
            mimeType.contains("m4a", ignoreCase = true) || url.endsWith(".m4a", ignoreCase = true) -> ".m4a"
            mimeType.contains("webm", ignoreCase = true) || url.endsWith(".webm", ignoreCase = true) -> ".webm"
            mimeType.contains("video", ignoreCase = true) -> ".mp4"
            else -> ".mp3"
        }
    }

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
            val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
            return String.format("%.1f %s", value, units[digitGroups.coerceIn(0, units.size - 1)])
        }

        fun formatSpeed(bytesPerSec: Long): String {
            return "${formatBytes(bytesPerSec)}/s"
        }
    }
}

data class DiskStats(
    val cachedBytes: Long,
    val availableBytes: Long,
    val totalDeviceBytes: Long
)
