/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.documentreader

import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import com.anegan.core.designsystem.theme.*
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

data class ArchiveEntry(
    val name: String,
    val size: Long,
    val isDirectory: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveViewerScreen(
    filePath: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val file = remember { File(filePath) }
    
    var entries by remember { mutableStateOf<List<ArchiveEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isExtracting by remember { mutableStateOf(false) }
    var extractionProgress by remember { mutableStateOf(0f) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(filePath) {
        withContext(Dispatchers.IO) {
            try {
                if (file.exists()) {
                    ZipFile(file).use { zip ->
                        val list = zip.entries().asSequence().map { entry ->
                            ArchiveEntry(
                                name = entry.name,
                                size = entry.size,
                                isDirectory = entry.isDirectory
                            )
                        }.toList()
                        withContext(Dispatchers.Main) {
                            entries = list
                            isLoading = false
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Archive file does not exist", Toast.LENGTH_SHORT).show()
                        onBack()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to read zip: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    onBack()
                }
            }
        }
    }

    fun extractArchive() {
        if (isExtracting) return
        scope.launch(Dispatchers.IO) {
            try {
                isExtracting = true
                val destDir = File(Environment.getExternalStorageDirectory(), "Anegan/Extracted/${file.nameWithoutExtension}")
                if (!destDir.exists()) destDir.mkdirs()
                
                ZipInputStream(FileInputStream(file)).use { zis ->
                    var entry = zis.nextEntry
                    var count = 0
                    val total = entries.size.coerceAtLeast(1)
                    
                    while (entry != null) {
                        val outFile = File(destDir, entry.name)
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            FileOutputStream(outFile).use { fos ->
                                zis.copyTo(fos)
                            }
                        }
                        count++
                        extractionProgress = count.toFloat() / total
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Successfully extracted to Anegan/Extracted/${file.nameWithoutExtension}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to extract: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                isExtracting = false
            }
        }
    }

    val filteredEntries = remember(searchQuery, entries) {
        if (searchQuery.isBlank()) entries
        else entries.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    BackHandler {
        onBack()
    }

    NovaBackground {
        Scaffold(
            topBar = {
                NovaTopBar(
                    title = file.name,
                    onBack = onBack,
                    neonAccent = NeonCyan,
                    actions = {
                        Text(
                            text = "${entries.size} files",
                            style = NovaTypography.tagMono,
                            color = NeonCyan,
                            modifier = Modifier.padding(end = NovaTokens.Spacing.md)
                        )
                    }
                )
            },
            containerColor = Color.Transparent,
            bottomBar = {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(NovaTokens.Spacing.md),
                    neonAccent = NeonCyan
                ) {
                    Column(modifier = Modifier.padding(NovaTokens.Spacing.md)) {
                        if (isExtracting) {
                            Text(
                                text = "Extracting files... ${(extractionProgress * 100).toInt()}%",
                                style = NovaTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = NeonCyan
                            )
                            Spacer(modifier = Modifier.height(NovaTokens.Spacing.xs))
                            LinearProgressIndicator(
                                progress = extractionProgress,
                                modifier = Modifier.fillMaxWidth(),
                                color = NeonCyan,
                                trackColor = NeonCyan.copy(alpha = 0.2f)
                            )
                        } else {
                            NovaPrimaryButton(
                                text = "Extract All to /Anegan/Extracted/",
                                neonColor = NeonCyan,
                                onClick = { extractArchive() },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Search field
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = NovaTokens.Spacing.xl, vertical = NovaTokens.Spacing.xs),
                    neonAccent = NeonCyan
                ) {
                    NovaTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Search files in archive...",
                        neonColor = NeonCyan
                    )
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NeonCyan)
                    }
                } else if (filteredEntries.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No files match search query", style = NovaTypography.bodyMedium, color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = NovaTokens.Spacing.xl, vertical = NovaTokens.Spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(NovaTokens.Spacing.xs)
                    ) {
                        items(filteredEntries.size) { index ->
                            val entry = filteredEntries[index]
                            NovaAnimatedItem(index = index) {
                                GlassCard(
                                    neonAccent = Color.Transparent
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(NovaTokens.Spacing.sm),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (entry.isDirectory) "📁" else "📄",
                                            fontSize = 20.sp
                                        )
                                        Spacer(modifier = Modifier.width(NovaTokens.Spacing.md))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = entry.name,
                                                style = NovaTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (!entry.isDirectory) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                val kb = entry.size / 1024f
                                                val sizeText = if (kb > 1024f) String.format(Locale.US, "%.2f MB", kb / 1024f) else String.format(Locale.US, "%.1f KB", kb)
                                                Text(
                                                    text = sizeText,
                                                    style = NovaTypography.tagMono,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
