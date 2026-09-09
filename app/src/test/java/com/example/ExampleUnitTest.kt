package com.example

import com.example.util.PlaylistParser
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testCuratedPlaylistsExistAndValid() {
        val curated = PlaylistParser.CuratedPlaylists
        assertTrue("Curated playlists must not be empty", curated.isNotEmpty())

        curated.forEach { playlist ->
            assertTrue("Playlist name should not be blank", playlist.name.isNotBlank())
            assertTrue("Playlist should have items", playlist.items.isNotEmpty())
            playlist.items.forEach { item ->
                assertTrue("Item title must not be blank", item.title.isNotBlank())
                assertTrue("Item URL must be http/https", item.sourceUrl.startsWith("http://") || item.sourceUrl.startsWith("https://"))
                assertEquals(playlist.name, item.playlist)
            }
        }
    }

    @Test
    fun testParseM3uContentWithMetadata() {
        val m3uContent = """
            #EXTM3U
            #EXTINF:180, Tycho - Awake
            https://example.com/audio/awake.mp3
            #EXTINF:240, Sevish - Acrylic Cortices
            https://example.com/audio/acrylic.ogg
            #EXTINF:60, Blender Foundation - Tears of Steel
            https://example.com/video/tos.mp4
        """.trimIndent()

        val parsed = PlaylistParser.parseM3uOrUrlText("Electronic Vibes", m3uContent)
        assertEquals(3, parsed.size)

        // Item 1
        assertEquals("Awake", parsed[0].title)
        assertEquals("Tycho", parsed[0].artist)
        assertEquals("https://example.com/audio/awake.mp3", parsed[0].sourceUrl)
        assertEquals("Electronic Vibes", parsed[0].playlist)
        assertFalse(parsed[0].isVideo)
        assertEquals("audio/mpeg", parsed[0].mimeType)

        // Item 2
        assertEquals("Acrylic Cortices", parsed[1].title)
        assertEquals("Sevish", parsed[1].artist)
        assertEquals("audio/ogg", parsed[1].mimeType)

        // Item 3
        assertEquals("Tears of Steel", parsed[2].title)
        assertEquals("Blender Foundation", parsed[2].artist)
        assertTrue(parsed[2].isVideo)
        assertEquals("video/mp4", parsed[2].mimeType)
    }

    @Test
    fun testParsePlainUrls() {
        val rawUrls = """
            https://example.com/files/manual.pdf
            https://example.com/files/dataset.zip
            https://example.com/music/track1.mp3
        """.trimIndent()

        val parsed = PlaylistParser.parseM3uOrUrlText("Docs & Media", rawUrls)
        assertEquals(3, parsed.size)

        assertEquals("manual", parsed[0].title)
        assertEquals("application/pdf", parsed[0].mimeType)
        assertEquals("Docs & Media", parsed[0].playlist)

        assertEquals("dataset", parsed[1].title)
        assertEquals("application/zip", parsed[1].mimeType)

        assertEquals("track1", parsed[2].title)
        assertEquals("audio/mpeg", parsed[2].mimeType)
    }
}
