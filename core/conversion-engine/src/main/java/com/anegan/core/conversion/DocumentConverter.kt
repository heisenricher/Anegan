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
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import com.tom_roush.pdfbox.pdmodel.PDDocument

interface DocumentConverter {
    suspend fun convertToPdf(input: File): Result<File>
    suspend fun mergePdfs(inputs: List<File>, outputName: String): Result<File>
    suspend fun removeProtection(input: File, password: String): Result<File>
}

class NativeDocumentConverter : DocumentConverter {

    override suspend fun convertToPdf(input: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            val outputFile = File(StorageManager.getAneganOutputDirectory("Documents"), "${input.nameWithoutExtension}_converted.pdf")
            val document = PdfDocument()
            
            if (input.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp")) {
                val bitmap = BitmapFactory.decodeFile(input.absolutePath)
                    ?: return@withContext Result.failure(Exception("Failed to decode image"))
                
                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
                val page = document.startPage(pageInfo)
                page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                document.finishPage(page)
                bitmap.recycle()
            } else if (input.extension.lowercase() == "txt") {
                val paint = android.graphics.Paint().apply {
                    textSize = 12f
                    color = android.graphics.Color.BLACK
                }
                val margin = 40f
                val pageWidth = 595 // A4 width
                val pageHeight = 842 // A4 height
                val lines = input.readLines()
                var lineIndex = 0
                var pageNum = 1
                
                while (lineIndex < lines.size) {
                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum++).create()
                    val page = document.startPage(pageInfo)
                    val canvas = page.canvas
                    var y = margin + 20f
                    
                    // Clear canvas with white background
                    canvas.drawColor(android.graphics.Color.WHITE)
                    
                    while (lineIndex < lines.size && y < pageHeight - margin) {
                        canvas.drawText(lines[lineIndex], margin, y, paint)
                        y += 18f
                        lineIndex++
                    }
                    document.finishPage(page)
                }
            } else {
                return@withContext Result.failure(Exception("Unsupported file format for PDF conversion"))
            }
            
            val outputStream = FileOutputStream(outputFile)
            document.writeTo(outputStream)
            document.close()
            outputStream.close()
            
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun mergePdfs(inputs: List<File>, outputName: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val outputFile = File(StorageManager.getAneganOutputDirectory("Documents"), "$outputName.pdf")
            val mergedDocument = PdfDocument()
            var pageIndex = 0

            for (inputFile in inputs) {
                val fileDescriptor = ParcelFileDescriptor.open(inputFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val pdfRenderer = PdfRenderer(fileDescriptor)
                
                for (i in 0 until pdfRenderer.pageCount) {
                    val page = pdfRenderer.openPage(i)
                    val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, pageIndex++).create()
                    val newPage = mergedDocument.startPage(pageInfo)
                    
                    val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    // Clear canvas with white background
                    newPage.canvas.drawColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    newPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    
                    mergedDocument.finishPage(newPage)
                    bitmap.recycle()
                    page.close()
                }
                pdfRenderer.close()
                fileDescriptor.close()
            }
            
            val outputStream = FileOutputStream(outputFile)
            mergedDocument.writeTo(outputStream)
            mergedDocument.close()
            outputStream.close()
            
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeProtection(input: File, password: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val outputFile = File(StorageManager.getAneganOutputDirectory("Documents"), "${input.nameWithoutExtension}_unlocked.pdf")
            val document = PDDocument.load(input, password)
            document.setAllSecurityToBeRemoved(true)
            document.save(outputFile)
            document.close()
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun splitPdf(input: File, startPage: Int, endPage: Int): Result<File> = withContext(Dispatchers.IO) {
        try {
            val outputFile = File(StorageManager.getAneganOutputDirectory("Documents"), "${input.nameWithoutExtension}_pages_${startPage}-${endPage}.pdf")
            val sourceDoc = PDDocument.load(input)
            val newDoc = PDDocument()

            val actualEnd = minOf(endPage, sourceDoc.numberOfPages)
            for (i in (startPage - 1) until actualEnd) {
                val page = sourceDoc.getPage(i)
                newDoc.importPage(page)
            }

            newDoc.save(outputFile)
            newDoc.close()
            sourceDoc.close()
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun compressPdf(input: File, dpi: Int = 150): Result<File> = withContext(Dispatchers.IO) {
        try {
            val outputFile = File(StorageManager.getAneganOutputDirectory("Documents"), "${input.nameWithoutExtension}_compressed.pdf")
            val fileDescriptor = ParcelFileDescriptor.open(input, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(fileDescriptor)
            val compressedDoc = PdfDocument()

            for (i in 0 until pdfRenderer.pageCount) {
                val page = pdfRenderer.openPage(i)
                val pageWidth = page.width
                val pageHeight = page.height
                
                // Render at reduced DPI relative to 72 base
                val scale = dpi / 72f
                val bitmapWidth = (pageWidth * scale).toInt()
                val bitmapHeight = (pageHeight * scale).toInt()
                val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, i + 1).create()
                val newPage = compressedDoc.startPage(pageInfo)
                val destRect = android.graphics.Rect(0, 0, pageWidth, pageHeight)
                newPage.canvas.drawBitmap(bitmap, null, destRect, null)
                compressedDoc.finishPage(newPage)
                bitmap.recycle()
                page.close()
            }

            pdfRenderer.close()
            fileDescriptor.close()

            val outputStream = FileOutputStream(outputFile)
            compressedDoc.writeTo(outputStream)
            compressedDoc.close()
            outputStream.close()

            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun encryptPdf(input: File, password: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val outputFile = File(StorageManager.getAneganOutputDirectory("Documents"), "${input.nameWithoutExtension}_encrypted.pdf")
            val document = PDDocument.load(input)

            val accessPermission = com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission()
            val protectionPolicy = com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy(
                password, // owner password
                password, // user password
                accessPermission
            )
            protectionPolicy.encryptionKeyLength = 128
            document.protect(protectionPolicy)
            document.save(outputFile)
            document.close()

            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun imagesToPdf(images: List<File>): Result<File> = withContext(Dispatchers.IO) {
        try {
            val outputFile = File(StorageManager.getAneganOutputDirectory("Documents"), "images_combined_${System.currentTimeMillis()}.pdf")
            val document = PdfDocument()

            images.forEachIndexed { index, imageFile ->
                val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                    ?: return@forEachIndexed
                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                val page = document.startPage(pageInfo)
                page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                document.finishPage(page)
                bitmap.recycle()
            }

            val outputStream = FileOutputStream(outputFile)
            document.writeTo(outputStream)
            document.close()
            outputStream.close()

            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pdfToImages(input: File, format: String = "jpg"): Result<List<File>> = withContext(Dispatchers.IO) {
        try {
            val outputFiles = mutableListOf<File>()
            val fileDescriptor = ParcelFileDescriptor.open(input, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(fileDescriptor)

            for (i in 0 until pdfRenderer.pageCount) {
                val page = pdfRenderer.openPage(i)
                val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val outputFile = File(StorageManager.getAneganOutputDirectory("Images"), "${input.nameWithoutExtension}_page_${i + 1}.$format")
                val compressFormat = if (format.lowercase() == "png") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                val fos = FileOutputStream(outputFile)
                bitmap.compress(compressFormat, 90, fos)
                fos.close()
                bitmap.recycle()
                outputFiles.add(outputFile)
            }

            pdfRenderer.close()
            fileDescriptor.close()

            Result.success(outputFiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
