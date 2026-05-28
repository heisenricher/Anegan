/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.dashboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.MidnightIndigo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.text.style.TextOverflow

data class Category(val title: String, val description: String, val icon: String = "🛠️")

val categories = listOf(
    Category("Survival Library",   "Exhaustive offline survival & medical books", "📚"),
    Category("Notes",             "Quick notes, reminders, checklists", "📝"),
    Category("Vault",             "Encrypted secure document storage", "🔒"),
    Category("Document Hub",      "Read your PDFs, DOCX, EPUB & text files", "📂"),
    Category("File Manager",      "Browse, zip, manage your files", "📁"),
    Category("Wi-Fi & FTP Transfer", "Share files locally with standard web browsers or FTP clients", "⚡"),
    Category("APK Extractor",   "Backup and share your installed application packages", "📲"),
    Category("Smart Saver",     "Structured organizer for receipts, IDs & certificates", "💾"),
    Category("Documents",         "Merge, text-to-pdf, unlock", "📄"),
    Category("PDF Tools",         "Split, compress, encrypt, images", "🗏"),
    Category("Images",            "JPG, PNG, WEBP, quality", "🖼️"),
    Category("Batch Image",       "Process multiple images", "📸"),
    Category("Video",             "Convert MP4, MKV, AVI", "🎥"),
    Category("Video Tools",       "Trim, compress, speed, GIF", "🎬"),
    Category("Audio",             "Convert MP3, M4A, FLAC", "🎵"),
    Category("Audio Tools",       "Cut audio, ringtones", "✂️"),
    Category("OCR / Extract Text","Image to Text (Offline)", "👁️"),
    Category("EXIF Metadata",     "View & strip photo metadata", "ℹ️"),
    Category("Developer Tools",   "Hash, Base64, QR codes", "💻"),
    Category("AI Background Remover", "Isolate subjects offline", "🪄"),
    Category("Image Watermark",   "Add text watermarks", "🖋️"),
    Category("PDF Reader & Editor", "Read, draw, sign and edit pages", "✍️"),
    Category("Calculator",        "Offline arithmetic calculator", "🧮"),
    Category("Flashlight",        "Torch, strobe & SOS beacon", "🔦"),
    Category("Compass",           "Offline bearing & orientation sensor", "🧭"),
    Category("Currency Converter","Offline exchange rate converter", "💱"),
    Category("Offline Comm",     "Bluetooth chat, SOS beacon, mesh", "📡"),
    Category("Color Picker",      "Analyze and save palettes", "🎨"),
    Category("Unit Converter",    "Offline length, mass, data size conversion", "⚖️"),
    Category("History",           "Recent Conversions", "📜"),
    Category("Settings",          "App Config", "⚙️"),
    Category("Feedback",          "Report bugs to GitHub", "💬")
)

@Composable
fun DashboardScreen(
    onCategorySelected: (String) -> Unit,
    onPresetSelected: (String, Map<String, String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val prefs = remember { context.getSharedPreferences("anegan_favorites", Context.MODE_PRIVATE) }
    val docPrefs = remember { context.getSharedPreferences("anegan_doc_reader", Context.MODE_PRIVATE) }
    
    var favoriteTitles by remember {
        mutableStateOf(prefs.getStringSet("favorite_categories", emptySet()) ?: emptySet())
    }

    var searchQuery by remember { mutableStateOf("") }
    
    // Cross-module search items
    var allNotes by remember { mutableStateOf(listOf<com.anegan.feature.notes.Note>()) }
    var allDocuments by remember { mutableStateOf(listOf<com.anegan.feature.documentreader.DocumentFile>()) }
    var recentFiles by remember { mutableStateOf(listOf<String>()) }
    
    LaunchedEffect(Unit) {
        allNotes = com.anegan.feature.notes.loadNotes(context)
        allDocuments = com.anegan.feature.documentreader.DocumentScanner.scanLocalDocuments(context)
        
        val recentsString = docPrefs.getString("pref_pdf_recent_list", "") ?: ""
        recentFiles = recentsString.split("|").filter { it.isNotBlank() && java.io.File(it).exists() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        // App Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Anegan",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 32.sp)
                )
                Text(
                    text = "Your offline utility suite — v2.5",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1B5E20).copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "🛡️ 100% Offline",
                            fontSize = 10.sp,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0D47A1).copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "🔒 No Account",
                            fontSize = 10.sp,
                            color = Color(0xFF1565C0),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Search Bar ─────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MidnightIndigo.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🔍", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "Search notes, files, tools...",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyLarge
                    )
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
                    Text("❌", fontSize = 12.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))

        if (searchQuery.isEmpty()) {
            // ── Standard Dashboard Sections ──────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                // ── Recent Documents Shelf ─────────────────────────────────
                if (recentFiles.isNotEmpty()) {
                    Text(
                        text = "Continue Reading 📚",
                        style = MaterialTheme.typography.titleMedium,
                        color = MidnightIndigo
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(recentFiles) { path ->
                            val file = java.io.File(path)
                            val hash = path.hashCode().toString()
                            val lastSavedPage = docPrefs.getInt("pref_pdf_page_$hash", -1)
                            val isStarred = docPrefs.getBoolean("pref_pdf_starred_$hash", false)
                            
                            Card(
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(100.dp)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onPresetSelected("Document Hub", mapOf("initialFilePath" to path))
                                    },
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = when (file.extension.lowercase(java.util.Locale.ROOT)) {
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
                                    Text(
                                        text = file.name,
                                        color = MidnightIndigo,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val sizeMb = file.length() / (1024f * 1024f)
                                        Text(
                                            text = String.format("%.2f MB", sizeMb),
                                            color = Color.Gray,
                                            fontSize = 10.sp
                                        )
                                        if (lastSavedPage >= 0 && file.extension.lowercase(java.util.Locale.ROOT) == "pdf") {
                                            Text(
                                                text = "Page ${lastSavedPage + 1}",
                                                color = Color(0xFF2E7D32),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // ── Favorites ──────────────────────────────────────────────
                if (favoriteTitles.isNotEmpty()) {
                    Text(
                        text = "Favorites ❤️",
                        style = MaterialTheme.typography.titleMedium,
                        color = MidnightIndigo
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val favoriteCats = categories.filter { favoriteTitles.contains(it.title) }
                        items(favoriteCats) { category ->
                            FavoriteCategoryCard(
                                category = category,
                                onClick = { 
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onCategorySelected(category.title) 
                                },
                                onUnfavorite = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val newFavorites = favoriteTitles - category.title
                                    favoriteTitles = newFavorites
                                    prefs.edit().putStringSet("favorite_categories", newFavorites).apply()
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }



                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(categories) { category ->
                        val isFav = favoriteTitles.contains(category.title)
                        CategoryCard(
                            category = category,
                            isFavorite = isFav,
                            onFavoriteToggle = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val newFavorites = if (isFav) {
                                    favoriteTitles - category.title
                                } else {
                                    favoriteTitles + category.title
                                }
                                favoriteTitles = newFavorites
                                prefs.edit().putStringSet("favorite_categories", newFavorites).apply()
                            },
                            onClick = { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onCategorySelected(category.title) 
                            }
                        )
                    }
                }
            }
        } else {
            // ── Search Results View ──────────────────────────────────
            val matchingCategories = remember(searchQuery) {
                categories.filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true)
                }
            }
            val matchingNotes = remember(searchQuery, allNotes) {
                allNotes.filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                    it.content.contains(searchQuery, ignoreCase = true)
                }
            }
            val matchingDocuments = remember(searchQuery, allDocuments) {
                allDocuments.filter {
                    it.name.contains(searchQuery, ignoreCase = true)
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (matchingCategories.isNotEmpty()) {
                    item {
                        Text(
                            text = "Tools & Utilities 🛠️",
                            style = MaterialTheme.typography.titleMedium,
                            color = MidnightIndigo,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(matchingCategories) { category ->
                        val isFav = favoriteTitles.contains(category.title)
                        CategoryCard(
                            category = category,
                            isFavorite = isFav,
                            onFavoriteToggle = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val newFavorites = if (isFav) {
                                    favoriteTitles - category.title
                                } else {
                                    favoriteTitles + category.title
                                }
                                favoriteTitles = newFavorites
                                prefs.edit().putStringSet("favorite_categories", newFavorites).apply()
                            },
                            onClick = { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onCategorySelected(category.title) 
                            }
                        )
                    }
                }
                
                if (matchingNotes.isNotEmpty()) {
                    item {
                        Text(
                            text = "Notes 📝",
                            style = MaterialTheme.typography.titleMedium,
                            color = MidnightIndigo,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(matchingNotes) { note ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onPresetSelected("Notes", mapOf("noteId" to note.id))
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = note.title.ifBlank { "Untitled Note" },
                                    fontWeight = FontWeight.Bold,
                                    color = MidnightIndigo,
                                    fontSize = 14.sp
                                )
                                if (note.content.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = note.content,
                                        color = Color.Gray,
                                        fontSize = 12.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
                
                if (matchingDocuments.isNotEmpty()) {
                    item {
                        Text(
                            text = "Documents 📚",
                            style = MaterialTheme.typography.titleMedium,
                            color = MidnightIndigo,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(matchingDocuments) { doc ->
                        val hash = doc.path.hashCode().toString()
                        val lastSavedPage = docPrefs.getInt("pref_pdf_page_$hash", -1)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onPresetSelected("Document Hub", mapOf("initialFilePath" to doc.path))
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
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
                                        fontSize = 18.sp
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
                            }
                        }
                    }
                }
                
                if (matchingCategories.isEmpty() && matchingNotes.isEmpty() && matchingDocuments.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 64.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🔍", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No results found",
                                fontWeight = FontWeight.Bold,
                                color = MidnightIndigo,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Try searching for another tool, note, or file.",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoriteCategoryCard(
    category: Category,
    onClick: () -> Unit,
    onUnfavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(170.dp)
            .height(105.dp)
            .semantics {
                contentDescription = "Favorite Category: ${category.title}. ${category.description}"
            }
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MidnightIndigo.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category.icon,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = category.title,
                    color = MidnightIndigo,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onUnfavorite() }
                        .wrapContentSize(Alignment.Center)
                ) {
                    Text(
                        text = "❤️",
                        fontSize = 12.sp
                    )
                }
            }
            Text(
                text = category.description,
                color = Color.Gray,
                fontSize = 10.sp,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CategoryCard(
    category: Category,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .semantics {
                contentDescription = "Launch ${category.title} converter: ${category.description}"
            }
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MidnightIndigo.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category.icon,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                    fontWeight = FontWeight.Bold,
                    color = MidnightIndigo,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onFavoriteToggle() }
                        .wrapContentSize(Alignment.Center)
                ) {
                    Text(
                        text = if (isFavorite) "⭐" else "☆",
                        fontSize = 16.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = category.description,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 11.sp),
                color = Color.Gray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

