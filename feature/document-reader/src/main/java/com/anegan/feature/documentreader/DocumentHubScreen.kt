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
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentHubScreen(
    initialFilePath: String? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("anegan_doc_reader", Context.MODE_PRIVATE) }

    var allDocuments by remember { mutableStateOf(listOf<DocumentFile>()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "PDF", "Docs", "Ebook", "Logs", "Starred"

    var activeFilePath by remember { mutableStateOf(initialFilePath) }
    var forceTextMode by remember { mutableStateOf(false) }

    // Recent Files Track
    var recentFiles by remember { mutableStateOf(listOf<String>()) }

    fun scanDocs() {
        isLoading = true
        scope.launch {
            val docs = DocumentScanner.scanLocalDocuments(context)
            allDocuments = docs
            isLoading = false
            
            // Read saved recently opened list
            val recentsString = prefs.getString("pref_pdf_recent_list", "") ?: ""
            recentFiles = recentsString.split("|").filter { it.isNotBlank() && File(it).exists() }
        }
    }

    LaunchedEffect(Unit) {
        scanDocs()
    }

    fun saveRecentFile(path: String) {
        val currentList = recentFiles.toMutableList()
        currentList.remove(path)
        currentList.add(0, path)
        val trimmedList = currentList.take(5) // Keep top 5 recents
        prefs.edit().putString("pref_pdf_recent_list", trimmedList.joinToString("|")).apply()
        recentFiles = trimmedList
    }

    if (activeFilePath != null) {
        val path = activeFilePath!!
        val file = File(path)
        val ext = file.extension.lowercase()
        
        // Launch correct viewer screen dynamically
        when {
            forceTextMode -> {
                TextCodeReaderScreen(
                    filePath = path,
                    onBack = { 
                        forceTextMode = false
                    }
                )
            }
            ext == "pdf" -> {
                PdfReaderScreen(
                    filePath = path,
                    onBack = { 
                        activeFilePath = null
                        scanDocs() 
                    }
                )
            }
            ext == "docx" || ext == "epub" -> {
                OfficeBookReaderScreen(
                    filePath = path,
                    onBack = { 
                        activeFilePath = null
                        scanDocs() 
                    }
                )
            }
            ext == "zip" -> {
                ArchiveViewerScreen(
                    filePath = path,
                    onBack = { 
                        activeFilePath = null
                        scanDocs() 
                    }
                )
            }
            ext == "apk" -> {
                ApkInfoScreen(
                    apkPath = path,
                    onBack = { 
                        activeFilePath = null
                        scanDocs() 
                    }
                )
            }
            ext in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp") -> {
                ImageViewerScreen(
                    filePath = path,
                    onBack = { 
                        activeFilePath = null
                        scanDocs() 
                    }
                )
            }
            ext in listOf("txt", "log", "md", "json", "xml", "html", "css", "js", "py", "kt", "java", "sh", "bat", "ps1", "yaml", "yml", "ini", "properties") -> {
                TextCodeReaderScreen(
                    filePath = path,
                    onBack = { 
                        activeFilePath = null
                        scanDocs() 
                    }
                )
            }
            else -> {
                GenericFileInfoScreen(
                    filePath = path,
                    onBack = { 
                        activeFilePath = null
                        scanDocs() 
                    },
                    onOpenAsText = {
                        forceTextMode = true
                    }
                )
            }
        }
    } else {
        // Main list screen
        BackHandler {
            onBack()
        }

        NovaBackground {
            Scaffold(
                topBar = {
                    NovaTopBar(
                        title = "Document Hub",
                        onBack = onBack,
                        neonAccent = NeonCyan,
                        actions = {
                            IconButton(onClick = { scanDocs() }) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "Refresh",
                                    tint = NeonCyan
                                )
                            }
                        }
                    )
                },
                containerColor = Color.Transparent
            ) { innerPadding ->
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Section 1: Dynamic Filter Chips
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = NovaTokens.Spacing.xl, vertical = NovaTokens.Spacing.xs),
                        horizontalArrangement = Arrangement.spacedBy(NovaTokens.Spacing.xs),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val filters = listOf("All", "PDFs", "Docs", "Ebooks", "Logs", "Starred")
                        items(filters) { filter ->
                            val isSelected = selectedFilter == filter
                            NovaChip(
                                text = filter,
                                selected = isSelected,
                                onClick = { selectedFilter = filter },
                                neonColor = NeonCyan
                            )
                        }
                    }

                    // Section 2: Recent Shelf
                    if (recentFiles.isNotEmpty() && selectedFilter == "All") {
                        Column(modifier = Modifier.padding(top = NovaTokens.Spacing.xs, bottom = NovaTokens.Spacing.sm)) {
                            NovaSectionHeader(
                                title = "Continue Reading",
                                neonColor = NeonCyan
                            )
                            Spacer(modifier = Modifier.height(NovaTokens.Spacing.xxs))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = NovaTokens.Spacing.xl),
                                horizontalArrangement = Arrangement.spacedBy(NovaTokens.Spacing.sm)
                            ) {
                                items(recentFiles) { path ->
                                    val doc = File(path)
                                    val hash = path.hashCode().toString()
                                    val isStarred = prefs.getBoolean("pref_pdf_starred_$hash", false)
                                    
                                    GlassCard(
                                        modifier = Modifier.width(180.dp),
                                        neonAccent = NeonCyan,
                                        onClick = {
                                            saveRecentFile(path)
                                            activeFilePath = path
                                        }
                                    ) {
                                        Column(modifier = Modifier.padding(NovaTokens.Spacing.sm)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = when (doc.extension.lowercase()) {
                                                        "pdf" -> "📄 PDF"
                                                        "epub" -> "📚 Book"
                                                        "docx" -> "📝 Doc"
                                                        else -> "📁 Log"
                                                    },
                                                    style = NovaTypography.tagMono,
                                                    color = NeonCyan
                                                )
                                                if (isStarred) {
                                                    Text("⭐", fontSize = 10.sp)
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(NovaTokens.Spacing.xs))
                                            Text(
                                                text = doc.name,
                                                style = NovaTypography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(NovaTokens.Spacing.xxxs))
                                            val sizeMb = doc.length() / (1024f * 1024f)
                                            Text(
                                                text = String.format(Locale.US, "%.2f MB", sizeMb),
                                                style = NovaTypography.tagMono,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Filter logic
                    val filteredDocs = remember(allDocuments, selectedFilter) {
                        allDocuments.filter { doc ->
                            val hash = doc.path.hashCode().toString()
                            when (selectedFilter) {
                                "PDFs" -> doc.category == "PDF"
                                "Docs" -> doc.category == "Docs"
                                "Ebooks" -> doc.category == "Ebook"
                                "Logs" -> doc.category == "Logs"
                                "Starred" -> prefs.getBoolean("pref_pdf_starred_$hash", false)
                                else -> true
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(NovaTokens.Spacing.xs))

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = NeonCyan)
                        } else if (filteredDocs.isEmpty()) {
                            NovaEmptyState(
                                icon = Icons.Rounded.FolderSpecial,
                                title = "No documents found",
                                subtitle = if (selectedFilter == "Starred") "Star your favorite documents for quick access here."
                                else "Put documents (.pdf, .docx, .epub, .txt, .log) in your Downloads or Documents folder to read offline.",
                                neonColor = NeonCyan
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = NovaTokens.Spacing.xl, vertical = NovaTokens.Spacing.xs),
                                verticalArrangement = Arrangement.spacedBy(NovaTokens.Spacing.xs)
                            ) {
                                items(filteredDocs.size) { index ->
                                    val doc = filteredDocs[index]
                                    val hash = doc.path.hashCode().toString()
                                    val isStarred = prefs.getBoolean("pref_pdf_starred_$hash", false)
                                    val lastSavedPage = prefs.getInt("pref_pdf_page_$hash", -1)

                                    NovaAnimatedItem(index = index) {
                                        GlassCard(
                                            neonAccent = if (isStarred) NeonCyan else Color.Transparent,
                                            onClick = {
                                                saveRecentFile(doc.path)
                                                activeFilePath = doc.path
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(NovaTokens.Spacing.sm),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            when (doc.category) {
                                                                "PDF" -> Color(0xFFEF4444).copy(alpha = 0.1f)
                                                                "Ebook" -> Color(0xFF3B82F6).copy(alpha = 0.1f)
                                                                "Docs" -> Color(0xFF10B981).copy(alpha = 0.1f)
                                                                else -> Color(0xFF6B7280).copy(alpha = 0.1f)
                                                            }
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = when (doc.category) {
                                                            "PDF" -> "📄"
                                                            "Ebook" -> "📚"
                                                            "Docs" -> "📝"
                                                            else -> "📁"
                                                        },
                                                        fontSize = 20.sp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(NovaTokens.Spacing.md))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = doc.name,
                                                        style = NovaTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        val sizeMb = doc.size / (1024f * 1024f)
                                                        Text(
                                                            text = String.format(Locale.US, "%.2f MB", sizeMb),
                                                            style = NovaTypography.tagMono,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                        )
                                                        if (lastSavedPage >= 0 && doc.category == "PDF") {
                                                            Spacer(modifier = Modifier.width(NovaTokens.Spacing.xs))
                                                            Text(
                                                                text = "• Page ${lastSavedPage + 1}",
                                                                style = NovaTypography.tagMono,
                                                                color = NovaSuccess
                                                            )
                                                        }
                                                    }
                                                }
                                                if (isStarred) {
                                                    Text("⭐", fontSize = 14.sp)
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
}
