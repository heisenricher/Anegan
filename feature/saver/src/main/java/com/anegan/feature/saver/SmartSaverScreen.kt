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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.HelpOutline
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.*
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
    val view = LocalView.current
    val prefs = remember { context.getSharedPreferences("anegan_doc_reader", Context.MODE_PRIVATE) }
    
    val folders = listOf("Receipts", "Scans", "Certificates", "IDs")
    
    var allSaverDocs by remember { mutableStateOf(listOf<SaverDocument>()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("Receipts") } // "Receipts", "Scans", "Certificates", "IDs", "Starred"
    var searchQuery by remember { mutableStateOf("") }
    var docToVault by remember { mutableStateOf<SaverDocument?>(null) }
    var showHowItWorks by remember { mutableStateOf(false) }

    val isDark = isSystemInDarkTheme()

    fun scanSaverDirs() {
        isLoading = true
        scope.launch(Dispatchers.IO) {
            val parentDir = File(Environment.getExternalStorageDirectory(), "Anegan")
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

    NovaBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                NovaTopBar(
                    title = "Smart Offline Saver",
                    onBack = onBack,
                    neonAccent = NeonPurple,
                    showHowItWorks = true,
                    onHowItWorks = { showHowItWorks = true },
                    actions = {
                        IconButton(onClick = {
                            NovaHaptics.click(view)
                            scanSaverDirs()
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Scan directory",
                                tint = NeonPurple
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Futuristic Search Bar
                NovaTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Search documents...",
                    neonColor = NeonPurple,
                    leadingIcon = Icons.Rounded.Search,
                    trailingIcon = if (searchQuery.isNotEmpty()) Icons.Rounded.Close else null,
                    onTrailingClick = {
                        NovaHaptics.click(view)
                        searchQuery = ""
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                )

                // Category Filter Row (Receipts, Scans, Certificates, IDs, Starred)
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
                        
                        NovaChip(
                            text = "$emoji $filter",
                            selected = isSelected,
                            onClick = {
                                selectedFilter = filter
                            },
                            neonColor = NeonPurple
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = NeonPurple)
                    } else if (filteredSaverDocs.isEmpty()) {
                        NovaEmptyState(
                            icon = Icons.Rounded.Folder,
                            title = "No $selectedFilter Found",
                            subtitle = "Move files into Anegan/$selectedFilter/ on your device storage to organize them here.",
                            neonColor = NeonPurple
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(filteredSaverDocs) { index, doc ->
                                val hash = doc.path.hashCode().toString()
                                val isStarred = prefs.getBoolean("pref_pdf_starred_$hash", false)
                                val lastSavedPage = prefs.getInt("pref_pdf_page_$hash", -1)
                                
                                NovaAnimatedItem(index = index) {
                                    GlassCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        neonAccent = NeonPurple.copy(alpha = 0.3f),
                                        onClick = {
                                            NovaHaptics.longPress(view)
                                            // Save path to recently read list and navigate straight into native readers
                                            val currentListString = prefs.getString("pref_pdf_recent_list", "") ?: ""
                                            val list = currentListString.split("|").filter { it.isNotBlank() }.toMutableList()
                                            list.remove(doc.path)
                                            list.add(0, doc.path)
                                            prefs.edit().putString("pref_pdf_recent_list", list.take(5).joinToString("|")).apply()
                                            
                                            // Trigger callback to deep link directly into Document Hub
                                            onPresetSelected("Document Hub", mapOf("initialFilePath" to doc.path))
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(NeonPurple.copy(alpha = 0.12f))
                                                    .border(BorderStroke(1.dp, NeonPurple.copy(alpha = 0.3f)), CircleShape),
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
                                                    color = if (isDark) NovaFrostWhite else NovaDeepInk,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    val sizeMb = doc.size / (1024f * 1024f)
                                                    Text(
                                                        text = String.format(Locale.ROOT, "%.2f MB • %s", sizeMb, doc.extension.uppercase(Locale.ROOT)),
                                                        color = if (isDark) NovaFrostWhite.copy(alpha = 0.6f) else NovaDeepInk.copy(alpha = 0.5f),
                                                        fontFamily = JetBrainsMono,
                                                        fontSize = 10.sp
                                                    )
                                                    if (lastSavedPage >= 0 && doc.extension == "pdf") {
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = "• Page ${lastSavedPage + 1}",
                                                            color = NeonLime,
                                                            fontFamily = JetBrainsMono,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                            IconButton(
                                                onClick = {
                                                    NovaHaptics.longPress(view)
                                                    docToVault = doc
                                                },
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Text(
                                                    text = "🔒",
                                                    fontSize = 18.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            IconButton(
                                                onClick = {
                                                    NovaHaptics.toggle(view)
                                                    prefs.edit().putBoolean("pref_pdf_starred_$hash", !isStarred).apply()
                                                    scanSaverDirs()
                                                },
                                                modifier = Modifier.size(40.dp)
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
    }

    if (docToVault != null) {
        AlertDialog(
            onDismissRequest = { docToVault = null },
            title = {
                Text(
                    text = "Move to Secure Vault?",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) NovaFrostWhite else NovaDeepInk
                )
            },
            text = {
                Text(
                    text = "This will encrypt '${docToVault?.name}' and move it into your Secure Vault. The original unencrypted file will be deleted.",
                    color = if (isDark) NovaFrostWhite.copy(alpha = 0.8f) else NovaDeepInk.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val doc = docToVault
                        docToVault = null
                        if (doc != null) {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val srcFile = File(doc.path)
                                    if (!srcFile.exists()) return@launch
                                    
                                    val fileId = java.util.UUID.randomUUID().toString()
                                    val encName = "file_$fileId.enc"
                                    val secureDir = File(context.filesDir, "vault_storage").apply { if (!exists()) mkdirs() }
                                    val destFile = File(secureDir, encName)
                                    
                                    val masterKeyAlias = androidx.security.crypto.MasterKeys.getOrCreate(androidx.security.crypto.MasterKeys.AES256_GCM_SPEC)
                                    val encryptedFile = androidx.security.crypto.EncryptedFile.Builder(
                                        destFile,
                                        context,
                                        masterKeyAlias,
                                        androidx.security.crypto.EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
                                    ).build()
                                    
                                    encryptedFile.openFileOutput().use { outputStream ->
                                        srcFile.inputStream().use { inputStream ->
                                            inputStream.copyTo(outputStream)
                                        }
                                    }
                                    
                                    val vaultCategory = when(doc.folderName) {
                                        "Certificates" -> "education"
                                        "IDs" -> {
                                            val lower = doc.name.lowercase(Locale.ROOT)
                                            if (lower.contains("pan")) "pan"
                                            else if (lower.contains("passport")) "passport"
                                            else "aadhaar"
                                        }
                                        "Receipts" -> "other"
                                        "Scans" -> "personal"
                                        else -> "other"
                                    }
                                    
                                    val mimeType = when(doc.extension) {
                                        "pdf" -> "application/pdf"
                                        "epub" -> "application/epub+zip"
                                        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                        "jpg", "jpeg" -> "image/jpeg"
                                        "png" -> "image/png"
                                        else -> "application/octet-stream"
                                    }
                                    
                                    val entity = com.anegan.core.database.VaultFileEntity(
                                        id = fileId,
                                        name = doc.name,
                                        category = vaultCategory,
                                        size = doc.size,
                                        mimeType = mimeType,
                                        encryptedFileName = encName,
                                        isPinned = false,
                                        isFavorite = false,
                                        tags = "Saver,${doc.folderName}",
                                        createdAt = System.currentTimeMillis(),
                                        updatedAt = System.currentTimeMillis()
                                    )
                                    
                                    val db = com.anegan.core.database.DatabaseProvider.getDatabase(context)
                                    db.vaultFileDao().upsert(entity)
                                    
                                    srcFile.delete()
                                    
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Moved to Secure Vault", Toast.LENGTH_SHORT).show()
                                        NovaHaptics.success(view)
                                        scanSaverDirs()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Encryption failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                        NovaHaptics.reject(view)
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Text("Move", color = NeonPurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { docToVault = null }) {
                    Text("Cancel", color = if (isDark) NovaFrostWhite.copy(alpha = 0.6f) else NovaDeepInk.copy(alpha = 0.6f))
                }
            },
            containerColor = if (isDark) NovaMidnightBlue else NovaPureWhite,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showHowItWorks) {
        com.anegan.core.designsystem.theme.HowItWorksDialog(
            title = "Smart Offline Saver",
            description = "A structured on-device organizer to scan, categorize, and protect your important offline documents.",
            steps = listOf(
                "Put files into Anegan/Receipts, Anegan/Scans, Anegan/Certificates, or Anegan/IDs directories on your device storage.",
                "Open the Smart Saver app and switch between tabs to see the recognized files.",
                "Tap on any file to open it in its native editor/viewer.",
                "Tap the star icon to bookmark the document.",
                "Tap the lock icon to encrypt and move the file into the Secure Vault."
            ),
            tips = listOf(
                "Moving files to Secure Vault keeps them encrypted using AES-256 and deletes the unencrypted copy from the device's public storage.",
                "You can access the Secure Vault feature directly from the Home Screen."
            ),
            faq = listOf(
                "Where are my files stored?" to "Initially under the Anegan/ folder on your internal storage. Once moved to the vault, they are encrypted in the app's secure sandbox.",
                "Can other apps read my vaulted files?" to "No. Vaulted files are fully encrypted and only accessible via Anegan with password/biometric authorization."
            ),
            onDismiss = { showHowItWorks = false }
        )
    }
}

