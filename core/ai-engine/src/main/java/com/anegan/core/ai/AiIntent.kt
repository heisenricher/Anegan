/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 *
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.core.ai

/**
 * All supported tool actions Anegan can route to.
 */
enum class AiAction(val displayName: String, val screenRoute: String) {
    COMPRESS_IMAGE("Compress Image", "Images"),
    CONVERT_IMAGE("Convert Image", "Images"),
    RESIZE_IMAGE("Resize Image", "Images"),
    BATCH_CONVERT("Batch Convert", "Batch Image"),
    TRIM_VIDEO("Trim Video", "Video Tools"),
    COMPRESS_VIDEO("Compress Video", "Video Tools"),
    EXTRACT_AUDIO("Extract Audio", "Audio Tools"),
    CONVERT_VIDEO("Convert Video", "Video"),
    CONVERT_AUDIO("Convert Audio", "Audio"),
    CONVERT_PDF("Convert PDF", "Documents"),
    SPLIT_PDF("Split PDF", "PDF Tools"),
    MERGE_PDF("Merge Documents", "Documents"),
    ENCRYPT_PDF("Encrypt PDF", "PDF Tools"),
    PDF_TO_IMAGES("PDF to Images", "PDF Tools"),
    EXTRACT_TEXT("Extract Text (OCR)", "OCR / Extract Text"),
    STRIP_EXIF("Strip EXIF Metadata", "EXIF Metadata"),
    VIEW_EXIF("View EXIF Metadata", "EXIF Metadata"),
    GENERATE_HASH("Generate Hash", "Developer Tools"),
    ENCODE_BASE64("Encode / Decode Base64", "Developer Tools"),
    GENERATE_QR("Generate QR Code", "Developer Tools"),
    REMOVE_BACKGROUND("Remove Background", "AI Background Remover"),
    ADD_WATERMARK("Add Watermark", "Image Watermark"),
    ORGANIZE_PDF("Organize PDF Pages", "PDF Organizer"),
    UNKNOWN("Browse Tools", "")
}

/**
 * Parsed intent from the user's natural language query.
 */
data class AiIntent(
    val action: AiAction,
    val target: String?,            // e.g. "WhatsApp", "Instagram", "email"
    val parameters: Map<String, String>, // e.g. "quality" -> "60"
    val confidence: Float,          // 0.0 – 1.0
    val rawQuery: String,
    val matchedKeywords: List<String> = emptyList()
)

/**
 * A ready-to-display tool suggestion with all parameters validated and clamped.
 */
data class ToolSuggestion(
    val label: String,              // e.g. "Compress Image for WhatsApp"
    val screenRoute: String,        // screen name used in MainActivity routing
    val presetParams: Map<String, String>,
    val confidence: Float,
    val action: AiAction,
    val emoji: String               // display emoji for the chip
)
