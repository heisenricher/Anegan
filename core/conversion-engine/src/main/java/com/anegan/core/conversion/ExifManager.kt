/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.core.conversion

import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import android.graphics.BitmapFactory

class ExifManager {
    suspend fun readExifMetadata(input: File): Result<Map<String, String>> = withContext(Dispatchers.IO) {
        try {
            val ex = ExifInterface(input.absolutePath)
            val metadata = mutableMapOf<String, String>()
            
            val tags = listOf(
                ExifInterface.TAG_MAKE to "Camera Make",
                ExifInterface.TAG_MODEL to "Camera Model",
                ExifInterface.TAG_DATETIME to "Date & Time",
                ExifInterface.TAG_GPS_LATITUDE to "Latitude",
                ExifInterface.TAG_GPS_LONGITUDE to "Longitude",
                ExifInterface.TAG_IMAGE_WIDTH to "Width",
                ExifInterface.TAG_IMAGE_LENGTH to "Height",
                ExifInterface.TAG_EXPOSURE_TIME to "Exposure Time",
                ExifInterface.TAG_F_NUMBER to "F-Number",
                ExifInterface.TAG_ISO_SPEED_RATINGS to "ISO Speed",
                ExifInterface.TAG_SOFTWARE to "Software"
            )
            
            tags.forEach { (tag, label) ->
                val value = ex.getAttribute(tag)
                if (!value.isNullOrBlank()) {
                    metadata[label] = value
                }
            }
            
            Result.success(metadata)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun stripExifMetadata(input: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            val bitmap = BitmapFactory.decodeFile(input.absolutePath)
                ?: return@withContext Result.failure(Exception("Failed to decode image"))
                
            val outputFile = File(StorageManager.getAneganOutputDirectory("Images"), "${input.nameWithoutExtension}_stripped.${input.extension}")
            val compressFormat = when (input.extension.lowercase()) {
                "png" -> android.graphics.Bitmap.CompressFormat.PNG
                "webp" -> if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R)
                    android.graphics.Bitmap.CompressFormat.WEBP_LOSSY
                else
                    @Suppress("DEPRECATION") android.graphics.Bitmap.CompressFormat.WEBP
                else -> android.graphics.Bitmap.CompressFormat.JPEG
            }
            
            val fos = FileOutputStream(outputFile)
            bitmap.compress(compressFormat, 95, fos)
            fos.flush()
            fos.close()
            bitmap.recycle()
            
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
