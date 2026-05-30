/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.history

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.database.DatabaseProvider
import com.anegan.core.database.ConversionHistoryEntity
import com.anegan.core.designsystem.theme.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Lock
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = context as? androidx.fragment.app.FragmentActivity
    val coroutineScope = rememberCoroutineScope()
    val historyDao = remember { DatabaseProvider.getDatabase(context).historyDao() }
    val historyList by historyDao.getAllHistory().collectAsState(initial = emptyList())

    var isUnlocked by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (activity != null) {
            BiometricHelper.authenticate(
                activity = activity,
                onSuccess = { 
                    isUnlocked = true 
                    NovaHaptics.success(view)
                },
                onError = { err -> 
                    authError = err 
                    NovaHaptics.warning(view)
                }
            )
        } else {
            isUnlocked = true
        }
    }

    NovaBackground(modifier = modifier) {
        if (!isUnlocked) {
            // Holographic Cyberpunk Security Terminal Lock
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                GlassCard(
                    modifier = Modifier
                        .widthIn(max = 340.dp)
                        .padding(24.dp),
                    neonAccent = NeonGold,
                    enableGlow = true
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        // Pulsing Lock Icon in Center of Gold Ring
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(100.dp)
                        ) {
                            NovaPulseRing(
                                neonColor = NeonGold,
                                isActive = true,
                                baseRadius = 36f,
                                pulseAmplitude = 10f
                            )
                            Text("🔒", fontSize = 36.sp)
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Text(
                            text = "HISTORY VAULT SECURED",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = NeonGold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = authError ?: "Verify your identity with biometrics to audit past conversion logs.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        NovaPrimaryButton(
                            text = "Verify Identity",
                            neonColor = NeonGold,
                            onClick = {
                                if (activity != null) {
                                    BiometricHelper.authenticate(
                                        activity = activity,
                                        onSuccess = { 
                                            isUnlocked = true 
                                            NovaHaptics.success(view)
                                        },
                                        onError = { err -> 
                                            authError = err 
                                            NovaHaptics.warning(view)
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        NovaSecondaryButton(
                            text = "Cancel & Back",
                            neonColor = NeonGold,
                            onClick = {
                                onBack()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        } else {
            // Main History Content Screen
            Column(modifier = Modifier.fillMaxSize()) {
                NovaTopBar(
                    title = "Audit History",
                    onBack = onBack,
                    neonAccent = NeonGold
                ) {
                    if (historyList.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                NovaHaptics.click(view)
                                coroutineScope.launch {
                                    historyDao.clearHistory()
                                }
                            }
                        ) {
                            Text(
                                "Clear All", 
                                color = NeonGold, 
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (historyList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        NovaEmptyState(
                            icon = Icons.Default.Refresh,
                            title = "History Void",
                            subtitle = "No offline conversion activities recorded yet. All your tasks will be logged locally here.",
                            neonColor = NeonGold
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        itemsIndexed(
                            items = historyList,
                            key = { _, item -> item.id }
                        ) { index, item ->
                            NovaAnimatedItem(index = index) {
                                HistoryItemRow(item = item)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItemRow(item: ConversionHistoryEntity) {
    val view = LocalView.current
    val dateString = remember(item.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
        sdf.format(Date(item.timestamp))
    }

    val isSuccess = item.status == "SUCCESS"
    val accentColor = if (isSuccess) NovaSuccess else NovaError

    GlassCard(
        neonAccent = accentColor.copy(alpha = 0.3f),
        cornerRadius = NovaTokens.Radius.lg,
        enableGlow = false
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Monospace paths & details
                Text(
                    text = "${item.originalFileName} → ${item.outputFileName.ifEmpty { "FAILED" }}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FiraCodeFontFamily,
                    fontSize = 13.sp,
                    maxLines = 2
                )
                
                Spacer(modifier = Modifier.height(6.dp))

                val sizeMb = item.originalSize / (1024f * 1024f)
                val outputSizeMb = item.outputSize / (1024f * 1024f)
                val sizeText = if (isSuccess) {
                    String.format("%.2f MB → %.2f MB", sizeMb, outputSizeMb)
                } else {
                    String.format("%.2f MB", sizeMb)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = dateString,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontFamily = FiraCodeFontFamily
                    )
                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Text(
                        text = sizeText,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontFamily = FiraCodeFontFamily
                    )
                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Text(
                        text = item.outputFormat.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        fontFamily = FiraCodeFontFamily
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Neon Badge indicator
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.12f))
                    .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                val mark = if (isSuccess) "✓" else "✗"
                Text(
                    text = mark,
                    color = accentColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }
        }
    }
}
