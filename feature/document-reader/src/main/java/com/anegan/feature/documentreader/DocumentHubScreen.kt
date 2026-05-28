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
import com.anegan.core.designsystem.theme.MidnightIndigo
import com.anegan.core.designsystem.theme.PureWhite
import com.anegan.core.designsystem.theme.LuminousGlow
import kotlinx.coroutines.launch
import java.io.File

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
            else -> {
                TextCodeReaderScreen(
                    filePath = path,
                    onBack = { 
                        activeFilePath = null
                        scanDocs() 
                    }
                )
            }
        }
    } else {
        // Main list screen
        BackHandler {
            onBack()
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Document Hub",
                                style = MaterialTheme.typography.displayLarge.copy(fontSize = 22.sp),
                                color = MidnightIndigo
                            )
                            Text("Your offline-first document toolkit", color = Color.Gray, fontSize = 10.sp)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Text("←", fontSize = 24.sp, color = MidnightIndigo, fontWeight = FontWeight.Bold)
                        }
                    },
                    actions = {
                        IconButton(onClick = { scanDocs() }) {
                            Text("🔄", fontSize = 18.sp)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            }
        ) { innerPadding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Section 1: Dynamic Filter Chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val filters = listOf("All", "PDFs", "Docs", "Ebooks", "Logs", "Starred")
                    items(filters) { filter ->
                        val isSelected = selectedFilter == filter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) MidnightIndigo else MaterialTheme.colorScheme.surface)
                                .clickable { selectedFilter = filter }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = filter,
                                color = if (isSelected) PureWhite else MidnightIndigo,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Section 2: Recent Shelf
                if (recentFiles.isNotEmpty() && selectedFilter == "All") {
                    Column(modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)) {
                        Text(
                            text = "Continue Reading",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MidnightIndigo,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(recentFiles) { path ->
                                val doc = File(path)
                                val hash = path.hashCode().toString()
                                val isStarred = prefs.getBoolean("pref_pdf_starred_$hash", false)
                                
                                Card(
                                    modifier = Modifier
                                        .width(180.dp)
                                        .clickable {
                                            saveRecentFile(path)
                                            activeFilePath = path
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = when (doc.extension.lowercase()) {
                                                    "pdf" -> "📄 PDF"
                                                    "epub" -> "📚 Book"
                                                    "docx" -> "📝 Doc"
                                                    else -> "📁 Log"
                                                },
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MidnightIndigo
                                            )
                                            if (isStarred) {
                                                Text("⭐", fontSize = 10.sp)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = doc.name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MidnightIndigo,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val sizeMb = doc.length() / (1024f * 1024f)
                                        Text(
                                            text = String.format("%.2f MB", sizeMb),
                                            color = Color.Gray,
                                            fontSize = 10.sp
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

                Divider(color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = MidnightIndigo)
                    } else if (filteredDocs.isEmpty()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text("📚", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No documents found",
                                fontWeight = FontWeight.Bold,
                                color = MidnightIndigo,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (selectedFilter == "Starred") "Star your favorite documents for quick access here."
                                else "Put documents (.pdf, .docx, .epub, .txt, .log) in your Downloads or Documents folder to read offline.",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredDocs) { doc ->
                                val hash = doc.path.hashCode().toString()
                                val isStarred = prefs.getBoolean("pref_pdf_starred_$hash", false)
                                val lastSavedPage = prefs.getInt("pref_pdf_page_$hash", -1)

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            saveRecentFile(doc.path)
                                            activeFilePath = doc.path
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
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
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = doc.name,
                                                fontWeight = FontWeight.Bold,
                                                color = MidnightIndigo,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                val sizeMb = doc.size / (1024f * 1024f)
                                                Text(
                                                    text = String.format("%.2f MB", sizeMb),
                                                    color = Color.Gray,
                                                    fontSize = 10.sp
                                                )
                                                if (lastSavedPage >= 0 && doc.category == "PDF") {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "• Page ${lastSavedPage + 1}",
                                                        color = Color(0xFF2E7D32),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold
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
