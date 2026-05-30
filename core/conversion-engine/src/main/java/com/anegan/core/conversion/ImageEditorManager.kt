/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.core.conversion

import android.content.Context
import android.graphics.*
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ImageEditorManager {

    suspend fun cropImage(
        input: File,
        leftPct: Float,
        topPct: Float,
        rightPct: Float,
        bottomPct: Float
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val bitmap = BitmapFactory.decodeFile(input.absolutePath)
                ?: return@withContext Result.failure(Exception("Failed to decode image"))

            val width = bitmap.width
            val height = bitmap.height
            val x = (leftPct * width).toInt().coerceIn(0, width - 1)
            val y = (topPct * height).toInt().coerceIn(0, height - 1)
            val w = ((rightPct - leftPct) * width).toInt().coerceIn(1, width - x)
            val h = ((bottomPct - topPct) * height).toInt().coerceIn(1, height - y)

            val croppedBmp = Bitmap.createBitmap(bitmap, x, y, w, h)
            val outputFile = saveBitmapToOutput(croppedBmp, input)
            croppedBmp.recycle()
            bitmap.recycle()

            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rotateAndFlipImage(
        input: File,
        degrees: Float,
        flipX: Boolean,
        flipY: Boolean
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val bitmap = BitmapFactory.decodeFile(input.absolutePath)
                ?: return@withContext Result.failure(Exception("Failed to decode image"))

            val matrix = Matrix()
            matrix.postRotate(degrees)
            val sx = if (flipX) -1f else 1f
            val sy = if (flipY) -1f else 1f
            matrix.postScale(sx, sy)

            val transformedBmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            val outputFile = saveBitmapToOutput(transformedBmp, input)
            transformedBmp.recycle()
            bitmap.recycle()

            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adjustImage(
        input: File,
        brightness: Float, // -1f to 1f
        contrast: Float,   // -1f to 1f
        saturation: Float  // -1f to 1f
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val bitmap = BitmapFactory.decodeFile(input.absolutePath)
                ?: return@withContext Result.failure(Exception("Failed to decode image"))

            val mutableBmp = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(mutableBmp)
            val paint = Paint()

            val cm = ColorMatrix()

            val scale = contrast + 1f
            val translate = brightness * 255f
            val contrastMatrix = floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
            cm.set(contrastMatrix)

            val satMatrix = ColorMatrix()
            satMatrix.setSaturation(saturation + 1f)
            cm.postConcat(satMatrix)

            paint.colorFilter = ColorMatrixColorFilter(cm)
            canvas.drawBitmap(bitmap, 0f, 0f, paint)

            val outputFile = saveBitmapToOutput(mutableBmp, input)
            mutableBmp.recycle()
            bitmap.recycle()

            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun applyFilter(
        input: File,
        filterType: String
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val bitmap = BitmapFactory.decodeFile(input.absolutePath)
                ?: return@withContext Result.failure(Exception("Failed to decode image"))

            val mutableBmp = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(mutableBmp)
            val paint = Paint()

            val cm = ColorMatrix()
            when (filterType.lowercase()) {
                "grayscale" -> cm.setSaturation(0f)
                "sepia" -> {
                    val sepiaMatrix = floatArrayOf(
                        0.393f, 0.769f, 0.189f, 0f, 0f,
                        0.349f, 0.686f, 0.168f, 0f, 0f,
                        0.272f, 0.534f, 0.131f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                    cm.set(sepiaMatrix)
                }
                "invert" -> {
                    val invertMatrix = floatArrayOf(
                        -1f, 0f, 0f, 0f, 255f,
                        0f, -1f, 0f, 0f, 255f,
                        0f, 0f, -1f, 0f, 255f,
                        0f, 0f, 0f, 1f, 0f
                    )
                    cm.set(invertMatrix)
                }
                "vintage" -> {
                    val vintageMatrix = floatArrayOf(
                        0.9f, 0f, 0f, 0f, 0f,
                        0f, 0.8f, 0f, 0f, 0f,
                        0f, 0f, 0.6f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                    cm.set(vintageMatrix)
                }
                "cool" -> {
                    val coolMatrix = floatArrayOf(
                        0.7f, 0f, 0f, 0f, 0f,
                        0f, 0.9f, 0f, 0f, 0f,
                        0f, 0f, 1.2f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                    cm.set(coolMatrix)
                }
                "warm" -> {
                    val warmMatrix = floatArrayOf(
                        1.2f, 0f, 0f, 0f, 0f,
                        0f, 0.9f, 0f, 0f, 0f,
                        0f, 0f, 0.7f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                    cm.set(warmMatrix)
                }
                else -> cm.setSaturation(1f) // Identity
            }

            paint.colorFilter = ColorMatrixColorFilter(cm)
            canvas.drawBitmap(bitmap, 0f, 0f, paint)

            val outputFile = saveBitmapToOutput(mutableBmp, input)
            mutableBmp.recycle()
            bitmap.recycle()

            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun saveBitmapToOutput(bitmap: Bitmap, sourceFile: File): File {
        val outputDir = StorageManager.getAneganOutputDirectory("Images")
        val outputFile = File(outputDir, "${sourceFile.nameWithoutExtension}_edited_${System.currentTimeMillis()}.${sourceFile.extension}")
        val compressFormat = when (sourceFile.extension.lowercase()) {
            "png" -> Bitmap.CompressFormat.PNG
            "webp" -> if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R)
                Bitmap.CompressFormat.WEBP_LOSSY
            else
                @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
            else -> Bitmap.CompressFormat.JPEG
        }

        FileOutputStream(outputFile).use { fos ->
            bitmap.compress(compressFormat, 95, fos)
            fos.flush()
        }
        return outputFile
    }
}
