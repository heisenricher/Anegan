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
    val quality: Int = 100 // 1 to 100
)

interface ImageConverter {
    suspend fun convertImage(input: File, options: ImageConversionOptions): Result<File>
}

class NativeImageConverter : ImageConverter {
    override suspend fun convertImage(input: File, options: ImageConversionOptions): Result<File> = withContext(Dispatchers.IO) {
        try {
            val bitmap = BitmapFactory.decodeFile(input.absolutePath)
                ?: return@withContext Result.failure(Exception("Failed to decode image file"))

            // 1. Resize if needed
            val processedBitmap = if (options.exactWidth != null && options.exactHeight != null) {
                Bitmap.createScaledBitmap(bitmap, options.exactWidth, options.exactHeight, true)
            } else {
                bitmap
            }

            // 2. Map format
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
            
            // 3. Compression with target size targeting (Binary Search for Quality)
            if (options.targetSizeBytes != null && compressFormat != Bitmap.CompressFormat.PNG) {
                var minQ = 1
                var maxQ = 100
                var bestQ = options.quality
                
                var currentSize: Long = Long.MAX_VALUE
                
                // Max 7 iterations for binary search on quality 1-100
                for (i in 0..7) {
                    val midQ = (minQ + maxQ) / 2
                    val fos = FileOutputStream(outputFile)
                    processedBitmap.compress(compressFormat, midQ, fos)
                    fos.flush()
                    fos.close()
                    
                    currentSize = outputFile.length()
                    
                    if (Math.abs(currentSize - options.targetSizeBytes) < (options.targetSizeBytes * 0.05)) {
                        break // within 5% tolerance
                    } else if (currentSize > options.targetSizeBytes) {
                        maxQ = midQ - 1 // Reduce quality to decrease size
                    } else {
                        bestQ = midQ
                        minQ = midQ + 1 // Increase quality to get closer to target
                    }
                }
                
                if (currentSize > options.targetSizeBytes * 1.05) {
                     val fos = FileOutputStream(outputFile)
                     processedBitmap.compress(compressFormat, bestQ, fos)
                     fos.close()
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
