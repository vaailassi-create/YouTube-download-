package com.example.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.Surface
import com.example.data.CacheStatus
import com.example.data.MediaCacheEntity
import com.example.data.MediaDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream

data class PlaybackState(
    val currentMedia: MediaCacheEntity? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isLooping: Boolean = false,
    val isBuffering: Boolean = false,
    val isLocalCachedPlayback: Boolean = false,
    val errorMessage: String? = null
)

class MediaPlayerController(
    private val context: Context,
    private val mediaDao: MediaDao,
    private val scope: CoroutineScope
) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentFileInputStream: FileInputStream? = null
    private var attachedSurface: Surface? = null
    private var progressTickerJob: Job? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    fun playMedia(media: MediaCacheEntity) {
        scope.launch(Dispatchers.Main) {
            try {
                // If the same media is currently loaded and player exists
                if (_playbackState.value.currentMedia?.id == media.id && mediaPlayer != null) {
                    togglePlayPause()
                    return@launch
                }

                _playbackState.value = _playbackState.value.copy(
                    currentMedia = media,
                    isBuffering = true,
                    errorMessage = null
                )

                cleanupPlayer()

                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(
                                if (media.isVideo) AudioAttributes.CONTENT_TYPE_MOVIE
                                else AudioAttributes.CONTENT_TYPE_MUSIC
                            )
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )

                    // Check if a valid cached file exists locally
                    val localFile = media.localFilePath?.let { File(it) }
                    val isCached = localFile != null &&
                            localFile.exists() &&
                            localFile.length() > 0 &&
                            media.cacheStatus == CacheStatus.CACHED.name

                    if (isCached && localFile != null) {
                        // CRITICAL: Passing a local file path string to MediaPlayer fails with
                        // error (1, -2147483648) because mediaserver cannot access the app's private sandbox.
                        // We must open a FileInputStream and pass the FileDescriptor!
                        val fis = FileInputStream(localFile)
                        currentFileInputStream = fis
                        setDataSource(fis.fd, 0L, localFile.length())
                    } else {
                        // For remote URLs, provide standard headers so CDNs do not reject the connection
                        val uri = Uri.parse(media.sourceUrl)
                        val headers = mapOf(
                            "User-Agent" to "Mozilla/5.0 (Linux; Android 14) MediaCache/1.0",
                            "Accept" to "*/*"
                        )
                        setDataSource(context, uri, headers)
                    }

                    attachedSurface?.let { setSurface(it) }

                    isLooping = _playbackState.value.isLooping

                    setOnPreparedListener { mp ->
                        val duration = mp.duration.toLong().coerceAtLeast(0L)
                        _playbackState.value = _playbackState.value.copy(
                            isBuffering = false,
                            isPlaying = true,
                            durationMs = duration,
                            isLocalCachedPlayback = isCached,
                            errorMessage = null
                        )

                        // Resume from last position if valid
                        if (media.lastPlayedPositionMs > 0 && media.lastPlayedPositionMs < duration - 2000) {
                            mp.seekTo(media.lastPlayedPositionMs.toInt())
                        }

                        applyPlaybackSpeed(_playbackState.value.playbackSpeed)
                        mp.start()
                        startTicker()
                    }

                    setOnBufferingUpdateListener { _, _ ->
                        // Buffering progress update
                    }

                    setOnCompletionListener {
                        _playbackState.value = _playbackState.value.copy(isPlaying = false)
                        stopTicker()
                        savePlaybackPosition(0L)
                    }

                    setOnErrorListener { mp, what, extra ->
                        Log.e("MediaPlayerController", "MediaPlayer error: what=$what, extra=$extra")
                        stopTicker()
                        try {
                            mp.reset()
                        } catch (e: Exception) {
                            // Ignored
                        }

                        val friendlyMessage = when (extra) {
                            -2147483648 -> {
                                if (isCached) {
                                    "Error accessing cached file. Try caching again."
                                } else {
                                    "Streaming direct failed. Tap 'Cache Offline' to download and play seamlessly."
                                }
                            }
                            MediaPlayer.MEDIA_ERROR_IO -> "Media network I/O error occurred"
                            MediaPlayer.MEDIA_ERROR_MALFORMED -> "Media format appears corrupted or unsupported"
                            MediaPlayer.MEDIA_ERROR_UNSUPPORTED -> "Unsupported media encoding"
                            MediaPlayer.MEDIA_ERROR_TIMED_OUT -> "Media stream connection timed out"
                            else -> "Playback error (code $what, $extra)"
                        }

                        _playbackState.value = _playbackState.value.copy(
                            isPlaying = false,
                            isBuffering = false,
                            errorMessage = friendlyMessage
                        )
                        true
                    }

                    prepareAsync()
                }
            } catch (e: Exception) {
                Log.e("MediaPlayerController", "Exception initializing playback", e)
                _playbackState.value = _playbackState.value.copy(
                    isBuffering = false,
                    isPlaying = false,
                    errorMessage = e.localizedMessage ?: "Failed to initialize playback"
                )
            }
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: return
        try {
            if (player.isPlaying) {
                player.pause()
                _playbackState.value = _playbackState.value.copy(isPlaying = false)
                stopTicker()
                saveCurrentPosition()
            } else {
                player.start()
                _playbackState.value = _playbackState.value.copy(isPlaying = true)
                startTicker()
            }
        } catch (e: Exception) {
            Log.e("MediaPlayerController", "Error toggling play/pause", e)
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let { player ->
            try {
                player.seekTo(positionMs.toInt())
                _playbackState.value = _playbackState.value.copy(currentPositionMs = positionMs)
                savePlaybackPosition(positionMs)
            } catch (e: Exception) {
                Log.e("MediaPlayerController", "Error seeking", e)
            }
        }
    }

    fun skipForward(seconds: Int = 30) {
        val player = mediaPlayer ?: return
        try {
            val newPos = (player.currentPosition + seconds * 1000).coerceAtMost(player.duration)
            seekTo(newPos.toLong())
        } catch (e: Exception) {
            // Ignored
        }
    }

    fun skipBackward(seconds: Int = 10) {
        val player = mediaPlayer ?: return
        try {
            val newPos = (player.currentPosition - seconds * 1000).coerceAtLeast(0)
            seekTo(newPos.toLong())
        } catch (e: Exception) {
            // Ignored
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackState.value = _playbackState.value.copy(playbackSpeed = speed)
        applyPlaybackSpeed(speed)
    }

    private fun applyPlaybackSpeed(speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mediaPlayer?.let { player ->
                try {
                    val params = player.playbackParams ?: PlaybackParams()
                    params.speed = speed
                    player.playbackParams = params
                } catch (e: Exception) {
                    // Ignored on some devices
                }
            }
        }
    }

    fun toggleLoop() {
        val newLoop = !_playbackState.value.isLooping
        _playbackState.value = _playbackState.value.copy(isLooping = newLoop)
        try {
            mediaPlayer?.isLooping = newLoop
        } catch (e: Exception) {
            // Ignored
        }
    }

    fun attachSurface(surface: Surface?) {
        attachedSurface = surface
        try {
            mediaPlayer?.setSurface(surface)
        } catch (e: Exception) {
            Log.e("MediaPlayerController", "Error attaching surface", e)
        }
    }

    fun detachSurface() {
        attachedSurface = null
        try {
            mediaPlayer?.setSurface(null)
        } catch (e: Exception) {
            // Ignored
        }
    }

    fun clearError() {
        _playbackState.value = _playbackState.value.copy(errorMessage = null)
    }

    private fun startTicker() {
        stopTicker()
        progressTickerJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                mediaPlayer?.let { player ->
                    try {
                        if (player.isPlaying) {
                            val current = player.currentPosition.toLong()
                            val duration = player.duration.toLong().coerceAtLeast(0L)
                            _playbackState.value = _playbackState.value.copy(
                                currentPositionMs = current,
                                durationMs = duration
                            )
                        }
                    } catch (e: Exception) {
                        // Player may have been reset
                    }
                }
                delay(300)
            }
        }
    }

    private fun stopTicker() {
        progressTickerJob?.cancel()
        progressTickerJob = null
    }

    private fun saveCurrentPosition() {
        mediaPlayer?.let { player ->
            try {
                val pos = player.currentPosition.toLong()
                savePlaybackPosition(pos)
            } catch (e: Exception) {
                // Ignored
            }
        }
    }

    private fun savePlaybackPosition(pos: Long) {
        val mediaId = _playbackState.value.currentMedia?.id ?: return
        val duration = _playbackState.value.durationMs
        scope.launch(Dispatchers.IO) {
            try {
                mediaDao.updatePlaybackPosition(mediaId, pos, duration)
            } catch (e: Exception) {
                // Ignored
            }
        }
    }

    private fun cleanupPlayer() {
        try {
            mediaPlayer?.stop()
        } catch (e: Exception) {
            // Ignored
        }
        try {
            mediaPlayer?.release()
        } catch (e: Exception) {
            // Ignored
        }
        mediaPlayer = null

        try {
            currentFileInputStream?.close()
        } catch (e: Exception) {
            // Ignored
        }
        currentFileInputStream = null
    }

    fun stop() {
        stopTicker()
        saveCurrentPosition()
        cleanupPlayer()
        _playbackState.value = PlaybackState()
    }

    fun release() {
        stopTicker()
        cleanupPlayer()
    }

    companion object {
        fun formatDuration(ms: Long): String {
            if (ms <= 0) return "00:00"
            val totalSeconds = ms / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val hours = minutes / 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes % 60, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }
    }
}
