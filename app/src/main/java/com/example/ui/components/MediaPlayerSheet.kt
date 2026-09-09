package com.example.ui.components

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.CacheStatus
import com.example.player.MediaPlayerController
import com.example.player.PlaybackState
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSecondary
import com.example.ui.theme.StatusStreaming
import kotlin.math.sin

@Composable
fun MiniPlayerBar(
    playbackState: PlaybackState,
    onTogglePlayPause: () -> Unit,
    onOpenFullPlayer: () -> Unit,
    onClosePlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val media = playbackState.currentMedia ?: return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenFullPlayer() }
            .testTag("mini_player_bar"),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Linear Progress along top
            val progress = if (playbackState.durationMs > 0) {
                playbackState.currentPositionMs.toFloat() / playbackState.durationMs.toFloat()
            } else 0f

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = if (playbackState.isLocalCachedPlayback) EmeraldSecondary else CyanPrimary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Media Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (media.isVideo) Icons.Default.PlayArrow else Icons.Default.Audiotrack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Track Info & Offline Badge
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = media.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (playbackState.isLocalCachedPlayback) "Offline (Local)" else "Streaming",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (playbackState.isLocalCachedPlayback) EmeraldSecondary else StatusStreaming,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = " • ${MediaPlayerController.formatDuration(playbackState.currentPositionMs)} / ${MediaPlayerController.formatDuration(playbackState.durationMs)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Play / Pause
                FilledIconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("mini_play_pause"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Close
                IconButton(
                    onClick = onClosePlayer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Stop & Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerModal(
    playbackState: PlaybackState,
    playerController: MediaPlayerController,
    onDismiss: () -> Unit,
    onCacheCurrentMedia: () -> Unit
) {
    val media = playbackState.currentMedia ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(0f) }

    val currentPos = if (isDraggingSlider) sliderValue else playbackState.currentPositionMs.toFloat()
    val maxPos = playbackState.durationMs.coerceAtLeast(1L).toFloat()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("full_player_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Cache Source Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (playbackState.isLocalCachedPlayback)
                        EmeraldSecondary.copy(alpha = 0.15f)
                    else StatusStreaming.copy(alpha = 0.15f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (playbackState.isLocalCachedPlayback) Icons.Default.CheckCircle else Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = if (playbackState.isLocalCachedPlayback) EmeraldSecondary else StatusStreaming,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (playbackState.isLocalCachedPlayback) "Playing Offline Cache" else "Streaming Direct",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (playbackState.isLocalCachedPlayback) EmeraldSecondary else StatusStreaming
                        )
                    }
                }

                IconButton(onClick = { playerController.toggleLoop() }) {
                    Icon(
                        imageVector = if (playbackState.isLooping) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "Loop",
                        tint = if (playbackState.isLooping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Video Display or Audio Visualizer Canvas
            if (media.isVideo) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            SurfaceView(ctx).apply {
                                holder.addCallback(object : SurfaceHolder.Callback {
                                    override fun surfaceCreated(holder: SurfaceHolder) {
                                        playerController.attachSurface(holder.surface)
                                    }

                                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

                                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                                        playerController.detachSurface()
                                    }
                                })
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    DisposableEffect(Unit) {
                        onDispose {
                            playerController.detachSurface()
                        }
                    }
                }
            } else {
                // Audio Canvas Visualizer
                AudioVisualizerCard(isPlaying = playbackState.isPlaying)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Track Title & Details
            Text(
                text = media.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${media.artist} • ${media.tag}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // Cache prompt if not yet cached
            if (!playbackState.isLocalCachedPlayback && media.cacheStatus != CacheStatus.CACHED.name) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onCacheCurrentMedia,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save to Local Cache for Offline Playback", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrubbing Slider
            Slider(
                value = currentPos.coerceIn(0f, maxPos),
                onValueChange = {
                    isDraggingSlider = true
                    sliderValue = it
                },
                onValueChangeFinished = {
                    isDraggingSlider = false
                    playerController.seekTo(sliderValue.toLong())
                },
                valueRange = 0f..maxPos,
                colors = SliderDefaults.colors(
                    thumbColor = if (playbackState.isLocalCachedPlayback) EmeraldSecondary else CyanPrimary,
                    activeTrackColor = if (playbackState.isLocalCachedPlayback) EmeraldSecondary else CyanPrimary,
                    inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Timestamps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = MediaPlayerController.formatDuration(currentPos.toLong()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = MediaPlayerController.formatDuration(playbackState.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Playback Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Rewind 10s
                IconButton(
                    onClick = { playerController.skipBackward(10) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "Rewind 10s",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Play / Pause Large FAB
                FilledIconButton(
                    onClick = { playerController.togglePlayPause() },
                    modifier = Modifier
                        .size(68.dp)
                        .testTag("full_player_play_pause"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (playbackState.isLocalCachedPlayback) EmeraldSecondary else CyanPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Fast Forward 30s
                IconButton(
                    onClick = { playerController.skipForward(30) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Forward 30s",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Playback Speed Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                    FilterChip(
                        selected = playbackState.playbackSpeed == speed,
                        onClick = { playerController.setPlaybackSpeed(speed) },
                        label = { Text("${speed}x", fontSize = 11.sp) },
                        modifier = Modifier.padding(horizontal = 3.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun AudioVisualizerCard(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer")
    val animPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val barCount = 32
            val barWidth = size.width / (barCount * 1.5f)
            val centerY = size.height / 2f
            val maxBarHeight = size.height * 0.7f

            for (i in 0 until barCount) {
                val x = i * (barWidth * 1.5f) + barWidth * 0.5f
                val factor = if (isPlaying) {
                    val sin1 = sin((i * 0.3f + animPhase).toDouble()).toFloat()
                    val sin2 = sin((i * 0.7f - animPhase).toDouble()).toFloat()
                    ((sin1 + sin2 + 2f) / 4f).coerceIn(0.15f, 1f)
                } else {
                    0.15f
                }
                val barH = maxBarHeight * factor

                drawRoundRect(
                    color = CyanPrimary.copy(alpha = 0.35f + factor * 0.55f),
                    topLeft = Offset(x, centerY - barH / 2f),
                    size = Size(barWidth, barH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                )
            }
        }

        // Center floating glowing music icon
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = null,
                tint = CyanPrimary,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
