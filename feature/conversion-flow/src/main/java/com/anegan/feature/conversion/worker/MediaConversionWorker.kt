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
import com.anegan.core.conversion.FFmpegMediaConverter
import com.anegan.core.conversion.VideoConversionOptions
import com.anegan.core.conversion.AudioExtractionOptions
import java.io.File

class MediaConversionWorker(
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
        val tempFilePath = inputData.getString("tempFilePath") ?: return androidx.work.ListenableWorker.Result.failure()
        val originalFileName = inputData.getString("originalFileName") ?: "unknown"
        val originalFileSize = inputData.getLong("originalFileSize", 0L)
        
        val tempFile = File(tempFilePath)
        if (!tempFile.exists()) {
            return androidx.work.ListenableWorker.Result.failure()
        }

        val title = "Processing $originalFileName"
        // Initially set the worker to foreground
        updateProgress(title, 0)

        val converter = FFmpegMediaConverter()
        var outputFormat = ""
        
        val result = try {
            val conversionResult = when (operation) {
                "CONVERT_VIDEO" -> {
                    outputFormat = inputData.getString("outputFormat") ?: "MP4"
                    val targetResolution = inputData.getString("targetResolution")
                    val options = VideoConversionOptions(
                        outputFormat = outputFormat.lowercase(),
                        targetResolution = targetResolution
                    )
                    converter.convertVideo(tempFile, options) { progress ->
                        val percent = (progress * 100).toInt()
                        setProgressAsync(workDataOf("progress" to percent))
                        notificationManager.notify(notificationId, createNotification(title, percent))
                    }
                }
                "EXTRACT_AUDIO" -> {
                    outputFormat = inputData.getString("targetFormat") ?: "MP3"
                    val options = AudioExtractionOptions(
                        targetFormat = outputFormat.lowercase()
                    )
                    converter.extractAudio(tempFile, options) { progress ->
                        val percent = (progress * 100).toInt()
                        setProgressAsync(workDataOf("progress" to percent))
                        notificationManager.notify(notificationId, createNotification(title, percent))
                    }
                }
                "TRIM_VIDEO" -> {
                    outputFormat = tempFile.extension.uppercase()
                    val startTime = inputData.getDouble("startTime", 0.0)
                    val endTime = inputData.getDouble("endTime", 0.0)
                    converter.trimVideo(tempFile, startTime, endTime) { progress ->
                        val percent = (progress * 100).toInt()
                        setProgressAsync(workDataOf("progress" to percent))
                        notificationManager.notify(notificationId, createNotification(title, percent))
                    }
                }
                "COMPRESS_VIDEO" -> {
                    outputFormat = "MP4"
                    val crf = inputData.getInt("crf", 28)
                    val resolution = inputData.getString("resolution")
                    converter.compressVideo(tempFile, crf, resolution) { progress ->
                        val percent = (progress * 100).toInt()
                        setProgressAsync(workDataOf("progress" to percent))
                        notificationManager.notify(notificationId, createNotification(title, percent))
                    }
                }
                "TRIM_AUDIO" -> {
                    outputFormat = tempFile.extension.uppercase()
                    val startTime = inputData.getDouble("startTime", 0.0)
                    val endTime = inputData.getDouble("endTime", 0.0)
                    converter.trimAudio(tempFile, startTime, endTime) { progress ->
                        val percent = (progress * 100).toInt()
                        setProgressAsync(workDataOf("progress" to percent))
                        notificationManager.notify(notificationId, createNotification(title, percent))
                    }
                }
                "SPEED_VIDEO" -> {
                    outputFormat = "MP4"
                    val speedFactor = inputData.getFloat("speedFactor", 1.0f)
                    converter.changeVideoSpeed(tempFile, speedFactor) { progress ->
                        val percent = (progress * 100).toInt()
                        setProgressAsync(workDataOf("progress" to percent))
                        notificationManager.notify(notificationId, createNotification(title, percent))
                    }
                }
                "VIDEO_TO_GIF" -> {
                    outputFormat = "GIF"
                    val startTime = inputData.getDouble("startTime", 0.0)
                    val duration = inputData.getDouble("duration", 5.0)
                    val fps = inputData.getInt("fps", 10)
                    val width = inputData.getInt("width", 480)
                    converter.videoToGif(tempFile, startTime, duration, fps, width) { progress ->
                        val percent = (progress * 100).toInt()
                        setProgressAsync(workDataOf("progress" to percent))
                        notificationManager.notify(notificationId, createNotification(title, percent))
                    }
                }
                else -> kotlin.Result.failure(Exception("Unknown operation: $operation"))
            }

            if (conversionResult.isSuccess) {
                val outFile = conversionResult.getOrThrow()
                saveToHistory(
                    originalFileName = originalFileName,
                    outputFileName = outFile.name,
                    originalFormat = tempFile.extension.uppercase(),
                    outputFormat = outputFormat.uppercase(),
                    status = "SUCCESS",
                    originalSize = originalFileSize,
                    outputSize = outFile.length(),
                    outputPath = outFile.absolutePath
                )
                showSuccessNotification("Conversion Successful", "Saved to ${outFile.name}")
                androidx.work.ListenableWorker.Result.success(workDataOf(
                    "outputPath" to outFile.absolutePath,
                    "outputFileName" to outFile.name
                ))
            } else {
                val ex = conversionResult.exceptionOrNull()
                saveToHistory(
                    originalFileName = originalFileName,
                    outputFileName = "",
                    originalFormat = tempFile.extension.uppercase(),
                    outputFormat = outputFormat.uppercase(),
                    status = "FAILED",
                    originalSize = originalFileSize,
                    outputSize = 0L,
                    outputPath = ""
                )
                showFailureNotification("Conversion Failed", ex?.message ?: "Unknown error")
                androidx.work.ListenableWorker.Result.failure(workDataOf("error" to (ex?.message ?: "Unknown error")))
            }
        } catch (e: Exception) {
            saveToHistory(
                originalFileName = originalFileName,
                outputFileName = "",
                originalFormat = tempFile.extension.uppercase(),
                outputFormat = outputFormat.uppercase(),
                status = "FAILED",
                originalSize = originalFileSize,
                outputSize = 0L,
                outputPath = ""
            )
            showFailureNotification("Conversion Failed", e.message ?: "Unknown error")
            androidx.work.ListenableWorker.Result.failure(workDataOf("error" to (e.message ?: "Unknown error")))
        } finally {
            // Clean up temporary file
            try {
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
        
        return result
    }
}
