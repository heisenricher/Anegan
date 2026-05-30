/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.notes

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// ─────────────────────────────────────────────────────────────────────────────
// Data model
// ─────────────────────────────────────────────────────────────────────────────

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val content: String = "",
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isChecklist: Boolean = false,
    val colorLabel: String = "None",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val hasReminder: Boolean = false,
    val reminderTime: Long = 0L,
    val notebook: String = "Inbox", // PARA folders: Inbox, Projects, Areas, Resources, Archive
    val tags: List<String> = emptyList() // Extracted via hashtag parsing
)

// ─────────────────────────────────────────────────────────────────────────────
// Color label mapping
// ─────────────────────────────────────────────────────────────────────────────

val NOTE_LABEL_COLORS: Map<String, Color> = mapOf(
    "Red"    to Color(0xFFEF9A9A),
    "Orange" to Color(0xFFFFCC80),
    "Yellow" to Color(0xFFFFF176),
    "Green"  to Color(0xFFA5D6A7),
    "Blue"   to Color(0xFF90CAF9),
    "Purple" to Color(0xFFCE93D8),
    "None"   to Color.Transparent
)

// ─────────────────────────────────────────────────────────────────────────────
// SharedPreferences persistence helpers (Gson-free, using JSONArray)
// ─────────────────────────────────────────────────────────────────────────────

private const val PREFS_NAME = "anegan_notes_prefs"
private const val KEY_NOTES  = "notes_json"

private fun Note.toJson(): JSONObject = JSONObject().apply {
    put("id",          id)
    put("title",       title)
    put("content",     content)
    put("isPinned",    isPinned)
    put("isArchived",  isArchived)
    put("isChecklist", isChecklist)
    put("colorLabel",  colorLabel)
    put("createdAt",   createdAt)
    put("updatedAt",   updatedAt)
    put("hasReminder", hasReminder)
    put("reminderTime",reminderTime)
    put("notebook",    notebook)
    put("tags",        JSONArray(tags))
}

private fun JSONObject.toNote(): Note {
    val tagsArray = optJSONArray("tags")
    val tagsList = if (tagsArray != null) {
        (0 until tagsArray.length()).map { tagsArray.getString(it) }
    } else {
        emptyList()
    }
    return Note(
        id          = optString("id",          UUID.randomUUID().toString()),
        title       = optString("title",       ""),
        content     = optString("content",     ""),
        isPinned    = optBoolean("isPinned",    false),
        isArchived  = optBoolean("isArchived",  false),
        isChecklist = optBoolean("isChecklist", false),
        colorLabel  = optString("colorLabel",  "None"),
        createdAt   = optLong("createdAt",   System.currentTimeMillis()),
        updatedAt   = optLong("updatedAt",   System.currentTimeMillis()),
        hasReminder = optBoolean("hasReminder", false),
        reminderTime= optLong("reminderTime",0L),
        notebook    = optString("notebook",    optString("notebook", "Inbox")),
        tags        = tagsList
    )
}

fun loadNotes(context: Context): List<Note> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val json  = prefs.getString(KEY_NOTES, null) ?: return emptyList()
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { arr.getJSONObject(it).toNote() }
    } catch (e: Exception) {
        emptyList()
    }
}

fun saveNotes(context: Context, notes: List<Note>) {
    val arr = JSONArray()
    notes.forEach { arr.put(it.toJson()) }
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_NOTES, arr.toString())
        .apply()
}

fun upsertNote(context: Context, note: Note): List<Note> {
    val existing = loadNotes(context).toMutableList()
    val idx = existing.indexOfFirst { it.id == note.id }
    if (idx >= 0) existing[idx] = note else existing.add(0, note)
    saveNotes(context, existing)
    return existing
}

fun deleteNote(context: Context, noteId: String): List<Note> {
    val updated = loadNotes(context).filter { it.id != noteId }
    saveNotes(context, updated)
    return updated
}

fun exportNotesBackup(context: Context): String? {
    try {
        val notes = loadNotes(context)
        val arr = JSONArray()
        notes.forEach { arr.put(it.toJson()) }
        
        val baseDir = java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "Anegan")
        val docDir = java.io.File(baseDir, "Documents").apply { if (!exists()) mkdirs() }
        
        val sdf = SimpleDateFormat("yyyyMMdd_HHmm", Locale.ROOT)
        val timestamp = sdf.format(Date())
        val backupFile = java.io.File(docDir, "Anegan_Notes_Backup_$timestamp.json")
        
        java.io.FileWriter(backupFile).use { it.write(arr.toString()) }
        return backupFile.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

fun importNotesBackup(context: Context, backupFile: java.io.File): Result<Int> {
    return try {
        val json = backupFile.readText()
        val arr = JSONArray(json)
        val imported = (0 until arr.length()).map { arr.getJSONObject(it).toNote() }
        
        val existing = loadNotes(context).toMutableList()
        var mergedCount = 0
        
        for (impNote in imported) {
            val idx = existing.indexOfFirst { it.id == impNote.id }
            if (idx >= 0) {
                val existingNote = existing[idx]
                if (impNote.updatedAt > existingNote.updatedAt) {
                    existing[idx] = impNote
                    mergedCount++
                }
            } else {
                existing.add(impNote)
                mergedCount++
            }
        }
        
        saveNotes(context, existing)
        Result.success(mergedCount)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// Helper to parse Wiki-Links [[Note Title]]
fun parseWikiLinks(content: String): List<String> {
    val regex = Regex("\\[\\[(.*?)\\]\\]")
    return regex.findAll(content).map { it.groupValues[1] }.toList()
}

// ─────────────────────────────────────────────────────────────────────────────
// PARA Folders
// ─────────────────────────────────────────────────────────────────────────────

enum class PARAFolder(val key: String, val label: String, val emoji: String) {
    ALL("All", "All Notes", "📥"),
    INBOX("Inbox", "Inbox", "📥"),
    PROJECTS("Projects", "Projects", "🚀"),
    AREAS("Areas", "Areas", "🎯"),
    RESOURCES("Resources", "Resources", "📚"),
    ARCHIVE("Archive", "Archive", "🗄️")
}

// ─────────────────────────────────────────────────────────────────────────────
// NoteListScreen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NoteListScreen(
    onBack: () -> Unit,
    onOpenNote: (String?) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current

    // ── state ────────────────────────────────────────────────────────────────
    var notes        by remember { mutableStateOf(loadNotes(context)) }
    var searchQuery  by remember { mutableStateOf("") }
    var isSearching  by remember { mutableStateOf(false) }
    var activeFolder by remember { mutableStateOf(PARAFolder.ALL) }
    var selectedTag  by remember { mutableStateOf<String?>(null) }
    var showGraph    by remember { mutableStateOf(false) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }

    var backupFilesToSelect by remember { mutableStateOf(listOf<java.io.File>()) }
    var showImportDialog by remember { mutableStateOf(false) }

    // Reload notes whenever the screen becomes active
    LaunchedEffect(Unit) {
        notes = loadNotes(context)
    }

    // Dynamic hashtag indexer gathers all unique tags
    val allTags = remember(notes) {
        notes.flatMap { it.tags }.distinct().sorted()
    }

    // ── derived filtered + searched list ────────────────────────────────────
    val displayedNotes by remember(notes, activeFolder, selectedTag, searchQuery) {
        derivedStateOf {
            val filteredByFolder = when (activeFolder) {
                PARAFolder.ALL -> notes.filter { !it.isArchived }
                PARAFolder.ARCHIVE -> notes.filter { it.isArchived }
                else -> notes.filter { it.notebook == activeFolder.key && !it.isArchived }
            }
            
            val filteredByTag = if (selectedTag == null) filteredByFolder
                                else filteredByFolder.filter { it.tags.contains(selectedTag) }

            if (searchQuery.isBlank()) filteredByTag
            else filteredByTag.filter { note ->
                note.title.contains(searchQuery, ignoreCase = true) ||
                note.content.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // ── delete confirmation dialog ───────────────────────────────────────────
    if (noteToDelete != null) {
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("Delete Note", color = NeonGold, fontWeight = FontWeight.Bold) },
            text  = { Text("Are you sure you want to permanently delete \"${noteToDelete!!.title.ifBlank { "Untitled" }}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    notes = deleteNote(context, noteToDelete!!.id)
                    noteToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Select Backup to Import", color = NeonGold, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    backupFilesToSelect.forEach { file ->
                        Text(
                            text = file.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val res = importNotesBackup(context, file)
                                    if (res.isSuccess) {
                                        notes = loadNotes(context)
                                        Toast.makeText(context, "Merged ${res.getOrNull()} notes successfully!", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Import failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                    showImportDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Cancel") }
            }
        )
    }

    NovaBackground {
        Scaffold(
            topBar = {
                if (isSearching) {
                    TopAppBar(
                        title = {
                            NovaTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = "Search second brain…",
                                neonColor = NeonGold,
                                leadingIcon = Icons.Rounded.Search,
                                trailingIcon = Icons.Rounded.Close,
                                onTrailingClick = {
                                    searchQuery = ""
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 12.dp)
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                isSearching = false
                                searchQuery = ""
                            }) {
                                Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Close search", tint = NeonGold)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                } else {
                    NovaTopBar(
                        title = "Second Brain 🧠",
                        onBack = onBack,
                        neonAccent = NeonGold
                    ) {
                        // Slider view toggle between list and graph
                        TextButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showGraph = !showGraph
                            }
                        ) {
                            Text(
                                text = if (showGraph) "📄 List" else "🌐 Graph",
                                color = NeonGold,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                        IconButton(onClick = { isSearching = true }) {
                            Icon(
                                imageVector        = Icons.Rounded.Search,
                                contentDescription = "Search",
                                tint               = NeonGold
                            )
                        }
                        var showMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector        = Icons.Rounded.MoreVert,
                                contentDescription = "More options",
                                tint               = NeonGold
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export Notes Backup 💾") },
                                onClick = {
                                    showMenu = false
                                    val path = exportNotesBackup(context)
                                    if (path != null) {
                                        Toast.makeText(context, "Backup saved to Downloads/Anegan/Documents!", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Backup failed!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Import Notes Backup 📂") },
                                onClick = {
                                    showMenu = false
                                    val baseDir = java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "Anegan")
                                    val docDir = java.io.File(baseDir, "Documents")
                                    val backups = if (docDir.exists() && docDir.isDirectory) {
                                        docDir.listFiles { _, name -> name.startsWith("Anegan_Notes_Backup") && name.endsWith(".json") }?.toList() ?: emptyList()
                                    } else {
                                        emptyList()
                                    }
                                    
                                    if (backups.isEmpty()) {
                                        Toast.makeText(context, "No backups found in Downloads/Anegan/Documents/ yet. Please export first.", Toast.LENGTH_LONG).show()
                                    } else {
                                        backupFilesToSelect = backups
                                        showImportDialog = true
                                    }
                                }
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick            = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpenNote(null)
                    },
                    containerColor     = NeonGold,
                    contentColor       = NovaDeepInk,
                    shape              = CircleShape,
                    modifier = Modifier.neonGlow(NeonGold, alpha = 0.3f, radiusMultiplier = 0.7f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Note")
                }
            },
            containerColor = Color.Transparent
        ) { innerPadding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (!showGraph) {
                    // ── PARA Folders / Notebook chips row ──────────────────────────────
                    LazyRow(
                        contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(PARAFolder.values()) { folder ->
                            NovaChip(
                                text = "${folder.emoji} ${folder.label}",
                                selected = activeFolder == folder,
                                onClick = {
                                    activeFolder = folder
                                },
                                neonColor = NeonGold
                            )
                        }
                    }

                    // ── Hashtags row ─────────────────────────────────────────────────────
                    if (allTags.isNotEmpty()) {
                        LazyRow(
                            contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(allTags) { tag ->
                                val isSelected = selectedTag == tag
                                NovaChip(
                                    text = "#$tag",
                                    selected = isSelected,
                                    onClick = {
                                        selectedTag = if (isSelected) null else tag
                                    },
                                    neonColor = NeonGold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // ── Content List ──────────────────────────────────────────────────────
                    if (displayedNotes.isEmpty()) {
                        Box(
                            modifier        = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            NovaEmptyState(
                                icon = Icons.Rounded.EditNote,
                                title = "Second Brain Empty",
                                subtitle = if (searchQuery.isNotBlank() || selectedTag != null) "No notes match active search query or filters."
                                           else "No notes yet. Tap the action button to spawn a note.",
                                neonColor = NeonGold
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement   = Arrangement.spacedBy(12.dp),
                            modifier              = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            itemsIndexed(
                                items = displayedNotes,
                                key   = { _, item -> item.id }
                            ) { index, note ->
                                NovaAnimatedItem(index = index) {
                                    NoteCard(
                                        note         = note,
                                        onClick      = { onOpenNote(note.id) },
                                        onLongClick  = { noteToDelete = note }
                                    )
                                }
                            }
                            // Extra bottom padding for FAB
                            item { Spacer(Modifier.height(72.dp)) }
                        }
                    }
                } else {
                    // ── Interactive Force-directed Brain Graph ────────────────────────────────────
                    BrainGraphView(
                        notes = notes.filter { !it.isArchived },
                        onOpenNote = onOpenNote,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NoteCard
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note        : Note,
    onClick     : () -> Unit,
    onLongClick : () -> Unit,
    modifier    : Modifier = Modifier
) {
    val labelColor = NOTE_LABEL_COLORS[note.colorLabel] ?: Color.Transparent
    val hasColor   = note.colorLabel != "None"

    val dateString = remember(note.updatedAt) {
        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        sdf.format(Date(note.updatedAt))
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        neonAccent = if (hasColor) labelColor else Color.Transparent,
        onClick = onClick,
        cornerRadius = NovaTokens.Radius.lg
    ) {
        // combinedClickable workaround inside GlassCard - using Box overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(14.dp)
        ) {
            Column {
                Row(
                    modifier            = Modifier.fillMaxWidth(),
                    verticalAlignment   = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Title
                    Text(
                        text       = note.title.ifBlank { "Untitled" },
                        style      = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color      = MaterialTheme.colorScheme.onSurface,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        modifier   = Modifier.weight(1f)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        // Color dot
                        if (hasColor) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(labelColor)
                                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                            )
                        }
                        // Pin icon
                        if (note.isPinned) {
                            Icon(
                                imageVector        = Icons.Rounded.PushPin,
                                contentDescription = "Pinned",
                                tint               = NeonGold,
                                modifier           = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                if (note.content.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    // Strip Markdown tokens from card preview to look super neat
                    val strippedContent = remember(note.content) {
                        note.content.replace(Regex("\\[\\[(.*?)\\]\\]"), "$1")
                            .replace(Regex("[#*`_]"), "")
                    }
                    Text(
                        text     = strippedContent,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Display hashtag pills inside card
                if (note.tags.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        note.tags.take(3).forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NeonGold.copy(alpha = 0.08f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "#$tag", 
                                    fontSize = 9.sp, 
                                    color = NeonGold, 
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FiraCodeFontFamily
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text  = dateString,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            fontFamily = FiraCodeFontFamily
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(NeonGold.copy(alpha = 0.08f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = note.notebook.uppercase(), 
                                fontSize = 8.sp, 
                                color = NeonGold, 
                                fontWeight = FontWeight.Bold,
                                fontFamily = FiraCodeFontFamily
                            )
                        }
                    }
                    if (note.isChecklist) {
                        Text(
                            text  = "☑ Checklist",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (note.hasReminder) {
                        Text(
                            text  = "🔔",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BrainGraphView Composable
// ─────────────────────────────────────────────────────────────────────────────

class GraphNode(
    val note: Note,
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    val radius: Float = 42f
)

@Composable
fun BrainGraphView(
    notes: List<Note>,
    onOpenNote: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (notes.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No notes found to build graph.", color = Color.Gray)
        }
        return
    }

    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    // Pan and zoom states
    var scale by remember { mutableStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Floating card detail state
    var selectedNode by remember { mutableStateOf<GraphNode?>(null) }

    // Reconstruct nodes when notes change
    val nodes = remember(notes) {
        notes.mapIndexed { index, note ->
            val angle = index * 2.0 * Math.PI / notes.size
            val dist = 320f
            GraphNode(
                note = note,
                x = (540f + dist * Math.cos(angle)).toFloat(),
                y = (800f + dist * Math.sin(angle)).toFloat()
            )
        }
    }

    // Connect nodes based on [[Wiki-Links]] parsed content
    val links = remember(nodes) {
        val list = mutableListOf<Pair<GraphNode, GraphNode>>()
        for (n1 in nodes) {
            val targets = parseWikiLinks(n1.note.content)
            for (title in targets) {
                val n2 = nodes.firstOrNull { it.note.title.trim().equals(title.trim(), ignoreCase = true) }
                if (n2 != null) {
                    list.add(Pair(n1, n2))
                }
            }
        }
        list
    }

    // Dynamic particle simulation step ticker
    var frameCount by remember { mutableStateOf(0) }
    LaunchedEffect(nodes, links) {
        while (isActive) {
            // 1. Repulsion force between all nodes
            for (i in nodes.indices) {
                val n1 = nodes[i]
                for (j in i + 1 until nodes.size) {
                    val n2 = nodes[j]
                    val dx = n2.x - n1.x
                    val dy = n2.y - n1.y
                    val distSq = dx * dx + dy * dy + 0.1f
                    val dist = kotlin.math.sqrt(distSq)
                    if (dist < 260f) {
                        val force = (260f - dist) * 0.08f
                        val fx = (dx / dist) * force
                        val fy = (dy / dist) * force
                        n1.vx -= fx
                        n1.vy -= fy
                        n2.vx += fx
                        n2.vy += fy
                    }
                }
            }
            // 2. Attraction pull along note relationships links
            for (link in links) {
                val n1 = link.first
                val n2 = link.second
                val dx = n2.x - n1.x
                val dy = n2.y - n1.y
                val distSq = dx * dx + dy * dy + 0.1f
                val dist = kotlin.math.sqrt(distSq)
                if (dist > 140f) {
                    val force = (dist - 140f) * 0.06f
                    val fx = (dx / dist) * force
                    val fy = (dy / dist) * force
                    n1.vx += fx
                    n1.vy += fy
                    n2.vx -= fx
                    n2.vy -= fy
                }
            }
            // 3. Gravity center force & Friction dampener
            val cx = 540f
            val cy = 800f
            for (node in nodes) {
                val dx = cx - node.x
                val dy = cy - node.y
                node.vx += dx * 0.005f
                node.vy += dy * 0.005f

                node.x += node.vx
                node.y += node.vy
                node.vx *= 0.80f
                node.vy *= 0.80f
            }
            frameCount++
            delay(16)
        }
    }

    // Touch hit test for nodes
    var activeDraggedNode by remember { mutableStateOf<GraphNode?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(nodes) {
                    detectTapGestures { pressOffset ->
                        val transformedX = (pressOffset.x - panOffset.x) / scale
                        val transformedY = (pressOffset.y - panOffset.y) / scale
                        val tapped = nodes.firstOrNull { node ->
                            val dx = node.x - transformedX
                            val dy = node.y - transformedY
                            (dx * dx + dy * dy) <= (node.radius * 2.5f * node.radius * 2.5f)
                        }
                        if (tapped != null) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedNode = tapped
                        } else {
                            selectedNode = null
                        }
                    }
                }
                .pointerInput(nodes) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            val transformedX = (startOffset.x - panOffset.x) / scale
                            val transformedY = (startOffset.y - panOffset.y) / scale
                            activeDraggedNode = nodes.firstOrNull { node ->
                                val dx = node.x - transformedX
                                val dy = node.y - transformedY
                                (dx * dx + dy * dy) <= (node.radius * 2f * node.radius * 2f)
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val node = activeDraggedNode
                            if (node != null) {
                                node.x += dragAmount.x / scale
                                node.y += dragAmount.y / scale
                                node.vx = 0f
                                node.vy = 0f
                            } else {
                                panOffset += dragAmount
                            }
                        },
                        onDragEnd = {
                            activeDraggedNode = null
                        }
                    )
                }
        ) {
            val frame = frameCount

            withTransform({
                translate(panOffset.x, panOffset.y)
                scale(scale, scale, Offset(0f, 0f))
            }) {
                // ── Draw Backlink connection lines ─────────────────────────
                for (link in links) {
                    val from = link.first
                    val to = link.second
                    drawLine(
                        color = NeonGold.copy(alpha = 0.35f),
                        start = Offset(from.x, from.y),
                        end = Offset(to.x, to.y),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                // ── Draw Nodes ─────────────────────────────────────────────
                for (node in nodes) {
                    val labelColor = NOTE_LABEL_COLORS[node.note.colorLabel] ?: NeonGold
                    val finalNodeColor = if (node.note.colorLabel == "None") NeonGold else labelColor
                    
                    // Draw outer glowing halo if selected
                    if (selectedNode == node) {
                        drawCircle(
                            color = finalNodeColor.copy(alpha = 0.25f),
                            center = Offset(node.x, node.y),
                            radius = node.radius + 12.dp.toPx()
                        )
                    }

                    // Inner main node body circle
                    drawCircle(
                        color = finalNodeColor,
                        center = Offset(node.x, node.y),
                        radius = node.radius
                    )

                    // Draw inner accent symbol
                    drawCircle(
                        color = Color.White.copy(alpha = 0.6f),
                        center = Offset(node.x, node.y),
                        radius = node.radius / 2f
                    )

                    // Text titles next to nodes
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(220, 255, 255, 255)
                        textSize = 11.sp.toPx()
                        typeface = android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD)
                    }
                    val text = node.note.title.ifBlank { "Untitled" }
                    val textWidth = paint.measureText(text)
                    drawContext.canvas.nativeCanvas.drawText(
                        text,
                        node.x - textWidth / 2f,
                        node.y + node.radius + 16.dp.toPx(),
                        paint
                    )
                }
            }
        }

        // ── Selection Detail Glassmorphic shelf Card ──────────────────────────
        AnimatedVisibility(
            visible = selectedNode != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp, start = 20.dp, end = 20.dp)
        ) {
            val node = selectedNode
            if (node != null) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    neonAccent = NeonGold,
                    enableGlow = true,
                    cornerRadius = NovaTokens.Radius.xl
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = node.note.title.ifBlank { "Untitled" },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = NeonGold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { selectedNode = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = NeonGold)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (node.note.content.isBlank()) "Empty Note content" else node.note.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        NovaPrimaryButton(
                            text = "Open in Editor 🧠",
                            neonColor = NeonGold,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onOpenNote(node.note.id)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
