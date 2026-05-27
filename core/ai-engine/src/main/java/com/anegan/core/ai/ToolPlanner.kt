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
 * Maps a validated [AiIntent] to a user-facing [ToolSuggestion].
 * Applies emoji, human-readable labels, and final preset parameters.
 */
object ToolPlanner {

    fun plan(intent: AiIntent): ToolSuggestion? {
        val action = intent.action
        val route = action.screenRoute ?: return null   // UNKNOWN has no route

        val label = buildLabel(intent)
        val emoji = actionEmoji(action)

        return ToolSuggestion(
            label = label,
            screenRoute = route,
            presetParams = intent.parameters,
            confidence = intent.confidence,
            action = action,
            emoji = emoji
        )
    }

    private fun buildLabel(intent: AiIntent): String {
        val base = intent.action.displayName
        return when {
            intent.target != null -> "$base for ${intent.target}"
            intent.parameters.containsKey("quality") -> {
                val q = intent.parameters["quality"]?.toIntOrNull()
                when {
                    q != null && q >= 90 -> "$base (High Quality)"
                    q != null && q <= 55 -> "$base (Compact Size)"
                    else -> base
                }
            }
            else -> base
        }
    }

    private fun actionEmoji(action: AiAction): String = when (action) {
        AiAction.COMPRESS_IMAGE    -> "🖼️"
        AiAction.CONVERT_IMAGE     -> "🔄"
        AiAction.RESIZE_IMAGE      -> "✂️"
        AiAction.BATCH_CONVERT     -> "📦"
        AiAction.TRIM_VIDEO        -> "🎬"
        AiAction.COMPRESS_VIDEO    -> "📹"
        AiAction.EXTRACT_AUDIO     -> "🎵"
        AiAction.CONVERT_VIDEO     -> "🎞️"
        AiAction.CONVERT_AUDIO     -> "🎧"
        AiAction.CONVERT_PDF       -> "📄"
        AiAction.SPLIT_PDF         -> "✂️"
        AiAction.MERGE_PDF         -> "📎"
        AiAction.ENCRYPT_PDF       -> "🔒"
        AiAction.PDF_TO_IMAGES     -> "🖼️"
        AiAction.EXTRACT_TEXT      -> "🔍"
        AiAction.STRIP_EXIF        -> "🕵️"
        AiAction.VIEW_EXIF         -> "📷"
        AiAction.GENERATE_HASH     -> "#️⃣"
        AiAction.ENCODE_BASE64     -> "💻"
        AiAction.GENERATE_QR       -> "▦"
        AiAction.REMOVE_BACKGROUND -> "✨"
        AiAction.ADD_WATERMARK     -> "🖊️"
        AiAction.ORGANIZE_PDF      -> "📋"
        AiAction.UNKNOWN           -> "🔍"
    }
}
