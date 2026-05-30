/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 *
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.filemanager

import android.os.Environment
import android.os.StatFs
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// ─────────────────────────────────────────────────────────
//  Data models
// ─────────────────────────────────────────────────────────

data class StorageCategory(
    val name: String,
    val emoji: String,
    val sizeBytes: Long,
    val color: Color
)

data class LargeFile(
    val name: String,
    val path: String,
    val sizeBytes: Long
)

// ─────────────────────────────────────────────────────────
//  Scanner helpers
// ─────────────────────────────────────────────────────────

private val imageExts = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "svg")
private val videoExts = setOf("mp4", "mkv", "avi", "mov", "webm", "3gp", "flv", "ts")
private val audioExts = setOf("mp3", "m4a", "flac", "wav", "aac", "ogg", "wma", "opus")
private val docExts = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md", "odt", "csv")
private val apkExts = setOf("apk")

private const val LARGE_FILE_THRESHOLD = 50L * 1024 * 1024 // 50 MB

data class ScanResult(
    val images: Long,
    val videos: Long,
    val audio: Long,
    val documents: Long,
    val apks: Long,
    val other: Long,
    val largeFiles: List<LargeFile>
)

private fun scanDirectory(root: File): ScanResult {
    var images = 0L
    var videos = 0L
    var audio = 0L
    var documents = 0L
    var apks = 0L
    var other = 0L
    val largeFiles = mutableListOf<LargeFile>()

    fun traverse(file: File) {
        if (!file.exists()) return
        if (file.isDirectory) {
            if (file.name.startsWith(".")) return
            try {
                file.listFiles()?.forEach { traverse(it) }
            } catch (_: SecurityException) {}
        } else {
            val ext = file.extension.lowercase()
            val size = file.length()
            when (ext) {
                in imageExts -> images += size
                in videoExts -> videos += size
                in audioExts -> audio += size
                in docExts -> documents += size
                in apkExts -> apks += size
                else -> other += size
            }
            if (size >= LARGE_FILE_THRESHOLD) {
                largeFiles.add(LargeFile(name = file.name, path = file.absolutePath, sizeBytes = size))
            }
        }
    }

    traverse(root)
    largeFiles.sortByDescending { it.sizeBytes }
    return ScanResult(images, videos, audio, documents, apks, other, largeFiles)
}

// ─────────────────────────────────────────────────────────
//  Main Screen
// ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageAnalyzerScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val isDark = isSystemInDarkTheme()
    val snackbarHostState = remember { SnackbarHostState() }

    // Storage stats
    val stat = remember { StatFs(Environment.getExternalStorageDirectory().path) }
    val totalBytes = remember { stat.totalBytes }
    val freeBytes = remember { stat.availableBytes }
    val usedBytes = remember { totalBytes - freeBytes }

    // Scan state
    var isScanning by remember { mutableStateOf(true) }
    var scanResult by remember { mutableStateOf<ScanResult?>(null) }
    val largeFilesState = remember { mutableStateListOf<LargeFile>() }

    // Delete dialog
    var deleteTarget by remember { mutableStateOf<LargeFile?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Animated progress
    val usedFraction = if (totalBytes > 0) usedBytes.toFloat() / totalBytes.toFloat() else 0f
    val animatedProgress = remember { Animatable(0f) }

    val primaryAccent = NeonCyan // Cyan for files / storage analysis

    LaunchedEffect(Unit) {
        val result = withContext(Dispatchers.IO) {
            scanDirectory(Environment.getExternalStorageDirectory())
        }
        scanResult = result
        largeFilesState.clear()
        largeFilesState.addAll(result.largeFiles)
        isScanning = false
        // Animate progress ring
        animatedProgress.animateTo(
            targetValue = usedFraction,
            animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
        )
    }

    BackHandler {
        onBack()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            NovaTopBar(
                title = "Disk Diagnostics",
                onBack = onBack,
                neonAccent = primaryAccent
            )
        }
    ) { innerPadding ->
        NovaBackground {
            if (isScanning) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    GlassCard(
                        neonAccent = primaryAccent,
                        enableGlow = true
                    ) {
                        Column(
                            modifier = Modifier.padding(NovaTokens.Spacing.xl),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = primaryAccent, 
                                modifier = Modifier.size(56.dp),
                                strokeWidth = 4.dp
                            )
                            Spacer(modifier = Modifier.height(NovaTokens.Spacing.md))
                            Text(
                                text = "Traversing directory clusters...", 
                                style = NovaTypography.tagMono.copy(color = primaryAccent)
                            )
                            Spacer(modifier = Modifier.height(NovaTokens.Spacing.xxs))
                            Text(
                                text = "Indexing local disk sector sizes offline", 
                                style = NovaTypography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(horizontal = NovaTokens.Spacing.md, vertical = NovaTokens.Spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(NovaTokens.Spacing.md)
                ) {
                    // ── Storage Ring Diagnostic Card ──────────────────────────────────────────
                    item {
                        StorageRingCard(
                            totalBytes = totalBytes,
                            usedBytes = usedBytes,
                            freeBytes = freeBytes,
                            animatedProgress = animatedProgress.value,
                            neonColor = primaryAccent
                        )
                    }

                    // ── Category breakdown ─────────────────────────────────────
                    item {
                        scanResult?.let { result ->
                            val total = (result.images + result.videos + result.audio +
                                    result.documents + result.apks + result.other).coerceAtLeast(1L)
                            
                            // Highly themed categories mapping to exact V3.2 palette coordinates
                            val categories = listOf(
                                StorageCategory("Images / DCIM Assets", "🖼️", result.images, NeonBlue),
                                StorageCategory("Videos / Movies Content", "🎬", result.videos, NeonMagenta),
                                StorageCategory("Audio / Music Tracks", "🎵", result.audio, NeonGold),
                                StorageCategory("Documents / Raw Data", "📄", result.documents, NeonCyan),
                                StorageCategory("APK Packages / Extracted", "📦", result.apks, NeonLime),
                                StorageCategory("Unmapped Sectors / Other", "📁", result.other, if (isDark) NovaBorderDark else NovaBorderLight)
                            )
                            CategoryBreakdownCard(categories = categories, total = total, neonColor = primaryAccent)
                        }
                    }

                    // ── Large files section ────────────────────────────────────
                    if (largeFilesState.isNotEmpty()) {
                        item {
                            NovaSectionHeader(
                                title = "Identified Large Files (> 50 MB)",
                                neonColor = NovaError
                            )
                        }
                        items(largeFilesState, key = { it.path }) { lf ->
                            LargeFileRow(
                                largeFile = lf,
                                onDelete = {
                                    NovaHaptics.warning(view)
                                    deleteTarget = lf
                                    showDeleteDialog = true
                                },
                                neonColor = primaryAccent
                            )
                        }
                    } else {
                        item {
                            GlassCard(
                                neonAccent = primaryAccent,
                                enableGlow = true
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "✅ Storage optimized: No heavy files found (> 50 MB)",
                                        style = NovaTypography.tagMono.copy(
                                            color = primaryAccent,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        deleteTarget?.let { lf ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false; deleteTarget = null },
                title = { 
                    Text(
                        text = "Destroy Large File Asset?", 
                        style = NovaTypography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = NovaError)
                    ) 
                },
                text = {
                    Text(
                        text = "Permanently purge \"${lf.name}\" (${lf.sizeBytes.toHumanReadable()}) from internal storage? This bypasses all recovery grids and cannot be undone.",
                        style = NovaTypography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    NovaPrimaryButton(
                        text = "Confirm Destruction",
                        neonColor = NovaError,
                        onClick = {
                            scope.launch {
                                val ok = withContext(Dispatchers.IO) {
                                    runCatching { File(lf.path).delete() }.getOrDefault(false)
                                }
                                if (ok) {
                                    largeFilesState.remove(lf)
                                    NovaHaptics.success(view)
                                } else {
                                    NovaHaptics.warning(view)
                                }
                                showDeleteDialog = false
                                deleteTarget = null
                                snackbarHostState.showSnackbar(
                                    if (ok) "Asset successfully purged from storage" else "Error: Asset delete operation blocked"
                                )
                            }
                        }
                    )
                },
                dismissButton = {
                    NovaSecondaryButton(
                        text = "Cancel",
                        neonColor = primaryAccent,
                        onClick = { 
                            showDeleteDialog = false
                            deleteTarget = null 
                        }
                    )
                },
                containerColor = if (isDark) NovaMidnightBlue else Color.White,
                shape = RoundedCornerShape(NovaTokens.Radius.lg),
                modifier = Modifier.border(1.dp, NovaError.copy(alpha = 0.2f), RoundedCornerShape(NovaTokens.Radius.lg))
            )
        }
    }
}

// ─────────────────────────────────────────────────────────
//  Storage ring card
// ─────────────────────────────────────────────────────────

@Composable
private fun StorageRingCard(
    totalBytes: Long,
    usedBytes: Long,
    freeBytes: Long,
    animatedProgress: Float,
    neonColor: Color
) {
    val isDark = isSystemInDarkTheme()
    
    GlassCard(
        neonAccent = neonColor,
        enableGlow = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(NovaTokens.Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Disk Diagnostics Core",
                style = NovaTypography.tagMono.copy(fontWeight = FontWeight.Bold, color = neonColor)
            )
            Spacer(modifier = Modifier.height(NovaTokens.Spacing.lg))

            // Circular progress ring styled as high-tech futuristic holographic core
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = 1f,
                    modifier = Modifier.size(160.dp),
                    color = neonColor.copy(alpha = 0.12f),
                    strokeWidth = 12.dp,
                    strokeCap = StrokeCap.Round
                )
                CircularProgressIndicator(
                    progress = animatedProgress,
                    modifier = Modifier.size(160.dp),
                    color = neonColor,
                    strokeWidth = 12.dp,
                    strokeCap = StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(animatedProgress * 100).toInt()}%",
                        style = NovaTypography.displayLarge.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = neonColor
                        )
                    )
                    Text(
                        text = "CORE LOADED", 
                        style = NovaTypography.tagMono.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(NovaTokens.Spacing.lg))

            // Stats row (Monospace JetBrains Mono values)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StorageStat(
                    label = "TOTAL SPACE", 
                    value = totalBytes.toHumanReadable(), 
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(if (isDark) NovaBorderDark.copy(alpha = 0.2f) else NovaBorderLight.copy(alpha = 0.2f))
                )
                StorageStat(
                    label = "SECTORS USED", 
                    value = usedBytes.toHumanReadable(), 
                    color = neonColor
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(if (isDark) NovaBorderDark.copy(alpha = 0.2f) else NovaBorderLight.copy(alpha = 0.2f))
                )
                StorageStat(
                    label = "FREE MEMORY", 
                    value = freeBytes.toHumanReadable(), 
                    color = NeonLime
                )
            }
        }
    }
}

@Composable
private fun StorageStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value, 
            style = NovaTypography.tagMono.copy(fontWeight = FontWeight.Bold, color = color, fontSize = 12.sp)
        )
        Text(
            text = label, 
            style = NovaTypography.tagMono.copy(fontSize = 8.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

// ─────────────────────────────────────────────────────────
//  Category breakdown card
// ─────────────────────────────────────────────────────────

@Composable
private fun CategoryBreakdownCard(
    categories: List<StorageCategory>, 
    total: Long,
    neonColor: Color
) {
    GlassCard(
        neonAccent = neonColor,
        enableGlow = false
    ) {
        Column(modifier = Modifier.padding(NovaTokens.Spacing.md)) {
            NovaSectionHeader(
                title = "Sector Allocation Breakdown",
                neonColor = neonColor
            )
            Spacer(modifier = Modifier.height(NovaTokens.Spacing.sm))
            categories.forEach { cat ->
                CategoryRow(category = cat, total = total)
                if (cat != categories.last()) {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(category: StorageCategory, total: Long) {
    val fraction = (category.sizeBytes.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(category.emoji, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = category.name, 
                    style = NovaTypography.headlineSmall.copy(fontWeight = FontWeight.Medium)
                )
            }
            Text(
                text = category.sizeBytes.toHumanReadable(),
                style = NovaTypography.tagMono.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = fraction,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = category.color,
            trackColor = category.color.copy(alpha = 0.15f),
            strokeCap = StrokeCap.Round
        )
    }
}

// ─────────────────────────────────────────────────────────
//  Large file row
// ─────────────────────────────────────────────────────────

@Composable
private fun LargeFileRow(
    largeFile: LargeFile, 
    onDelete: () -> Unit,
    neonColor: Color
) {
    val isDark = isSystemInDarkTheme()
    
    GlassCard(
        neonAccent = NovaError.copy(alpha = 0.3f),
        enableGlow = false
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NovaTokens.Spacing.md, vertical = NovaTokens.Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cyber Icon Container
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(NovaTokens.Radius.sm))
                    .background(NovaError.copy(alpha = 0.08f))
                    .border(1.dp, NovaError.copy(alpha = 0.2f), RoundedCornerShape(NovaTokens.Radius.sm)),
                contentAlignment = Alignment.Center
            ) {
                val fakeItem = FileItem(
                    name = largeFile.name,
                    path = largeFile.path,
                    isDirectory = false,
                    size = largeFile.sizeBytes,
                    lastModified = 0L,
                    extension = largeFile.name.substringAfterLast('.', "").lowercase()
                )
                Text(fileEmoji(fakeItem), fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = largeFile.name,
                    style = NovaTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = largeFile.sizeBytes.toHumanReadable(),
                    style = NovaTypography.tagMono.copy(
                        color = NovaError, 
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Purge file",
                    tint = NovaError.copy(alpha = 0.8f)
                )
            }
        }
    }
}
