/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 *
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.core.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 100% offline keyword intelligence engine.
 *
 * No model file, no downloads, no external dependencies.
 * Responds in < 5 ms on any Android device.
 *
 * Algorithm:
 * 1. Normalise query (lowercase, trim)
 * 2. Score each [AiAction] by counting keyword matches using substring search
 * 3. Detect target platform (WhatsApp, Instagram …) for preset params
 * 4. Return top-scoring [AiIntent] objects as a Flow
 */
class KeywordAiRouter : AiRouter {

    override val engineDescription: String =
        "Keyword Intelligence Engine — instant, offline, 0 MB"

    override fun route(query: String): Flow<AiIntent> = flow {
        val normalised = query.trim().lowercase()
        if (normalised.isBlank()) {
            emit(unknownIntent(query))
            return@flow
        }

        // Detect target platform
        val target = detectTarget(normalised)

        // Score every action
        val scores = mutableListOf<Pair<AiAction, ScoredMatch>>()

        scores += score(normalised, AiAction.COMPRESS_IMAGE,    AiConstants.COMPRESS_IMAGE_KEYWORDS)
        scores += score(normalised, AiAction.CONVERT_IMAGE,     AiConstants.CONVERT_IMAGE_KEYWORDS)
        scores += score(normalised, AiAction.RESIZE_IMAGE,      AiConstants.RESIZE_IMAGE_KEYWORDS)
        scores += score(normalised, AiAction.BATCH_CONVERT,     AiConstants.BATCH_CONVERT_KEYWORDS)
        scores += score(normalised, AiAction.TRIM_VIDEO,        AiConstants.TRIM_VIDEO_KEYWORDS)
        scores += score(normalised, AiAction.COMPRESS_VIDEO,    AiConstants.COMPRESS_VIDEO_KEYWORDS)
        scores += score(normalised, AiAction.EXTRACT_AUDIO,     AiConstants.EXTRACT_AUDIO_KEYWORDS)
        scores += score(normalised, AiAction.CONVERT_VIDEO,     AiConstants.CONVERT_VIDEO_KEYWORDS)
        scores += score(normalised, AiAction.CONVERT_AUDIO,     AiConstants.CONVERT_AUDIO_KEYWORDS)
        scores += score(normalised, AiAction.SPLIT_PDF,         AiConstants.SPLIT_PDF_KEYWORDS)
        scores += score(normalised, AiAction.MERGE_PDF,         AiConstants.MERGE_PDF_KEYWORDS)
        scores += score(normalised, AiAction.ENCRYPT_PDF,       AiConstants.ENCRYPT_PDF_KEYWORDS)
        scores += score(normalised, AiAction.CONVERT_PDF,       AiConstants.CONVERT_PDF_KEYWORDS)
        scores += score(normalised, AiAction.PDF_TO_IMAGES,     AiConstants.PDF_TO_IMAGES_KEYWORDS)
        scores += score(normalised, AiAction.ORGANIZE_PDF,      AiConstants.ORGANIZE_PDF_KEYWORDS)
        scores += score(normalised, AiAction.EXTRACT_TEXT,      AiConstants.EXTRACT_TEXT_KEYWORDS)
        scores += score(normalised, AiAction.STRIP_EXIF,        AiConstants.STRIP_EXIF_KEYWORDS)
        scores += score(normalised, AiAction.VIEW_EXIF,         AiConstants.VIEW_EXIF_KEYWORDS)
        scores += score(normalised, AiAction.GENERATE_HASH,     AiConstants.GENERATE_HASH_KEYWORDS)
        scores += score(normalised, AiAction.ENCODE_BASE64,     AiConstants.ENCODE_BASE64_KEYWORDS)
        scores += score(normalised, AiAction.GENERATE_QR,       AiConstants.GENERATE_QR_KEYWORDS)
        scores += score(normalised, AiAction.REMOVE_BACKGROUND, AiConstants.REMOVE_BACKGROUND_KEYWORDS)
        scores += score(normalised, AiAction.ADD_WATERMARK,     AiConstants.ADD_WATERMARK_KEYWORDS)

        // Sort by raw match count descending
        val ranked = scores
            .filter { it.second.matchCount > 0 }
            .sortedByDescending { it.second.matchCount }

        if (ranked.isEmpty()) {
            emit(unknownIntent(query))
            return@flow
        }

        // Normalise confidence: top result gets 1.0 scale, others are relative
        val maxCount = ranked.first().second.matchCount.toFloat().coerceAtLeast(1f)

        // Emit top 2 results
        val topResults = ranked.take(2)
        for ((action, match) in topResults) {
            val rawConfidence = (match.matchCount / maxCount) * confidenceMultiplier(action, normalised)
            val clampedConfidence = rawConfidence.coerceIn(0.05f, 1.0f)

            // Quality bias from quality keywords
            val qualityBias = detectQualityBias(normalised)

            val params = buildParameters(action, target, qualityBias)

            emit(
                AiIntent(
                    action = action,
                    target = target,
                    parameters = params,
                    confidence = clampedConfidence,
                    rawQuery = query,
                    matchedKeywords = match.keywords
                )
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────

    private data class ScoredMatch(val matchCount: Int, val keywords: List<String>)

    private fun score(query: String, action: AiAction, patterns: List<String>): Pair<AiAction, ScoredMatch> {
        val matched = patterns.filter { query.contains(it) }
        // Multi-word patterns score more (word count bonus)
        val weightedCount = matched.sumOf { it.split(" ").size }
        return action to ScoredMatch(weightedCount, matched)
    }

    /** Give a confidence boost for exact high-specificity matches */
    private fun confidenceMultiplier(action: AiAction, query: String): Float {
        return when {
            action == AiAction.COMPRESS_IMAGE && query.contains("compress") -> 1.0f
            action == AiAction.TRIM_VIDEO && query.contains("trim") -> 1.0f
            action == AiAction.EXTRACT_TEXT && query.contains("ocr") -> 1.0f
            action == AiAction.REMOVE_BACKGROUND && query.contains("background") -> 1.0f
            action == AiAction.GENERATE_QR && query.contains("qr") -> 1.0f
            action == AiAction.GENERATE_HASH && (query.contains("md5") || query.contains("sha")) -> 1.0f
            action == AiAction.ENCODE_BASE64 && query.contains("base64") -> 1.0f
            else -> 0.85f
        }
    }

    private fun detectTarget(query: String): String? {
        return when {
            AiConstants.TARGET_WHATSAPP.any { query.contains(it) } -> "WhatsApp"
            AiConstants.TARGET_INSTAGRAM.any { query.contains(it) } -> "Instagram"
            AiConstants.TARGET_EMAIL.any { query.contains(it) } -> "Email"
            AiConstants.TARGET_TWITTER.any { query.contains(it) } -> "Twitter"
            AiConstants.TARGET_PRINT.any { query.contains(it) } -> "Print"
            AiConstants.TARGET_WEB.any { query.contains(it) } -> "Web"
            else -> null
        }
    }

    private enum class QualityBias { LOW, MEDIUM, HIGH, NONE }

    private fun detectQualityBias(query: String): QualityBias {
        return when {
            AiConstants.QUALITY_HIGH_KEYWORDS.any { query.contains(it) } -> QualityBias.HIGH
            AiConstants.QUALITY_LOW_KEYWORDS.any { query.contains(it) } -> QualityBias.LOW
            AiConstants.QUALITY_MEDIUM_KEYWORDS.any { query.contains(it) } -> QualityBias.MEDIUM
            else -> QualityBias.NONE
        }
    }

    private fun buildParameters(
        action: AiAction,
        target: String?,
        qualityBias: QualityBias
    ): Map<String, String> {
        // Start from platform preset
        val base: MutableMap<String, String> = when {
            target == "WhatsApp" && action.screenRoute == "Images" ->
                AiConstants.PRESET_WHATSAPP_IMAGE.toMutableMap()
            target == "Instagram" && action.screenRoute == "Images" ->
                AiConstants.PRESET_INSTAGRAM_IMAGE.toMutableMap()
            target == "Email" && action.screenRoute == "Images" ->
                AiConstants.PRESET_EMAIL_IMAGE.toMutableMap()
            target == "Twitter" && action.screenRoute == "Images" ->
                AiConstants.PRESET_TWITTER_IMAGE.toMutableMap()
            target == "WhatsApp" && action.screenRoute == "Video Tools" ->
                AiConstants.PRESET_WHATSAPP_VIDEO.toMutableMap()
            target == "Instagram" && action.screenRoute == "Video Tools" ->
                AiConstants.PRESET_INSTAGRAM_VIDEO.toMutableMap()
            else -> mutableMapOf()
        }

        // Apply quality bias override
        when (qualityBias) {
            QualityBias.HIGH -> {
                base["quality"] = "100"
                base["format"] = base["format"] ?: "PNG"
            }
            QualityBias.LOW -> {
                base["quality"] = (base["quality"]?.toIntOrNull()?.minus(15) ?: 45)
                    .coerceAtLeast(AiConstants.IMAGE_QUALITY_MIN).toString()
            }
            QualityBias.MEDIUM -> {
                if (!base.containsKey("quality")) base["quality"] = "75"
            }
            QualityBias.NONE -> { /* keep platform preset as-is */ }
        }

        return base
    }

    private fun unknownIntent(query: String) = AiIntent(
        action = AiAction.UNKNOWN,
        target = null,
        parameters = emptyMap(),
        confidence = 0.0f,
        rawQuery = query
    )
}
