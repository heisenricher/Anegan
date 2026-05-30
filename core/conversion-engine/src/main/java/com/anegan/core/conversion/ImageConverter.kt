/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.core.conversion

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class ImageConversionOptions(
    val format: String, // "JPG", "PNG", "WEBP"
    val targetSizeBytes: Long? = null,
    val exactWidth: Int? = null,
    val exactHeight: Int? = null,
    val rotationDegrees: Float? = null,
    val cropRect: android.graphics.Rect? = null, // Left, Top, Right, Bottom bounds
    val quality: Int = 100 // 1 to 100
)

interface ImageConverter {
    suspend fun convertImage(input: File, options: ImageConversionOptions): Result<File>
}

class NativeImageConverter : ImageConverter {
    override suspend fun convertImage(input: File, options: ImageConversionOptions): Result<File> = withContext(Dispatchers.IO) {
        try {
            // 0. Decode bitmap with modern HEIC / AVIF support on Android 9+ (fallback to BitmapFactory)
            var bitmap = try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    val source = android.graphics.ImageDecoder.createSource(input)
                    android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                        decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                } else {
                    BitmapFactory.decodeFile(input.absolutePath)
                }
            } catch (e: Exception) {
                BitmapFactory.decodeFile(input.absolutePath)
            }

            if (bitmap == null) {
                return@withContext Result.failure(Exception("Failed to decode image file"))
            }

            // 1. Crop if requested
            var processedBitmap = if (options.cropRect != null) {
                val rect = options.cropRect
                val x = rect.left.coerceIn(0, bitmap.width)
                val y = rect.top.coerceIn(0, bitmap.height)
                val width = rect.width().coerceIn(1, bitmap.width - x)
                val height = rect.height().coerceIn(1, bitmap.height - y)
                Bitmap.createBitmap(bitmap, x, y, width, height)
            } else {
                bitmap
            }

            // 2. Rotate if requested
            if (options.rotationDegrees != null && options.rotationDegrees != 0f) {
                val matrix = android.graphics.Matrix().apply {
                    postRotate(options.rotationDegrees)
                }
                val rotated = Bitmap.createBitmap(
                    processedBitmap,
                    0, 0,
                    processedBitmap.width,
                    processedBitmap.height,
                    matrix,
                    true
                )
                if (processedBitmap != bitmap) {
                    processedBitmap.recycle()
                }
                processedBitmap = rotated
            }

            // 3. Resize if requested
            if (options.exactWidth != null && options.exactHeight != null) {
                val scaled = Bitmap.createScaledBitmap(processedBitmap, options.exactWidth, options.exactHeight, true)
                if (processedBitmap != bitmap) {
                    processedBitmap.recycle()
                }
                processedBitmap = scaled
            }

            // 4. Map output format
            val compressFormat = when (options.format.uppercase()) {
                "PNG" -> Bitmap.CompressFormat.PNG
                "WEBP" -> if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R)
                    Bitmap.CompressFormat.WEBP_LOSSY
                else
                    @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
                "JPG", "JPEG" -> Bitmap.CompressFormat.JPEG
                else -> Bitmap.CompressFormat.JPEG
            }

            val outputFile = File(StorageManager.getAneganOutputDirectory("Images"), "${input.nameWithoutExtension}_converted.${options.format.lowercase()}")
            
            // 5. Compression size targeting (Binary Search with 1% tolerance & Downscaling)
            if (options.targetSizeBytes != null && compressFormat != Bitmap.CompressFormat.PNG) {
                var scaleFactor = 1.0f
                var bestQ = 80
                var bestSize = 0L
                var tempBitmap = processedBitmap
                
                var attempts = 0
                var success = false
                
                while (attempts < 5 && !success) {
                    var minQ = 1
                    var maxQ = 100
                    var localBestQ = 1
                    var localBestSize = 0L
                    
                    for (i in 0..7) {
                        val midQ = (minQ + maxQ) / 2
                        val fos = FileOutputStream(outputFile)
                        tempBitmap.compress(compressFormat, midQ, fos)
                        fos.flush()
                        fos.close()
                        
                        val currentSize = outputFile.length()
                        if (currentSize <= options.targetSizeBytes) {
                            localBestQ = midQ
                            localBestSize = currentSize
                            minQ = midQ + 1
                        } else {
                            maxQ = midQ - 1
                        }
                    }
                    
                    val targetMin = (options.targetSizeBytes * 0.99).toLong()
                    if (localBestSize in targetMin..options.targetSizeBytes) {
                        bestQ = localBestQ
                        bestSize = localBestSize
                        success = true
                    } else if (localBestSize == 0L) {
                        // Even at Q=1, it exceeds target size. We must downscale.
                        scaleFactor *= 0.8f
                        if (tempBitmap != processedBitmap && tempBitmap != bitmap) {
                            tempBitmap.recycle()
                        }
                        val newW = (processedBitmap.width * scaleFactor).toInt().coerceAtLeast(1)
                        val newH = (processedBitmap.height * scaleFactor).toInt().coerceAtLeast(1)
                        tempBitmap = Bitmap.createScaledBitmap(processedBitmap, newW, newH, true)
                        attempts++
                    } else {
                        // localBestSize is < targetMin. That means the maximum quality we can achieve without exceeding target
                        // is actually below targetMin. If localBestQ is 100, we can't do any better, so it's a success.
                        // If localBestQ < 100 but next Q is too big, then we are at the best possible quality.
                        bestQ = localBestQ
                        bestSize = localBestSize
                        success = true
                    }
                }
                
                // Write final compressed image
                val fos = FileOutputStream(outputFile)
                tempBitmap.compress(compressFormat, bestQ, fos)
                fos.flush()
                fos.close()
                
                if (tempBitmap != processedBitmap && tempBitmap != bitmap) {
                    tempBitmap.recycle()
                }
            } else {
                val fos = FileOutputStream(outputFile)
                processedBitmap.compress(compressFormat, options.quality, fos)
                fos.flush()
                fos.close()
            }

            if (processedBitmap != bitmap) {
                processedBitmap.recycle()
            }
            bitmap.recycle()

            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
