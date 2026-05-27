package com.anegan.feature.notes

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.MidnightIndigo
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
    val reminderTime: Long = 0L
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
}

private fun JSONObject.toNote(): Note = Note(
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
    reminderTime= optLong("reminderTime",0L)
)

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

// ─────────────────────────────────────────────────────────────────────────────
// Filter enum
// ─────────────────────────────────────────────────────────────────────────────

enum class NoteFilter(val label: String) {
    ALL("All"),
    PINNED("Pinned"),
    CHECKLISTS("Checklists"),
    ARCHIVED("Archived")
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

    // ── state ────────────────────────────────────────────────────────────────
    var notes       by remember { mutableStateOf(loadNotes(context)) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var activeFilter by remember { mutableStateOf(NoteFilter.ALL) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }

    // Reload notes whenever the screen becomes active (e.g., on back from editor)
    LaunchedEffect(Unit) {
        notes = loadNotes(context)
    }

    // ── derived filtered + searched list ────────────────────────────────────
    val displayedNotes by remember(notes, activeFilter, searchQuery) {
        derivedStateOf {
            val filtered = when (activeFilter) {
                NoteFilter.ALL        -> notes.filter { !it.isArchived }
                NoteFilter.PINNED     -> notes.filter { it.isPinned && !it.isArchived }
                NoteFilter.CHECKLISTS -> notes.filter { it.isChecklist && !it.isArchived }
                NoteFilter.ARCHIVED   -> notes.filter { it.isArchived }
            }
            if (searchQuery.isBlank()) filtered
            else filtered.filter { note ->
                note.title.contains(searchQuery, ignoreCase = true) ||
                note.content.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // ── delete confirmation dialog ───────────────────────────────────────────
    if (noteToDelete != null) {
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("Delete Note") },
            text  = { Text("Are you sure you want to permanently delete \"${noteToDelete!!.title.ifBlank { "Untitled" }}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    notes = deleteNote(context, noteToDelete!!.id)
                    noteToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            if (isSearching) {
                // ── Search bar replaces top bar ──────────────────────────────
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search notes…") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor   = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor   = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSearching = false
                            searchQuery = ""
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Close search")
                        }
                    },
                    actions = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            } else {
                // ── Normal top bar ───────────────────────────────────────────
                TopAppBar(
                    title = {
                        Text(
                            text  = "Notes",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MidnightIndigo
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector        = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint               = MidnightIndigo
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(
                                imageVector        = Icons.Default.Search,
                                contentDescription = "Search",
                                tint               = MidnightIndigo
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick            = { onOpenNote(null) },
                containerColor     = MidnightIndigo,
                contentColor       = Color.White,
                shape              = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Note")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── Filter chips ─────────────────────────────────────────────────
            LazyRow(
                contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(NoteFilter.values()) { filter ->
                    FilterChip(
                        selected = activeFilter == filter,
                        onClick  = { activeFilter = filter },
                        label    = { Text(filter.label) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor    = MidnightIndigo,
                            selectedLabelColor        = Color.White,
                            containerColor            = MaterialTheme.colorScheme.surface,
                            labelColor                = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            // ── Content ──────────────────────────────────────────────────────
            AnimatedVisibility(
                visible = displayedNotes.isEmpty(),
                enter   = fadeIn(),
                exit    = fadeOut(),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier        = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "📝", fontSize = 56.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text  = if (searchQuery.isNotBlank()) "No notes match your search."
                                    else "No notes yet. Tap + to create your first note.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = displayedNotes.isNotEmpty(),
                enter   = fadeIn(),
                exit    = fadeOut(),
                modifier = Modifier.weight(1f)
            ) {
                LazyColumn(
                    contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement   = Arrangement.spacedBy(10.dp),
                    modifier              = Modifier.fillMaxSize()
                ) {
                    items(
                        items = displayedNotes,
                        key   = { it.id }
                    ) { note ->
                        NoteCard(
                            note         = note,
                            onClick      = { onOpenNote(note.id) },
                            onLongClick  = { noteToDelete = note },
                            modifier     = Modifier.animateItemPlacement()
                        )
                    }
                    // Extra bottom padding for FAB
                    item { Spacer(Modifier.height(72.dp)) }
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

    val cardBackground = if (hasColor) labelColor.copy(alpha = 0.18f)
                         else MaterialTheme.colorScheme.surface

    val dateString = remember(note.updatedAt) {
        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        sdf.format(Date(note.updatedAt))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick     = onClick,
                onLongClick = onLongClick
            ),
        shape  = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier            = Modifier.fillMaxWidth(),
                verticalAlignment   = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Title
                Text(
                    text       = note.title.ifBlank { "Untitled" },
                    style      = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
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
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                        )
                    }
                    // Pin icon
                    if (note.isPinned) {
                        Icon(
                            imageVector        = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint               = MidnightIndigo,
                            modifier           = Modifier.size(14.dp)
                        )
                    }
                }
            }

            if (note.content.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text     = note.content,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text  = dateString,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                if (note.isChecklist) {
                    Text(
                        text  = "☑ Checklist",
                        style = MaterialTheme.typography.labelSmall,
                        color = MidnightIndigo.copy(alpha = 0.7f)
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
