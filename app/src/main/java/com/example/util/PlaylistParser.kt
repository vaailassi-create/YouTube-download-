package com.example.util

import com.example.data.MediaCacheEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class CuratedPlaylist(
    val name: String,
    val description: String,
    val category: String,
    val items: List<MediaCacheEntity>,
    val totalSizeBytes: Long
)

object PlaylistParser {

    val CuratedPlaylists: List<CuratedPlaylist> = listOf(
        CuratedPlaylist(
            name = "Lo-Fi & Synth Sessions",
            description = "Relaxing CC-BY electronic and chill beats for focus & study",
            category = "Audio Playlist",
            totalSizeBytes = 9_790_000L,
            items = listOf(
                MediaCacheEntity(
                    title = "Acrylic Cortices Synth",
                    artist = "Sevish Audio Studio",
                    sourceUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__acrylic_cortices.mp3",
                    mimeType = "audio/mpeg",
                    isVideo = false,
                    totalBytes = 4_520_000L,
                    tag = "Synth Track",
                    playlist = "Lo-Fi & Synth Sessions"
                ),
                MediaCacheEntity(
                    title = "Lepidoptera Chill Track",
                    artist = "Creative Commons Studio",
                    sourceUrl = "https://commondatastorage.googleapis.com/codeskulptor-assets/Epoq-Lepidoptera.mp3",
                    mimeType = "audio/mpeg",
                    isVideo = false,
                    totalBytes = 3_120_000L,
                    tag = "Chill Lo-Fi",
                    playlist = "Lo-Fi & Synth Sessions"
                ),
                MediaCacheEntity(
                    title = "Heavy Rain Ambiance",
                    artist = "Nature Sounds Collective",
                    sourceUrl = "https://actions.google.com/sounds/v1/ambiences/rain_heavy.ogg",
                    mimeType = "audio/ogg",
                    isVideo = false,
                    totalBytes = 2_150_000L,
                    tag = "Nature Ambient",
                    playlist = "Lo-Fi & Synth Sessions"
                )
            )
        ),
        CuratedPlaylist(
            name = "Open Cinema Trailer Pack",
            description = "High-definition Blender Foundation open-movie video clips",
            category = "Video Playlist",
            totalSizeBytes = 36_800_000L,
            items = listOf(
                MediaCacheEntity(
                    title = "Tears of Steel 4K Clip",
                    artist = "Blender Foundation (CC-BY)",
                    sourceUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                    mimeType = "video/mp4",
                    isVideo = true,
                    totalBytes = 14_000_000L,
                    tag = "Sci-Fi Video",
                    playlist = "Open Cinema Trailer Pack"
                ),
                MediaCacheEntity(
                    title = "Sintel Animated Trailer",
                    artist = "Durian Open Movie (CC-BY)",
                    sourceUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                    mimeType = "video/mp4",
                    isVideo = true,
                    totalBytes = 7_000_000L,
                    tag = "Animation",
                    playlist = "Open Cinema Trailer Pack"
                ),
                MediaCacheEntity(
                    title = "Big Buck Bunny 480p Clip",
                    artist = "Blender Open Source Studio",
                    sourceUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    mimeType = "video/mp4",
                    isVideo = true,
                    totalBytes = 15_800_000L,
                    tag = "Video Animation",
                    playlist = "Open Cinema Trailer Pack"
                )
            )
        ),
        CuratedPlaylist(
            name = "Self-Hosted Study Soundscape",
            description = "Concentration soundscape with ambient rain and chill synthesis",
            category = "Ambient Playlist",
            totalSizeBytes = 6_670_000L,
            items = listOf(
                MediaCacheEntity(
                    title = "Heavy Rain Ambiance",
                    artist = "Nature Sounds Collective",
                    sourceUrl = "https://actions.google.com/sounds/v1/ambiences/rain_heavy.ogg",
                    mimeType = "audio/ogg",
                    isVideo = false,
                    totalBytes = 2_150_000L,
                    tag = "Nature Ambient",
                    playlist = "Self-Hosted Study Soundscape"
                ),
                MediaCacheEntity(
                    title = "Acrylic Cortices Synth",
                    artist = "Sevish Audio Studio",
                    sourceUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__acrylic_cortices.mp3",
                    mimeType = "audio/mpeg",
                    isVideo = false,
                    totalBytes = 4_520_000L,
                    tag = "Synth Track",
                    playlist = "Self-Hosted Study Soundscape"
                )
            )
        )
    )

    /**
     * Parses M3U, M3U8, or plain text list of URLs into MediaCacheEntity items.
     */
    fun parseM3uOrUrlText(
        playlistName: String,
        rawText: String,
        defaultArtist: String = "Playlist Import"
    ): List<MediaCacheEntity> {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val results = mutableListOf<MediaCacheEntity>()

        var pendingTitle: String? = null
        var pendingArtist: String? = null

        for (line in lines) {
            if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                // Example: #EXTINF:123,Artist - Title OR #EXTINF:-1,Title
                val commaIndex = line.indexOf(',')
                if (commaIndex != -1 && commaIndex < line.length - 1) {
                    val info = line.substring(commaIndex + 1).trim()
                    if (info.contains(" - ")) {
                        pendingArtist = info.substringBefore(" - ").trim()
                        pendingTitle = info.substringAfter(" - ").trim()
                    } else {
                        pendingTitle = info
                        pendingArtist = defaultArtist
                    }
                }
            } else if (line.startsWith("http://", ignoreCase = true) || line.startsWith("https://", ignoreCase = true)) {
                val url = line
                val title = pendingTitle?.ifBlank { null }
                    ?: extractTitleFromUrl(url)
                val artist = pendingArtist?.ifBlank { null } ?: defaultArtist

                val lower = url.lowercase()
                val isVideo = lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".webm") || lower.endsWith(".mov")
                val mimeType = when {
                    isVideo -> "video/mp4"
                    lower.endsWith(".mp3") -> "audio/mpeg"
                    lower.endsWith(".ogg") -> "audio/ogg"
                    lower.endsWith(".wav") -> "audio/wav"
                    lower.endsWith(".flac") -> "audio/flac"
                    lower.endsWith(".pdf") -> "application/pdf"
                    lower.endsWith(".zip") -> "application/zip"
                    else -> if (isVideo) "video/mp4" else "audio/mpeg"
                }

                results.add(
                    MediaCacheEntity(
                        title = title,
                        artist = artist,
                        sourceUrl = url,
                        mimeType = mimeType,
                        isVideo = isVideo,
                        playlist = playlistName.ifBlank { "Imported Playlist" },
                        tag = if (isVideo) "Video" else "Audio"
                    )
                )

                // Reset pending metadata for next entry
                pendingTitle = null
                pendingArtist = null
            }
        }

        return results
    }

    /**
     * Fetches a remote M3U / M3U8 / TXT file from a URL and parses it.
     */
    suspend fun fetchRemotePlaylist(
        playlistName: String,
        url: String
    ): Result<List<MediaCacheEntity>> = withContext(Dispatchers.IO) {
        try {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("User-Agent", "MediaCacheDownloader/1.0")
            }
            if (connection.responseCode !in 200..299) {
                return@withContext Result.failure(Exception("HTTP error ${connection.responseCode} while fetching playlist"))
            }

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val text = reader.readText()
            reader.close()

            val items = parseM3uOrUrlText(playlistName, text)
            if (items.isEmpty()) {
                Result.failure(Exception("No valid media URLs found in playlist."))
            } else {
                Result.success(items)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractTitleFromUrl(url: String): String {
        val clean = url.substringBefore('?').substringBefore('#')
        val lastSegment = clean.substringAfterLast('/')
        return if (lastSegment.isNotBlank()) {
            lastSegment.replace('_', ' ').replace('-', ' ').substringBeforeLast('.')
        } else {
            "Track ${System.currentTimeMillis() % 1000}"
        }
    }
}
