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
    suspend fun readExifMetadata(input: File): Result<Map<String, Map<String, String>>> = withContext(Dispatchers.IO) {
        try {
            val ex = ExifInterface(input.absolutePath)
            val metadata = mutableMapOf<String, MutableMap<String, String>>()
            
            // Define categories
            val cameraTags = listOf(
                ExifInterface.TAG_MAKE to "Camera Make",
                ExifInterface.TAG_MODEL to "Camera Model",
                ExifInterface.TAG_SOFTWARE to "Software",
                ExifInterface.TAG_LENS_MAKE to "Lens Make",
                ExifInterface.TAG_LENS_MODEL to "Lens Model",
                ExifInterface.TAG_FOCAL_LENGTH to "Focal Length",
                ExifInterface.TAG_FLASH to "Flash Mode",
                ExifInterface.TAG_WHITE_BALANCE to "White Balance"
            )
            
            val exposureTags = listOf(
                ExifInterface.TAG_EXPOSURE_TIME to "Exposure Time",
                ExifInterface.TAG_F_NUMBER to "F-Number",
                ExifInterface.TAG_ISO_SPEED_RATINGS to "ISO Speed",
                ExifInterface.TAG_APERTURE_VALUE to "Aperture Value",
                ExifInterface.TAG_SHUTTER_SPEED_VALUE to "Shutter Speed",
                ExifInterface.TAG_EXPOSURE_BIAS_VALUE to "Exposure Bias"
            )
            
            val locationTags = listOf(
                ExifInterface.TAG_GPS_LATITUDE to "Latitude Raw",
                ExifInterface.TAG_GPS_LONGITUDE to "Longitude Raw",
                ExifInterface.TAG_GPS_ALTITUDE to "Altitude",
                ExifInterface.TAG_GPS_TIMESTAMP to "GPS Timestamp",
                ExifInterface.TAG_GPS_DATESTAMP to "GPS Datestamp"
            )
            
            val imageTags = listOf(
                ExifInterface.TAG_IMAGE_WIDTH to "Width",
                ExifInterface.TAG_IMAGE_LENGTH to "Height",
                ExifInterface.TAG_ORIENTATION to "Orientation",
                ExifInterface.TAG_COLOR_SPACE to "Color Space",
                ExifInterface.TAG_COMPRESSION to "Compression"
            )
            
            val dateTags = listOf(
                ExifInterface.TAG_DATETIME to "Date & Time",
                ExifInterface.TAG_DATETIME_ORIGINAL to "Date Original",
                ExifInterface.TAG_DATETIME_DIGITIZED to "Date Digitized"
            )

            fun populateCategory(categoryName: String, tags: List<Pair<String, String>>) {
                val catMap = mutableMapOf<String, String>()
                tags.forEach { (tag, label) ->
                    val value = ex.getAttribute(tag)
                    if (!value.isNullOrBlank()) {
                        catMap[label] = value
                    }
                }
                if (catMap.isNotEmpty()) {
                    metadata[categoryName] = catMap
                }
            }

            populateCategory("Camera Details", cameraTags)
            populateCategory("Exposure Settings", exposureTags)
            populateCategory("Image Properties", imageTags)
            populateCategory("Date & Time", dateTags)

            // Special handling for GPS to extract Lat/Long Decimals
            val latLong = FloatArray(2)
            if (ex.getLatLong(latLong)) {
                val locMap = mutableMapOf<String, String>()
                locMap["Latitude"] = String.format("%.6f°", latLong[0])
                locMap["Longitude"] = String.format("%.6f°", latLong[1])
                
                val alt = ex.getAttribute(ExifInterface.TAG_GPS_ALTITUDE)
                if (!alt.isNullOrBlank()) {
                    locMap["Altitude"] = alt
                }
                val ts = ex.getAttribute(ExifInterface.TAG_GPS_TIMESTAMP)
                if (!ts.isNullOrBlank()) {
                    locMap["GPS Timestamp"] = ts
                }
                metadata["Location Metadata"] = locMap
            } else {
                populateCategory("Location Metadata", locationTags)
            }
            
            Result.success(metadata)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun stripSelectiveMetadata(input: File, categories: Set<String>): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (categories.contains("ALL")) {
                return@withContext stripExifMetadata(input)
            }
            
            val outputFile = File(StorageManager.getAneganOutputDirectory("Images"), "${input.nameWithoutExtension}_stripped.${input.extension}")
            input.copyTo(outputFile, overwrite = true)
            val ex = ExifInterface(outputFile.absolutePath)
            
            if (categories.contains("LOCATION")) {
                ex.setAttribute(ExifInterface.TAG_GPS_LATITUDE, null)
                ex.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, null)
                ex.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, null)
                ex.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, null)
                ex.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, null)
                ex.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, null)
                ex.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, null)
                ex.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, null)
                ex.setAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD, null)
            }
            
            if (categories.contains("CAMERA")) {
                ex.setAttribute(ExifInterface.TAG_MAKE, null)
                ex.setAttribute(ExifInterface.TAG_MODEL, null)
                ex.setAttribute(ExifInterface.TAG_SOFTWARE, null)
                ex.setAttribute(ExifInterface.TAG_LENS_MAKE, null)
                ex.setAttribute(ExifInterface.TAG_LENS_MODEL, null)
                ex.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, null)
                ex.setAttribute(ExifInterface.TAG_FLASH, null)
                ex.setAttribute(ExifInterface.TAG_WHITE_BALANCE, null)
            }
            
            if (categories.contains("DATE")) {
                ex.setAttribute(ExifInterface.TAG_DATETIME, null)
                ex.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, null)
                ex.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, null)
            }
            
            ex.saveAttributes()
            Result.success(outputFile)
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
