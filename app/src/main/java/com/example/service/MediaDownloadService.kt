package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.example.cache.DownloadProgress
import com.example.data.AppDatabase
import com.example.data.CacheStatus
import com.example.data.MediaCacheEntity
import com.example.data.MediaDao
import com.example.network.RetrofitClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Android Service dedicated to managing background streaming downloads using Retrofit.
 * Handles queuing, chunk-by-chunk writing to sandbox storage, and live progress reporting.
 */
class MediaDownloadService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private val activeDownloadJobs = ConcurrentHashMap<Long, Job>()

    private val _downloadProgressMap = MutableStateFlow<Map<Long, DownloadProgress>>(emptyMap())
    val downloadProgressMap: StateFlow<Map<Long, DownloadProgress>> = _downloadProgressMap.asStateFlow()

    private val binder = DownloadBinder()
    private lateinit var mediaDao: MediaDao

    inner class DownloadBinder : Binder() {
        fun getService(): MediaDownloadService = this@MediaDownloadService
    }

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getDatabase(applicationContext, serviceScope)
        mediaDao = database.mediaDao()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_DOWNLOAD -> {
                val mediaId = intent.getLongExtra(EXTRA_MEDIA_ID, -1L)
                val url = intent.getStringExtra(EXTRA_URL)
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Media"
                val mimeType = intent.getStringExtra(EXTRA_MIME_TYPE) ?: "audio/mpeg"
                val totalBytes = intent.getLongExtra(EXTRA_TOTAL_BYTES, 0L)

                if (mediaId != -1L && !url.isNullOrBlank()) {
                    enqueueDownload(
                        MediaCacheEntity(
                            id = mediaId,
                            title = title,
                            sourceUrl = url,
                            mimeType = mimeType,
                            totalBytes = totalBytes
                        )
                    )
                }
            }
            ACTION_CANCEL_DOWNLOAD -> {
                val mediaId = intent.getLongExtra(EXTRA_MEDIA_ID, -1L)
                if (mediaId != -1L) {
                    cancelDownload(mediaId)
                }
            }
        }
        return START_NOT_STICKY
    }

    fun enqueueDownload(media: MediaCacheEntity) {
        if (activeDownloadJobs.containsKey(media.id)) return

        val cacheDir = getCacheDirectory(this)
        val job = serviceScope.launch {
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

                val response = RetrofitClient.downloadApi.downloadMedia(media.sourceUrl)
                if (!response.isSuccessful) {
                    throw Exception("Server returned HTTP ${response.code()}: ${response.message()}")
                }

                val responseBody = response.body() ?: throw Exception("Empty response body from server")
                val contentLength = responseBody.contentLength().let { if (it > 0) it else media.totalBytes }

                val serverMime = responseBody.contentType()?.let { "${it.type}/${it.subtype}" } ?: media.mimeType
                val ext = getExtension(serverMime, media.sourceUrl)
                tempFile = File(cacheDir, "media_${media.id}.tmp")
                finalFile = File(cacheDir, "media_${media.id}$ext")

                var downloadedSoFar = 0L
                var lastUpdateMillis = System.currentTimeMillis()
                var bytesSinceLastUpdate = 0L
                var currentSpeed = 0L

                responseBody.byteStream().use { inputStream ->
                    FileOutputStream(tempFile).use { outputStream ->
                        val buffer = ByteArray(32 * 1024)
                        var readBytes: Int

                        while (inputStream.read(buffer).also { readBytes = it } != -1) {
                            outputStream.write(buffer, 0, readBytes)
                            downloadedSoFar += readBytes
                            bytesSinceLastUpdate += readBytes

                            val now = System.currentTimeMillis()
                            val interval = now - lastUpdateMillis
                            if (interval >= 300) {
                                currentSpeed = (bytesSinceLastUpdate * 1000) / interval
                                lastUpdateMillis = now
                                bytesSinceLastUpdate = 0L

                                val progressPercent = if (contentLength > 0) {
                                    ((downloadedSoFar * 100) / contentLength).toInt().coerceIn(0, 100)
                                } else 0

                                val progress = DownloadProgress(
                                    mediaId = media.id,
                                    downloadedBytes = downloadedSoFar,
                                    totalBytes = contentLength,
                                    speedBytesPerSec = currentSpeed,
                                    percent = progressPercent
                                )
                                publishProgress(media.id, progress)

                                mediaDao.updateDownloadStatus(
                                    id = media.id,
                                    downloadedBytes = downloadedSoFar,
                                    totalBytes = contentLength,
                                    status = CacheStatus.DOWNLOADING.name,
                                    filePath = null
                                )
                            }
                        }
                    }
                }

                // Download completed: Rename temp to final
                if (tempFile.exists()) {
                    if (finalFile.exists()) finalFile.delete()
                    tempFile.renameTo(finalFile)
                }

                mediaDao.updateDownloadStatus(
                    id = media.id,
                    downloadedBytes = downloadedSoFar,
                    totalBytes = downloadedSoFar,
                    status = CacheStatus.CACHED.name,
                    filePath = finalFile.absolutePath,
                    error = null
                )

                removeProgress(media.id)

            } catch (e: CancellationException) {
                tempFile?.delete()
                removeProgress(media.id)
                mediaDao.updateDownloadStatus(
                    id = media.id,
                    downloadedBytes = 0L,
                    totalBytes = media.totalBytes,
                    status = CacheStatus.ONLINE_ONLY.name,
                    filePath = null
                )
            } catch (e: Exception) {
                tempFile?.delete()
                removeProgress(media.id)
                mediaDao.updateDownloadStatus(
                    id = media.id,
                    downloadedBytes = 0L,
                    totalBytes = media.totalBytes,
                    status = CacheStatus.FAILED.name,
                    filePath = null,
                    error = e.localizedMessage ?: "Download failed"
                )
            } finally {
                activeDownloadJobs.remove(media.id)
                checkStopSelf()
            }
        }

        activeDownloadJobs[media.id] = job
    }

    fun cancelDownload(mediaId: Long) {
        activeDownloadJobs[mediaId]?.cancel()
        activeDownloadJobs.remove(mediaId)
        removeProgress(mediaId)
        checkStopSelf()
    }

    fun isDownloading(mediaId: Long): Boolean {
        return activeDownloadJobs.containsKey(mediaId)
    }

    private fun publishProgress(mediaId: Long, progress: DownloadProgress) {
        val map = _downloadProgressMap.value.toMutableMap()
        map[mediaId] = progress
        _downloadProgressMap.value = map
    }

    private fun removeProgress(mediaId: Long) {
        val map = _downloadProgressMap.value.toMutableMap()
        map.remove(mediaId)
        _downloadProgressMap.value = map
    }

    private fun checkStopSelf() {
        if (activeDownloadJobs.isEmpty()) {
            // Can gracefully idle
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val ACTION_START_DOWNLOAD = "com.example.action.START_DOWNLOAD"
        const val ACTION_CANCEL_DOWNLOAD = "com.example.action.CANCEL_DOWNLOAD"
        const val EXTRA_MEDIA_ID = "extra_media_id"
        const val EXTRA_URL = "extra_url"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MIME_TYPE = "extra_mime_type"
        const val EXTRA_TOTAL_BYTES = "extra_total_bytes"

        fun getCacheDirectory(context: Context): File {
            val dir = File(context.filesDir, "media_cache")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

        fun startDownloadIntent(context: Context, media: MediaCacheEntity) {
            val intent = Intent(context, MediaDownloadService::class.java).apply {
                action = ACTION_START_DOWNLOAD
                putExtra(EXTRA_MEDIA_ID, media.id)
                putExtra(EXTRA_URL, media.sourceUrl)
                putExtra(EXTRA_TITLE, media.title)
                putExtra(EXTRA_MIME_TYPE, media.mimeType)
                putExtra(EXTRA_TOTAL_BYTES, media.totalBytes)
            }
            context.startService(intent)
        }

        fun cancelDownloadIntent(context: Context, mediaId: Long) {
            val intent = Intent(context, MediaDownloadService::class.java).apply {
                action = ACTION_CANCEL_DOWNLOAD
                putExtra(EXTRA_MEDIA_ID, mediaId)
            }
            context.startService(intent)
        }

        private fun getExtension(mimeType: String, url: String): String {
            // First check URL path for valid extension
            val cleanUrl = url.substringBefore('?').substringBefore('#')
            val lastSegment = cleanUrl.substringAfterLast('/', "")
            if (lastSegment.contains('.')) {
                val candidate = "." + lastSegment.substringAfterLast('.').lowercase()
                if (candidate.length in 2..7 && candidate.all { it.isLetterOrDigit() || it == '.' }) {
                    return candidate
                }
            }

            // Fallback to mimeType mappings
            return when {
                mimeType.contains("pdf", ignoreCase = true) -> ".pdf"
                mimeType.contains("zip", ignoreCase = true) -> ".zip"
                mimeType.contains("json", ignoreCase = true) -> ".json"
                mimeType.contains("text", ignoreCase = true) -> ".txt"
                mimeType.contains("image/png", ignoreCase = true) -> ".png"
                mimeType.contains("image/jpeg", ignoreCase = true) -> ".jpg"
                mimeType.contains("wav", ignoreCase = true) -> ".wav"
                mimeType.contains("flac", ignoreCase = true) -> ".flac"
                mimeType.contains("mp4", ignoreCase = true) -> ".mp4"
                mimeType.contains("mpeg", ignoreCase = true) || mimeType.contains("mp3", ignoreCase = true) -> ".mp3"
                mimeType.contains("ogg", ignoreCase = true) -> ".ogg"
                mimeType.contains("m4a", ignoreCase = true) -> ".m4a"
                mimeType.contains("webm", ignoreCase = true) -> ".webm"
                mimeType.contains("video", ignoreCase = true) -> ".mp4"
                mimeType.contains("audio", ignoreCase = true) -> ".mp3"
                else -> ".bin"
            }
        }
    }
}
