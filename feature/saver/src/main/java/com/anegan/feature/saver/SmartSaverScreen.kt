/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.saver

import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.MidnightIndigo
import com.anegan.core.designsystem.theme.PureWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

data class SaverDocument(
    val name: String,
    val path: String,
    val size: Long,
    val lastModified: Long,
    val extension: String,
    val folderName: String // "Receipts", "Scans", "Certificates", "IDs"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartSaverScreen(
    onPresetSelected: (String, Map<String, String>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val prefs = remember { context.getSharedPreferences("anegan_doc_reader", Context.MODE_PRIVATE) }
    
    val folders = listOf("Receipts", "Scans", "Certificates", "IDs")
    
    var allSaverDocs by remember { mutableStateOf(listOf<SaverDocument>()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("Receipts") } // "Receipts", "Scans", "Certificates", "IDs", "Starred"
    var searchQuery by remember { mutableStateOf("") }

    fun scanSaverDirs() {
        isLoading = true
        scope.launch(Dispatchers.IO) {
            val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val parentDir = File(publicDir, "Anegan")
            if (!parentDir.exists()) parentDir.mkdirs()
            
            // Ensure folder structures are pre-created
            folders.forEach { folder ->
                val dir = File(parentDir, folder)
                if (!dir.exists()) dir.mkdirs()
            }
            
            val tempDocs = mutableListOf<SaverDocument>()
            folders.forEach { folder ->
                val dir = File(parentDir, folder)
                dir.listFiles()?.forEach { file ->
                    if (file.isFile && !file.name.startsWith(".")) {
                        tempDocs.add(
                            SaverDocument(
                                name = file.name,
                                path = file.absolutePath,
                                size = file.length(),
                                lastModified = file.lastModified(),
                                extension = file.extension.lowercase(Locale.ROOT),
                                folderName = folder
                            )
                        )
                    }
                }
            }
            
            // Sort by last modified (newest first)
            val sorted = tempDocs.sortedByDescending { it.lastModified }
            withContext(Dispatchers.Main) {
                allSaverDocs = sorted
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        scanSaverDirs()
    }

    BackHandler {
        onBack()
    }

    val filteredSaverDocs = remember(allSaverDocs, selectedFilter, searchQuery) {
        allSaverDocs.filter { doc ->
            val hash = doc.path.hashCode().toString()
            val matchesFilter = if (selectedFilter == "Starred") {
                prefs.getBoolean("pref_pdf_starred_$hash", false)
            } else {
                doc.folderName == selectedFilter
            }
            val matchesSearch = doc.name.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Smart Offline Saver",
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 22.sp),
                            color = MidnightIndigo
                        )
                        Text("Structured on-device organizer for receipts, IDs & scans", color = Color.Gray, fontSize = 10.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = 24.sp, color = MidnightIndigo, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = { scanSaverDirs() }) {
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
            // Search Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MidnightIndigo.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔍", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text("Search documents...", color = Color.Gray, fontSize = 14.sp)
                    }
                    androidx.compose.foundation.text.BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clickable { 
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                searchQuery = "" 
                            }
                            .padding(4.dp)
                    ) {
                        Text("❌", fontSize = 11.sp)
                    }
                }
            }

            // Filter Row (Receipts, Scans, Certificates, IDs, Starred)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val filters = listOf("Receipts", "Scans", "Certificates", "IDs", "Starred")
                items(filters) { filter ->
                    val isSelected = selectedFilter == filter
                    val emoji = when (filter) {
                        "Receipts" -> "🧾"
                        "Scans" -> "📸"
                        "Certificates" -> "🎓"
                        "IDs" -> "💳"
                        else -> "⭐"
                    }
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) MidnightIndigo else MaterialTheme.colorScheme.surface)
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$emoji $filter",
                            color = if (isSelected) PureWhite else MidnightIndigo,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Divider(color = Color.LightGray.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))

            Box(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = MidnightIndigo)
                } else if (filteredSaverDocs.isEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = when (selectedFilter) {
                                "Receipts" -> "🧾"
                                "Scans" -> "📸"
                                "Certificates" -> "🎓"
                                "IDs" -> "💳"
                                else -> "⭐"
                            },
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No $selectedFilter found",
                            fontWeight = FontWeight.Bold,
                            color = MidnightIndigo,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Move files into Downloads/Anegan/$selectedFilter/ on your device to organize them here.",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredSaverDocs) { doc ->
                            val hash = doc.path.hashCode().toString()
                            val isStarred = prefs.getBoolean("pref_pdf_starred_$hash", false)
                            val lastSavedPage = prefs.getInt("pref_pdf_page_$hash", -1)
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        // Save path to recently read list and navigate straight into native readers
                                        val currentListString = prefs.getString("pref_pdf_recent_list", "") ?: ""
                                        val list = currentListString.split("|").filter { it.isNotBlank() }.toMutableList()
                                        list.remove(doc.path)
                                        list.add(0, doc.path)
                                        prefs.edit().putString("pref_pdf_recent_list", list.take(5).joinToString("|")).apply()
                                        
                                        // Trigger callback to deep link directly into Document Hub
                                        onPresetSelected("Document Hub", mapOf("initialFilePath" to doc.path))
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
                                            .background(MidnightIndigo.copy(alpha = 0.08f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when (doc.extension) {
                                                "pdf" -> "📄"
                                                "epub" -> "📚"
                                                "docx" -> "📝"
                                                "jpg", "png", "jpeg" -> "🖼️"
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
                                                text = String.format(Locale.ROOT, "%.2f MB • %s", sizeMb, doc.extension.uppercase(Locale.ROOT)),
                                                color = Color.Gray,
                                                fontSize = 10.sp
                                            )
                                            if (lastSavedPage >= 0 && doc.extension == "pdf") {
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
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                prefs.edit().putBoolean("pref_pdf_starred_$hash", !isStarred).apply()
                                                scanSaverDirs()
                                            }
                                            .wrapContentSize(Alignment.Center)
                                    ) {
                                        Text(
                                            text = if (isStarred) "⭐" else "☆",
                                            fontSize = 18.sp
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
