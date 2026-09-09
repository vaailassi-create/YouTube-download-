package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cache.DownloadProgress
import com.example.cache.MediaCacheManager
import com.example.data.CacheStatus
import com.example.data.MediaCacheEntity
import com.example.ui.theme.EmeraldSecondary
import com.example.ui.theme.StatusFailed
import com.example.ui.theme.StatusStreaming

@Composable
fun MediaItemCard(
    media: MediaCacheEntity,
    isPlaying: Boolean,
    isCurrentMedia: Boolean,
    downloadProgress: DownloadProgress?,
    onPlayClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onCancelDownloadClick: () -> Unit,
    onClearCacheClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onSaveToStorageClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val isCached = media.cacheStatus == CacheStatus.CACHED.name
    val isDownloading = media.cacheStatus == CacheStatus.DOWNLOADING.name

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("media_item_card_${media.id}")
            .clickable { onPlayClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentMedia) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrentMedia) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Media Type Icon Badge
                val isAudio = media.mimeType.contains("audio", ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (media.isVideo) MaterialTheme.colorScheme.primaryContainer
                            else if (isAudio) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.tertiaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (media.isVideo) Icons.Default.Videocam
                        else if (isAudio) Icons.Default.Audiotrack
                        else Icons.Default.Folder,
                        contentDescription = if (media.isVideo) "Video" else if (isAudio) "Audio" else "File",
                        tint = if (media.isVideo) MaterialTheme.colorScheme.onPrimaryContainer
                        else if (isAudio) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title & Subtitle
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = media.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = media.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (media.playlist.isNotBlank() && media.playlist != "Default") {
                            Text(
                                text = " • ${media.playlist}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = " • ${media.tag}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Play / Pause Button
                FilledIconButton(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("play_button_${media.id}"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isCurrentMedia && isPlaying) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (isCurrentMedia && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isCurrentMedia && isPlaying) "Pause" else "Play",
                        tint = if (isCurrentMedia && isPlaying) MaterialTheme.colorScheme.onSecondary
                        else MaterialTheme.colorScheme.onPrimary
                    )
                }

                // Overflow Menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.testTag("menu_button_${media.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Save to Device Storage") },
                            onClick = {
                                showMenu = false
                                onSaveToStorageClick()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        )
                        if (isCached) {
                            DropdownMenuItem(
                                text = { Text("Clear Local Cache") },
                                onClick = {
                                    showMenu = false
                                    onClearCacheClick()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                }
                            )
                        } else if (!isDownloading) {
                            DropdownMenuItem(
                                text = { Text("Cache for Offline") },
                                onClick = {
                                    showMenu = false
                                    onDownloadClick()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Download, contentDescription = null)
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Remove from Library", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDeleteClick()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Cache Status Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Status Pill
                StatusPill(
                    cacheStatus = media.cacheStatus,
                    downloadProgress = downloadProgress,
                    downloadedBytes = media.downloadedBytes,
                    totalBytes = media.totalBytes
                )

                // Cache Action Button
                if (isDownloading) {
                    OutlinedIconButton(
                        onClick = onCancelDownloadClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel Download",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else if (!isCached) {
                    Surface(
                        onClick = onDownloadClick,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.clip(CircleShape)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "Download Cache",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Cache Offline",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            onClick = onSaveToStorageClick,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.clip(CircleShape)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = "Save to Device Storage",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "To Storage",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Downloading Progress Bar
            AnimatedVisibility(visible = isDownloading) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    val progressFloat = downloadProgress?.percent?.let { it / 100f } ?: 0f
                    LinearProgressIndicator(
                        progress = { progressFloat },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${downloadProgress?.percent ?: 0}% (${MediaCacheManager.formatBytes(downloadProgress?.downloadedBytes ?: 0L)} / ${MediaCacheManager.formatBytes(downloadProgress?.totalBytes ?: 0L)})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = downloadProgress?.speedBytesPerSec?.let { MediaCacheManager.formatSpeed(it) } ?: "Caching...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusPill(
    cacheStatus: String,
    downloadProgress: DownloadProgress?,
    downloadedBytes: Long,
    totalBytes: Long
) {
    val (bgColor, textColor, text, icon) = when (cacheStatus) {
        CacheStatus.CACHED.name -> Quad(
            EmeraldSecondary.copy(alpha = 0.15f),
            EmeraldSecondary,
            "Cached Offline (${MediaCacheManager.formatBytes(if (downloadedBytes > 0) downloadedBytes else totalBytes)})",
            Icons.Default.CheckCircle
        )
        CacheStatus.DOWNLOADING.name -> Quad(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.primary,
            "Caching ${downloadProgress?.percent ?: 0}%",
            Icons.Default.Download
        )
        CacheStatus.FAILED.name -> Quad(
            StatusFailed.copy(alpha = 0.15f),
            StatusFailed,
            "Cache Failed",
            Icons.Default.ErrorOutline
        )
        else -> Quad(
            StatusStreaming.copy(alpha = 0.15f),
            StatusStreaming,
            "Streaming Only",
            Icons.Default.CloudDownload
        )
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = textColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
