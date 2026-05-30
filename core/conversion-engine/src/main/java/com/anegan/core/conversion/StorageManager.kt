package com.anegan.core.conversion

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import java.io.File

object StorageManager {
    /**
     * Gets the dedicated Anegan output folder as requested:
     * "The output folder should be named: Anegan. The app should automatically save files there"
     */
    fun getAneganOutputDirectory(subFolder: String = "Conversions"): File {
        val publicDir = Environment.getExternalStorageDirectory()
        val AneganDir = File(publicDir, "Anegan/$subFolder")
        if (!AneganDir.exists()) {
            AneganDir.mkdirs()
        }
        return AneganDir
    }
    
    /**
     * Deletes temporary processing files.
     */
    fun clearTempFiles(tempDir: File) {
        if (tempDir.exists() && tempDir.isDirectory) {
            tempDir.listFiles()?.forEach { it.delete() }
        }
    }

    /**
     * Copies a scoped Uri content stream into a temporary file.
     * This bridges standard Compose Uri launchers with native File-based logic.
     */
    fun copyUriToTempFile(context: Context, uri: Uri): File? {
        return try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            
            var name = "temp_input"
            var ext = "tmp"
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        val displayName = it.getString(displayNameIndex)
                        if (displayName.contains(".")) {
                            name = displayName.substringBeforeLast(".")
                            ext = displayName.substringAfterLast(".", "tmp")
                        } else {
                            name = displayName
                        }
                    }
                }
            }
            
            val tempFile = File.createTempFile(name, ".$ext", context.cacheDir)
            tempFile.deleteOnExit()
            
            tempFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
