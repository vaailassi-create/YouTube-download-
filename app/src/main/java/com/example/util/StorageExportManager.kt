package com.example.util

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.data.MediaCacheEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object StorageExportManager {

    /**
     * Saves a cached file to public device Downloads storage directory.
     * Uses MediaStore.Downloads on Android 10+ (zero-permission) and
     * Environment.DIRECTORY_DOWNLOADS on legacy Android.
     */
    suspend fun exportToDeviceDownloads(
        context: Context,
        media: MediaCacheEntity,
        subFolder: String = "MediaCache"
    ): Result<String> = withContext(Dispatchers.IO) {
        val localPath = media.localFilePath ?: return@withContext Result.failure(
            IllegalStateException("File is not cached yet. Tap 'Cache Offline' first.")
        )
        val srcFile = File(localPath)
        if (!srcFile.exists() || srcFile.length() == 0L) {
            return@withContext Result.failure(IllegalStateException("Cached file is missing on disk."))
        }

        // Determine sanitized filename with proper extension
        val srcExtension = srcFile.extension.let { if (it.isNotBlank()) ".$it" else "" }
        val rawTitle = media.title.trim().ifBlank { "downloaded_file" }
        val sanitizedBase = rawTitle.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(60)
        val fileName = if (sanitizedBase.endsWith(srcExtension, ignoreCase = true)) {
            sanitizedBase
        } else {
            "$sanitizedBase$srcExtension"
        }

        val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/$subFolder"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Modern Android Scoped Storage using MediaStore.Downloads
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, media.mimeType.ifBlank { "application/octet-stream" })
                    put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri: Uri = resolver.insert(collection, contentValues)
                    ?: return@withContext Result.failure(Exception("Unable to create entry in Downloads storage"))

                resolver.openOutputStream(itemUri).use { outStream ->
                    if (outStream == null) throw Exception("Cannot write to Downloads URI: $itemUri")
                    FileInputStream(srcFile).use { inStream ->
                        inStream.copyTo(outStream)
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(itemUri, contentValues, null, null)

                Log.d("StorageExportManager", "Successfully exported $fileName to MediaStore.Downloads")
                Result.success("Saved to Downloads/$subFolder/$fileName")
            } else {
                // Legacy Android
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetDir = File(downloadsDir, subFolder).apply { if (!exists()) mkdirs() }
                val targetFile = File(targetDir, fileName)

                FileInputStream(srcFile).use { inStream ->
                    FileOutputStream(targetFile).use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }

                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(targetFile.absolutePath),
                    arrayOf(media.mimeType)
                ) { _, _ -> }

                Result.success("Saved to Downloads/$subFolder/$fileName")
            }
        } catch (e: Exception) {
            Log.e("StorageExportManager", "Failed to export file to storage", e)
            Result.failure(e)
        }
    }
}
