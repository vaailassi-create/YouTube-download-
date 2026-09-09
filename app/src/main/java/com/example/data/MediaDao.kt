package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_items ORDER BY addedTimestamp DESC")
    fun getAllMedia(): Flow<List<MediaCacheEntity>>

    @Query("SELECT * FROM media_items WHERE id = :id")
    fun getMediaById(id: Long): Flow<MediaCacheEntity?>

    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun getMediaByIdDirect(id: Long): MediaCacheEntity?

    @Query("SELECT * FROM media_items WHERE cacheStatus = :status ORDER BY addedTimestamp DESC")
    fun getMediaByStatus(status: String): Flow<List<MediaCacheEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: MediaCacheEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MediaCacheEntity>)

    @Update
    suspend fun update(item: MediaCacheEntity)

    @Delete
    suspend fun delete(item: MediaCacheEntity)

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE media_items SET downloadedBytes = :downloadedBytes, totalBytes = :totalBytes, cacheStatus = :status, localFilePath = :filePath, errorMessage = :error WHERE id = :id")
    suspend fun updateDownloadStatus(
        id: Long,
        downloadedBytes: Long,
        totalBytes: Long,
        status: String,
        filePath: String?,
        error: String? = null
    )

    @Query("UPDATE media_items SET lastPlayedPositionMs = :position, durationMs = CASE WHEN :duration > 0 THEN :duration ELSE durationMs END WHERE id = :id")
    suspend fun updatePlaybackPosition(id: Long, position: Long, duration: Long)

    @Query("UPDATE media_items SET cacheStatus = 'ONLINE_ONLY', localFilePath = NULL, downloadedBytes = 0 WHERE id = :id")
    suspend fun clearCacheForItem(id: Long)

    @Query("UPDATE media_items SET cacheStatus = 'ONLINE_ONLY', localFilePath = NULL, downloadedBytes = 0")
    suspend fun clearAllCache()

    @Query("SELECT SUM(downloadedBytes) FROM media_items WHERE cacheStatus = 'CACHED'")
    fun getTotalCachedBytes(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM media_items")
    suspend fun getCount(): Int

    @Query("SELECT * FROM media_items WHERE cacheStatus = 'CACHED'")
    suspend fun getCachedMediaList(): List<MediaCacheEntity>

    @Query("SELECT DISTINCT playlist FROM media_items WHERE playlist IS NOT NULL AND playlist != '' ORDER BY playlist ASC")
    fun getAllPlaylists(): Flow<List<String>>

    @Query("SELECT * FROM media_items WHERE playlist = :playlist ORDER BY addedTimestamp DESC")
    fun getMediaByPlaylist(playlist: String): Flow<List<MediaCacheEntity>>

    @Query("SELECT * FROM media_items WHERE playlist = :playlist")
    suspend fun getMediaListByPlaylist(playlist: String): List<MediaCacheEntity>

    @Query("UPDATE media_items SET playlist = :playlist WHERE id = :id")
    suspend fun updatePlaylistForItem(id: Long, playlist: String)
}
