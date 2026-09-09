package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.MediaCacheEntity
import com.example.util.CuratedPlaylist
import com.example.util.PlaylistParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDownloadDialog(
    onDismiss: () -> Unit,
    onDownloadCurated: (CuratedPlaylist) -> Unit,
    onDownloadCustomPlaylist: (name: String, items: List<MediaCacheEntity>, cacheImmediately: Boolean) -> Unit,
    onDownloadRemoteUrl: (name: String, url: String, cacheImmediately: Boolean) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Presets, 1 = Custom M3U / URLs

    // Custom Playlist State
    var playlistName by remember { mutableStateOf("") }
    var playlistUrlsText by remember { mutableStateOf("") }
    var remotePlaylistUrl by remember { mutableStateOf("") }
    var isRemoteUrlMode by remember { mutableStateOf(false) }
    var cacheImmediately by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .testTag("playlist_download_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.QueueMusic,
                                    contentDescription = "Playlist",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Playlist Downloader",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Download & cache full collections",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Presets") },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("tab_presets")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("M3U / URLs") },
                        icon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("tab_custom_playlist")
                    )
                }

                if (selectedTab == 0) {
                    // Presets Tab
                    Text(
                        text = "Curated Ready-to-Download Playlists",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )

                    PlaylistParser.CuratedPlaylists.forEach { playlist ->
                        CuratedPlaylistItemCard(
                            playlist = playlist,
                            onDownload = {
                                onDownloadCurated(playlist)
                                onDismiss()
                            }
                        )
                    }
                } else {
                    // Custom / M3U Tab
                    Text(
                        text = "Import Custom M3U or Media URLs",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedTextField(
                        value = playlistName,
                        onValueChange = {
                            playlistName = it
                            errorMessage = null
                        },
                        label = { Text("Playlist Name") },
                        placeholder = { Text("e.g., Roadtrip Chill Sessions") },
                        leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_playlist_name_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Switch between raw URLs vs remote M3U URL
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !isRemoteUrlMode,
                            onClick = { isRemoteUrlMode = false },
                            label = { Text("Paste URLs / M3U") },
                            leadingIcon = {
                                if (!isRemoteUrlMode) Icon(Icons.Default.Check, contentDescription = null, Modifier.size(16.dp))
                            },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = isRemoteUrlMode,
                            onClick = { isRemoteUrlMode = true },
                            label = { Text("M3U Web URL") },
                            leadingIcon = {
                                if (isRemoteUrlMode) Icon(Icons.Default.Check, contentDescription = null, Modifier.size(16.dp))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (isRemoteUrlMode) {
                        OutlinedTextField(
                            value = remotePlaylistUrl,
                            onValueChange = {
                                remotePlaylistUrl = it
                                errorMessage = null
                            },
                            label = { Text("Remote Playlist URL (.m3u / .m3u8)") },
                            placeholder = { Text("https://example.com/stream/playlist.m3u") },
                            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("remote_playlist_url_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    } else {
                        OutlinedTextField(
                            value = playlistUrlsText,
                            onValueChange = {
                                playlistUrlsText = it
                                errorMessage = null
                            },
                            label = { Text("Paste URLs or M3U Content") },
                            placeholder = {
                                Text(
                                    "#EXTM3U\nhttps://domain.com/track1.mp3\nhttps://domain.com/track2.mp3\n\n(or 1 URL per line)"
                                )
                            },
                            minLines = 4,
                            maxLines = 8,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_playlist_urls_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = cacheImmediately,
                            onCheckedChange = { cacheImmediately = it },
                            modifier = Modifier.testTag("checkbox_cache_immediately")
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Download all items immediately to offline cache",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    AnimatedVisibility(visible = errorMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val name = playlistName.trim().ifBlank { "My Playlist" }
                            if (isRemoteUrlMode) {
                                if (remotePlaylistUrl.isBlank() || (!remotePlaylistUrl.startsWith("http://", ignoreCase = true) && !remotePlaylistUrl.startsWith("https://", ignoreCase = true))) {
                                    errorMessage = "Please enter a valid http/https URL for the remote playlist."
                                    return@Button
                                }
                                onDownloadRemoteUrl(name, remotePlaylistUrl.trim(), cacheImmediately)
                                onDismiss()
                            } else {
                                if (playlistUrlsText.isBlank()) {
                                    errorMessage = "Please paste at least one URL or M3U track entry."
                                    return@Button
                                }
                                val parsed = PlaylistParser.parseM3uOrUrlText(name, playlistUrlsText)
                                if (parsed.isEmpty()) {
                                    errorMessage = "No valid URLs found in input. Ensure each URL begins with http:// or https://"
                                    return@Button
                                }
                                onDownloadCustomPlaylist(name, parsed, cacheImmediately)
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_start_playlist_download"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (cacheImmediately) "Download Entire Playlist" else "Save Playlist",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CuratedPlaylistItemCard(
    playlist: CuratedPlaylist,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${playlist.items.size} items",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Text(
                        text = playlist.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Track preview list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                playlist.items.take(3).forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            if (item.isVideo) Icons.Default.Videocam else Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${item.title} (${item.artist})",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Action button
            Button(
                onClick = onDownload,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.DownloadForOffline, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Download Playlist (${formatBytes(playlist.totalSizeBytes)})", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format("%.1f MB", mb)
    } else {
        String.format("%.0f KB", kb)
    }
}
