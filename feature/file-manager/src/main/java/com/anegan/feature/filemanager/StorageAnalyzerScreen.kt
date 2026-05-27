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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.MidnightIndigo
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

    LaunchedEffect(Unit) {
        // Kick off scan
        val result = withContext(Dispatchers.IO) {
            scanDirectory(Environment.getExternalStorageDirectory())
        }
        scanResult = result
        largeFilesState.clear()
        largeFilesState.addAll(result.largeFiles)
        isScanning = false
        // Animate ring after data loads
        animatedProgress.animateTo(
            targetValue = usedFraction,
            animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Storage Analyzer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        if (isScanning) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MidnightIndigo, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Scanning storage…", style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Storage ring ──────────────────────────────────────────
                item {
                    StorageRingCard(
                        totalBytes = totalBytes,
                        usedBytes = usedBytes,
                        freeBytes = freeBytes,
                        animatedProgress = animatedProgress.value
                    )
                }

                // ── Category breakdown ─────────────────────────────────────
                item {
                    scanResult?.let { result ->
                        val total = (result.images + result.videos + result.audio +
                                result.documents + result.apks + result.other).coerceAtLeast(1L)
                        val categories = listOf(
                            StorageCategory("Images", "🖼️", result.images, Color(0xFF3B82F6)),
                            StorageCategory("Videos", "🎬", result.videos, Color(0xFF8B5CF6)),
                            StorageCategory("Audio", "🎵", result.audio, Color(0xFF10B981)),
                            StorageCategory("Documents", "📄", result.documents, Color(0xFFF59E0B)),
                            StorageCategory("APKs", "📦", result.apks, Color(0xFFEF4444)),
                            StorageCategory("Other", "📁", result.other, Color(0xFF6B7280))
                        )
                        CategoryBreakdownCard(categories = categories, total = total)
                    }
                }

                // ── Large files section ────────────────────────────────────
                if (largeFilesState.isNotEmpty()) {
                    item {
                        Text(
                            "Large Files  (> 50 MB)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MidnightIndigo
                        )
                    }
                    items(largeFilesState, key = { it.path }) { lf ->
                        LargeFileRow(
                            largeFile = lf,
                            onDelete = {
                                deleteTarget = lf
                                showDeleteDialog = true
                            }
                        )
                    }
                } else {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "✅ No large files found (> 50 MB)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
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
                title = { Text("Delete file?") },
                text = {
                    Text("Delete \"${lf.name}\" (${lf.sizeBytes.toHumanReadable()})? This cannot be undone.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                val ok = withContext(Dispatchers.IO) {
                                    runCatching { File(lf.path).delete() }.getOrDefault(false)
                                }
                                if (ok) largeFilesState.remove(lf)
                                showDeleteDialog = false
                                deleteTarget = null
                                snackbarHostState.showSnackbar(
                                    if (ok) "Deleted ${lf.name}" else "Could not delete file"
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                    ) { Text("Delete", color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false; deleteTarget = null }) {
                        Text("Cancel")
                    }
                }
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
    animatedProgress: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Internal Storage",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Circular progress ring
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = 1f,
                    modifier = Modifier.size(160.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 14.dp,
                    strokeCap = StrokeCap.Round
                )
                CircularProgressIndicator(
                    progress = animatedProgress,
                    modifier = Modifier.size(160.dp),
                    color = MidnightIndigo,
                    strokeWidth = 14.dp,
                    strokeCap = StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${(animatedProgress * 100).toInt()}%",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MidnightIndigo
                    )
                    Text("used", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StorageStat(label = "Total", value = totalBytes.toHumanReadable(), color = MaterialTheme.colorScheme.onSurface)
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                StorageStat(label = "Used", value = usedBytes.toHumanReadable(), color = MidnightIndigo)
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                StorageStat(label = "Free", value = freeBytes.toHumanReadable(), color = Color(0xFF10B981))
            }
        }
    }
}

@Composable
private fun StorageStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
    }
}

// ─────────────────────────────────────────────────────────
//  Category breakdown card
// ─────────────────────────────────────────────────────────

@Composable
private fun CategoryBreakdownCard(categories: List<StorageCategory>, total: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "By Category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            categories.forEach { cat ->
                CategoryRow(category = cat, total = total)
                Spacer(modifier = Modifier.height(12.dp))
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
                Text(category.emoji, fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(category.name, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                category.sizeBytes.toHumanReadable(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round
        )
    }
}

// ─────────────────────────────────────────────────────────
//  Large file row
// ─────────────────────────────────────────────────────────

@Composable
private fun LargeFileRow(largeFile: LargeFile, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File type icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
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
                Text(fileEmoji(fakeItem), fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    largeFile.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    largeFile.sizeBytes.toHumanReadable(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFEF4444)
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color(0xFFEF4444)
                )
            }
        }
    }
}
