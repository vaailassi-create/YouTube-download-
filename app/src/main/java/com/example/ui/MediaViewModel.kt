package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cache.DiskStats
import com.example.cache.DownloadProgress
import com.example.cache.MediaCacheManager
import com.example.data.AppDatabase
import com.example.data.CacheStatus
import com.example.data.MediaCacheEntity
import com.example.player.MediaPlayerController
import com.example.player.PlaybackState
import com.example.util.NetworkMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MediaFilter(val label: String) {
    ALL("All"),
    PLAYLISTS("Playlists"),
    CACHED("Cached Offline"),
    ONLINE_ONLY("Streaming Only"),
    AUDIO("Audio"),
    VIDEO("Video")
}

class MediaViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val dao = database.mediaDao()

    val cacheManager = MediaCacheManager(application, dao, viewModelScope)
    val playerController = MediaPlayerController(application, dao, viewModelScope)
    private val networkMonitor = NetworkMonitor(application)

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(MediaFilter.ALL)
    val selectedFilter: StateFlow<MediaFilter> = _selectedFilter.asStateFlow()

    private val _diskStats = MutableStateFlow(cacheManager.getDiskStats())
    val diskStats: StateFlow<DiskStats> = _diskStats.asStateFlow()

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    private val _showStorageDialog = MutableStateFlow(false)
    val showStorageDialog: StateFlow<Boolean> = _showStorageDialog.asStateFlow()

    private val _showPlaylistDialog = MutableStateFlow(false)
    val showPlaylistDialog: StateFlow<Boolean> = _showPlaylistDialog.asStateFlow()

    private val _selectedPlaylist = MutableStateFlow<String?>(null)
    val selectedPlaylist: StateFlow<String?> = _selectedPlaylist.asStateFlow()

    val allPlaylists: StateFlow<List<String>> = dao.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showFullPlayer = MutableStateFlow(false)
    val showFullPlayer: StateFlow<Boolean> = _showFullPlayer.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    val downloadProgressMap: StateFlow<Map<Long, DownloadProgress>> = cacheManager.downloadProgressMap
    val playbackState: StateFlow<PlaybackState> = playerController.playbackState

    val filteredMediaList: StateFlow<List<MediaCacheEntity>> = combine(
        dao.getAllMedia(),
        _searchQuery,
        _selectedFilter,
        _selectedPlaylist
    ) { list, query, filter, activePlaylist ->
        list.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.title.contains(query, ignoreCase = true) ||
                    item.artist.contains(query, ignoreCase = true) ||
                    item.tag.contains(query, ignoreCase = true) ||
                    item.playlist.contains(query, ignoreCase = true)

            val matchesPlaylist = if (activePlaylist != null) {
                item.playlist.equals(activePlaylist, ignoreCase = true)
            } else true

            val matchesFilter = when (filter) {
                MediaFilter.ALL -> true
                MediaFilter.PLAYLISTS -> true // Will be grouped/handled in UI
                MediaFilter.CACHED -> item.cacheStatus == CacheStatus.CACHED.name
                MediaFilter.ONLINE_ONLY -> item.cacheStatus != CacheStatus.CACHED.name
                MediaFilter.AUDIO -> !item.isVideo
                MediaFilter.VIDEO -> item.isVideo
            }

            matchesQuery && matchesPlaylist && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshDiskStats()
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onFilterSelected(filter: MediaFilter) {
        _selectedFilter.value = filter
    }

    fun setShowAddDialog(show: Boolean) {
        _showAddDialog.value = show
    }

    fun setShowPlaylistDialog(show: Boolean) {
        _showPlaylistDialog.value = show
    }

    fun setSelectedPlaylist(playlist: String?) {
        _selectedPlaylist.value = playlist
    }

    fun setShowStorageDialog(show: Boolean) {
        if (show) refreshDiskStats()
        _showStorageDialog.value = show
    }

    fun setShowFullPlayer(show: Boolean) {
        _showFullPlayer.value = show
    }

    fun dismissToast() {
        _toastMessage.value = null
    }

    fun startDownload(media: MediaCacheEntity) {
        if (!_toastMessage.value.isNullOrBlank()) dismissToast()
        cacheManager.startDownload(media)
        _toastMessage.value = "Caching '${media.title}' for offline use"
        refreshDiskStats()
    }

    fun cancelDownload(mediaId: Long) {
        cacheManager.cancelDownload(mediaId)
    }

    fun clearItemCache(media: MediaCacheEntity) {
        viewModelScope.launch {
            cacheManager.clearCacheForItem(media)
            refreshDiskStats()
            _toastMessage.value = "Cleared cache for '${media.title}'"
        }
    }

    fun purgeAllCache() {
        viewModelScope.launch {
            cacheManager.clearAllCache()
            refreshDiskStats()
            _toastMessage.value = "All cached files purged"
        }
    }

    fun playMedia(media: MediaCacheEntity) {
        playerController.playMedia(media)
    }

    fun deleteMediaItem(media: MediaCacheEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            cacheManager.clearCacheForItem(media)
            dao.delete(media)
            refreshDiskStats()
        }
    }

    fun addNewMedia(
        title: String,
        artist: String,
        sourceUrl: String,
        isVideo: Boolean,
        tag: String,
        cacheImmediately: Boolean,
        customMimeType: String? = null,
        playlist: String = "Default"
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val trimmedUrl = sourceUrl.trim()
            val detectedMime = customMimeType ?: when {
                isVideo -> "video/mp4"
                trimmedUrl.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                trimmedUrl.endsWith(".zip", ignoreCase = true) -> "application/zip"
                trimmedUrl.endsWith(".ogg", ignoreCase = true) -> "audio/ogg"
                trimmedUrl.endsWith(".wav", ignoreCase = true) -> "audio/wav"
                trimmedUrl.endsWith(".flac", ignoreCase = true) -> "audio/flac"
                trimmedUrl.endsWith(".mp4", ignoreCase = true) -> "video/mp4"
                else -> "audio/mpeg"
            }

            val finalTitle = if (title.isNotBlank()) title.trim() else {
                val clean = trimmedUrl.substringBefore('?').substringBefore('#')
                clean.substringAfterLast('/', "Downloaded File")
            }

            val newItem = MediaCacheEntity(
                title = finalTitle,
                artist = if (artist.isBlank()) "Direct Storage Download" else artist.trim(),
                sourceUrl = trimmedUrl,
                isVideo = isVideo,
                mimeType = detectedMime,
                tag = if (tag.isBlank()) (if (isVideo) "Video" else "File") else tag.trim(),
                playlist = if (playlist.isBlank()) "Default" else playlist.trim()
            )
            val id = dao.insert(newItem)
            if (cacheImmediately) {
                val insertedItem = newItem.copy(id = id)
                cacheManager.startDownload(insertedItem)
            }
            refreshDiskStats()
        }
    }

    fun exportToDeviceStorage(media: MediaCacheEntity) {
        viewModelScope.launch {
            if (media.localFilePath.isNullOrBlank() || media.cacheStatus != CacheStatus.CACHED.name) {
                _toastMessage.value = "Downloading '${media.title}' to storage first..."
                startDownload(media)
                return@launch
            }
            val result = com.example.util.StorageExportManager.exportToDeviceDownloads(getApplication(), media)
            result.onSuccess { msg ->
                _toastMessage.value = msg
                refreshDiskStats()
            }.onFailure { err ->
                _toastMessage.value = "Storage export failed: ${err.message}"
            }
        }
    }

    fun exportAllToDeviceStorage() {
        viewModelScope.launch(Dispatchers.IO) {
            val cachedList = dao.getCachedMediaList()
            if (cachedList.isEmpty()) {
                _toastMessage.value = "No cached files found to export."
                return@launch
            }
            var count = 0
            cachedList.forEach { item ->
                val res = com.example.util.StorageExportManager.exportToDeviceDownloads(getApplication(), item)
                if (res.isSuccess) count++
            }
            _toastMessage.value = "Exported $count file(s) to Downloads/MediaCache"
            refreshDiskStats()
        }
    }

    fun downloadPlaylist(playlistName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val items = dao.getMediaListByPlaylist(playlistName)
            val uncached = items.filter { it.cacheStatus != CacheStatus.CACHED.name }
            if (uncached.isEmpty()) {
                _toastMessage.value = "All tracks in '$playlistName' are already cached offline!"
                return@launch
            }
            _toastMessage.value = "Queued ${uncached.size} item(s) from '$playlistName' for offline caching"
            uncached.forEach { media ->
                cacheManager.startDownload(media)
            }
            refreshDiskStats()
        }
    }

    fun exportPlaylistToStorage(playlistName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val items = dao.getMediaListByPlaylist(playlistName)
            val cached = items.filter { it.cacheStatus == CacheStatus.CACHED.name }
            if (cached.isEmpty()) {
                _toastMessage.value = "No cached files in '$playlistName' yet. Tap 'Download Playlist' first!"
                return@launch
            }
            var count = 0
            val cleanFolderName = playlistName.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(40)
            cached.forEach { item ->
                val res = com.example.util.StorageExportManager.exportToDeviceDownloads(
                    getApplication(),
                    item,
                    subFolder = "MediaCache/$cleanFolderName"
                )
                if (res.isSuccess) count++
            }
            _toastMessage.value = "Exported $count file(s) from '$playlistName' to Downloads/MediaCache/$cleanFolderName"
            refreshDiskStats()
        }
    }

    fun addAndDownloadPlaylist(
        playlistName: String,
        items: List<MediaCacheEntity>,
        cacheImmediately: Boolean = true
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (items.isEmpty()) {
                _toastMessage.value = "No items to add to playlist."
                return@launch
            }
            val insertedIds = mutableListOf<Long>()
            items.forEach { item ->
                val entity = item.copy(playlist = playlistName)
                val id = dao.insert(entity)
                insertedIds.add(id)
            }

            if (cacheImmediately) {
                _toastMessage.value = "Downloading playlist '$playlistName' (${items.size} items)..."
                items.forEachIndexed { index, item ->
                    val entityWithId = item.copy(id = insertedIds[index], playlist = playlistName)
                    cacheManager.startDownload(entityWithId)
                }
            } else {
                _toastMessage.value = "Added ${items.size} item(s) to playlist '$playlistName'"
            }
            _selectedPlaylist.value = playlistName
            refreshDiskStats()
        }
    }

    fun importRemotePlaylist(
        playlistName: String,
        url: String,
        cacheImmediately: Boolean = true
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _toastMessage.value = "Fetching playlist from network..."
            val result = com.example.util.PlaylistParser.fetchRemotePlaylist(playlistName, url)
            result.onSuccess { items ->
                addAndDownloadPlaylist(playlistName, items, cacheImmediately)
            }.onFailure { err ->
                _toastMessage.value = "Failed to load playlist: ${err.message}"
            }
        }
    }

    fun refreshDiskStats() {
        viewModelScope.launch(Dispatchers.IO) {
            val stats = cacheManager.getDiskStats()
            _diskStats.value = stats
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerController.release()
    }
}
