package com.anegan.core.conversion

import android.os.Environment
import java.io.File

object StorageManager {
    /**
     * Gets the dedicated Anegan output folder as requested:
     * "The output folder should be named: Anegan. The app should automatically save files there"
     */
    fun getAneganOutputDirectory(subFolder: String = "Conversions"): File {
        val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
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
}
