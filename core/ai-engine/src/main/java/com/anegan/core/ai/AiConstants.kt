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
 * All keyword patterns, presets and safe parameter ranges for the
 * keyword intelligence engine. Centralised here so they are easy to
 * tune without touching routing logic.
 */
object AiConstants {

    // ─────────────────────────────────────────────────────────────────
    // Target / platform keyword patterns
    // ─────────────────────────────────────────────────────────────────
    val TARGET_WHATSAPP = listOf("whatsapp", "whats app", "wa", "whatsapp image", "whatsapp video")
    val TARGET_INSTAGRAM = listOf("instagram", "insta", "ig", "reel", "story", "instagram post")
    val TARGET_EMAIL = listOf("email", "gmail", "mail", "attach", "send via email")
    val TARGET_TWITTER = listOf("twitter", "tweet", "x.com", "post on x")
    val TARGET_PRINT = listOf("print", "printing", "printer", "physical copy")
    val TARGET_WEB = listOf("web", "website", "upload", "online", "browser")

    // ─────────────────────────────────────────────────────────────────
    // Action keyword groups — ordered by specificity (most specific first)
    // ─────────────────────────────────────────────────────────────────

    // Image
    val COMPRESS_IMAGE_KEYWORDS = listOf(
        "compress image", "compress photo", "reduce image size", "shrink image",
        "reduce photo size", "smaller image", "compress jpg", "compress png",
        "compress jpeg", "reduce file size", "make image smaller", "image size",
        "photo size", "reduce quality", "lower quality", "compress picture"
    )
    val CONVERT_IMAGE_KEYWORDS = listOf(
        "convert image", "image to jpg", "image to png", "image to webp",
        "jpg to png", "png to jpg", "heic to jpg", "convert photo",
        "change image format", "image format", "webp to jpg", "avif to jpg"
    )
    val RESIZE_IMAGE_KEYWORDS = listOf(
        "resize image", "resize photo", "change resolution", "scale image",
        "crop image", "rotate image", "image dimensions", "make image smaller",
        "thumbnail", "change size", "width height", "resolution"
    )
    val BATCH_CONVERT_KEYWORDS = listOf(
        "batch", "multiple images", "many photos", "bulk convert", "all images",
        "folder of images", "process multiple", "batch process", "convert all"
    )

    // Video
    val TRIM_VIDEO_KEYWORDS = listOf(
        "trim video", "cut video", "clip video", "video cut", "shorten video",
        "trim clip", "cut clip", "video trim", "start end video", "crop video"
    )
    val COMPRESS_VIDEO_KEYWORDS = listOf(
        "compress video", "reduce video size", "shrink video", "smaller video",
        "video size", "compress mp4", "reduce mp4", "video quality", "lower resolution"
    )
    val EXTRACT_AUDIO_KEYWORDS = listOf(
        "extract audio", "video to audio", "mp4 to mp3", "rip audio",
        "get audio from video", "strip audio", "audio from video",
        "video to mp3", "extract sound", "save audio"
    )
    val CONVERT_VIDEO_KEYWORDS = listOf(
        "convert video", "video to mp4", "mkv to mp4", "avi to mp4",
        "video format", "change video format", "convert mp4", "video conversion"
    )

    // Audio
    val CONVERT_AUDIO_KEYWORDS = listOf(
        "convert audio", "mp3 to m4a", "flac to mp3", "audio format",
        "change audio format", "convert mp3", "audio conversion", "wav to mp3"
    )

    // PDF
    val SPLIT_PDF_KEYWORDS = listOf(
        "split pdf", "extract page", "pdf page", "separate pdf",
        "pdf split", "extract from pdf", "page range", "remove page"
    )
    val MERGE_PDF_KEYWORDS = listOf(
        "merge pdf", "combine pdf", "join pdf", "merge documents",
        "pdf merge", "combine documents", "multiple pdf", "join documents"
    )
    val ENCRYPT_PDF_KEYWORDS = listOf(
        "encrypt pdf", "password pdf", "protect pdf", "lock pdf",
        "pdf password", "secure pdf", "pdf protection"
    )
    val CONVERT_PDF_KEYWORDS = listOf(
        "pdf to word", "word to pdf", "text to pdf", "convert pdf",
        "pdf conversion", "document conversion", "pdf to doc"
    )
    val PDF_TO_IMAGES_KEYWORDS = listOf(
        "pdf to image", "pdf to jpg", "pdf to png", "export pdf pages",
        "render pdf", "pdf screenshot", "convert pdf to image"
    )
    val ORGANIZE_PDF_KEYWORDS = listOf(
        "organize pdf", "reorder pages", "rearrange pdf", "delete page",
        "move page", "pdf organizer", "page order", "reorder pdf"
    )

    // OCR
    val EXTRACT_TEXT_KEYWORDS = listOf(
        "ocr", "extract text", "image to text", "scan text", "read text",
        "text from image", "copy text from image", "recognize text",
        "photo to text", "document scan", "text recognition"
    )

    // EXIF
    val STRIP_EXIF_KEYWORDS = listOf(
        "strip exif", "remove metadata", "delete metadata", "privacy photo",
        "remove location", "gps data", "clear metadata", "exif remove"
    )
    val VIEW_EXIF_KEYWORDS = listOf(
        "view exif", "see metadata", "photo metadata", "exif data",
        "image info", "camera info", "when was photo taken", "gps location photo"
    )

    // Developer Tools
    val GENERATE_HASH_KEYWORDS = listOf(
        "hash", "md5", "sha256", "sha1", "checksum", "file hash",
        "generate hash", "verify file", "integrity check"
    )
    val ENCODE_BASE64_KEYWORDS = listOf(
        "base64", "encode base64", "decode base64", "base 64",
        "b64", "base64 encode", "base64 decode"
    )
    val GENERATE_QR_KEYWORDS = listOf(
        "qr code", "qr", "generate qr", "create qr", "scan qr",
        "barcode", "qr generator"
    )

    // Background Remover
    val REMOVE_BACKGROUND_KEYWORDS = listOf(
        "remove background", "background remover", "isolate subject",
        "cut out", "transparent background", "remove bg", "erase background",
        "subject only", "background eraser"
    )

    // Watermark
    val ADD_WATERMARK_KEYWORDS = listOf(
        "watermark", "add watermark", "add text to image", "stamp image",
        "watermark photo", "copyright image", "brand image"
    )

    // ─────────────────────────────────────────────────────────────────
    // Quality / size keywords
    // ─────────────────────────────────────────────────────────────────
    val QUALITY_LOW_KEYWORDS = listOf(
        "small", "tiny", "smallest", "minimal", "low quality", "very compressed",
        "heavy compress", "under 100kb", "under 50kb", "under 200kb", "under 500kb"
    )
    val QUALITY_HIGH_KEYWORDS = listOf(
        "lossless", "high quality", "best quality", "maximum quality",
        "no loss", "original quality", "preserve quality", "keep quality"
    )
    val QUALITY_MEDIUM_KEYWORDS = listOf(
        "medium", "balanced", "moderate", "normal quality"
    )

    // ─────────────────────────────────────────────────────────────────
    // Preset parameter mappings
    // ─────────────────────────────────────────────────────────────────
    val PRESET_WHATSAPP_IMAGE = mapOf("quality" to "60", "maxWidth" to "1280", "format" to "JPEG")
    val PRESET_INSTAGRAM_IMAGE = mapOf("quality" to "85", "width" to "1080", "height" to "1080", "format" to "JPEG")
    val PRESET_EMAIL_IMAGE = mapOf("quality" to "70", "maxWidth" to "1600", "format" to "JPEG")
    val PRESET_TWITTER_IMAGE = mapOf("quality" to "80", "maxWidth" to "1200", "format" to "JPEG")
    val PRESET_LOSSLESS_IMAGE = mapOf("quality" to "100", "format" to "PNG")
    val PRESET_SMALL_IMAGE = mapOf("quality" to "55", "maxWidth" to "1024", "format" to "JPEG")
    val PRESET_WHATSAPP_VIDEO = mapOf("crf" to "28", "resolution" to "720p", "preset" to "fast")
    val PRESET_INSTAGRAM_VIDEO = mapOf("crf" to "23", "resolution" to "1080p", "preset" to "medium")
    val PRESET_LOSSLESS_VIDEO = mapOf("crf" to "18", "preset" to "slow")

    // ─────────────────────────────────────────────────────────────────
    // Safe parameter ranges (enforced by SuggestionValidator)
    // ─────────────────────────────────────────────────────────────────
    const val IMAGE_QUALITY_MIN = 10
    const val IMAGE_QUALITY_MAX = 100
    const val IMAGE_DIMENSION_MIN = 32
    const val IMAGE_DIMENSION_MAX = 8192
    const val VIDEO_CRF_MIN = 18
    const val VIDEO_CRF_MAX = 40
    const val TARGET_SIZE_KB_MIN = 10
    const val TARGET_SIZE_KB_MAX = 50000
    const val PDF_PAGE_MIN = 1
    const val PDF_PAGE_MAX = 9999

    // ─────────────────────────────────────────────────────────────────
    // Confidence thresholds
    // ─────────────────────────────────────────────────────────────────
    const val CONFIDENCE_HIGH = 0.75f
    const val CONFIDENCE_MEDIUM = 0.50f
    const val CONFIDENCE_LOW = 0.30f
}
