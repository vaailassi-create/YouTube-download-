package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CacheStatus {
    ONLINE_ONLY,
    DOWNLOADING,
    CACHED,
    FAILED
}

@Entity(tableName = "media_items")
data class MediaCacheEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val artist: String = "Self-Hosted",
    val sourceUrl: String,
    val localFilePath: String? = null,
    val mimeType: String = "audio/mpeg",
    val isVideo: Boolean = false,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val cacheStatus: String = CacheStatus.ONLINE_ONLY.name,
    val errorMessage: String? = null,
    val durationMs: Long = 0L,
    val lastPlayedPositionMs: Long = 0L,
    val tag: String = "Media",
    val playlist: String = "Default",
    val addedTimestamp: Long = System.currentTimeMillis()
)
