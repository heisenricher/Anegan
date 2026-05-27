/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.conversion.worker

import android.content.Context
import android.os.Build
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.anegan.core.conversion.NativeDocumentConverter
import java.io.File

class DocumentConversionWorker(
    context: Context,
    parameters: WorkerParameters
) : BaseConversionWorker(context, parameters) {

    override fun getForegroundServiceTypeOverride(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }
    }

    override suspend fun doWork(): androidx.work.ListenableWorker.Result {
        val operation = inputData.getString("operation") ?: return androidx.work.ListenableWorker.Result.failure()
        val originalFileName = inputData.getString("originalFileName") ?: "unknown"
        val originalFileSize = inputData.getLong("originalFileSize", 0L)
        
        val title = "Processing $originalFileName"
        updateProgress(title, 0)

        val converter = NativeDocumentConverter()
        var outputFormat = "PDF"
        
        val result = try {
            val conversionResult: kotlin.Result<Any> = when (operation) {
                "CONVERT_TO_PDF" -> {
                    val tempFilePath = inputData.getString("tempFilePath") ?: throw Exception("Missing input path")
                    outputFormat = "PDF"
                    updateProgress(title, 50)
                    converter.convertToPdf(File(tempFilePath))
                }
                "REMOVE_PROTECTION" -> {
                    val tempFilePath = inputData.getString("tempFilePath") ?: throw Exception("Missing input path")
                    val password = inputData.getString("password") ?: ""
                    outputFormat = "PDF"
                    updateProgress(title, 50)
                    converter.removeProtection(File(tempFilePath), password)
                }
                "SPLIT_PDF" -> {
                    val tempFilePath = inputData.getString("tempFilePath") ?: throw Exception("Missing input path")
                    val startPage = inputData.getInt("startPage", 1)
                    val endPage = inputData.getInt("endPage", 1)
                    outputFormat = "PDF"
                    updateProgress(title, 50)
                    converter.splitPdf(File(tempFilePath), startPage, endPage)
                }
                "COMPRESS_PDF" -> {
                    val tempFilePath = inputData.getString("tempFilePath") ?: throw Exception("Missing input path")
                    val dpi = inputData.getInt("dpi", 150)
                    outputFormat = "PDF"
                    updateProgress(title, 50)
                    converter.compressPdf(File(tempFilePath), dpi)
                }
                "ENCRYPT_PDF" -> {
                    val tempFilePath = inputData.getString("tempFilePath") ?: throw Exception("Missing input path")
                    val password = inputData.getString("password") ?: ""
                    outputFormat = "PDF"
                    updateProgress(title, 50)
                    converter.encryptPdf(File(tempFilePath), password)
                }
                "MERGE_PDFS" -> {
                    val pathsStr = inputData.getString("tempFilePaths") ?: throw Exception("Missing input paths")
                    val outputName = inputData.getString("outputName") ?: "merged"
                    val files = pathsStr.split(",").map { File(it) }
                    outputFormat = "PDF"
                    updateProgress(title, 50)
                    converter.mergePdfs(files, outputName)
                }
                "IMAGES_TO_PDF" -> {
                    val pathsStr = inputData.getString("tempFilePaths") ?: throw Exception("Missing input paths")
                    val files = pathsStr.split(",").map { File(it) }
                    outputFormat = "PDF"
                    updateProgress(title, 50)
                    converter.imagesToPdf(files)
                }
                "PDF_TO_IMAGES" -> {
                    val tempFilePath = inputData.getString("tempFilePath") ?: throw Exception("Missing input path")
                    val format = inputData.getString("format") ?: "jpg"
                    outputFormat = format.uppercase()
                    updateProgress(title, 50)
                    converter.pdfToImages(File(tempFilePath), format)
                }
                else -> kotlin.Result.failure(Exception("Unknown document operation: $operation"))
            }

            if (conversionResult.isSuccess) {
                val value = conversionResult.getOrThrow()
                if (value is File) {
                    saveToHistory(
                        originalFileName = originalFileName,
                        outputFileName = value.name,
                        originalFormat = if (operation == "IMAGES_TO_PDF") "IMAGES" else "PDF",
                        outputFormat = outputFormat,
                        status = "SUCCESS",
                        originalSize = originalFileSize,
                        outputSize = value.length(),
                        outputPath = value.absolutePath
                    )
                    showSuccessNotification("Document Action Successful", "Saved to ${value.name}")
                    androidx.work.ListenableWorker.Result.success(workDataOf(
                        "outputPath" to value.absolutePath,
                        "outputFileName" to value.name
                    ))
                } else if (value is List<*>) {
                    // pdfToImages outputs list of files
                    val files = value.filterIsInstance<File>()
                    var totalSize = 0L
                    for (file in files) {
                        totalSize += file.length()
                    }
                    saveToHistory(
                        originalFileName = originalFileName,
                        outputFileName = "${files.size} Images",
                        originalFormat = "PDF",
                        outputFormat = outputFormat,
                        status = "SUCCESS",
                        originalSize = originalFileSize,
                        outputSize = totalSize,
                        outputPath = files.firstOrNull()?.parentFile?.absolutePath ?: ""
                    )
                    showSuccessNotification("PDF Export Successful", "Exported ${files.size} pages as $outputFormat")
                    androidx.work.ListenableWorker.Result.success(workDataOf(
                        "outputPath" to (files.firstOrNull()?.parentFile?.absolutePath ?: ""),
                        "outputFileName" to "${files.size} Pages"
                    ))
                } else {
                    androidx.work.ListenableWorker.Result.failure()
                }
            } else {
                val ex = conversionResult.exceptionOrNull()
                saveToHistory(
                    originalFileName = originalFileName,
                    outputFileName = "",
                    originalFormat = if (operation == "IMAGES_TO_PDF") "IMAGES" else "PDF",
                    outputFormat = outputFormat,
                    status = "FAILED",
                    originalSize = originalFileSize,
                    outputSize = 0L,
                    outputPath = ""
                )
                showFailureNotification("Document Action Failed", ex?.message ?: "Unknown error")
                androidx.work.ListenableWorker.Result.failure(workDataOf("error" to (ex?.message ?: "Unknown error")))
            }
        } catch (e: Exception) {
            saveToHistory(
                originalFileName = originalFileName,
                outputFileName = "",
                originalFormat = if (operation == "IMAGES_TO_PDF") "IMAGES" else "PDF",
                outputFormat = outputFormat,
                status = "FAILED",
                originalSize = originalFileSize,
                outputSize = 0L,
                outputPath = ""
            )
            showFailureNotification("Document Action Failed", e.message ?: "Unknown error")
            androidx.work.ListenableWorker.Result.failure(workDataOf("error" to (e.message ?: "Unknown error")))
        } finally {
            // Cleanup input temp files
            try {
                val tempFilePath = inputData.getString("tempFilePath")
                if (tempFilePath != null) {
                    val f = File(tempFilePath)
                    if (f.exists()) f.delete()
                }
                val pathsStr = inputData.getString("tempFilePaths")
                if (pathsStr != null) {
                    pathsStr.split(",").forEach {
                        val f = File(it)
                        if (f.exists()) f.delete()
                    }
                }
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
        
        return result
    }
}
