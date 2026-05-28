/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.documentreader

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

data class DocumentFile(
    val name: String,
    val path: String,
    val size: Long,
    val lastModified: Long,
    val extension: String,
    val category: String // "PDF", "Ebook", "Docs", "Logs"
)

object DocumentScanner {
    suspend fun scanLocalDocuments(context: Context): List<DocumentFile> = withContext(Dispatchers.IO) {
        val documents = mutableListOf<DocumentFile>()
        
        // Target Directories: Downloads and Documents
        val targetDirs = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            context.getExternalFilesDir(null) // Include sandbox external files
        ).filterNotNull()

        for (dir in targetDirs) {
            scanDirectoryRecursively(dir, documents)
        }

        // Sort by last modified (newest first)
        documents.sortedByDescending { it.lastModified }
    }

    private fun scanDirectoryRecursively(directory: File, results: MutableList<DocumentFile>) {
        if (!directory.exists() || !directory.isDirectory) return
        
        val files = directory.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                // Avoid hidden system folders
                if (!file.name.startsWith(".")) {
                    scanDirectoryRecursively(file, results)
                }
            } else {
                val ext = file.extension.lowercase(Locale.ROOT)
                val category = when (ext) {
                    "pdf" -> "PDF"
                    "epub" -> "Ebook"
                    "docx" -> "Docs"
                    "txt", "csv", "json", "log" -> "Logs"
                    else -> null
                }
                
                if (category != null) {
                    results.add(
                        DocumentFile(
                            name = file.name,
                            path = file.absolutePath,
                            size = file.length(),
                            lastModified = file.lastModified(),
                            extension = ext,
                            category = category
                        )
                    )
                }
            }
        }
    }
}
