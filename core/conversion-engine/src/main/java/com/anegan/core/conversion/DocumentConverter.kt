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
    suspend fun splitPdf(input: File, startPage: Int, endPage: Int): Result<File>
    suspend fun compressPdf(input: File, targetSizeBytes: Long? = null, qualityPercent: Int? = null): Result<File>
    suspend fun encryptPdf(input: File, password: String): Result<File>
    suspend fun imagesToPdf(images: List<File>): Result<File>
    suspend fun pdfToImages(input: File, format: String = "jpg"): Result<List<File>>
    suspend fun epubToPdf(input: File): Result<File>
    suspend fun docxToPdf(input: File): Result<File>
    suspend fun zipFiles(inputs: List<File>, outputName: String): Result<File>
    suspend fun unzipFile(input: File): Result<File>
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
                val maxWidth = pageWidth - 2 * margin
                
                val content = input.readText()
                val paragraphs = content.split("\n")
                val wrappedLines = mutableListOf<String>()
                for (p in paragraphs) {
                    if (p.trim().isEmpty()) {
                        wrappedLines.add("")
                    } else {
                        wrappedLines.addAll(wrapText(p, paint, maxWidth))
                    }
                }
                
                var lineIndex = 0
                var pageNum = 1
                while (lineIndex < wrappedLines.size) {
                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum++).create()
                    val page = document.startPage(pageInfo)
                    val canvas = page.canvas
                    canvas.drawColor(android.graphics.Color.WHITE)
                    
                    var y = margin + 20f
                    while (lineIndex < wrappedLines.size && y < pageHeight - margin) {
                        val line = wrappedLines[lineIndex]
                        if (line.isNotEmpty()) {
                            canvas.drawText(line, margin, y, paint)
                        }
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

    override suspend fun splitPdf(input: File, startPage: Int, endPage: Int): Result<File> = withContext(Dispatchers.IO) {
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

    override suspend fun compressPdf(
        input: File,
        targetSizeBytes: Long?,
        qualityPercent: Int?
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val originalSize = input.length()
            if (originalSize <= 0) return@withContext Result.failure(Exception("Empty file"))

            fun executeCompress(dpi: Int, quality: Int): File {
                val outputFile = File(StorageManager.getAneganOutputDirectory("Documents"), "${input.nameWithoutExtension}_compressed.pdf")
                val fileDescriptor = ParcelFileDescriptor.open(input, ParcelFileDescriptor.MODE_READ_ONLY)
                val pdfRenderer = PdfRenderer(fileDescriptor)
                val compressedDoc = PdfDocument()

                for (i in 0 until pdfRenderer.pageCount) {
                    val page = pdfRenderer.openPage(i)
                    val pageWidth = page.width
                    val pageHeight = page.height
                    
                    val scale = dpi / 72f
                    val bitmapWidth = (pageWidth * scale).toInt().coerceAtLeast(1)
                    val bitmapHeight = (pageHeight * scale).toInt().coerceAtLeast(1)
                    
                    val srcBitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(srcBitmap)
                    canvas.drawColor(android.graphics.Color.WHITE)
                    page.render(srcBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    val bos = java.io.ByteArrayOutputStream()
                    srcBitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos)
                    val jpegBytes = bos.toByteArray()
                    val compressedBitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)

                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, i + 1).create()
                    val newPage = compressedDoc.startPage(pageInfo)
                    val destRect = android.graphics.Rect(0, 0, pageWidth, pageHeight)
                    
                    if (compressedBitmap != null) {
                        newPage.canvas.drawBitmap(compressedBitmap, null, destRect, null)
                        compressedBitmap.recycle()
                    } else {
                        newPage.canvas.drawBitmap(srcBitmap, null, destRect, null)
                    }
                    
                    compressedDoc.finishPage(newPage)
                    srcBitmap.recycle()
                    page.close()
                }

                pdfRenderer.close()
                fileDescriptor.close()

                val outputStream = FileOutputStream(outputFile)
                compressedDoc.writeTo(outputStream)
                compressedDoc.close()
                outputStream.close()
                
                return outputFile
            }

            if (targetSizeBytes != null) {
                val ratio = targetSizeBytes.toFloat() / originalSize.toFloat()
                if (ratio >= 0.95f) {
                    val resultFile = executeCompress(180, 85)
                    return@withContext Result.success(resultFile)
                }
                
                var currentRatio = ratio.coerceIn(0.05f, 0.95f)
                var bestFile: File? = null
                
                for (run in 1..3) {
                    val dpi = (160 * Math.sqrt(currentRatio.toDouble())).toInt().coerceIn(55, 180)
                    val quality = (85 * currentRatio).toInt().coerceIn(25, 80)
                    
                    val attemptFile = executeCompress(dpi, quality)
                    val size = attemptFile.length()
                    
                    bestFile = attemptFile
                    
                    if (size <= targetSizeBytes && size >= targetSizeBytes * 0.90) {
                        break
                    }
                    
                    if (size > targetSizeBytes) {
                        val overshotRatio = targetSizeBytes.toFloat() / size.toFloat()
                        currentRatio = (currentRatio * overshotRatio * 0.9f).coerceIn(0.05f, 0.95f)
                    } else {
                        break
                    }
                }
                
                Result.success(bestFile ?: executeCompress(100, 50))
            } else {
                val quality = qualityPercent ?: 80
                val dpi = 70 + (quality * 1.3f).toInt()
                Result.success(executeCompress(dpi, quality))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun encryptPdf(input: File, password: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val outputFile = File(StorageManager.getAneganOutputDirectory("Documents"), "${input.nameWithoutExtension}_encrypted.pdf")
            val document = PDDocument.load(input)

            val accessPermission = com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission()
            val protectionPolicy = com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy(
                password,
                password,
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

    override suspend fun imagesToPdf(images: List<File>): Result<File> = withContext(Dispatchers.IO) {
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

    override suspend fun pdfToImages(input: File, format: String): Result<List<File>> = withContext(Dispatchers.IO) {
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

    override suspend fun epubToPdf(input: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            val tempDir = File(input.parentFile, "epub_temp_${System.currentTimeMillis()}")
            if (!tempDir.exists()) tempDir.mkdirs()
            
            java.util.zip.ZipInputStream(java.io.FileInputStream(input)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val outFile = File(tempDir, entry.name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        java.io.FileOutputStream(outFile).use { fos -> zis.copyTo(fos) }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            val containerXmlFile = File(tempDir, "META-INF/container.xml")
            if (!containerXmlFile.exists()) {
                tempDir.deleteRecursively()
                return@withContext Result.failure(Exception("Invalid EPUB: container.xml not found"))
            }
            val containerContent = containerXmlFile.readText()
            val opfRegex = Regex("""full-path="([^"]+)"""")
            val match = opfRegex.find(containerContent)
            val opfSubPath = match?.groupValues?.get(1) ?: "OEBPS/content.opf"
            val opfFile = File(tempDir, opfSubPath)
            if (!opfFile.exists()) {
                tempDir.deleteRecursively()
                return@withContext Result.failure(Exception("Invalid EPUB: content.opf not found at $opfSubPath"))
            }

            val opfDir = opfFile.parentFile ?: tempDir
            val opfContent = opfFile.readText()

            val manifestItemRegex = Regex("""<item\s+[^>]*id="([^"]+)"\s+[^>]*href="([^"]+)"[^>]*>""")
            val manifestMap = manifestItemRegex.findAll(opfContent).associate {
                it.groupValues[1] to it.groupValues[2]
            }

            val spineIdrefRegex = Regex("""<itemref\s+[^>]*idref="([^"]+)"[^>]*>""")
            val spineList = spineIdrefRegex.findAll(opfContent).map { it.groupValues[1] }.toList()

            val textBuilder = StringBuilder()
            spineList.forEach { idref ->
                val href = manifestMap[idref]
                if (href != null) {
                    val chapterFile = File(opfDir, href)
                    if (chapterFile.exists()) {
                        val htmlContent = chapterFile.readText()
                        var text = htmlContent
                            .replace(Regex("(?i)<br\\s*/?>"), "\n")
                            .replace(Regex("(?i)</p>"), "\n\n")
                            .replace(Regex("(?i)</div>"), "\n")
                            .replace(Regex("(?i)</h1>"), "\n\n")
                            .replace(Regex("(?i)</h2>"), "\n\n")
                            .replace(Regex("(?i)</h3>"), "\n\n")
                            .replace(Regex("<[^>]*>"), "")
                        
                        text = text
                            .replace("&nbsp;", " ")
                            .replace("&amp;", "&")
                            .replace("&lt;", "<")
                            .replace("&gt;", ">")
                            .replace("&quot;", "\"")
                            .replace("&#39;", "'")
                        
                        textBuilder.append(text).append("\n\n")
                    }
                }
            }

            tempDir.deleteRecursively()

            if (textBuilder.isEmpty()) {
                return@withContext Result.failure(Exception("EPUB contained no readable text content"))
            }

            val outputFile = File(StorageManager.getAneganOutputDirectory("Documents"), "${input.nameWithoutExtension}_epub.pdf")
            val document = PdfDocument()
            val paint = android.graphics.Paint().apply {
                textSize = 12f
                color = android.graphics.Color.BLACK
            }
            val margin = 40f
            val pageWidth = 595
            val pageHeight = 842
            val maxWidth = pageWidth - 2 * margin

            val paragraphList = textBuilder.toString().split("\n")
            val wrappedLines = mutableListOf<String>()
            for (p in paragraphList) {
                if (p.trim().isEmpty()) {
                    wrappedLines.add("")
                } else {
                    wrappedLines.addAll(wrapText(p, paint, maxWidth))
                }
            }

            var lineIndex = 0
            var pageNum = 1
            while (lineIndex < wrappedLines.size) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum++).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas
                canvas.drawColor(android.graphics.Color.WHITE)
                
                var y = margin + 20f
                while (lineIndex < wrappedLines.size && y < pageHeight - margin) {
                    val line = wrappedLines[lineIndex]
                    if (line.isNotEmpty()) {
                        canvas.drawText(line, margin, y, paint)
                    }
                    y += 18f
                    lineIndex++
                }
                document.finishPage(page)
            }

            val fos = FileOutputStream(outputFile)
            document.writeTo(fos)
            document.close()
            fos.close()

            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun docxToPdf(input: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            val tempDir = File(input.parentFile, "docx_temp_${System.currentTimeMillis()}")
            if (!tempDir.exists()) tempDir.mkdirs()

            java.util.zip.ZipInputStream(java.io.FileInputStream(input)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val outFile = File(tempDir, entry.name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        java.io.FileOutputStream(outFile).use { fos -> zis.copyTo(fos) }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            val docXmlFile = File(tempDir, "word/document.xml")
            if (!docXmlFile.exists()) {
                tempDir.deleteRecursively()
                return@withContext Result.failure(Exception("Invalid DOCX: word/document.xml not found"))
            }

            val xmlContent = docXmlFile.readText()
            tempDir.deleteRecursively()

            val pRegex = Regex("<w:p[^>]*>(.*?)</w:p>")
            val tRegex = Regex("<w:t[^>]*>(.*?)</w:t>")
            
            val textBuilder = StringBuilder()
            pRegex.findAll(xmlContent).forEach { pMatch ->
                val pContent = pMatch.groupValues[1]
                val pText = tRegex.findAll(pContent).map { it.groupValues[1] }.joinToString("")
                textBuilder.append(pText).append("\n")
            }

            if (textBuilder.isEmpty()) {
                return@withContext Result.failure(Exception("DOCX contained no readable text"))
            }

            val outputFile = File(StorageManager.getAneganOutputDirectory("Documents"), "${input.nameWithoutExtension}_docx.pdf")
            val document = PdfDocument()
            val paint = android.graphics.Paint().apply {
                textSize = 12f
                color = android.graphics.Color.BLACK
            }
            val margin = 40f
            val pageWidth = 595
            val pageHeight = 842
            val maxWidth = pageWidth - 2 * margin

            val paragraphs = textBuilder.toString().split("\n")
            val wrappedLines = mutableListOf<String>()
            for (p in paragraphs) {
                if (p.trim().isEmpty()) {
                    wrappedLines.add("")
                } else {
                    wrappedLines.addAll(wrapText(p, paint, maxWidth))
                }
            }

            var lineIndex = 0
            var pageNum = 1
            while (lineIndex < wrappedLines.size) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum++).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas
                canvas.drawColor(android.graphics.Color.WHITE)

                var y = margin + 20f
                while (lineIndex < wrappedLines.size && y < pageHeight - margin) {
                    val line = wrappedLines[lineIndex]
                    if (line.isNotEmpty()) {
                        canvas.drawText(line, margin, y, paint)
                    }
                    y += 18f
                    lineIndex++
                }
                document.finishPage(page)
            }

            val fos = FileOutputStream(outputFile)
            document.writeTo(fos)
            document.close()
            fos.close()

            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun zipFiles(inputs: List<File>, outputName: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val outputFile = File(StorageManager.getAneganOutputDirectory("Documents"), "$outputName.zip")
            java.util.zip.ZipOutputStream(java.io.FileOutputStream(outputFile)).use { zos ->
                inputs.forEach { file ->
                    if (file.exists() && file.isFile) {
                        zos.putNextEntry(java.util.zip.ZipEntry(file.name))
                        java.io.FileInputStream(file).use { fis -> fis.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unzipFile(input: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            val destDir = File(StorageManager.getAneganOutputDirectory("Documents"), "${input.nameWithoutExtension}_extracted")
            if (!destDir.exists()) destDir.mkdirs()
            java.util.zip.ZipInputStream(java.io.FileInputStream(input)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val outFile = File(destDir, entry.name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        java.io.FileOutputStream(outFile).use { fos -> zis.copyTo(fos) }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            Result.success(destDir)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun wrapText(text: String, paint: android.graphics.Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = java.lang.StringBuilder()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "${currentLine} $word"
            val width = paint.measureText(testLine)
            if (width <= maxWidth) {
                currentLine.append(if (currentLine.isEmpty()) word else " $word")
            } else {
                lines.add(currentLine.toString())
                currentLine = java.lang.StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines
    }
}
