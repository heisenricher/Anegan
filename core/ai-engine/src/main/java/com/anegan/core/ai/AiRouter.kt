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

/**
 * The core AI routing interface. Implementations may be:
 *  - [KeywordAiRouter] — pure Kotlin, 0 MB, instant (current implementation)
 *  - Future: LLM-backed implementation (if size constraints allow)
 *
 * Returns a Flow of [AiIntent] objects. The first emission is the primary result.
 * Additional emissions (if any) are alternative suggestions.
 */
interface AiRouter {
    /**
     * Route a natural language [query] to one or more [AiIntent] results.
     * The flow completes after all results are emitted.
     */
    fun route(query: String): Flow<AiIntent>

    /**
     * Human-readable description of this router implementation.
     * Shown in the AI About screen.
     */
    val engineDescription: String
}
