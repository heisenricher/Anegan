/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.core.conversion

import android.graphics.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class WatermarkPosition {
    TOP_LEFT,
    TOP_RIGHT,
    CENTER,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    TILED
}

class ImageWatermarkManager {

    suspend fun addTextWatermark(
        input: File,
        text: String,
        position: WatermarkPosition,
        colorHex: String,
        opacityPercent: Float, // 0.0 to 1.0
        sizePercent: Float, // 0.01 to 0.1 (percentage of image width)
        angleDegrees: Float = -45f
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val bitmap = BitmapFactory.decodeFile(input.absolutePath)
                ?: return@withContext Result.failure(Exception("Failed to decode image"))

            val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            bitmap.recycle()

            val canvas = Canvas(mutableBitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor(colorHex)
                alpha = (opacityPercent * 255).toInt().coerceIn(0, 255)
                style = Paint.Style.FILL
                // Compute text size relative to image dimensions
                textSize = mutableBitmap.width * sizePercent
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val textBounds = Rect()
            paint.getTextBounds(text, 0, text.length, textBounds)
            val textWidth = textBounds.width().toFloat()
            val textHeight = textBounds.height().toFloat()

            val padding = mutableBitmap.width * 0.05f

            if (position == WatermarkPosition.TILED) {
                // Tiled mode: Draw in a grid across the image with a rotation
                val stepX = textWidth * 2f
                val stepY = textHeight * 4f
                var x = -stepX
                while (x < mutableBitmap.width + stepX) {
                    var y = -stepY
                    while (y < mutableBitmap.height + stepY) {
                        canvas.save()
                        canvas.rotate(angleDegrees, x + textWidth / 2f, y - textHeight / 2f)
                        canvas.drawText(text, x, y, paint)
                        canvas.restore()
                        y += stepY
                    }
                    x += stepX
                }
            } else {
                // Single position mode
                val x = when (position) {
                    WatermarkPosition.TOP_LEFT -> padding
                    WatermarkPosition.TOP_RIGHT -> mutableBitmap.width - textWidth - padding
                    WatermarkPosition.CENTER -> (mutableBitmap.width - textWidth) / 2f
                    WatermarkPosition.BOTTOM_LEFT -> padding
                    WatermarkPosition.BOTTOM_RIGHT -> mutableBitmap.width - textWidth - padding
                    else -> padding
                }

                val y = when (position) {
                    WatermarkPosition.TOP_LEFT -> textHeight + padding
                    WatermarkPosition.TOP_RIGHT -> textHeight + padding
                    WatermarkPosition.CENTER -> (mutableBitmap.height + textHeight) / 2f
                    WatermarkPosition.BOTTOM_LEFT -> mutableBitmap.height - padding
                    WatermarkPosition.BOTTOM_RIGHT -> mutableBitmap.height - padding
                    else -> textHeight + padding
                }

                canvas.save()
                canvas.rotate(angleDegrees, x + textWidth / 2f, y - textHeight / 2f)
                canvas.drawText(text, x, y, paint)
                canvas.restore()
            }

            // Save output to Anegan Images output directory
            val outputDir = StorageManager.getAneganOutputDirectory("Images")
            val outputFile = File(outputDir, "${input.nameWithoutExtension}_watermarked.${input.extension}")
            val compressFormat = when (input.extension.lowercase()) {
                "png" -> Bitmap.CompressFormat.PNG
                "webp" -> if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R)
                    Bitmap.CompressFormat.WEBP_LOSSY
                else
                    @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
                else -> Bitmap.CompressFormat.JPEG
            }

            val fos = FileOutputStream(outputFile)
            mutableBitmap.compress(compressFormat, 95, fos)
            fos.flush()
            fos.close()
            mutableBitmap.recycle()

            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
