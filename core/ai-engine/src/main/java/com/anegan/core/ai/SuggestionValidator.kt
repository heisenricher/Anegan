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
 * Validates and clamps every AI-suggested parameter before it reaches
 * the converter layer. Ensures no invalid values can cause crashes.
 */
object SuggestionValidator {

    /**
     * Returns a [ToolSuggestion] with all parameters clamped to safe ranges.
     * Returns null if the suggestion is fundamentally invalid (no route).
     */
    fun validate(suggestion: ToolSuggestion): ToolSuggestion {
        val safe = suggestion.presetParams.toMutableMap()

        safe["quality"]?.toIntOrNull()?.let {
            safe["quality"] = it.coerceIn(
                AiConstants.IMAGE_QUALITY_MIN,
                AiConstants.IMAGE_QUALITY_MAX
            ).toString()
        }

        safe["maxWidth"]?.toIntOrNull()?.let {
            safe["maxWidth"] = it.coerceIn(
                AiConstants.IMAGE_DIMENSION_MIN,
                AiConstants.IMAGE_DIMENSION_MAX
            ).toString()
        }

        safe["width"]?.toIntOrNull()?.let {
            safe["width"] = it.coerceIn(
                AiConstants.IMAGE_DIMENSION_MIN,
                AiConstants.IMAGE_DIMENSION_MAX
            ).toString()
        }

        safe["height"]?.toIntOrNull()?.let {
            safe["height"] = it.coerceIn(
                AiConstants.IMAGE_DIMENSION_MIN,
                AiConstants.IMAGE_DIMENSION_MAX
            ).toString()
        }

        safe["crf"]?.toIntOrNull()?.let {
            safe["crf"] = it.coerceIn(
                AiConstants.VIDEO_CRF_MIN,
                AiConstants.VIDEO_CRF_MAX
            ).toString()
        }

        safe["targetSizeKb"]?.toIntOrNull()?.let {
            safe["targetSizeKb"] = it.coerceIn(
                AiConstants.TARGET_SIZE_KB_MIN,
                AiConstants.TARGET_SIZE_KB_MAX
            ).toString()
        }

        // Reject unsupported image formats — only allow known safe ones
        safe["format"]?.let { fmt ->
            val supported = setOf("JPEG", "PNG", "WEBP", "JPG")
            if (fmt.uppercase() !in supported) {
                safe.remove("format")
            } else {
                safe["format"] = fmt.uppercase()
            }
        }

        return suggestion.copy(presetParams = safe)
    }

    /**
     * Validate an [AiIntent] confidence value — clamp to [0, 1].
     */
    fun validateConfidence(confidence: Float): Float =
        confidence.coerceIn(0.0f, 1.0f)
}
