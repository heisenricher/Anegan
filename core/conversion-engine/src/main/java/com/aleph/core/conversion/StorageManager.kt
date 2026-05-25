package com.aleph.core.conversion

import android.os.Environment
import java.io.File

object StorageManager {
    /**
     * Gets the dedicated Aleph output folder as requested:
     * "The output folder should be named: Aleph. The app should automatically save files there"
     */
    fun getAlephOutputDirectory(subFolder: String = "Conversions"): File {
        val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val alephDir = File(publicDir, "Aleph/$subFolder")
        if (!alephDir.exists()) {
            alephDir.mkdirs()
        }
        return alephDir
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
