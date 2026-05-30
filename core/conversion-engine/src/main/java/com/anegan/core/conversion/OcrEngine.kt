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
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

interface OcrEngine {
    suspend fun extractTextFromImage(context: Context, imageUri: Uri, lang: String = "latin", detectTables: Boolean = false): Result<String>
    suspend fun extractTextFromInputImage(image: InputImage, lang: String = "latin", detectTables: Boolean = false): Result<String>
}

class NativeOcrEngine : OcrEngine {
    override suspend fun extractTextFromImage(
        context: Context,
        imageUri: Uri,
        lang: String,
        detectTables: Boolean
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val image = InputImage.fromFilePath(context, imageUri)
            extractTextFromInputImage(image, lang, detectTables)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun extractTextFromInputImage(
        image: InputImage,
        lang: String,
        detectTables: Boolean
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val recognizer = getRecognizerForLanguage(lang)
            
            suspendCancellableCoroutine { continuation ->
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val resultText = if (detectTables) {
                            detectTableStructure(visionText)
                        } else {
                            visionText.text
                        }
                        continuation.resume(Result.success(resultText))
                    }
                    .addOnFailureListener { e ->
                        continuation.resume(Result.failure(e))
                    }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getRecognizerForLanguage(lang: String): TextRecognizer {
        val options: TextRecognizerOptionsInterface = when (lang.lowercase()) {
            "devanagari" -> {
                try {
                    val clazz = Class.forName("com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions")
                    val field = clazz.getField("DEFAULT_OPTIONS")
                    field.get(null) as TextRecognizerOptionsInterface
                } catch (e: Exception) {
                    TextRecognizerOptions.DEFAULT_OPTIONS
                }
            }
            "chinese" -> {
                try {
                    val clazz = Class.forName("com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions")
                    val field = clazz.getField("DEFAULT_OPTIONS")
                    field.get(null) as TextRecognizerOptionsInterface
                } catch (e: Exception) {
                    TextRecognizerOptions.DEFAULT_OPTIONS
                }
            }
            "japanese" -> {
                try {
                    val clazz = Class.forName("com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions")
                    val field = clazz.getField("DEFAULT_OPTIONS")
                    field.get(null) as TextRecognizerOptionsInterface
                } catch (e: Exception) {
                    TextRecognizerOptions.DEFAULT_OPTIONS
                }
            }
            "korean" -> {
                try {
                    val clazz = Class.forName("com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions")
                    val field = clazz.getField("DEFAULT_OPTIONS")
                    field.get(null) as TextRecognizerOptionsInterface
                } catch (e: Exception) {
                    TextRecognizerOptions.DEFAULT_OPTIONS
                }
            }
            "tamil" -> {
                try {
                    val clazz = Class.forName("com.google.mlkit.vision.text.tamil.TamilTextRecognizerOptions")
                    val field = clazz.getField("DEFAULT_OPTIONS")
                    field.get(null) as TextRecognizerOptionsInterface
                } catch (e: Exception) {
                    TextRecognizerOptions.DEFAULT_OPTIONS
                }
            }
            "telugu" -> {
                try {
                    val clazz = Class.forName("com.google.mlkit.vision.text.telugu.TeluguTextRecognizerOptions")
                    val field = clazz.getField("DEFAULT_OPTIONS")
                    field.get(null) as TextRecognizerOptionsInterface
                } catch (e: Exception) {
                    TextRecognizerOptions.DEFAULT_OPTIONS
                }
            }
            "arabic" -> {
                try {
                    val clazz = Class.forName("com.google.mlkit.vision.text.arabic.ArabicTextRecognizerOptions")
                    val field = clazz.getField("DEFAULT_OPTIONS")
                    field.get(null) as TextRecognizerOptionsInterface
                } catch (e: Exception) {
                    TextRecognizerOptions.DEFAULT_OPTIONS
                }
            }
            else -> TextRecognizerOptions.DEFAULT_OPTIONS
        }
        return TextRecognition.getClient(options)
    }

    private fun detectTableStructure(visionText: com.google.mlkit.vision.text.Text): String {
        val allLines = visionText.textBlocks.flatMap { it.lines }
        if (allLines.isEmpty()) return ""

        // Sort lines by their top Y coordinate
        val sortedLines = allLines.sortedBy { it.boundingBox?.top ?: 0 }

        // Group lines into rows based on vertical overlap
        val rows = mutableListOf<MutableList<com.google.mlkit.vision.text.Text.Line>>()
        
        for (line in sortedLines) {
            val lineBox = line.boundingBox ?: continue
            val lineTop = lineBox.top
            val lineHeight = lineBox.height()
            
            var addedToRow = false
            for (row in rows) {
                val rowBox = row[0].boundingBox ?: continue
                val rowTop = rowBox.top
                val rowHeight = rowBox.height()
                
                // If top difference is less than 70% of row height, group in same row
                if (Math.abs(lineTop - rowTop) < Math.max(rowHeight, lineHeight) * 0.7) {
                    row.add(line)
                    addedToRow = true
                    break
                }
            }
            if (!addedToRow) {
                rows.add(mutableListOf(line))
            }
        }

        // Sort elements horizontally in each row
        val formattedRows = rows.map { row ->
            row.sortedBy { it.boundingBox?.left ?: 0 }
        }

        // Sort rows vertically by average Y coordinate
        val finalRows = formattedRows.sortedBy { row ->
            row.map { it.boundingBox?.top ?: 0 }.average()
        }

        val sb = java.lang.StringBuilder()
        val maxCols = finalRows.maxOfOrNull { it.size } ?: 0
        if (maxCols == 0) return visionText.text

        finalRows.forEachIndexed { index, row ->
            sb.append("|")
            for (i in 0 until maxCols) {
                if (i < row.size) {
                    sb.append(" ").append(row[i].text.replace("|", "\\|")).append(" |")
                } else {
                    sb.append("   |")
                }
            }
            sb.append("\n")
            
            if (index == 0 && finalRows.size > 1) {
                sb.append("|")
                for (i in 0 until maxCols) {
                    sb.append(" --- |")
                }
                sb.append("\n")
            }
        }
        
        return sb.toString()
    }
}
