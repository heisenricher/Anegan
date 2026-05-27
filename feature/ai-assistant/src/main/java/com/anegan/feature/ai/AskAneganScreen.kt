/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 *
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.ai

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.ai.AiAction
import com.anegan.core.ai.AiIntent
import com.anegan.core.ai.KeywordAiRouter
import com.anegan.core.ai.SuggestionValidator
import com.anegan.core.ai.ToolPlanner
import com.anegan.core.ai.ToolSuggestion
import com.anegan.core.designsystem.theme.MidnightIndigo
import com.anegan.core.designsystem.theme.PureWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

private val AiGradientStart = Color(0xFF1A1040)
private val AiGradientEnd   = Color(0xFF0F2952)
private val AiAccent        = Color(0xFF7C6FFF)
private val AiAccentSoft    = Color(0xFF9D8FFF)
private val AiChipBg        = Color(0xFF1E1B4B)
private val AiSurface       = Color(0xFF16213E)

// Quick-try examples shown when input is empty
private val EXAMPLE_QUERIES = listOf(
    "Compress photo for WhatsApp",
    "Convert video to MP4",
    "Extract text from image",
    "Remove background from photo",
    "Trim my video",
    "Compress PDF",
    "Generate QR code",
    "Split PDF pages",
    "Convert image to PNG lossless",
    "Add watermark to image"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun AskAneganScreen(
    onBack: () -> Unit,
    onNavigateToTool: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val router = remember { KeywordAiRouter() }
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var query by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    val suggestions = remember { mutableStateListOf<ToolSuggestion>() }
    var showLowConfidence by remember { mutableStateOf(false) }
    var lastQuery by remember { mutableStateOf("") }
    var processingJob by remember { mutableStateOf<Job?>(null) }

    fun runQuery(q: String) {
        if (q.isBlank()) return
        processingJob?.cancel()
        suggestions.clear()
        showLowConfidence = false
        isProcessing = true
        lastQuery = q
        keyboardController?.hide()

        processingJob = coroutineScope.launch {
            router.route(q)
                .flowOn(Dispatchers.Default)
                .collect { intent ->
                    if (intent.action == AiAction.UNKNOWN || intent.confidence < 0.25f) {
                        showLowConfidence = true
                    } else {
                        val suggestion = ToolPlanner.plan(intent)
                        if (suggestion != null) {
                            val validated = SuggestionValidator.validate(suggestion)
                            suggestions.add(validated)
                        }
                    }
                }
            isProcessing = false
            if (suggestions.isEmpty()) showLowConfidence = true
        }
    }

    DisposableEffect(Unit) {
        onDispose { processingJob?.cancel() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(AiGradientStart, AiGradientEnd)
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 24.dp)
        ) {
            // ── Header ────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "←",
                        color = AiAccentSoft,
                        fontSize = 24.sp,
                        modifier = Modifier
                            .clickable { onBack() }
                            .padding(end = 16.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ask Anegan",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                        Text(
                            text = "Describe what you want to do",
                            fontSize = 13.sp,
                            color = AiAccentSoft.copy(alpha = 0.8f)
                        )
                    }
                    // Offline badge
                    OfflineBadge()
                }
            }

            // ── Search Input ──────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = {
                        Text(
                            "e.g. compress photo for WhatsApp",
                            color = Color.White.copy(alpha = 0.35f),
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AiAccent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite,
                        cursorColor = AiAccent,
                        focusedContainerColor = Color.White.copy(alpha = 0.06f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.04f)
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { runQuery(query) }),
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            Button(
                                onClick = { runQuery(query) },
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .height(36.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AiAccent),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Text("Ask", color = PureWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    },
                    singleLine = true
                )
            }

            // ── Processing shimmer ────────────────────────────────────
            if (isProcessing) {
                item {
                    ProcessingIndicator()
                }
            }

            // ── Suggestions ───────────────────────────────────────────
            if (suggestions.isNotEmpty()) {
                item {
                    Text(
                        text = "Suggested Tools",
                        color = AiAccentSoft,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(suggestions) { suggestion ->
                    SuggestionCard(
                        suggestion = suggestion,
                        onClick = { onNavigateToTool(suggestion.screenRoute) }
                    )
                }
            }

            // ── Low confidence fallback ───────────────────────────────
            if (showLowConfidence && !isProcessing) {
                item {
                    LowConfidenceFallback(onBrowse = onBack)
                }
            }

            // ── Example queries (shown when idle) ─────────────────────
            if (suggestions.isEmpty() && !isProcessing && !showLowConfidence) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Try asking",
                        color = AiAccentSoft,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ExampleChips(examples = EXAMPLE_QUERIES) { example ->
                        query = example
                        runQuery(example)
                    }
                }
            }

            // ── Engine info footer ────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "⚡ ${KeywordAiRouter().engineDescription}",
                    color = Color.White.copy(alpha = 0.25f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// Sub-composables
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun OfflineBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1B2A1B))
            .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50))
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "100% Offline",
                color = Color(0xFF81C784),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ProcessingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(AiAccent.copy(alpha = alpha))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Analysing your request…",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp
        )
    }
}

@Composable
private fun SuggestionCard(
    suggestion: ToolSuggestion,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AiSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            AiAccent.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(Float.MAX_VALUE, Float.MAX_VALUE)
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            AiAccent.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = suggestion.emoji,
                        fontSize = 28.sp
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = suggestion.label,
                            color = PureWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "→ ${suggestion.screenRoute}",
                            color = AiAccentSoft,
                            fontSize = 12.sp
                        )
                    }
                    // Confidence badge
                    val pct = (suggestion.confidence * 100).toInt()
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(AiAccent.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$pct%",
                            color = AiAccentSoft,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Preset params summary
                if (suggestion.presetParams.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color.White.copy(alpha = 0.06f))
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(suggestion.presetParams.entries.toList()) { (key, value) ->
                            PresetParamChip(key = key, value = value)
                        }
                    }
                }

                // Tap CTA
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(AiAccent)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Open Tool →",
                            color = PureWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetParamChip(key: String, value: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = "${key.replaceFirstChar { it.uppercase() }}: $value",
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun LowConfidenceFallback(onBrowse: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1210)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    Color(0xFFFF9800).copy(alpha = 0.3f),
                    RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🤔", fontSize = 32.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Not sure which tool to suggest",
                    color = PureWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Try a more specific phrase like\n\"compress image for WhatsApp\" or\n\"extract text from photo\"",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onBrowse,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, Color(0xFFFF9800).copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Browse All Tools", color = Color(0xFFFF9800), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun ExampleChips(
    examples: List<String>,
    onChipTap: (String) -> Unit
) {
    val rows = examples.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { example ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .border(
                                1.dp,
                                AiAccent.copy(alpha = 0.25f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onChipTap(example) }
                            .padding(12.dp)
                    ) {
                        Text(
                            text = example,
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
                // Pad if odd number in row
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
