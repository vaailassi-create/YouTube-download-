package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.FloatingActionButton
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cache.MediaCacheManager
import com.example.data.CacheStatus
import com.example.ui.components.AddMediaDialog
import com.example.ui.components.FullPlayerModal
import com.example.ui.components.MediaItemCard
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.PlaylistDownloadDialog
import com.example.ui.components.PlaylistSummaryCard
import com.example.ui.components.StorageManagerDialog
import com.example.ui.components.StorageProgressIndicator
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSecondary
import com.example.ui.theme.StatusStreaming

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaCacheScreen(
    viewModel: MediaViewModel,
    modifier: Modifier = Modifier
) {
    val mediaList by viewModel.filteredMediaList.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsStateWithLifecycle()
    val allPlaylists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    val diskStats by viewModel.diskStats.collectAsStateWithLifecycle()
    val downloadProgressMap by viewModel.downloadProgressMap.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val showAddDialog by viewModel.showAddDialog.collectAsStateWithLifecycle()
    val showPlaylistDialog by viewModel.showPlaylistDialog.collectAsStateWithLifecycle()
    val showStorageDialog by viewModel.showStorageDialog.collectAsStateWithLifecycle()
    val showFullPlayer by viewModel.showFullPlayer.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissToast()
        }
    }

    LaunchedEffect(playbackState.errorMessage) {
        playbackState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.playerController.clearError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Media Cache",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // Connectivity Badge
                    Surface(
                        shape = CircleShape,
                        color = if (isOnline) EmeraldSecondary.copy(alpha = 0.15f) else StatusStreaming.copy(alpha = 0.15f),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = if (isOnline) Icons.Default.Wifi else Icons.Default.CloudOff,
                                contentDescription = if (isOnline) "Online" else "Offline",
                                tint = if (isOnline) EmeraldSecondary else StatusStreaming,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isOnline) "Online" else "Offline",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isOnline) EmeraldSecondary else StatusStreaming
                            )
                        }
                    }

                    // Playlist Downloader Icon
                    IconButton(
                        onClick = { viewModel.setShowPlaylistDialog(true) },
                        modifier = Modifier.testTag("button_open_playlist_downloader")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = "Playlist Downloader",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Storage manager icon
                    IconButton(
                        onClick = { viewModel.setShowStorageDialog(true) },
                        modifier = Modifier.testTag("button_open_storage_manager")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = "Storage Manager",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = if (playbackState.currentMedia != null) 72.dp else 0.dp)
            ) {
                FloatingActionButton(
                    onClick = { viewModel.setShowPlaylistDialog(true) },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("fab_download_playlist")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = "Download Playlist",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Playlist", fontWeight = FontWeight.Bold)
                    }
                }

                ExtendedFloatingActionButton(
                    onClick = { viewModel.setShowAddDialog(true) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add Media") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("fab_add_media")
                )
            }
        },
        bottomBar = {
            if (playbackState.currentMedia != null) {
                MiniPlayerBar(
                    playbackState = playbackState,
                    onTogglePlayPause = { viewModel.playerController.togglePlayPause() },
                    onOpenFullPlayer = { viewModel.setShowFullPlayer(true) },
                    onClosePlayer = { viewModel.playerController.stop() },
                    modifier = Modifier.navigationBarsPadding()
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Offline Mode Warning Banner (if offline)
            AnimatedVisibility(visible = !isOnline) {
                Surface(
                    color = StatusStreaming.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = StatusStreaming,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Offline Mode: Cached media is available with 0 network usage.",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusStreaming,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Storage Overview Header Card with Visual Progress Indicator
            val cachedItemCount = remember(mediaList) {
                mediaList.count { it.cacheStatus == CacheStatus.CACHED.name }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { viewModel.setShowStorageDialog(true) }
                    .testTag("card_storage_overview"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldSecondary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = EmeraldSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Device Storage & Media Cache",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Cached media vs available space",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Manage",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    StorageProgressIndicator(
                        diskStats = diskStats,
                        cachedItemCount = cachedItemCount,
                        showDetails = true
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("Search by title, artist, or tag...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("input_search_media"),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MediaFilter.values().forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = {
                            viewModel.onFilterSelected(filter)
                            if (filter != MediaFilter.PLAYLISTS) {
                                viewModel.setSelectedPlaylist(null)
                            }
                        },
                        label = { Text(filter.label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("filter_chip_${filter.name}")
                    )
                }
            }

            // Playlist Sub-filter Bar (when Playlists tab is active or a playlist is selected)
            if (selectedFilter == MediaFilter.PLAYLISTS || selectedPlaylist != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = { viewModel.setShowPlaylistDialog(true) },
                        label = { Text("+ Download Playlist", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(Icons.Default.DownloadForOffline, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("chip_download_new_playlist")
                    )

                    FilterChip(
                        selected = selectedPlaylist == null,
                        onClick = { viewModel.setSelectedPlaylist(null) },
                        label = { Text("All Playlists", fontSize = 11.sp) },
                        modifier = Modifier.testTag("chip_playlist_all")
                    )

                    allPlaylists.forEach { playlistName ->
                        FilterChip(
                            selected = selectedPlaylist == playlistName,
                            onClick = {
                                if (selectedPlaylist == playlistName) {
                                    viewModel.setSelectedPlaylist(null)
                                } else {
                                    viewModel.setSelectedPlaylist(playlistName)
                                }
                            },
                            label = { Text(playlistName, fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.QueueMusic, contentDescription = null, modifier = Modifier.size(14.dp))
                            },
                            modifier = Modifier.testTag("chip_playlist_${playlistName.replace(" ", "_")}")
                        )
                    }
                }
            }

            // Active Playlist Summary Card
            if (selectedPlaylist != null) {
                val activeName = selectedPlaylist ?: ""
                val playlistItems = remember(mediaList, activeName) {
                    mediaList.filter { it.playlist.equals(activeName, ignoreCase = true) }
                }
                PlaylistSummaryCard(
                    playlistName = activeName,
                    items = playlistItems,
                    onDownloadPlaylist = { viewModel.downloadPlaylist(it) },
                    onExportPlaylistToStorage = { viewModel.exportPlaylistToStorage(it) },
                    onClearFilter = { viewModel.setSelectedPlaylist(null) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // Media List
            if (mediaList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedFilter != MediaFilter.ALL)
                                "No media matches your filter"
                            else "No media in cache library",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add self-hosted audio or video streams to cache and play offline anytime.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = if (playbackState.currentMedia != null) 90.dp else 80.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(mediaList, key = { it.id }) { item ->
                        val isCurrent = playbackState.currentMedia?.id == item.id
                        val progress = downloadProgressMap[item.id]

                        MediaItemCard(
                            media = item,
                            isPlaying = isCurrent && playbackState.isPlaying,
                            isCurrentMedia = isCurrent,
                            downloadProgress = progress,
                            onPlayClick = { viewModel.playMedia(item) },
                            onDownloadClick = { viewModel.startDownload(item) },
                            onCancelDownloadClick = { viewModel.cancelDownload(item.id) },
                            onClearCacheClick = { viewModel.clearItemCache(item) },
                            onDeleteClick = { viewModel.deleteMediaItem(item) },
                            onSaveToStorageClick = { viewModel.exportToDeviceStorage(item) }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAddDialog) {
        AddMediaDialog(
            onDismiss = { viewModel.setShowAddDialog(false) },
            onAddMedia = { title, artist, url, isVideo, tag, cacheImmediately, playlist ->
                viewModel.addNewMedia(
                    title = title,
                    artist = artist,
                    sourceUrl = url,
                    isVideo = isVideo,
                    tag = tag,
                    cacheImmediately = cacheImmediately,
                    playlist = playlist
                )
            }
        )
    }

    if (showPlaylistDialog) {
        PlaylistDownloadDialog(
            onDismiss = { viewModel.setShowPlaylistDialog(false) },
            onDownloadCurated = { curated ->
                viewModel.addAndDownloadPlaylist(curated.name, curated.items, cacheImmediately = true)
            },
            onDownloadCustomPlaylist = { name, items, cacheImmediately ->
                viewModel.addAndDownloadPlaylist(name, items, cacheImmediately)
            },
            onDownloadRemoteUrl = { name, url, cacheImmediately ->
                viewModel.importRemotePlaylist(name, url, cacheImmediately)
            }
        )
    }

    if (showStorageDialog) {
        val cachedCount = mediaList.count { it.cacheStatus == CacheStatus.CACHED.name }
        StorageManagerDialog(
            diskStats = diskStats,
            cachedItemCount = cachedCount,
            onPurgeAll = { viewModel.purgeAllCache() },
            onExportAllToStorage = { viewModel.exportAllToDeviceStorage() },
            onDismiss = { viewModel.setShowStorageDialog(false) }
        )
    }

    if (showFullPlayer && playbackState.currentMedia != null) {
        FullPlayerModal(
            playbackState = playbackState,
            playerController = viewModel.playerController,
            onDismiss = { viewModel.setShowFullPlayer(false) },
            onCacheCurrentMedia = {
                playbackState.currentMedia?.let { viewModel.startDownload(it) }
            }
        )
    }
}
