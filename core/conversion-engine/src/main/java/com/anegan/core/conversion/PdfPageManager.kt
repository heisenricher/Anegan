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
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfPageManager {

    suspend fun getPageCount(inputFile: File): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val pfd = ParcelFileDescriptor.open(inputFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val count = renderer.pageCount
            renderer.close()
            pfd.close()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun renderPageThumbnail(inputFile: File, pageIndex: Int, targetWidth: Int = 300): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            val pfd = ParcelFileDescriptor.open(inputFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                renderer.close()
                pfd.close()
                return@withContext Result.failure(IllegalArgumentException("Invalid page index: $pageIndex"))
            }

            val page = renderer.openPage(pageIndex)
            val aspectRatio = page.height.toFloat() / page.width.toFloat()
            val targetHeight = (targetWidth * aspectRatio).toInt().coerceAtLeast(1)

            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            pfd.close()

            Result.success(bitmap)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reorderOrDeletePages(inputFile: File, pageOrder: List<Int>): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (pageOrder.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Page list cannot be empty"))
            }
            
            val document = PDDocument.load(inputFile)
            val newDocument = PDDocument()

            for (pageIdx in pageOrder) {
                if (pageIdx >= 0 && pageIdx < document.numberOfPages) {
                    val page = document.getPage(pageIdx)
                    newDocument.importPage(page)
                }
            }

            val outputDir = StorageManager.getAneganOutputDirectory("Documents")
            val outputFile = File(outputDir, "${inputFile.nameWithoutExtension}_organized.pdf")
            val fos = FileOutputStream(outputFile)
            newDocument.save(fos)
            fos.close()
            newDocument.close()
            document.close()

            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
