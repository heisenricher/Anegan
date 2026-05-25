/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.core.conversion

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.ReturnCode

data class VideoConversionOptions(
    val outputFormat: String, // "mp4", "mkv", "avi"
    val targetResolution: String? = null, // "1920x1080", "1280x720"
    val targetBitrate: String? = null, // "2M", "5M"
    val preset: String = "fast" // "ultrafast", "fast", "medium", "slow"
)

data class AudioExtractionOptions(
    val targetFormat: String, // "mp3", "m4a", "flac"
    val bitrate: String = "192k",
    val sampleRate: String = "44100"
)

interface MediaConverter {
    suspend fun convertVideo(input: File, options: VideoConversionOptions, onProgress: (Float) -> Unit): Result<File>
    suspend fun extractAudio(input: File, options: AudioExtractionOptions, onProgress: (Float) -> Unit): Result<File>
}

class FFmpegMediaConverter : MediaConverter {
    override suspend fun convertVideo(input: File, options: VideoConversionOptions, onProgress: (Float) -> Unit): Result<File> = withContext(Dispatchers.IO) {
        try {
            val outputFile = File(StorageManager.getAneganOutputDirectory("Video"), "${input.nameWithoutExtension}_converted.${options.outputFormat.lowercase()}")
            
            // Get input file duration using ffprobe
            val mediaInformation = FFprobeKit.getMediaInformation(input.absolutePath)
            val durationString = mediaInformation?.mediaInformation?.duration
            val durationSeconds = durationString?.toDoubleOrNull() ?: 1.0

            val cmd = buildList {
                add("-y")
                add("-i")
                add(input.absolutePath)
                add("-c:v")
                add("libx264")
                if (options.targetResolution != null) {
                    add("-vf")
                    add("scale=${options.targetResolution}")
                }
                if (options.targetBitrate != null) {
                    add("-b:v")
                    add(options.targetBitrate)
                }
                add("-preset")
                add(options.preset)
                add("-c:a")
                add("aac")
                add("-b:a")
                add("128k")
                add(outputFile.absolutePath)
            }
            
            val commandString = cmd.joinToString(" ")
            
            suspendCancellableCoroutine { continuation ->
                val session = FFmpegKit.executeAsync(commandString, { activeSession ->
                    val returnCode = activeSession.returnCode
                    if (ReturnCode.isSuccess(returnCode)) {
                        continuation.resume(Result.success(outputFile))
                    } else {
                        continuation.resume(Result.failure(Exception("FFmpeg execution failed with code $returnCode")))
                    }
                }, { log ->
                    // Logs can be handled if needed
                }, { statistics ->
                    val timeInMillis = statistics.time
                    val progress = (timeInMillis / 1000.0) / durationSeconds
                    onProgress(progress.coerceIn(0.0, 1.0).toFloat())
                })
                
                continuation.invokeOnCancellation {
                    FFmpegKit.cancel(session.sessionId)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun extractAudio(input: File, options: AudioExtractionOptions, onProgress: (Float) -> Unit): Result<File> = withContext(Dispatchers.IO) {
        try {
            val outputFile = File(StorageManager.getAneganOutputDirectory("Audio"), "${input.nameWithoutExtension}_extracted.${options.targetFormat.lowercase()}")
            
            val mediaInformation = FFprobeKit.getMediaInformation(input.absolutePath)
            val durationString = mediaInformation?.mediaInformation?.duration
            val durationSeconds = durationString?.toDoubleOrNull() ?: 1.0

            val cmd = buildList {
                add("-y")
                add("-i")
                add(input.absolutePath)
                if (options.targetFormat.lowercase() == "mp3") {
                    add("-vn")
                    add("-c:a")
                    add("libmp3lame")
                    add("-b:a")
                    add(options.bitrate)
                    add("-ar")
                    add(options.sampleRate)
                } else if (options.targetFormat.lowercase() == "m4a") {
                    add("-vn")
                    add("-c:a")
                    add("aac")
                    add("-b:a")
                    add(options.bitrate)
                } else {
                    add("-vn")
                }
                add(outputFile.absolutePath)
            }
            
            val commandString = cmd.joinToString(" ")
            
            suspendCancellableCoroutine { continuation ->
                val session = FFmpegKit.executeAsync(commandString, { activeSession ->
                    val returnCode = activeSession.returnCode
                    if (ReturnCode.isSuccess(returnCode)) {
                        continuation.resume(Result.success(outputFile))
                    } else {
                        continuation.resume(Result.failure(Exception("FFmpeg extraction failed with code $returnCode")))
                    }
                }, { log ->
                    // Logs can be handled if needed
                }, { statistics ->
                    val timeInMillis = statistics.time
                    val progress = (timeInMillis / 1000.0) / durationSeconds
                    onProgress(progress.coerceIn(0.0, 1.0).toFloat())
                })
                
                continuation.invokeOnCancellation {
                    FFmpegKit.cancel(session.sessionId)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun trimVideo(input: File, startTimeSeconds: Double, endTimeSeconds: Double, onProgress: (Float) -> Unit): Result<File> = withContext(Dispatchers.IO) {
        try {
            val outputFile = File(StorageManager.getAneganOutputDirectory("Video"), "${input.nameWithoutExtension}_trimmed.${input.extension}")
            val duration = endTimeSeconds - startTimeSeconds

            val cmd = "-y -i \"${input.absolutePath}\" -ss $startTimeSeconds -to $endTimeSeconds -c copy \"${outputFile.absolutePath}\""

            suspendCancellableCoroutine { continuation ->
                val session = FFmpegKit.executeAsync(cmd, { activeSession ->
                    val returnCode = activeSession.returnCode
                    if (ReturnCode.isSuccess(returnCode)) {
                        continuation.resume(Result.success(outputFile))
                    } else {
                        continuation.resume(Result.failure(Exception("FFmpeg trim failed with code $returnCode")))
                    }
                }, { _ -> }, { statistics ->
                    val timeInMillis = statistics.time
                    val progress = (timeInMillis / 1000.0) / duration
                    onProgress(progress.coerceIn(0.0, 1.0).toFloat())
                })
                continuation.invokeOnCancellation { FFmpegKit.cancel(session.sessionId) }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun compressVideo(input: File, crf: Int = 28, resolution: String? = null, onProgress: (Float) -> Unit): Result<File> = withContext(Dispatchers.IO) {
        try {
            val outputFile = File(StorageManager.getAneganOutputDirectory("Video"), "${input.nameWithoutExtension}_compressed.mp4")

            val mediaInformation = FFprobeKit.getMediaInformation(input.absolutePath)
            val durationSeconds = mediaInformation?.mediaInformation?.duration?.toDoubleOrNull() ?: 1.0

            val vf = if (resolution != null) "-vf scale=$resolution" else ""
            val cmd = "-y -i \"${input.absolutePath}\" -c:v libx264 -crf $crf $vf -preset fast -c:a aac -b:a 128k \"${outputFile.absolutePath}\""

            suspendCancellableCoroutine { continuation ->
                val session = FFmpegKit.executeAsync(cmd, { activeSession ->
                    val returnCode = activeSession.returnCode
                    if (ReturnCode.isSuccess(returnCode)) {
                        continuation.resume(Result.success(outputFile))
                    } else {
                        continuation.resume(Result.failure(Exception("FFmpeg compress failed with code $returnCode")))
                    }
                }, { _ -> }, { statistics ->
                    val timeInMillis = statistics.time
                    val progress = (timeInMillis / 1000.0) / durationSeconds
                    onProgress(progress.coerceIn(0.0, 1.0).toFloat())
                })
                continuation.invokeOnCancellation { FFmpegKit.cancel(session.sessionId) }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun trimAudio(input: File, startTimeSeconds: Double, endTimeSeconds: Double, onProgress: (Float) -> Unit): Result<File> = withContext(Dispatchers.IO) {
        try {
            val outputFile = File(StorageManager.getAneganOutputDirectory("Audio"), "${input.nameWithoutExtension}_trimmed.${input.extension}")
            val duration = endTimeSeconds - startTimeSeconds

            val cmd = "-y -i \"${input.absolutePath}\" -ss $startTimeSeconds -to $endTimeSeconds -c copy \"${outputFile.absolutePath}\""

            suspendCancellableCoroutine { continuation ->
                val session = FFmpegKit.executeAsync(cmd, { activeSession ->
                    val returnCode = activeSession.returnCode
                    if (ReturnCode.isSuccess(returnCode)) {
                        continuation.resume(Result.success(outputFile))
                    } else {
                        continuation.resume(Result.failure(Exception("FFmpeg audio trim failed with code $returnCode")))
                    }
                }, { _ -> }, { statistics ->
                    val timeInMillis = statistics.time
                    val progress = (timeInMillis / 1000.0) / duration
                    onProgress(progress.coerceIn(0.0, 1.0).toFloat())
                })
                continuation.invokeOnCancellation { FFmpegKit.cancel(session.sessionId) }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun changeVideoSpeed(input: File, speedFactor: Float, onProgress: (Float) -> Unit): Result<File> = withContext(Dispatchers.IO) {
        try {
            val outputFile = File(StorageManager.getAneganOutputDirectory("Video"), "${input.nameWithoutExtension}_speed${speedFactor}x.mp4")

            val mediaInformation = FFprobeKit.getMediaInformation(input.absolutePath)
            val durationSeconds = mediaInformation?.mediaInformation?.duration?.toDoubleOrNull() ?: 1.0

            val pts = 1.0 / speedFactor
            // atempo only supports 0.5-2.0 range, chain for wider ranges
            val atempoFilters = buildList {
                var remaining = speedFactor.toDouble()
                while (remaining > 2.0) { add("atempo=2.0"); remaining /= 2.0 }
                while (remaining < 0.5) { add("atempo=0.5"); remaining /= 0.5 }
                add("atempo=$remaining")
            }.joinToString(",")

            val cmd = "-y -i \"${input.absolutePath}\" -vf setpts=${pts}*PTS -af $atempoFilters \"${outputFile.absolutePath}\""

            suspendCancellableCoroutine { continuation ->
                val session = FFmpegKit.executeAsync(cmd, { activeSession ->
                    val returnCode = activeSession.returnCode
                    if (ReturnCode.isSuccess(returnCode)) {
                        continuation.resume(Result.success(outputFile))
                    } else {
                        continuation.resume(Result.failure(Exception("FFmpeg speed change failed with code $returnCode")))
                    }
                }, { _ -> }, { statistics ->
                    val timeInMillis = statistics.time
                    val progress = (timeInMillis / 1000.0) / (durationSeconds / speedFactor)
                    onProgress(progress.coerceIn(0.0, 1.0).toFloat())
                })
                continuation.invokeOnCancellation { FFmpegKit.cancel(session.sessionId) }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun videoToGif(input: File, startTimeSeconds: Double = 0.0, durationSeconds: Double = 5.0, fps: Int = 10, width: Int = 480, onProgress: (Float) -> Unit): Result<File> = withContext(Dispatchers.IO) {
        try {
            val outputFile = File(StorageManager.getAneganOutputDirectory("Images"), "${input.nameWithoutExtension}.gif")

            val cmd = "-y -i \"${input.absolutePath}\" -ss $startTimeSeconds -t $durationSeconds -vf \"fps=$fps,scale=$width:-1:flags=lanczos,split[s0][s1];[s0]palettegen[p];[s1][p]paletteuse\" \"${outputFile.absolutePath}\""

            suspendCancellableCoroutine { continuation ->
                val session = FFmpegKit.executeAsync(cmd, { activeSession ->
                    val returnCode = activeSession.returnCode
                    if (ReturnCode.isSuccess(returnCode)) {
                        continuation.resume(Result.success(outputFile))
                    } else {
                        continuation.resume(Result.failure(Exception("FFmpeg GIF creation failed with code $returnCode")))
                    }
                }, { _ -> }, { statistics ->
                    val timeInMillis = statistics.time
                    val progress = (timeInMillis / 1000.0) / durationSeconds
                    onProgress(progress.coerceIn(0.0, 1.0).toFloat())
                })
                continuation.invokeOnCancellation { FFmpegKit.cancel(session.sessionId) }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
