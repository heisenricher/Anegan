/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 *
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var feedbackText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var submissionMessage by remember { mutableStateOf<String?>(null) }
    var submissionSuccess by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    BackHandler {
        onBack()
    }

    val isDark = isSystemInDarkTheme()

    NovaBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                NovaTopBar(
                    title = "Submit Feedback",
                    onBack = {
                        NovaHaptics.click(view)
                        onBack()
                    },
                    neonAccent = NeonGold
                )
            }
        ) { innerPadding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Feature descriptor header card
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    neonAccent = NeonGold.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💬", fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Help Us Build The Future",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isDark) NovaFrostWhite else NovaDeepInk
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Your telemetry data stays 100% offline. Write bugs or suggestion logs securely to our developers.",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 11.sp,
                                color = if (isDark) NovaFrostWhite.copy(alpha = 0.6f) else NovaDeepInk.copy(alpha = 0.6f),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Frosted Text Input Wrapper
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    neonAccent = NeonGold.copy(alpha = 0.3f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "💡 WHAT CAN WE IMPROVE?",
                            style = NovaTypography.tagMono,
                            color = NeonGold
                        )
                        NovaTextField(
                            value = feedbackText,
                            onValueChange = { feedbackText = it },
                            placeholder = "Describe feature requests, usability issues or code crashes...",
                            neonColor = NeonGold,
                            singleLine = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                }

                // Submission Result Banner
                AnimatedVisibility(visible = submissionMessage != null) {
                    submissionMessage?.let { msg ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            neonAccent = if (submissionSuccess) NovaSuccess.copy(alpha = 0.4f) else NovaError.copy(alpha = 0.4f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (submissionSuccess) "✅" else "⚠️",
                                    fontSize = 20.sp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = msg,
                                    fontSize = 12.sp,
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Bold,
                                    color = if (submissionSuccess) NovaSuccess else NovaError
                                )
                            }
                        }
                    }
                }

                // Submit Action Button
                NovaPrimaryButton(
                    text = "🚀 Send Securely",
                    neonColor = NeonGold,
                    enabled = !isSubmitting && feedbackText.isNotBlank(),
                    isLoading = isSubmitting,
                    onClick = {
                        if (feedbackText.isNotBlank()) {
                            isSubmitting = true
                            scope.launch {
                                val success = submitFeedback(feedbackText)
                                isSubmitting = false
                                submissionSuccess = success
                                if (success) {
                                    submissionMessage = "Feedback submitted successfully!"
                                    feedbackText = ""
                                    NovaHaptics.success(view)
                                } else {
                                    submissionMessage = "Failed to submit. Check network connection."
                                    NovaHaptics.reject(view)
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

suspend fun submitFeedback(message: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL("https://formspree.io/f/YOUR_FORM_ID")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val jsonObject = JSONObject()
            jsonObject.put("feedback", message)

            connection.outputStream.use { os ->
                val input = jsonObject.toString().toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }

            val responseCode = connection.responseCode
            // Formspree returns 200/201 on success.
            responseCode in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
