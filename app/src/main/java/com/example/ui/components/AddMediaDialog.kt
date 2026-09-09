package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class MediaPreset(
    val title: String,
    val artist: String,
    val url: String,
    val isVideo: Boolean,
    val tag: String,
    val isGenericFile: Boolean = false
)

val SamplePresets = listOf(
    MediaPreset(
        title = "Acrylic Cortices (CC-BY)",
        artist = "Sevish Audio Studio",
        url = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__acrylic_cortices.mp3",
        isVideo = false,
        tag = "Synth Track"
    ),
    MediaPreset(
        title = "Tears of Steel 4K Clip",
        artist = "Blender Foundation (CC-BY)",
        url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
        isVideo = true,
        tag = "Sci-Fi Video"
    ),
    MediaPreset(
        title = "Lepidoptera Chill Track",
        artist = "Creative Commons Studio",
        url = "https://commondatastorage.googleapis.com/codeskulptor-assets/Epoq-Lepidoptera.mp3",
        isVideo = false,
        tag = "Chill Lo-Fi"
    ),
    MediaPreset(
        title = "RFC 2616 Network Specs",
        artist = "IETF Network Working Group",
        url = "https://www.ietf.org/rfc/rfc2616.txt",
        isVideo = false,
        tag = "Text Document",
        isGenericFile = true
    ),
    MediaPreset(
        title = "Sintel Animated Trailer",
        artist = "Durian Open Movie (CC-BY)",
        url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
        isVideo = true,
        tag = "Animation"
    ),
    MediaPreset(
        title = "Heavy Rain Ambiance",
        artist = "Open Sound Collection",
        url = "https://actions.google.com/sounds/v1/ambiences/rain_heavy.ogg",
        isVideo = false,
        tag = "Nature Ambient"
    )
)

@Composable
fun AddMediaDialog(
    onDismiss: () -> Unit,
    onAddMedia: (title: String, artist: String, url: String, isVideo: Boolean, tag: String, cacheImmediately: Boolean, playlist: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var mediaType by remember { mutableStateOf("AUDIO") } // "AUDIO", "VIDEO", "FILE"
    var tag by remember { mutableStateOf("Podcast") }
    var playlist by remember { mutableStateOf("") }
    var cacheImmediately by remember { mutableStateOf(true) }
    var urlError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Download Any File to Storage",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Quick Load Sample Preset:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SamplePresets.forEach { preset ->
                        SuggestionChip(
                            onClick = {
                                title = preset.title
                                artist = preset.artist
                                url = preset.url
                                mediaType = when {
                                    preset.isVideo -> "VIDEO"
                                    preset.isGenericFile -> "FILE"
                                    else -> "AUDIO"
                                }
                                tag = preset.tag
                                urlError = false
                            },
                            label = { Text(preset.title, fontSize = 11.sp, maxLines = 1) },
                            icon = {
                                Icon(
                                    imageVector = when {
                                        preset.isVideo -> Icons.Default.Videocam
                                        preset.isGenericFile -> Icons.Default.Folder
                                        else -> Icons.Default.Audiotrack
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Media Type Selector: Audio, Video, Any File
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = mediaType == "AUDIO",
                        onClick = {
                            mediaType = "AUDIO"
                            if (tag == "Video" || tag == "File") tag = "Audio"
                        },
                        label = { Text("Audio", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Audiotrack, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                    FilterChip(
                        selected = mediaType == "VIDEO",
                        onClick = {
                            mediaType = "VIDEO"
                            if (tag == "Audio" || tag == "File") tag = "Video"
                        },
                        label = { Text("Video", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                    FilterChip(
                        selected = mediaType == "FILE",
                        onClick = {
                            mediaType = "FILE"
                            if (tag == "Audio" || tag == "Video") tag = "File"
                        },
                        label = { Text("Any File", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Direct Media URL
                OutlinedTextField(
                    value = url,
                    onValueChange = { newUrl ->
                        url = newUrl
                        urlError = false
                        // Auto-extract filename if title is blank
                        if (title.isBlank() && newUrl.contains("://")) {
                            val clean = newUrl.substringBefore('?').substringBefore('#')
                            val segment = clean.substringAfterLast('/', "")
                            if (segment.isNotBlank()) title = segment
                        }
                        // Auto-detect type from extension
                        val lower = newUrl.lowercase()
                        when {
                            lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".webm") -> mediaType = "VIDEO"
                            lower.endsWith(".mp3") || lower.endsWith(".ogg") || lower.endsWith(".wav") || lower.endsWith(".flac") || lower.endsWith(".m4a") -> mediaType = "AUDIO"
                            lower.endsWith(".pdf") || lower.endsWith(".zip") || lower.endsWith(".txt") || lower.endsWith(".tar") || lower.endsWith(".json") -> mediaType = "FILE"
                        }
                    },
                    label = { Text("File / Media URL (HTTP/HTTPS)") },
                    placeholder = { Text("https://my-nas.local:8080/data.zip") },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                    isError = urlError,
                    supportingText = {
                        if (urlError) Text("Valid file URL is required", color = MaterialTheme.colorScheme.error)
                        else Text("Download any file (MP3, MP4, PDF, ZIP, OGG, WAV, TXT)")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_media_url"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Filename / Title") },
                    placeholder = { Text("My_Archive.zip or Episode 42") },
                    leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_media_title"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Artist / Host
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Source / Host / Server") },
                    placeholder = { Text("Cloud Storage / NAS / Studio") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Tag / Category
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text("Category / Tag") },
                    placeholder = { Text("Podcast, Archive, Document, Video") },
                    leadingIcon = { Icon(Icons.Default.Bookmark, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Playlist
                OutlinedTextField(
                    value = playlist,
                    onValueChange = { playlist = it },
                    label = { Text("Playlist Name (Optional)") },
                    placeholder = { Text("e.g. Chill Beats, Work Mix") },
                    leadingIcon = { Icon(Icons.Default.QueueMusic, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_media_playlist"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Cache Immediately Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = cacheImmediately,
                        onCheckedChange = { cacheImmediately = it },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("checkbox_cache_immediately")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Download to storage immediately",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Fetch and cache the file on device storage right now",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (url.isBlank() || !url.contains("://")) {
                        urlError = true
                        return@Button
                    }
                    val finalTitle = if (title.isBlank()) {
                        val clean = url.substringBefore('?').substringBefore('#')
                        clean.substringAfterLast('/', "Downloaded File")
                    } else title

                    val isVideoChosen = mediaType == "VIDEO"
                    val finalTag = if (tag.isBlank()) {
                        when (mediaType) {
                            "VIDEO" -> "Video"
                            "FILE" -> "File"
                            else -> "Audio"
                        }
                    } else tag

                    val finalPlaylist = playlist.trim().ifBlank { "Default" }
                    onAddMedia(finalTitle, artist, url, isVideoChosen, finalTag, cacheImmediately, finalPlaylist)
                    onDismiss()
                },
                modifier = Modifier.testTag("button_confirm_add_media")
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Download to Storage")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
