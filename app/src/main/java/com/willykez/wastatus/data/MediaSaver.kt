package com.willykez.wastatus.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.willykez.wastatus.model.StatusType
import java.io.File
import java.io.FileOutputStream

/**
 * Copies a real status file into the device's public gallery so it survives
 * outside WhatsApp's own cache, the way every status-saver app works. Uses
 * MediaStore's scoped-storage API on Android 10+ and a direct file write
 * (with a legacy media-scan) on older versions — both paths are real, not
 * simulated.
 */
object MediaSaver {

    const val DEFAULT_ALBUM_NAME = "WaStatus"
    const val VAULT_ALBUM_NAME = "WaStatus Vault"

    fun saveToGallery(
        context: Context,
        sourceUri: Uri,
        displayName: String,
        mimeType: String,
        type: StatusType,
        albumName: String = DEFAULT_ALBUM_NAME
    ): Uri? {
        val resolver = context.contentResolver
        val isVideo = type == StatusType.VIDEO
        val collection = if (isVideo) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val relativePath = if (isVideo) "Movies/$albumName" else "Pictures/$albumName"
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val itemUri = resolver.insert(collection, values) ?: return null
            runCatching {
                resolver.openOutputStream(itemUri)?.use { out ->
                    resolver.openInputStream(sourceUri)?.use { input -> input.copyTo(out) }
                        ?: throw IllegalStateException("Cannot open source stream")
                } ?: throw IllegalStateException("Cannot open destination stream")
            }.onFailure {
                resolver.delete(itemUri, null, null)
                return null
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(itemUri, values, null, null)
            itemUri
        } else {
            @Suppress("DEPRECATION")
            val publicDir = Environment.getExternalStoragePublicDirectory(
                if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
            )
            val albumDir = File(publicDir, albumName).apply { mkdirs() }
            val destFile = File(albumDir, displayName)
            runCatching {
                resolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(destFile).use { output -> input.copyTo(output) }
                } ?: return null
            }.onFailure { return null }

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.DATA, destFile.absolutePath)
            }
            resolver.insert(collection, values)
        }
    }
}
