/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 *
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.MidnightIndigo
import com.anegan.core.designsystem.theme.PureWhite

private val AiGradientStart = Color(0xFF1A1040)
private val AiGradientEnd   = Color(0xFF0F2952)
private val AiAccent        = Color(0xFF7C6FFF)
private val AiAccentSoft    = Color(0xFF9D8FFF)

/**
 * Informational screen explaining the keyword intelligence engine approach.
 * Shows what the assistant can and cannot do, and why no model download is needed.
 */
@Composable
fun ModelManagerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(AiGradientStart, AiGradientEnd)
                )
            )
            .padding(24.dp)
            .verticalScroll(scrollState)
    ) {
        // Header
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
            Text(
                text = "AI Engine Info",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = PureWhite
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Status card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121B2E)),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Status dot
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1B2A1B))
                            .border(2.dp, Color(0xFF4CAF50).copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚡", fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Engine Active",
                        color = Color(0xFF81C784),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Keyword Intelligence Engine",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Why no download card
        InfoCard(
            emoji = "🔒",
            title = "100% Offline — No Downloads",
            body = "Anegan's AI assistant runs entirely in Kotlin code. There is no model file to download. All intent detection happens instantly on-device using a fast keyword intelligence engine."
        )

        Spacer(modifier = Modifier.height(12.dp))

        InfoCard(
            emoji = "⚡",
            title = "Instant Responses",
            body = "Keyword matching runs in under 5 ms on any Android device. No GPU, no memory overhead, no warm-up time. The assistant is always ready the moment you open the app."
        )

        Spacer(modifier = Modifier.height(12.dp))

        InfoCard(
            emoji = "🛡️",
            title = "Zero Privacy Risk",
            body = "Your queries never leave the device. No telemetry, no server calls, no analytics from the AI layer. Everything stays private on your phone."
        )

        Spacer(modifier = Modifier.height(20.dp))

        // What it can do
        SectionHeader("What the Assistant Can Do")
        Spacer(modifier = Modifier.height(12.dp))
        CapabilityRow("✅", "Route you to the right tool by description")
        CapabilityRow("✅", "Apply presets for WhatsApp, Instagram, Email")
        CapabilityRow("✅", "Detect quality goals (lossless, small, balanced)")
        CapabilityRow("✅", "Suggest conversion parameters")
        CapabilityRow("✅", "Handle 24+ tool categories")

        Spacer(modifier = Modifier.height(16.dp))

        SectionHeader("What It Cannot Do")
        Spacer(modifier = Modifier.height(12.dp))
        CapabilityRow("❌", "Process or read file contents")
        CapabilityRow("❌", "Make network requests")
        CapabilityRow("❌", "Answer general knowledge questions")
        CapabilityRow("❌", "Generate or edit images directly")

        Spacer(modifier = Modifier.height(28.dp))

        // Engine details
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Engine Details",
                    color = AiAccentSoft,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                DetailRow("Type", "Keyword Intelligence Engine")
                DetailRow("Model file", "None (0 MB)")
                DetailRow("Response time", "< 5 ms")
                DetailRow("Actions supported", "24")
                DetailRow("Platforms recognised", "WhatsApp, Instagram, Email, Twitter, Web, Print")
                DetailRow("Internet required", "Never")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun InfoCard(emoji: String, title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121B2E)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(emoji, fontSize = 24.sp, modifier = Modifier.padding(end = 14.dp, top = 2.dp))
            Column {
                Text(title, color = PureWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(body, color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        color = AiAccentSoft,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun CapabilityRow(icon: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 16.sp, modifier = Modifier.padding(end = 10.dp))
        Text(text, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
    }
}

@Composable
private fun DetailRow(key: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(key, color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp, modifier = Modifier.weight(0.4f))
        Text(value, color = PureWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(0.6f))
    }
    Divider(color = Color.White.copy(alpha = 0.04f), modifier = Modifier.padding(top = 4.dp))
}
