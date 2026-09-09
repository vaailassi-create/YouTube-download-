package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE media_items ADD COLUMN playlist TEXT NOT NULL DEFAULT 'Default'")
    }
}

@Database(entities = [MediaCacheEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "media_cache_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialMedia(database.mediaDao())
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                try {
                    db.execSQL("UPDATE media_items SET sourceUrl = 'https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__acrylic_cortices.mp3', title = 'Acrylic Cortices Synth (CC-BY)', mimeType = 'audio/mpeg' WHERE sourceUrl LIKE '%567406%'")
                    db.execSQL("UPDATE media_items SET sourceUrl = 'https://commondatastorage.googleapis.com/codeskulptor-assets/Epoq-Lepidoptera.mp3', title = 'Lepidoptera Chill (CC-BY)', mimeType = 'audio/mpeg' WHERE sourceUrl LIKE '%415804%'")
                    db.execSQL("UPDATE media_items SET sourceUrl = 'https://actions.google.com/sounds/v1/ambiences/rain_heavy.ogg', title = 'Heavy Rain Ambiance (Public Domain)', mimeType = 'audio/ogg' WHERE sourceUrl LIKE '%608645%'")
                    // Categorize existing items into default playlists if playlist is 'Default' or empty
                    db.execSQL("UPDATE media_items SET playlist = 'Synth & Lo-Fi' WHERE (playlist = 'Default' OR playlist IS NULL OR playlist = '') AND (tag LIKE '%Synth%' OR tag LIKE '%Lo-Fi%')")
                    db.execSQL("UPDATE media_items SET playlist = 'Open Cinema' WHERE (playlist = 'Default' OR playlist IS NULL OR playlist = '') AND isVideo = 1")
                    db.execSQL("UPDATE media_items SET playlist = 'Nature Sounds' WHERE (playlist = 'Default' OR playlist IS NULL OR playlist = '') AND (tag LIKE '%Nature%' OR tag LIKE '%Ambient%')")
                } catch (e: Exception) {
                    // Ignore if table not yet created
                }
            }
        }

        suspend fun populateInitialMedia(dao: MediaDao) {
            if (dao.getCount() > 0) return

            val sampleMedia = listOf(
                MediaCacheEntity(
                    title = "Acrylic Cortices Synth (CC-BY)",
                    artist = "Sevish Audio Studio",
                    sourceUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__acrylic_cortices.mp3",
                    mimeType = "audio/mpeg",
                    isVideo = false,
                    totalBytes = 4_520_000L,
                    tag = "Audio Synth",
                    playlist = "Synth & Lo-Fi"
                ),
                MediaCacheEntity(
                    title = "Lepidoptera Chill (CC-BY)",
                    artist = "Creative Commons Studio",
                    sourceUrl = "https://commondatastorage.googleapis.com/codeskulptor-assets/Epoq-Lepidoptera.mp3",
                    mimeType = "audio/mpeg",
                    isVideo = false,
                    totalBytes = 3_120_000L,
                    tag = "Chill Lo-Fi",
                    playlist = "Synth & Lo-Fi"
                ),
                MediaCacheEntity(
                    title = "Big Buck Bunny 480p Clip (CC-BY)",
                    artist = "Blender Open Source Studio",
                    sourceUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    mimeType = "video/mp4",
                    isVideo = true,
                    totalBytes = 15_800_000L,
                    tag = "Video Animation",
                    playlist = "Open Cinema"
                ),
                MediaCacheEntity(
                    title = "For Bigger Blazes (CC Sample)",
                    artist = "Google Video Test Source",
                    sourceUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                    mimeType = "video/mp4",
                    isVideo = true,
                    totalBytes = 14_200_000L,
                    tag = "Video Short",
                    playlist = "Open Cinema"
                ),
                MediaCacheEntity(
                    title = "Heavy Rain Ambiance (Public Domain)",
                    artist = "Nature Sounds Collective",
                    sourceUrl = "https://actions.google.com/sounds/v1/ambiences/rain_heavy.ogg",
                    mimeType = "audio/ogg",
                    isVideo = false,
                    totalBytes = 2_150_000L,
                    tag = "Nature Ambient",
                    playlist = "Nature Sounds"
                )
            )
            dao.insertAll(sampleMedia)
        }
    }
}
