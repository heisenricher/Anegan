package com.anegan.feature.notes

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
// Date/Time picker helper (View interop)
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

    // ── Load or create note ──────────────────────────────────────────────────
    val existingNote = remember(noteId) {
        if (noteId != null) loadNotes(context).firstOrNull { it.id == noteId } else null
    }

    // ── Editable state ───────────────────────────────────────────────────────
    var title        by rememberSaveable { mutableStateOf(existingNote?.title   ?: "") }
    var content      by rememberSaveable { mutableStateOf(existingNote?.content ?: "") }
    var isPinned     by rememberSaveable { mutableStateOf(existingNote?.isPinned     ?: false) }
    var isArchived   by rememberSaveable { mutableStateOf(existingNote?.isArchived   ?: false) }
    var isChecklist  by rememberSaveable { mutableStateOf(existingNote?.isChecklist  ?: false) }
    var colorLabel   by rememberSaveable { mutableStateOf(existingNote?.colorLabel   ?: "None") }
    var hasReminder  by rememberSaveable { mutableStateOf(existingNote?.hasReminder  ?: false) }
    var reminderTime by rememberSaveable { mutableStateOf(existingNote?.reminderTime ?: 0L) }

    // Checklist items – derived from content when checklist mode is on
    val checklistItems = remember(isChecklist, content) {
        mutableStateListOf<ChecklistItem>().apply {
            if (isChecklist) addAll(content.toChecklistItems())
        }
    }

    // ── Text formatting flags (applied to selection, stored per-note as prefix tags) ──
    var isBoldActive   by remember { mutableStateOf(false) }
    var isItalicActive by remember { mutableStateOf(false) }

    // ── Sheet / dialog visibility ────────────────────────────────────────────
    var showColorSheet    by remember { mutableStateOf(false) }
    var showReminderSheet by remember { mutableStateOf(false) }
    var showOverflowMenu  by remember { mutableStateOf(false) }
    var showDeleteDialog  by remember { mutableStateOf(false) }

    val colorSheetState    = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val reminderSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val noteId_final = remember { existingNote?.id ?: java.util.UUID.randomUUID().toString() }
    val createdAt    = remember { existingNote?.createdAt ?: System.currentTimeMillis() }

    // ── Save helper ──────────────────────────────────────────────────────────
    fun doSave() {
        val finalContent = if (isChecklist) checklistItems.toContent() else content
        if (title.isBlank() && finalContent.isBlank()) return  // nothing to save
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
            reminderTime= reminderTime
        )
        upsertNote(context, note)
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

    // ── Main scaffold ────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = {
                        doSave()
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MidnightIndigo)
                    }
                },
                actions = {
                    // Save
                    IconButton(onClick = { doSave() }) {
                        Icon(Icons.Default.Save, contentDescription = "Save", tint = MidnightIndigo)
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
            // ── Scrollable editor body ────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Title field
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
                                    "Title",
                                    fontSize   = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                            }
                            inner()
                        }
                    }
                )

                Spacer(Modifier.height(8.dp))

                // Updated timestamp hint
                val updatedText = remember(existingNote) {
                    existingNote?.let {
                        "Edited " + SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(it.updatedAt))
                    } ?: ""
                }
                if (updatedText.isNotEmpty()) {
                    Text(
                        updatedText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    )
                }

                Divider(
                    modifier  = Modifier.padding(vertical = 10.dp),
                    color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )

                // ── Content area: checklist or plain text ──────────────────────
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
                            .height(300.dp),
                        decorationBox = { inner ->
                            Box {
                                if (content.isEmpty()) {
                                    Text(
                                        "Start typing…",
                                        fontSize = 16.sp,
                                        color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                }
                                inner()
                            }
                        }
                    )
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

            // ── Bottom toolbar ────────────────────────────────────────────────
            Surface(
                tonalElevation = 3.dp,
                modifier       = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    // Bold
                    ToolbarIconButton(
                        onClick  = { isBoldActive = !isBoldActive },
                        tint     = if (isBoldActive) MidnightIndigo else MaterialTheme.colorScheme.onSurface.copy(0.55f)
                    ) {
                        Icon(Icons.Default.FormatBold, contentDescription = "Bold")
                    }
                    // Italic
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
                                // switching to checklist: parse current content
                                isChecklist = true
                                val parsed = content.toChecklistItems()
                                checklistItems.clear()
                                checklistItems.addAll(parsed)
                            } else {
                                // switching back to plain text: serialise items
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
            Icon(Icons.Default.Close.also {}, contentDescription = null,
                modifier = Modifier.size(14.dp), tint = MidnightIndigo)
            Spacer(Modifier.width(4.dp))
            Text("+ Add item", color = MidnightIndigo, style = MaterialTheme.typography.bodyMedium)
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
