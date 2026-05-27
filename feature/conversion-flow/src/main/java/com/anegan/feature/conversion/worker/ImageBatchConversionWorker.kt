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
import android.graphics.Rect
import android.os.Build
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.anegan.core.conversion.NativeImageConverter
import com.anegan.core.conversion.NativeDocumentConverter
import com.anegan.core.conversion.ImageConversionOptions
import java.io.File

class ImageBatchConversionWorker(
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
        val isBatch = inputData.getBoolean("isBatch", false)
        val format = inputData.getString("format") ?: "JPG"
        val quality = inputData.getInt("quality", 85)

        val imageConverter = NativeImageConverter()
        val docConverter = NativeDocumentConverter()

        if (!isBatch) {
            // Single image conversion
            val tempFilePath = inputData.getString("tempFilePath") ?: return androidx.work.ListenableWorker.Result.failure()
            val originalFileName = inputData.getString("originalFileName") ?: "unknown"
            val originalFileSize = inputData.getLong("originalFileSize", 0L)
            
            val tempFile = File(tempFilePath)
            if (!tempFile.exists()) {
                return androidx.work.ListenableWorker.Result.failure()
            }

            val title = "Converting $originalFileName"
            updateProgress(title, 0)

            try {
                // Parse optional parameters
                val targetSize = inputData.getLong("targetSizeBytes", -1L)
                val targetSizeBytes = if (targetSize > 0) targetSize else null
                
                val exactWidth = inputData.getInt("exactWidth", -1).let { if (it > 0) it else null }
                val exactHeight = inputData.getInt("exactHeight", -1).let { if (it > 0) it else null }
                val rotationDegrees = inputData.getFloat("rotationDegrees", 0f).let { if (it != 0f) it else null }
                
                val cropX = inputData.getInt("cropX", -1)
                val cropY = inputData.getInt("cropY", -1)
                val cropW = inputData.getInt("cropW", -1)
                val cropH = inputData.getInt("cropH", -1)
                
                val cropRect = if (cropX >= 0 && cropY >= 0 && cropW > 0 && cropH > 0) {
                    Rect(cropX, cropY, cropX + cropW, cropY + cropH)
                } else {
                    null
                }

                updateProgress(title, 30)

                val result = if (format == "PDF") {
                    docConverter.convertToPdf(tempFile)
                } else {
                    val options = ImageConversionOptions(
                        format = format,
                        quality = quality,
                        targetSizeBytes = targetSizeBytes,
                        exactWidth = exactWidth,
                        exactHeight = exactHeight,
                        rotationDegrees = rotationDegrees,
                        cropRect = cropRect
                    )
                    imageConverter.convertImage(tempFile, options)
                }

                updateProgress(title, 80)

                if (result.isSuccess) {
                    val outFile = result.getOrThrow()
                    saveToHistory(
                        originalFileName = originalFileName,
                        outputFileName = outFile.name,
                        originalFormat = tempFile.extension.uppercase(),
                        outputFormat = format.uppercase(),
                        status = "SUCCESS",
                        originalSize = originalFileSize,
                        outputSize = outFile.length(),
                        outputPath = outFile.absolutePath
                    )
                    showSuccessNotification("Image Processed", "Saved to ${outFile.name}")
                    
                    // Delete temp file
                    if (tempFile.exists()) tempFile.delete()
                    
                    return androidx.work.ListenableWorker.Result.success(workDataOf(
                        "outputPath" to outFile.absolutePath,
                        "outputFileName" to outFile.name
                    ))
                } else {
                    val ex = result.exceptionOrNull()
                    saveToHistory(
                        originalFileName = originalFileName,
                        outputFileName = "",
                        originalFormat = tempFile.extension.uppercase(),
                        outputFormat = format.uppercase(),
                        status = "FAILED",
                        originalSize = originalFileSize,
                        outputSize = 0L,
                        outputPath = ""
                    )
                    showFailureNotification("Image Action Failed", ex?.message ?: "Unknown error")
                    if (tempFile.exists()) tempFile.delete()
                    return androidx.work.ListenableWorker.Result.failure(workDataOf("error" to (ex?.message ?: "Unknown error")))
                }
            } catch (e: Exception) {
                saveToHistory(
                    originalFileName = originalFileName,
                    outputFileName = "",
                    originalFormat = tempFile.extension.uppercase(),
                    outputFormat = format.uppercase(),
                    status = "FAILED",
                    originalSize = originalFileSize,
                    outputSize = 0L,
                    outputPath = ""
                )
                showFailureNotification("Image Action Failed", e.message ?: "Unknown error")
                if (tempFile.exists()) tempFile.delete()
                return androidx.work.ListenableWorker.Result.failure(workDataOf("error" to (e.message ?: "Unknown error")))
            }
        } else {
            // Batch image conversion
            val pathsStr = inputData.getString("tempFilePaths") ?: return androidx.work.ListenableWorker.Result.failure()
            val namesStr = inputData.getString("originalFileNames") ?: return androidx.work.ListenableWorker.Result.failure()
            val sizesStr = inputData.getString("originalFileSizes") ?: return androidx.work.ListenableWorker.Result.failure()

            val tempFilePaths = pathsStr.split(",")
            val originalFileNames = namesStr.split(",")
            val originalFileSizes = sizesStr.split(",").map { it.toLongOrNull() ?: 0L }

            val totalCount = tempFilePaths.size
            var successCount = 0
            var failedCount = 0

            tempFilePaths.forEachIndexed { index, path ->
                val name = originalFileNames.getOrElse(index) { "image_$index" }
                val size = originalFileSizes.getOrElse(index) { 0L }
                val tempFile = File(path)

                val progressPercent = ((index.toFloat() / totalCount) * 100).toInt()
                updateProgress("Batch: Processing $name ($index/$totalCount)", progressPercent)

                if (!tempFile.exists()) {
                    failedCount++
                    return@forEachIndexed
                }

                try {
                    val result = if (format == "PDF") {
                        docConverter.convertToPdf(tempFile)
                    } else {
                        val options = ImageConversionOptions(
                            format = format,
                            quality = quality
                        )
                        imageConverter.convertImage(tempFile, options)
                    }

                    if (result.isSuccess) {
                        val outFile = result.getOrThrow()
                        successCount++
                        saveToHistory(
                            originalFileName = name,
                            outputFileName = outFile.name,
                            originalFormat = tempFile.extension.uppercase(),
                            outputFormat = format.uppercase(),
                            status = "SUCCESS",
                            originalSize = size,
                            outputSize = outFile.length(),
                            outputPath = outFile.absolutePath
                        )
                    } else {
                        failedCount++
                        saveToHistory(
                            originalFileName = name,
                            outputFileName = "",
                            originalFormat = tempFile.extension.uppercase(),
                            outputFormat = format.uppercase(),
                            status = "FAILED",
                            originalSize = size,
                            outputSize = 0L,
                            outputPath = ""
                        )
                    }
                } catch (e: Exception) {
                    failedCount++
                    saveToHistory(
                        originalFileName = name,
                        outputFileName = "",
                        originalFormat = tempFile.extension.uppercase(),
                        outputFormat = format.uppercase(),
                        status = "FAILED",
                        originalSize = size,
                        outputSize = 0L,
                        outputPath = ""
                    )
                } finally {
                    if (tempFile.exists()) tempFile.delete()
                }
            }

            updateProgress("Batch Finished", 100)
            showSuccessNotification("Batch Conversion Finished", "Successfully converted $successCount/$totalCount images")
            
            return androidx.work.ListenableWorker.Result.success(workDataOf(
                "successCount" to successCount,
                "failedCount" to failedCount,
                "totalCount" to totalCount
            ))
        }
    }
}
