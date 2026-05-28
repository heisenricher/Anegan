/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.notes

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.widget.DatePicker
import android.widget.TimePicker
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.MidnightIndigo
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.text.style.TextOverflow

// ─────────────────────────────────────────────────────────────────────────────
// Checklist item model
// ─────────────────────────────────────────────────────────────────────────────

data class ChecklistItem(
    val text      : String  = "",
    val isChecked : Boolean = false
)

private fun String.toChecklistItems(): List<ChecklistItem> =
    if (isBlank()) listOf(ChecklistItem())
    else split("\n").map { line ->
        if (line.startsWith("[x] ", ignoreCase = true))
            ChecklistItem(line.removePrefix("[x] ").removePrefix("[X] "), true)
        else
            ChecklistItem(line.removePrefix("[ ] "), false)
    }

private fun List<ChecklistItem>.toContent(): String =
    joinToString("\n") { item ->
        if (item.isChecked) "[x] ${item.text}" else "[ ] ${item.text}"
    }

// ─────────────────────────────────────────────────────────────────────────────
// Color picker options (label → display color)
// ─────────────────────────────────────────────────────────────────────────────

private val EDITOR_COLORS = listOf(
    "None"   to Color(0xFFE0E0E0),
    "Red"    to Color(0xFFEF9A9A),
    "Orange" to Color(0xFFFFCC80),
    "Yellow" to Color(0xFFFFF176),
    "Green"  to Color(0xFFA5D6A7),
    "Blue"   to Color(0xFF90CAF9),
    "Purple" to Color(0xFFCE93D8)
)

// ─────────────────────────────────────────────────────────────────────────────
// Date/Time picker helper
// ─────────────────────────────────────────────────────────────────────────────

private fun showDateTimePicker(
    context    : Context,
    currentMs  : Long,
    onSelected : (Long) -> Unit
) {
    val cal = Calendar.getInstance().apply {
        if (currentMs > 0L) timeInMillis = currentMs
    }
    DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, day: Int ->
            cal.set(year, month, day)
            TimePickerDialog(
                context,
                { _: TimePicker, hour: Int, minute: Int ->
                    cal.set(Calendar.HOUR_OF_DAY, hour)
                    cal.set(Calendar.MINUTE,      minute)
                    cal.set(Calendar.SECOND,      0)
                    onSelected(cal.timeInMillis)
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                false
            ).show()
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).show()
}

// ─────────────────────────────────────────────────────────────────────────────
// NoteEditorScreen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteId : String?,
    onBack : () -> Unit
) {
    val context      = LocalContext.current
    val scope        = rememberCoroutineScope()
    val scrollState  = rememberScrollState()
    val haptic       = androidx.compose.ui.platform.LocalHapticFeedback.current

    // ── Load or create note ──────────────────────────────────────────────────
    var allNotesList = remember { loadNotes(context) }
    var existingNote = remember(noteId) {
        if (noteId != null) allNotesList.firstOrNull { it.id == noteId } else null
    }

    // ── Editable states ───────────────────────────────────────────────────────
    var title        by rememberSaveable { mutableStateOf(existingNote?.title   ?: "") }
    var content      by rememberSaveable { mutableStateOf(existingNote?.content ?: "") }
    var isPinned     by rememberSaveable { mutableStateOf(existingNote?.isPinned     ?: false) }
    var isArchived   by rememberSaveable { mutableStateOf(existingNote?.isArchived   ?: false) }
    var isChecklist  by rememberSaveable { mutableStateOf(existingNote?.isChecklist  ?: false) }
    var colorLabel   by rememberSaveable { mutableStateOf(existingNote?.colorLabel   ?: "None") }
    var hasReminder  by rememberSaveable { mutableStateOf(existingNote?.hasReminder  ?: false) }
    var reminderTime by rememberSaveable { mutableStateOf(existingNote?.reminderTime ?: 0L) }
    var notebook     by rememberSaveable { mutableStateOf(existingNote?.notebook     ?: "Inbox") }

    // Visual switches
    var isPreviewMode by rememberSaveable { mutableStateOf(false) }

    // Dynamic state modifiers for loaded note in place (backlink spawning)
    var noteId_final by remember { mutableStateOf(existingNote?.id ?: java.util.UUID.randomUUID().toString()) }
    var createdAt    by remember { mutableStateOf(existingNote?.createdAt ?: System.currentTimeMillis()) }

    // Checklist items – derived from content when checklist mode is on
    val checklistItems = remember(isChecklist, content) {
        mutableStateListOf<ChecklistItem>().apply {
            if (isChecklist) addAll(content.toChecklistItems())
        }
    }

    // ── Text formatting flags ───────────────────────────────────────────────
    var isBoldActive   by remember { mutableStateOf(false) }
    var isItalicActive by remember { mutableStateOf(false) }

    // ── In-Note Search States ───────────────────────────────────────────────
    var searchInNoteQuery by rememberSaveable { mutableStateOf("") }
    var isSearchInNoteActive by rememberSaveable { mutableStateOf(false) }

    // ── Backlinks Calculation ───────────────────────────────────────────────
    val backlinks = remember(allNotesList, title) {
        if (title.isBlank()) emptyList<Note>()
        else allNotesList.filter { otherNote ->
            otherNote.id != noteId_final && 
            otherNote.content.contains("[[$title]]", ignoreCase = true)
        }
    }

    // ── Table of Contents Outline Calculation ──────────────────────────────
    val tocHeadings = remember(content) {
        content.split("\n").filter { line ->
            val trimmed = line.trim()
            trimmed.startsWith("# ") || trimmed.startsWith("## ") || trimmed.startsWith("### ")
        }.map { line ->
            val trimmed = line.trim()
            val level = if (trimmed.startsWith("### ")) 3 else if (trimmed.startsWith("## ")) 2 else 1
            val text = trimmed.removePrefix("### ").removePrefix("## ").removePrefix("# ")
            Pair(level, text)
        }
    }

    // ── Sheet / dialog visibility ────────────────────────────────────────────
    var showColorSheet      by remember { mutableStateOf(false) }
    var showReminderSheet   by remember { mutableStateOf(false) }
    var showVersionHistorySheet by remember { mutableStateOf(false) }
    var showTOCSheet by remember { mutableStateOf(false) }
    var showBacklinksSheet by remember { mutableStateOf(false) }
    var showFolderMenu      by remember { mutableStateOf(false) }
    var showOverflowMenu    by remember { mutableStateOf(false) }
    var showDeleteDialog    by remember { mutableStateOf(false) }

    val colorSheetState    = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val reminderSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // ── Auto Tag Parser & Save helper ────────────────────────────────────────
    fun doSave() {
        val finalContent = if (isChecklist) checklistItems.toContent() else content
        if (title.isBlank() && finalContent.isBlank()) return  // nothing to save
        
        // Hashtag regex indexer parses all tags (e.g. #work, #personal)
        val regex = Regex("\\B#(\\w+)")
        val parsedTags = regex.findAll(finalContent + " " + title)
            .map { it.groupValues[1].lowercase() }
            .distinct()
            .toList()

        val note = Note(
            id          = noteId_final,
            title       = title,
            content     = finalContent,
            isPinned    = isPinned,
            isArchived  = isArchived,
            isChecklist = isChecklist,
            colorLabel  = colorLabel,
            createdAt   = createdAt,
            updatedAt   = System.currentTimeMillis(),
            hasReminder = hasReminder,
            reminderTime= reminderTime,
            notebook    = notebook,
            tags        = parsedTags
        )
        upsertNote(context, note)

        // Incremental version snapshotting helper
        if (finalContent.isNotBlank() || title.isNotBlank()) {
            val vPrefs = context.getSharedPreferences("anegan_notes_versions", Context.MODE_PRIVATE)
            val vKey = "versions_${noteId_final}"
            val existingVersionsStr = vPrefs.getString(vKey, "[]") ?: "[]"
            try {
                val vArr = org.json.JSONArray(existingVersionsStr)
                val latestContent = if (vArr.length() > 0) vArr.getJSONObject(vArr.length() - 1).optString("content", "") else ""
                if (finalContent != latestContent) {
                    val snap = org.json.JSONObject().apply {
                        put("timestamp", System.currentTimeMillis())
                        put("content", finalContent)
                        put("title", title)
                    }
                    vArr.put(snap)
                    val cappedArr = if (vArr.length() > 30) {
                        val newArr = org.json.JSONArray()
                        for (idx in (vArr.length() - 30) until vArr.length()) {
                            newArr.put(vArr.getJSONObject(idx))
                        }
                        newArr
                    } else vArr
                    vPrefs.edit().putString(vKey, cappedArr.toString()).apply()
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    // ── Background color for the editor surface ──────────────────────────────
    val bgColor = (NOTE_LABEL_COLORS[colorLabel] ?: Color.Transparent)
        .let { if (colorLabel == "None") MaterialTheme.colorScheme.background else it.copy(alpha = 0.12f) }

    // ── Delete confirmation ──────────────────────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title  = { Text("Delete Note") },
            text   = { Text("This note will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    deleteNote(context, noteId_final)
                    showDeleteDialog = false
                    onBack()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Color picker bottom sheet ────────────────────────────────────────────
    if (showColorSheet) {
        ModalBottomSheet(
            onDismissRequest  = { showColorSheet = false },
            sheetState        = colorSheetState,
            dragHandle        = { BottomSheetDefaults.DragHandle() },
            windowInsets      = WindowInsets.navigationBars
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    "Note Color",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    EDITOR_COLORS.forEach { (label, color) ->
                        ColorCircle(
                            color     = color,
                            label     = label,
                            isSelected = colorLabel == label,
                            onClick   = {
                                colorLabel     = label
                                showColorSheet = false
                            }
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // ── Reminder bottom sheet ────────────────────────────────────────────────
    if (showReminderSheet) {
        ModalBottomSheet(
            onDismissRequest  = { showReminderSheet = false },
            sheetState        = reminderSheetState,
            dragHandle        = { BottomSheetDefaults.DragHandle() },
            windowInsets      = WindowInsets.navigationBars
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    "Set Reminder",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))

                if (hasReminder && reminderTime > 0L) {
                    val formatted = SimpleDateFormat("EEE, MMM d yyyy • h:mm a", Locale.getDefault())
                        .format(Date(reminderTime))
                    Text(
                        "Current: $formatted",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MidnightIndigo
                    )
                    Spacer(Modifier.height(12.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(
                        onClick = {
                            showDateTimePicker(context, reminderTime) { selectedMs ->
                                reminderTime      = selectedMs
                                hasReminder       = true
                                showReminderSheet = false
                            }
                        }
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MidnightIndigo)
                        Spacer(Modifier.width(8.dp))
                        Text("Pick date & time", color = MidnightIndigo)
                    }

                    if (hasReminder) {
                        TextButton(onClick = {
                            hasReminder       = false
                            reminderTime      = 0L
                            showReminderSheet = false
                        }) {
                            Icon(Icons.Default.Close, contentDescription = null,
                                tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(4.dp))
                            Text("Remove", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showVersionHistorySheet) {
        val vPrefs = remember { context.getSharedPreferences("anegan_notes_versions", Context.MODE_PRIVATE) }
        val vKey = "versions_${noteId_final}"
        val existingVersionsStr = remember(showVersionHistorySheet) { vPrefs.getString(vKey, "[]") ?: "[]" }
        val versionsList = remember(existingVersionsStr) {
            val list = mutableListOf<org.json.JSONObject>()
            try {
                val arr = org.json.JSONArray(existingVersionsStr)
                for (idx in 0 until arr.length()) {
                    list.add(arr.getJSONObject(idx))
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            list.reversed()
        }

        ModalBottomSheet(
            onDismissRequest = { showVersionHistorySheet = false },
            windowInsets = WindowInsets.navigationBars
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    "Version History Snapshots 📜",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MidnightIndigo,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                if (versionsList.isEmpty()) {
                    Text(
                        "No incremental backup snapshots stored yet. Make edits and save notes to log snapshots.",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        versionsList.forEach { snap ->
                            val timestamp = snap.optLong("timestamp", 0L)
                            val snapContent = snap.optString("content", "")
                            val snapTitle = snap.optString("title", "")
                            val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
                            
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                                border = BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        content = snapContent
                                        title = snapTitle
                                        showVersionHistorySheet = false
                                        Toast.makeText(context, "Restored text snapshot from $dateStr!", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(dateStr, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MidnightIndigo)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        snapContent.take(120) + if (snapContent.length > 120) "..." else "",
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showTOCSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTOCSheet = false },
            windowInsets = WindowInsets.navigationBars
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    "Table of Contents Outline 📑",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MidnightIndigo,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                if (tocHeadings.isEmpty()) {
                    Text(
                        "No headings found in note. Use # H1, ## H2, ### H3 to populate outlines.",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        tocHeadings.forEach { (level, headingText) ->
                            val indent = (level - 1) * 16
                            Text(
                                text = headingText,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        showTOCSheet = false
                                        Toast.makeText(context, "Outline jumped to: $headingText!", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(start = indent.dp, top = 8.dp, bottom = 8.dp),
                                style = if (level == 1) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                        else MaterialTheme.typography.bodyMedium,
                                color = MidnightIndigo
                            )
                            Divider(color = Color.LightGray.copy(alpha = 0.2f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showBacklinksSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBacklinksSheet = false },
            windowInsets = WindowInsets.navigationBars
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    "Backlinks to this note 🔗",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MidnightIndigo,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                if (backlinks.isEmpty()) {
                    Text(
                        "No other notes link to this note yet. Use [[${title.ifBlank { "Current Title" }}]] in other notes to reference this one.",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        backlinks.forEach { noteLink ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                                border = BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        doSave()
                                        noteId_final = noteLink.id
                                        val newLoaded = allNotesList.firstOrNull { it.id == noteLink.id }
                                        title = newLoaded?.title ?: ""
                                        content = newLoaded?.content ?: ""
                                        isPinned = newLoaded?.isPinned ?: false
                                        isArchived = newLoaded?.isArchived ?: false
                                        isChecklist = newLoaded?.isChecklist ?: false
                                        colorLabel = newLoaded?.colorLabel ?: "None"
                                        hasReminder = newLoaded?.hasReminder ?: false
                                        reminderTime = newLoaded?.reminderTime ?: 0L
                                        notebook = newLoaded?.notebook ?: "Inbox"
                                        createdAt = newLoaded?.createdAt ?: System.currentTimeMillis()
                                        
                                        showBacklinksSheet = false
                                        Toast.makeText(context, "Opened linking note: ${noteLink.title}!", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("📄", fontSize = 16.sp, modifier = Modifier.padding(end = 10.dp))
                                    Text(
                                        text = noteLink.title.ifBlank { "Untitled" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MidnightIndigo
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // ── Main scaffold ────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Segment control for Edit vs Preview
                    TabRow(
                        selectedTabIndex = if (isPreviewMode) 1 else 0,
                        divider = {},
                        indicator = { tabPositions ->
                            TabRowDefaults.Indicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[if (isPreviewMode) 1 else 0]),
                                color = MidnightIndigo
                            )
                        },
                        modifier = Modifier
                            .width(180.dp)
                            .height(40.dp)
                            .background(Color.Transparent)
                    ) {
                        Tab(
                            selected = !isPreviewMode,
                            onClick = { isPreviewMode = false },
                            text = { Text("Edit", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MidnightIndigo) }
                        )
                        Tab(
                            selected = isPreviewMode,
                            onClick = {
                                doSave()
                                isPreviewMode = true
                            },
                            text = { Text("Preview", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MidnightIndigo) }
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        doSave()
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MidnightIndigo)
                    }
                },
                actions = {
                    // Search in note toggle
                    IconButton(onClick = { isSearchInNoteActive = !isSearchInNoteActive }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search in Note",
                            tint = MidnightIndigo
                        )
                    }

                    // Folder/Notebook picker
                    Box {
                        TextButton(onClick = { showFolderMenu = true }) {
                            Text(
                                text = "📁 ${notebook.uppercase()}",
                                color = MidnightIndigo,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        DropdownMenu(
                            expanded = showFolderMenu,
                            onDismissRequest = { showFolderMenu = false }
                        ) {
                            listOf("Inbox", "Projects", "Areas", "Resources").forEach { folderName ->
                                DropdownMenuItem(
                                    text = { Text(folderName) },
                                    onClick = {
                                        notebook = folderName
                                        showFolderMenu = false
                                        doSave()
                                    }
                                )
                            }
                        }
                    }
                    // Pin toggle
                    IconToggleButton(
                        checked  = isPinned,
                        onCheckedChange = { isPinned = it }
                    ) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = if (isPinned) "Unpin" else "Pin",
                            tint = if (isPinned) MidnightIndigo else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                    // Overflow menu
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = MidnightIndigo)
                        }
                        DropdownMenu(
                            expanded        = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Version History") },
                                leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                                onClick = {
                                    showVersionHistorySheet = true
                                    showOverflowMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Table of Contents") },
                                leadingIcon = { Icon(Icons.Default.List, contentDescription = null) },
                                onClick = {
                                    showTOCSheet = true
                                    showOverflowMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Backlinks") },
                                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                                onClick = {
                                    showBacklinksSheet = true
                                    showOverflowMenu = false
                                }
                            )
                            Divider()
                            DropdownMenuItem(
                                text = { Text(if (isArchived) "Unarchive" else "Archive") },
                                leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
                                onClick = {
                                    isArchived = !isArchived
                                    showOverflowMenu = false
                                    doSave()
                                    if (isArchived) onBack()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error)
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    showDeleteDialog  = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = bgColor
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            // Floating search query input row if active
            if (isSearchInNoteActive) {
                // Calculate match counts dynamically
                val matchesCount = remember(content, searchInNoteQuery) {
                    if (searchInNoteQuery.isBlank()) 0
                    else {
                        val occurrences = mutableListOf<Int>()
                        var pos = content.indexOf(searchInNoteQuery, 0, ignoreCase = true)
                        while (pos != -1) {
                            occurrences.add(pos)
                            pos = content.indexOf(searchInNoteQuery, pos + searchInNoteQuery.length, ignoreCase = true)
                        }
                        occurrences.size
                    }
                }
                var activeMatchIndex by remember { mutableStateOf(0) }
                LaunchedEffect(searchInNoteQuery) {
                    activeMatchIndex = 0
                }
                
                Surface(
                    tonalElevation = 2.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(0.5.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search in note",
                            tint = MidnightIndigo,
                            modifier = Modifier.size(18.dp)
                        )
                        BasicTextField(
                            value = searchInNoteQuery,
                            onValueChange = { searchInNoteQuery = it },
                            textStyle = TextStyle(
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MidnightIndigo),
                            modifier = Modifier.weight(1f),
                            decorationBox = { inner ->
                                Box {
                                    if (searchInNoteQuery.isEmpty()) {
                                        Text(
                                            "Search inside this note...",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        )
                                    }
                                    inner()
                                }
                            }
                        )
                        if (searchInNoteQuery.isNotEmpty()) {
                            Text(
                                text = if (matchesCount > 0) "${activeMatchIndex + 1} of $matchesCount" else "0 of 0",
                                fontSize = 12.sp,
                                color = MidnightIndigo,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = {
                                    if (matchesCount > 0) {
                                        activeMatchIndex = (activeMatchIndex - 1 + matchesCount) % matchesCount
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.ArrowUpward,
                                    contentDescription = "Previous match",
                                    tint = MidnightIndigo,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (matchesCount > 0) {
                                        activeMatchIndex = (activeMatchIndex + 1) % matchesCount
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.ArrowDownward,
                                    contentDescription = "Next match",
                                    tint = MidnightIndigo,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { searchInNoteQuery = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = { isSearchInNoteActive = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close search",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            // ── Scrollable editor body ────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Title field (editable in Edit Mode, read-only header in Preview)
                if (!isPreviewMode) {
                    BasicTextField(
                        value         = title,
                        onValueChange = { title = it },
                        textStyle     = TextStyle(
                            fontSize   = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush   = SolidColor(MidnightIndigo),
                        modifier      = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            Box {
                                if (title.isEmpty()) {
                                    Text(
                                        "Note Title",
                                        fontSize   = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                }
                                inner()
                            }
                        }
                    )
                } else {
                    Text(
                        text = title.ifBlank { "Untitled Note" },
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = MidnightIndigo,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Updated timestamp hint
                val updatedText = remember(existingNote) {
                    existingNote?.let {
                        "Edited " + SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(it.updatedAt))
                    } ?: "New Second Brain Note"
                }
                Text(
                    updatedText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                )

                Divider(
                    modifier  = Modifier.padding(vertical = 10.dp),
                    color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )

                // ── Content area: Edit vs Preview Markdown Renderer ──────────────────────
                if (isPreviewMode) {
                    // Modern Preview Mode
                    MarkdownPreview(
                        content = if (isChecklist) checklistItems.toContent() else content,
                        searchQuery = searchInNoteQuery,
                        onWikiLinkClicked = { targetTitle ->
                            // Obsidian wiki-link behavior: lookup or spawn instantly!
                            val matched = allNotesList.firstOrNull { it.title.trim().equals(targetTitle.trim(), ignoreCase = true) }
                            if (matched != null) {
                                // Save current in-progress edits
                                doSave()
                                // Navigate in-place instantly!
                                title = matched.title
                                content = matched.content
                                isPinned = matched.isPinned
                                isArchived = matched.isArchived
                                isChecklist = matched.isChecklist
                                colorLabel = matched.colorLabel
                                hasReminder = matched.hasReminder
                                reminderTime = matched.reminderTime
                                notebook = matched.notebook
                                noteId_final = matched.id
                                createdAt = matched.createdAt
                                isPreviewMode = false
                                Toast.makeText(context, "Loaded: ${matched.title}", Toast.LENGTH_SHORT).show()
                            } else {
                                // Spawns a new note with that title and loads it instantly!
                                doSave()
                                val newNote = Note(
                                    title = targetTitle,
                                    content = "Spawning note from backlink [[${targetTitle}]]...\n\n",
                                    notebook = notebook
                                )
                                upsertNote(context, newNote)
                                allNotesList = loadNotes(context)
                                
                                title = newNote.title
                                content = newNote.content
                                isPinned = newNote.isPinned
                                isArchived = newNote.isArchived
                                isChecklist = newNote.isChecklist
                                colorLabel = newNote.colorLabel
                                hasReminder = newNote.hasReminder
                                reminderTime = newNote.reminderTime
                                notebook = newNote.notebook
                                noteId_final = newNote.id
                                createdAt = newNote.createdAt
                                isPreviewMode = false
                                Toast.makeText(context, "Spawned & opened: $targetTitle", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onChecklistToggled = { lineIdx, isChecked ->
                            // Dynamically update checklist line and trigger save
                            val lines = content.split("\n").toMutableList()
                            if (lineIdx in lines.indices) {
                                val line = lines[lineIdx]
                                val cleaned = line.removePrefix("[x] ").removePrefix("[X] ").removePrefix("[ ] ")
                                lines[lineIdx] = if (isChecked) "[x] $cleaned" else "[ ] $cleaned"
                                content = lines.joinToString("\n")
                                doSave()
                            }
                        }
                    )
                } else {
                    // Standard Edit Mode
                    if (isChecklist) {
                        ChecklistEditor(
                            items     = checklistItems,
                            onUpdate  = { idx, item -> checklistItems[idx] = item },
                            onAdd     = { checklistItems.add(ChecklistItem()) },
                            onRemove  = { idx -> if (checklistItems.size > 1) checklistItems.removeAt(idx) }
                        )
                    } else {
                        BasicTextField(
                            value         = content,
                            onValueChange = { content = it },
                            textStyle     = TextStyle(
                                fontSize   = 16.sp,
                                fontWeight = if (isBoldActive)   FontWeight.Bold   else FontWeight.Normal,
                                fontStyle  = if (isItalicActive) FontStyle.Italic  else FontStyle.Normal,
                                color      = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush   = SolidColor(MidnightIndigo),
                            modifier      = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            decorationBox = { inner ->
                                Box {
                                    if (content.isEmpty()) {
                                        Text(
                                            "Start writing your thoughts using Markdown tags (#work, [[Note Link]], **bold**)...",
                                            fontSize = 16.sp,
                                            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                        )
                                    }
                                    inner()
                                }
                            }
                        )
                    }
                }

                // Reminder badge
                if (hasReminder && reminderTime > 0L) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MidnightIndigo.copy(alpha = 0.08f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MidnightIndigo,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            val fmt = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault())
                            Text(
                                fmt.format(Date(reminderTime)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MidnightIndigo
                            )
                        }
                    }
                }

                // Extra bottom padding inside scroll
                Spacer(Modifier.height(80.dp))
            }

            // ── Keyboard accessory accessory toolbar (Markdown shortcuts bar) ──
            if (!isPreviewMode) {
                Surface(
                    tonalElevation = 2.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // Quick insertion helper bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val helpers = listOf(
                                "Heading" to "# ",
                                "Bold" to "**bold**",
                                "Italic" to "*italic*",
                                "Link" to "[[Note]]",
                                "Checklist" to "- [ ] ",
                                "Tag" to "#tag"
                            )
                            helpers.forEach { (label, value) ->
                                TextButton(
                                    onClick = { content += value },
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text(label, fontSize = 11.sp, color = MidnightIndigo, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        
                        Divider(color = Color.LightGray.copy(alpha = 0.3f))

                        // Original layout action toolbar
                        Row(
                            modifier              = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            // Bold formatting switch
                            ToolbarIconButton(
                                onClick  = { isBoldActive = !isBoldActive },
                                tint     = if (isBoldActive) MidnightIndigo else MaterialTheme.colorScheme.onSurface.copy(0.55f)
                            ) {
                                Icon(Icons.Default.FormatBold, contentDescription = "Bold")
                            }
                            // Italic formatting switch
                            ToolbarIconButton(
                                onClick  = { isItalicActive = !isItalicActive },
                                tint     = if (isItalicActive) MidnightIndigo else MaterialTheme.colorScheme.onSurface.copy(0.55f)
                            ) {
                                Icon(Icons.Default.FormatItalic, contentDescription = "Italic")
                            }
                            // Checklist toggle
                            ToolbarIconButton(
                                onClick = {
                                    if (!isChecklist) {
                                        isChecklist = true
                                        val parsed = content.toChecklistItems()
                                        checklistItems.clear()
                                        checklistItems.addAll(parsed)
                                    } else {
                                        content = checklistItems.toContent()
                                        isChecklist = false
                                    }
                                },
                                tint = if (isChecklist) MidnightIndigo else MaterialTheme.colorScheme.onSurface.copy(0.55f)
                            ) {
                                Icon(Icons.Default.Checklist, contentDescription = "Checklist")
                            }
                            // Color picker
                            ToolbarIconButton(
                                onClick = { showColorSheet = true },
                                tint    = (NOTE_LABEL_COLORS[colorLabel] ?: MaterialTheme.colorScheme.onSurface.copy(0.55f))
                                    .let { if (colorLabel == "None") MaterialTheme.colorScheme.onSurface.copy(0.55f) else it }
                            ) {
                                Icon(Icons.Default.Palette, contentDescription = "Color")
                            }
                            // Reminder
                            ToolbarIconButton(
                                onClick = { showReminderSheet = true },
                                tint    = if (hasReminder) MidnightIndigo else MaterialTheme.colorScheme.onSurface.copy(0.55f)
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = "Reminder")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Markdown AST Node Models
// ─────────────────────────────────────────────────────────────────────────────

sealed interface RenderElement
data class HeadingElement(val level: Int, val text: String) : RenderElement
data class ChecklistElement(val originalLineIndex: Int, val text: String, val isChecked: Boolean) : RenderElement
data class ParagraphElement(val text: String) : RenderElement
data class CodeBlockElement(val content: String) : RenderElement
data class TableElement(val headers: List<String>, val rows: List<List<String>>) : RenderElement
data class CollapsibleElement(val title: String, val content: String) : RenderElement

fun parseMarkdownContent(content: String): List<RenderElement> {
    val elements = mutableListOf<RenderElement>()
    val lines = content.split("\n")
    var i = 0
    val totalLines = lines.size
    
    while (i < totalLines) {
        val line = lines[i]
        val trimmed = line.trim()
        
        when {
            // Code block parsing
            trimmed.startsWith("```") -> {
                val sb = java.lang.StringBuilder()
                val lang = trimmed.removePrefix("```").trim()
                i++
                while (i < totalLines && !lines[i].trim().startsWith("```")) {
                    sb.append(lines[i]).append("\n")
                    i++
                }
                elements.add(CodeBlockElement(sb.toString().trimEnd()))
                i++
            }
            
            // Collapsible Details HTML parsing
            trimmed.startsWith("<details>", ignoreCase = true) || trimmed.startsWith("<details ", ignoreCase = true) -> {
                var summary = "Summary Outline"
                val sb = java.lang.StringBuilder()
                i++
                while (i < totalLines && !lines[i].trim().equals("</details>", ignoreCase = true)) {
                    val currentLine = lines[i].trim()
                    if (currentLine.startsWith("<summary>", ignoreCase = true)) {
                        summary = currentLine
                            .replace("<summary>", "", ignoreCase = true)
                            .replace("</summary>", "", ignoreCase = true)
                    } else {
                        sb.append(lines[i]).append("\n")
                    }
                    i++
                }
                elements.add(CollapsibleElement(summary, sb.toString().trimEnd()))
                i++
            }
            
            // Table parsing (Markdown pipes)
            trimmed.startsWith("|") -> {
                val headerCols = trimmed.split("|").map { it.trim() }.filterIndexed { index, _ -> index > 0 && index < trimmed.split("|").size - 1 }
                i++
                if (i < totalLines && lines[i].trim().startsWith("|") && lines[i].contains("-")) {
                    i++
                }
                val rows = mutableListOf<List<String>>()
                while (i < totalLines && lines[i].trim().startsWith("|")) {
                    val rowLine = lines[i].trim()
                    val cols = rowLine.split("|").map { it.trim() }.filterIndexed { index, _ -> index > 0 && index < rowLine.split("|").size - 1 }
                    rows.add(cols)
                    i++
                }
                elements.add(TableElement(headerCols, rows))
            }
            
            // Headings
            trimmed.startsWith("# ") -> {
                elements.add(HeadingElement(1, trimmed.removePrefix("# ")))
                i++
            }
            trimmed.startsWith("## ") -> {
                elements.add(HeadingElement(2, trimmed.removePrefix("## ")))
                i++
            }
            trimmed.startsWith("### ") -> {
                elements.add(HeadingElement(3, trimmed.removePrefix("### ")))
                i++
            }
            
            // Checklist
            trimmed.startsWith("[ ] ") || trimmed.startsWith("[x] ") || trimmed.startsWith("[X] ") ||
            trimmed.startsWith("- [ ] ") || trimmed.startsWith("- [x] ") -> {
                val isChecked = trimmed.contains("[x]", ignoreCase = true)
                val cleaned = trimmed.removePrefix("- ").removePrefix("[x] ").removePrefix("[X] ").removePrefix("[ ] ")
                elements.add(ChecklistElement(i, cleaned, isChecked))
                i++
            }
            
            // Default Paragraph
            else -> {
                elements.add(ParagraphElement(line))
                i++
            }
        }
    }
    return elements
}

@Composable
fun MarkdownPreview(
    content: String,
    searchQuery: String = "",
    onWikiLinkClicked: (String) -> Unit,
    onChecklistToggled: (Int, Boolean) -> Unit
) {
    val elements = remember(content) { parseMarkdownContent(content) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        elements.forEach { elem ->
            when (elem) {
                is HeadingElement -> {
                    val annotated = renderMarkdownSpans(elem.text, searchQuery, onWikiLinkClicked)
                    val style = when (elem.level) {
                        1 -> MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, color = MidnightIndigo)
                        2 -> MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = MidnightIndigo)
                        else -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MidnightIndigo)
                    }
                    ClickableText(
                        text = annotated,
                        style = style,
                        onClick = { offset ->
                            annotated.getStringAnnotations(tag = "WIKI_LINK", start = offset, end = offset)
                                .firstOrNull()?.let { ann -> onWikiLinkClicked(ann.item) }
                        },
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
                is ChecklistElement -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = elem.isChecked,
                            onCheckedChange = { onChecklistToggled(elem.originalLineIndex, it) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MidnightIndigo,
                                checkmarkColor = Color.White
                            )
                        )
                        val annotated = renderMarkdownSpans(elem.text, searchQuery, onWikiLinkClicked)
                        ClickableText(
                            text = annotated,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = if (elem.isChecked) Color.Gray else MaterialTheme.colorScheme.onSurface,
                                textDecoration = if (elem.isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                            ),
                            onClick = { offset ->
                                annotated.getStringAnnotations(tag = "WIKI_LINK", start = offset, end = offset)
                                    .firstOrNull()?.let { ann -> onWikiLinkClicked(ann.item) }
                            }
                        )
                    }
                }
                is CodeBlockElement -> {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Gray.copy(alpha = 0.08f)),
                        border = BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = elem.content,
                            fontSize = 13.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = MidnightIndigo,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                is TableElement -> {
                    RenderTable(headers = elem.headers, rows = elem.rows, searchQuery = searchQuery, onWikiLinkClicked = onWikiLinkClicked)
                }
                is CollapsibleElement -> {
                    RenderCollapsible(
                        title = elem.title,
                        content = elem.content,
                        searchQuery = searchQuery,
                        onWikiLinkClicked = onWikiLinkClicked,
                        onChecklistToggled = onChecklistToggled
                    )
                }
                is ParagraphElement -> {
                    if (elem.text.isNotBlank()) {
                        val annotated = renderMarkdownSpans(elem.text, searchQuery, onWikiLinkClicked)
                        ClickableText(
                            text = annotated,
                            style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            onClick = { offset ->
                                annotated.getStringAnnotations(tag = "WIKI_LINK", start = offset, end = offset)
                                    .firstOrNull()?.let { ann -> onWikiLinkClicked(ann.item) }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun RenderTable(headers: List<String>, rows: List<List<String>>, searchQuery: String = "", onWikiLinkClicked: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
    ) {
        // Headers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MidnightIndigo.copy(alpha = 0.08f))
                .padding(vertical = 10.dp, horizontal = 12.dp)
        ) {
            headers.forEach { header ->
                Text(
                    text = header,
                    fontWeight = FontWeight.Bold,
                    color = MidnightIndigo,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Divider(color = Color.LightGray.copy(alpha = 0.3f))
        // Rows
        rows.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp, horizontal = 12.dp)
            ) {
                row.forEach { cell ->
                    val annotated = renderMarkdownSpans(cell, searchQuery, onWikiLinkClicked)
                    ClickableText(
                        text = annotated,
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        onClick = { offset ->
                            annotated.getStringAnnotations(tag = "WIKI_LINK", start = offset, end = offset)
                                .firstOrNull()?.let { ann -> onWikiLinkClicked(ann.item) }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (rowIndex < rows.size - 1) {
                Divider(color = Color.LightGray.copy(alpha = 0.2f))
            }
        }
    }
}

@Composable
fun RenderCollapsible(
    title: String,
    content: String,
    searchQuery: String = "",
    onWikiLinkClicked: (String) -> Unit,
    onChecklistToggled: (Int, Boolean) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
        border = BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
            ) {
                Text(
                    text = if (isExpanded) "▼" else "▶",
                    fontSize = 12.sp,
                    color = MidnightIndigo,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = MidnightIndigo,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
            }
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 10.dp, start = 20.dp)) {
                    MarkdownPreview(
                        content = content,
                        searchQuery = searchQuery,
                        onWikiLinkClicked = onWikiLinkClicked,
                        onChecklistToggled = onChecklistToggled
                    )
                }
            }
        }
    }
}

// ── Custom Span Builder for Nested Formatting (Bold, Italic, Wiki-Link, HashTags) ──
@Composable
fun renderMarkdownSpans(
    text: String,
    searchQuery: String = "",
    onWikiLinkClicked: (String) -> Unit
): androidx.compose.ui.text.AnnotatedString {
    val annotated = buildAnnotatedString {
        var currentIdx = 0
        val len = text.length
        
        while (currentIdx < len) {
            val wikiLinkMatch = Regex("\\[\\[(.*?)\\]\\]").find(text, currentIdx)
            val boldMatch = Regex("\\*\\*(.*?)\\*\\*").find(text, currentIdx)
            val italicMatch = Regex("\\*(.*?)\\*").find(text, currentIdx)
            val tagMatch = Regex("\\B#(\\w+)").find(text, currentIdx)
            
            val matches = listOfNotNull(wikiLinkMatch, boldMatch, italicMatch, tagMatch)
                .sortedBy { it.range.first }
            
            if (matches.isEmpty()) {
                append(text.substring(currentIdx))
                break
            }
            
            val firstMatch = matches.first()
            val matchStart = firstMatch.range.first
            val matchEnd = firstMatch.range.last + 1
            
            if (matchStart > currentIdx) {
                append(text.substring(currentIdx, matchStart))
            }
            
            when (firstMatch) {
                wikiLinkMatch -> {
                    val targetTitle = firstMatch.groupValues[1]
                    pushStringAnnotation(tag = "WIKI_LINK", annotation = targetTitle)
                    withStyle(
                        style = SpanStyle(
                            color = MidnightIndigo,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(targetTitle)
                    }
                    pop()
                }
                boldMatch -> {
                    val content = firstMatch.groupValues[1]
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(content)
                    }
                }
                italicMatch -> {
                    val content = firstMatch.groupValues[1]
                    withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(content)
                    }
                }
                tagMatch -> {
                    val tagName = firstMatch.groupValues[1]
                    withStyle(
                        style = SpanStyle(
                            color = MidnightIndigo.copy(alpha = 0.8f),
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
                        append("#$tagName")
                    }
                }
            }
            currentIdx = matchEnd
        }
    }
    
    if (searchQuery.isBlank()) return annotated
    
    return buildAnnotatedString {
        append(annotated)
        var searchStart = 0
        val textStr = annotated.text
        while (true) {
            val idx = textStr.indexOf(searchQuery, searchStart, ignoreCase = true)
            if (idx == -1) break
            addStyle(
                style = SpanStyle(
                    background = Color(0xFFFFF59D), // glowing high-contrast yellow background
                    color = Color.Black
                ),
                start = idx,
                end = idx + searchQuery.length
            )
            searchStart = idx + searchQuery.length
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Checklist editor
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChecklistEditor(
    items    : List<ChecklistItem>,
    onUpdate : (Int, ChecklistItem) -> Unit,
    onAdd    : () -> Unit,
    onRemove : (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEachIndexed { idx, item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked         = item.isChecked,
                    onCheckedChange = { onUpdate(idx, item.copy(isChecked = it)) },
                    colors          = CheckboxDefaults.colors(
                        checkedColor = MidnightIndigo,
                        checkmarkColor = Color.White
                    )
                )
                BasicTextField(
                    value         = item.text,
                    onValueChange = { onUpdate(idx, item.copy(text = it)) },
                    textStyle     = TextStyle(
                        fontSize  = 16.sp,
                        color     = if (item.isChecked)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        else
                            MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (item.isChecked)
                            androidx.compose.ui.text.style.TextDecoration.LineThrough
                        else null
                    ),
                    cursorBrush   = SolidColor(MidnightIndigo),
                    modifier      = Modifier.weight(1f),
                    decorationBox = { inner ->
                        Box {
                            if (item.text.isEmpty()) {
                                Text(
                                    "List item",
                                    fontSize = 16.sp,
                                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                            }
                            inner()
                        }
                    }
                )
                IconButton(onClick = { onRemove(idx) }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove item",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }
        TextButton(onClick = onAdd) {
            Icon(Icons.Default.Add, contentDescription = null,
                modifier = Modifier.size(14.dp), tint = MidnightIndigo)
            Spacer(Modifier.width(4.dp))
            Text("Add item", color = MidnightIndigo, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Color circle picker
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ColorCircle(
    color      : Color,
    label      : String,
    isSelected : Boolean,
    onClick    : () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width  = if (isSelected) 3.dp else 1.dp,
                color  = if (isSelected) MidnightIndigo else MaterialTheme.colorScheme.outline.copy(0.3f),
                shape  = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Text("✓", fontSize = 16.sp, color = MidnightIndigo, fontWeight = FontWeight.Bold)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Toolbar icon button wrapper
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ToolbarIconButton(
    onClick  : () -> Unit,
    tint     : Color,
    content  : @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides tint
        ) {
            content()
        }
    }
}
