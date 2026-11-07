package com.tk.toolkit.tv.launcher.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

object BackupManager {
    const val DEFAULT_FILE_NAME = "toolkit_launcher_backup.json"

    private fun internalBackupFile(context: Context): File =
        File(context.filesDir, DEFAULT_FILE_NAME)

    private fun publicDownloadFile(): File =
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), DEFAULT_FILE_NAME)

    /**
     * Saves the backup JSON to `/sdcard/Download/toolkit_launcher_backup.json` (via MediaStore on API 29+
     * and direct file write) plus an internal backup copy. Returns the display path on success.
     */
    fun exportDefaultBackup(context: Context): String? {
        val json = LauncherPrefs(context).exportToJson()
        var savedPublic = false

        // 1. Always save a local copy in app storage
        runCatching {
            internalBackupFile(context).writeText(json, Charsets.UTF_8)
        }

        // 2. Try writing directly to /sdcard/Download/toolkit_launcher_backup.json
        runCatching {
            val file = publicDownloadFile()
            file.parentFile?.mkdirs()
            file.writeText(json, Charsets.UTF_8)
            savedPublic = true
        }

        // 3. On Android 10+ (API 29+), also write/replace via MediaStore.Downloads
        if (!savedPublic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                val resolver = context.contentResolver
                val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                resolver.delete(
                    collection,
                    "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
                    arrayOf(DEFAULT_FILE_NAME),
                )
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, DEFAULT_FILE_NAME)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(collection, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    }
                    savedPublic = true
                }
            }
        }

        return if (savedPublic) {
            "Download/$DEFAULT_FILE_NAME"
        } else if (internalBackupFile(context).exists()) {
            internalBackupFile(context).name
        } else {
            null
        }
    }

    fun exportToUri(context: Context, uri: Uri): Boolean = runCatching {
        val json = LauncherPrefs(context).exportToJson()
        internalBackupFile(context).writeText(json, Charsets.UTF_8)
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(json.toByteArray(Charsets.UTF_8))
        } != null
    }.getOrDefault(false)

    /**
     * Restores the backup JSON from `/sdcard/Download/toolkit_launcher_backup.json` (or MediaStore.Downloads,
     * or the internal backup copy).
     */
    fun importDefaultBackup(context: Context): Boolean {
        val json = readPublicDownloadBackup(context)
            ?: runCatching { internalBackupFile(context).takeIf { it.exists() }?.readText(Charsets.UTF_8) }.getOrNull()
            ?: return false
        return LauncherPrefs(context).importFromJson(json)
    }

    fun importFromUri(context: Context, uri: Uri): Boolean = runCatching {
        val json = context.contentResolver.openInputStream(uri)?.use {
            it.readBytes().toString(Charsets.UTF_8)
        } ?: return false
        LauncherPrefs(context).importFromJson(json)
    }.getOrDefault(false)

    fun hasAvailableBackup(context: Context): Boolean {
        if (publicDownloadFile().exists() || internalBackupFile(context).exists()) return true
        return readPublicDownloadBackup(context) != null
    }

    private fun readPublicDownloadBackup(context: Context): String? {
        runCatching {
            val file = publicDownloadFile()
            if (file.exists() && file.canRead()) {
                return file.readText(Charsets.UTF_8)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                val resolver = context.contentResolver
                val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                resolver.query(
                    collection,
                    arrayOf(MediaStore.MediaColumns._ID),
                    "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
                    arrayOf(DEFAULT_FILE_NAME),
                    "${MediaStore.MediaColumns.DATE_MODIFIED} DESC",
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(0)
                        val uri = Uri.withAppendedPath(collection, id.toString())
                        return resolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                    }
                }
            }
        }
        return null
    }
}

