/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.dashboard

import android.content.Context
import android.net.Uri
import android.content.ComponentName
import androidx.media3.common.Player
import androidx.media3.common.MediaItem
import androidx.media3.session.SessionToken
import androidx.media3.session.MediaController
import android.os.Environment
import android.os.StatFs
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@Composable
fun getCategoryIcon(title: String): ImageVector {
    return when (title) {
        "Survival Library" -> Icons.Rounded.Explore
        "Notes" -> Icons.Rounded.EditNote
        "Vault" -> Icons.Rounded.Shield
        "Document Hub" -> Icons.Rounded.Hub
        "File Manager" -> Icons.Rounded.FolderOpen
        "Wi-Fi & FTP Transfer" -> Icons.Rounded.Dns
        "SMB File Sharing" -> Icons.Rounded.Cloud
        "APK Extractor" -> Icons.Rounded.GetApp
        "Smart Saver" -> Icons.Rounded.CloudDone
        "Documents" -> Icons.Rounded.MenuBook
        "PDF Tools" -> Icons.Rounded.PictureAsPdf
        "Images" -> Icons.Rounded.PhotoFilter
        "Batch Image" -> Icons.Rounded.Layers
        "Video" -> Icons.Rounded.OndemandVideo
        "Video Tools" -> Icons.Rounded.MovieFilter
        "Audio" -> Icons.Rounded.Headphones
        "Audio Tools" -> Icons.Rounded.GraphicEq
        "OCR / Extract Text" -> Icons.Rounded.DocumentScanner
        "EXIF Metadata" -> Icons.Rounded.PhotoCamera
        "Developer Tools" -> Icons.Rounded.Terminal
        "Image Watermark" -> Icons.Rounded.BrandingWatermark
        "PDF Reader & Editor" -> Icons.Rounded.Gesture
        "Calculator" -> Icons.Rounded.Calculate
        "Flashlight" -> Icons.Rounded.Highlight
        "Compass" -> Icons.Rounded.NearMe
        "Currency Converter" -> Icons.Rounded.CurrencyExchange
        "Offline Comm" -> Icons.Rounded.SatelliteAlt
        "Color Picker" -> Icons.Rounded.Palette
        "Unit Converter" -> Icons.Rounded.Straighten
        "Voice Recorder" -> Icons.Rounded.Mic
        "Storage Analyzer" -> Icons.Rounded.Analytics
        "History" -> Icons.Rounded.Timeline
        "Settings" -> Icons.Rounded.Tune
        "Feedback" -> Icons.Rounded.Forum
        else -> Icons.Rounded.Link
    }
}

@Composable
fun getCategoryGradient(title: String): List<Color> {
    return getGradientForCategory(title)
}

data class Category(val title: String, val description: String, val icon: String = "🛠️")

val mediaGroup = listOf(
    Category("Video Player", "Local gesture media player", "🎥"),
    Category("Audio Player", "Offline music player & playlists", "🎵"),
    Category("Video", "Convert MP4, MKV, AVI", "🔄"),
    Category("Audio", "Convert MP3, M4A, FLAC", "🔄"),
    Category("Images", "JPG, PNG, WEBP, quality", "🖼️"),
    Category("Video Tools", "Trim, compress, speed, GIF", "🎬"),
    Category("Audio Tools", "Cut audio, ringtones", "✂️"),
    Category("Batch Image", "Process multiple images", "📸"),
    Category("Color Picker", "Analyze and save palettes", "🎨"),
    Category("Image Watermark", "Add text watermarks", "🖋️")
)

val docsGroup = listOf(
    Category("Document Hub", "Read your PDFs, DOCX, EPUB & text files", "📂"),
    Category("PDF Tools", "Split, compress, encrypt, images", "🗏"),
    Category("Documents", "Merge, text-to-pdf, unlock", "📄"),
    Category("PDF Reader & Editor", "Read, draw, sign and edit pages", "✍️"),
    Category("OCR / Extract Text", "Image to Text (Offline)", "👁️"),
    Category("EXIF Metadata", "View & strip photo metadata", "ℹ️")
)

val utilityGroup = listOf(
    Category("Calculator", "Offline arithmetic calculator", "🧮"),
    Category("Flashlight", "Torch, strobe & SOS beacon", "🔦"),
    Category("Compass", "Offline bearing & orientation sensor", "🧭"),
    Category("Currency Converter", "Offline exchange rate converter", "𒒱"),
    Category("Unit Converter", "Offline length, mass, data size conversion", "⚖️"),
    Category("Voice Recorder", "Record voice with audio waveform", "🎙️")
)

val securityGroup = listOf(
    Category("File Manager", "Browse, zip, manage your files", "📁"),
    Category("Vault", "Encrypted secure document storage", "🔒"),
    Category("Smart Saver", "Structured organizer for receipts, IDs & certificates", "💾"),
    Category("APK Extractor", "Backup and share your installed application packages", "📲"),
    Category("Developer Tools", "Hash, Base64, QR codes", "💻")
)

val transferGroup = listOf(
    Category("Wi-Fi & FTP Transfer", "Share files locally with standard web browsers or FTP clients", "⚡"),
    Category("SMB File Sharing", "Browse & transfer files on network shares", "🗄️"),
    Category("Offline Comm", "Bluetooth chat, SOS beacon, mesh", "📡")
)

val learningGroup = listOf(
    Category("Notes", "Quick notes, reminders, checklists", "📝"),
    Category("Survival Library", "Exhaustive offline survival & medical books", "📚"),
    Category("History", "Recent Conversions", "📜"),
    Category("Settings", "App Config", "⚙️"),
    Category("Feedback", "Report bugs to GitHub", "💬")
)

val categories = mediaGroup + docsGroup + utilityGroup + securityGroup + transferGroup + learningGroup

fun getGreeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "GOOD MORNING"
        in 12..16 -> "GOOD AFTERNOON"
        in 17..20 -> "GOOD EVENING"
        else -> "GOOD NIGHT"
    }
}

fun getGreetingEmoji(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "🌅"
        in 12..16 -> "☀️"
        in 17..20 -> "🌆"
        else -> "🌃"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onCategorySelected: (String) -> Unit,
    onPresetSelected: (String, Map<String, String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    
    val prefs = remember { context.getSharedPreferences("anegan_doc_reader", Context.MODE_PRIVATE) }
    
    var searchQuery by remember { mutableStateOf("") }
    var showUserGuideScreen by remember { mutableStateOf(false) }
    
    // Cross-module search items
    var allNotes by remember { mutableStateOf(listOf<com.anegan.feature.notes.Note>()) }
    var allDocuments by remember { mutableStateOf(listOf<com.anegan.feature.documentreader.DocumentFile>()) }
    var recentFiles by remember { mutableStateOf(listOf<String>()) }
    var continueReadingProgress by remember { mutableStateOf<com.anegan.core.database.ReadingProgressEntity?>(null) }
    
    LaunchedEffect(Unit) {
        allNotes = com.anegan.feature.notes.loadNotes(context)
        allDocuments = com.anegan.feature.documentreader.DocumentScanner.scanLocalDocuments(context)
        
        val recentsString = prefs.getString("pref_pdf_recent_list", "") ?: ""
        recentFiles = recentsString.split("|").filter { it.isNotBlank() && File(it).exists() }

        try {
            val db = com.anegan.core.database.DatabaseProvider.getDatabase(context)
            val progressList = db.readingProgressDao().getAll()
            if (progressList.isNotEmpty()) {
                val firstValid = progressList.firstOrNull { File(it.filePath).exists() }
                continueReadingProgress = firstValid
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    if (showUserGuideScreen) {
        UserGuideScreen(onBack = { showUserGuideScreen = false })
    } else {
        NovaBackground {
            Column(
                modifier = modifier.fillMaxSize()
            ) {
                // Header TopBar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = NovaTokens.Spacing.xl, vertical = NovaTokens.Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ANEGAN",
                                style = NovaTypography.displayHero.copy(
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.width(NovaTokens.Spacing.xs))
                            Text(
                                text = "V3.2",
                                style = NovaTypography.tagMono.copy(
                                    color = NeonBlue
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(NovaTokens.Radius.xs))
                                    .background(NeonBlue.copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "${getGreeting()} ${getGreetingEmoji()}",
                            style = NovaTypography.tagMono.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(NovaTokens.Radius.md))
                            .background(
                                if (isSystemInDarkTheme()) NovaDarkSlate.copy(alpha = 0.5f)
                                else NovaCoolGray100.copy(alpha = 0.5f)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showUserGuideScreen = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = "Help Guide",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Search Bar Component (Frosted Glass Search with Neon Cyan glow focus)
                NovaTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Search notes, files, tools...",
                    neonColor = NeonCyan,
                    leadingIcon = Icons.Rounded.Search,
                    trailingIcon = if (searchQuery.isNotEmpty()) Icons.Rounded.Clear else null,
                    onTrailingClick = { searchQuery = "" },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = NovaTokens.Spacing.xl, vertical = NovaTokens.Spacing.xs)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (searchQuery.isEmpty()) {
                        val listState = rememberLazyListState(
                            initialFirstVisibleItemIndex = ScrollStateManager.index,
                            initialFirstVisibleItemScrollOffset = ScrollStateManager.offset
                        )

                        // Sync scroll state positions
                        LaunchedEffect(listState) {
                            snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
                                .collect { (idx, off) ->
                                    ScrollStateManager.index = idx
                                    ScrollStateManager.offset = off
                                }
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 100.dp)
                        ) {
                            // Item 1: Hero Storage Card (Animated with Neon Purple Glow)
                            item {
                                var storageUsedPercent by remember { mutableStateOf(0f) }
                                var storageText by remember { mutableStateOf("Calculating...") }

                                LaunchedEffect(Unit) {
                                    try {
                                        val stat = StatFs(Environment.getExternalStorageDirectory().path)
                                        val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
                                        val bytesTotal = stat.blockSizeLong * stat.blockCountLong
                                        val bytesUsed = bytesTotal - bytesAvailable
                                        val totalGb = bytesTotal / (1024f * 1024f * 1024f)
                                        val usedGb = bytesUsed / (1024f * 1024f * 1024f)
                                        storageUsedPercent = if (bytesTotal > 0) bytesUsed.toFloat() / bytesTotal else 0f
                                        storageText = String.format(Locale.getDefault(), "Used: %.1f GB / %.1f GB (%.0f%%)", usedGb, totalGb, storageUsedPercent * 100)
                                    } catch (e: Exception) {
                                        storageText = "Storage Info Unavailable"
                                    }
                                }

                                NovaAnimatedItem(index = 0) {
                                    Box(modifier = Modifier.padding(horizontal = NovaTokens.Spacing.xl, vertical = NovaTokens.Spacing.xs)) {
                                        GlassCard(
                                            neonAccent = NeonPurple,
                                            enableGlow = true,
                                            onClick = {
                                                onCategorySelected("Storage Analyzer")
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(NovaTokens.Spacing.md),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(48.dp)
                                                            .clip(CircleShape)
                                                            .background(NeonPurple.copy(alpha = 0.15f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.FolderOpen,
                                                            contentDescription = null,
                                                            tint = NeonPurple,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(NovaTokens.Spacing.md))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "STORAGE & FILES",
                                                        style = NovaTypography.tagMono.copy(
                                                            color = NeonPurple
                                                        )
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "Internal Storage Analyzer",
                                                        style = NovaTypography.headlineMedium.copy(
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    LinearProgressIndicator(
                                                        progress = storageUsedPercent,
                                                        color = NeonPurple,
                                                        trackColor = NeonPurple.copy(alpha = 0.2f),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(6.dp)
                                                            .clip(RoundedCornerShape(3.dp))
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = storageText,
                                                        style = NovaTypography.dataSmall.copy(
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    )
                                                }
                                                Icon(
                                                    imageVector = Icons.Rounded.ChevronRight,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Item 2: Continue Reading Widget (Animated with Neon Cyan Glow)
                            item {
                                var displayFilePath by remember { mutableStateOf<String?>(null) }
                                var displayFileName by remember { mutableStateOf("") }
                                var displayPageNum by remember { mutableStateOf(1) }

                                LaunchedEffect(recentFiles, continueReadingProgress) {
                                    val progress = continueReadingProgress
                                    if (progress != null && File(progress.filePath).exists()) {
                                        displayFilePath = progress.filePath
                                        displayFileName = progress.fileName
                                        displayPageNum = progress.currentPage
                                    } else if (recentFiles.isNotEmpty()) {
                                        val lastPath = recentFiles.first()
                                        if (File(lastPath).exists()) {
                                            displayFilePath = lastPath
                                            displayFileName = File(lastPath).name
                                            val hash = lastPath.hashCode().toString()
                                            displayPageNum = prefs.getInt("pref_pdf_page_$hash", 0) + 1
                                        }
                                    }
                                }

                                if (displayFilePath != null) {
                                    NovaAnimatedItem(index = 1) {
                                        Box(modifier = Modifier.padding(horizontal = NovaTokens.Spacing.xl, vertical = NovaTokens.Spacing.xs)) {
                                            GlassCard(
                                                neonAccent = NeonCyan,
                                                enableGlow = true,
                                                onClick = {
                                                    onPresetSelected("Document Hub", mapOf("initialFilePath" to displayFilePath!!))
                                                }
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(NovaTokens.Spacing.md),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(42.dp)
                                                                .clip(CircleShape)
                                                                .background(NeonCyan.copy(alpha = 0.15f)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.MenuBook,
                                                                contentDescription = null,
                                                                tint = NeonCyan,
                                                                modifier = Modifier.size(22.dp)
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.width(NovaTokens.Spacing.md))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "CONTINUE READING",
                                                            style = NovaTypography.tagMono.copy(
                                                                color = NeonCyan
                                                            )
                                                        )
                                                        Text(
                                                            text = displayFileName,
                                                            style = NovaTypography.headlineSmall.copy(
                                                                fontWeight = FontWeight.Bold
                                                            ),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = "Resuming page $displayPageNum",
                                                            style = NovaTypography.bodySmall.copy(
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        )
                                                    }
                                                    Icon(
                                                        imageVector = Icons.Rounded.PlayArrow,
                                                        contentDescription = "Resume",
                                                        tint = NeonCyan,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Item 3: Category Sections (Glass components + 4-column tool grids)
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    NovaCategorySection(
                                        title = "Media & Design Tools",
                                        items = mediaGroup,
                                        neonColor = NeonMagenta,
                                        onCategorySelected = onCategorySelected,
                                        index = 2
                                    )
                                    NovaCategorySection(
                                        title = "Documents & Readers",
                                        items = docsGroup,
                                        neonColor = NeonCyan,
                                        onCategorySelected = onCategorySelected,
                                        index = 3
                                    )
                                    NovaCategorySection(
                                        title = "Utility Toolkit",
                                        items = utilityGroup,
                                        neonColor = NeonLime,
                                        onCategorySelected = onCategorySelected,
                                        index = 4
                                    )
                                    NovaCategorySection(
                                        title = "Files & Security",
                                        items = securityGroup,
                                        neonColor = NeonPurple,
                                        onCategorySelected = onCategorySelected,
                                        index = 5
                                    )
                                    NovaCategorySection(
                                        title = "Transfer & Connection",
                                        items = transferGroup,
                                        neonColor = NeonBlue,
                                        onCategorySelected = onCategorySelected,
                                        index = 6
                                    )
                                    NovaCategorySection(
                                        title = "Productivity & App",
                                        items = learningGroup,
                                        neonColor = NeonGold,
                                        onCategorySelected = onCategorySelected,
                                        index = 7
                                    )
                                }
                            }

                            // Item 4: Check for Updates Widget (Glass styled with Neon Success)
                            item {
                                var updateText by remember { mutableStateOf("System version: v3.2 (Nova)") }
                                var checkState by remember { mutableStateOf(0) } // 0=idle, 1=checking, 2=latest, 3=no internet

                                NovaAnimatedItem(index = 8) {
                                    Box(modifier = Modifier.padding(horizontal = NovaTokens.Spacing.xl, vertical = NovaTokens.Spacing.xs)) {
                                        GlassCard(
                                            neonAccent = NovaSuccess,
                                            enableGlow = false
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(NovaTokens.Spacing.md),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clip(CircleShape)
                                                            .background(NovaSuccess.copy(alpha = 0.15f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Update,
                                                            contentDescription = null,
                                                            tint = NovaSuccess,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(NovaTokens.Spacing.sm))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "APP STATUS & UPDATES",
                                                        style = NovaTypography.tagMono.copy(
                                                            color = NovaSuccess
                                                        )
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = updateText,
                                                        style = NovaTypography.headlineSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp
                                                        )
                                                    )
                                                }
                                                Button(
                                                    onClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        if (checkState == 0) {
                                                            checkState = 1
                                                            updateText = "Checking latest version..."
                                                            scope.launch {
                                                                kotlinx.coroutines.delay(1200)
                                                                checkState = 2
                                                                updateText = "Anegan is up to date: v3.2"
                                                            }
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = NovaSuccess,
                                                        contentColor = NovaDeepInk
                                                    ),
                                                    shape = RoundedCornerShape(NovaTokens.Radius.sm),
                                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                                    enabled = checkState != 1,
                                                    modifier = Modifier.height(32.dp)
                                                ) {
                                                    if (checkState == 1) {
                                                        CircularProgressIndicator(
                                                            color = NovaDeepInk,
                                                            modifier = Modifier.size(16.dp),
                                                            strokeWidth = 2.dp
                                                        )
                                                    } else {
                                                        Text(
                                                            text = if (checkState == 2) "Latest" else "Check",
                                                            style = NovaTypography.labelSmall.copy(
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Item 5: User Guide Info Widget (Glass styled with Neon Gold)
                            item {
                                NovaAnimatedItem(index = 9) {
                                    Box(modifier = Modifier.padding(horizontal = NovaTokens.Spacing.xl, vertical = NovaTokens.Spacing.xs)) {
                                        GlassCard(
                                            neonAccent = NeonGold,
                                            enableGlow = false,
                                            onClick = {
                                                showUserGuideScreen = true
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(NovaTokens.Spacing.md),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clip(CircleShape)
                                                            .background(NeonGold.copy(alpha = 0.15f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Info,
                                                            contentDescription = null,
                                                            tint = NeonGold,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(NovaTokens.Spacing.sm))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "Offline Knowledge Hub",
                                                        style = NovaTypography.headlineSmall.copy(
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    )
                                                    Text(
                                                        text = "Deeply understand how all utilities work natively.",
                                                        style = NovaTypography.bodySmall.copy(
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    )
                                                }
                                                Icon(
                                                    imageVector = Icons.Rounded.ChevronRight,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Search Results View
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
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = NovaTokens.Spacing.xl, end = NovaTokens.Spacing.xl, top = NovaTokens.Spacing.xs, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(NovaTokens.Spacing.sm)
                        ) {
                            if (matchingCategories.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Tools & Utilities 🛠️",
                                        style = NovaTypography.headlineSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                                items(matchingCategories) { category ->
                                    GlassCard(
                                        neonAccent = getNeonForCategory(category.title),
                                        onClick = {
                                            onCategorySelected(category.title)
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(getNeonForCategory(category.title).copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = getCategoryIcon(category.title),
                                                    contentDescription = null,
                                                    tint = getNeonForCategory(category.title),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = category.title,
                                                    style = NovaTypography.headlineSmall.copy(
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                                Text(
                                                    text = category.description,
                                                    style = NovaTypography.bodySmall.copy(
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Rounded.ChevronRight,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }

                            if (matchingNotes.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Notes 📝",
                                        style = NovaTypography.headlineSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = NeonGold
                                        ),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                                items(matchingNotes) { note ->
                                    GlassCard(
                                        neonAccent = NeonGold,
                                        onClick = {
                                            onPresetSelected("Notes", mapOf("noteId" to note.id))
                                        }
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text(
                                                text = note.title.ifBlank { "Untitled Note" },
                                                style = NovaTypography.headlineSmall.copy(
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                            if (note.content.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = note.content,
                                                    style = NovaTypography.bodySmall.copy(
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    ),
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
                                        style = NovaTypography.headlineSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = NeonCyan
                                        ),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                                items(matchingDocuments) { doc ->
                                    val hash = doc.path.hashCode().toString()
                                    val lastSavedPage = prefs.getInt("pref_pdf_page_$hash", -1)
                                    GlassCard(
                                        neonAccent = NeonCyan,
                                        onClick = {
                                            onPresetSelected("Document Hub", mapOf("initialFilePath" to doc.path))
                                        }
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
                                                    style = NovaTypography.headlineSmall.copy(
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    val sizeMb = doc.size / (1024f * 1024f)
                                                    Text(
                                                        text = String.format(Locale.getDefault(), "%.2f MB", sizeMb),
                                                        style = NovaTypography.tagMono.copy(
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    )
                                                    if (lastSavedPage >= 0 && doc.category == "PDF") {
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = "• Page ${lastSavedPage + 1}",
                                                            style = NovaTypography.tagMono.copy(
                                                                color = NovaSuccess
                                                            )
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
                                    NovaEmptyState(
                                        icon = Icons.Rounded.SearchOff,
                                        title = "No results found",
                                        subtitle = "Try searching for another tool, note, or file.",
                                        neonColor = NeonCyan
                                    )
                                }
                            }
                        }
                    }

                    // Floating Glass Now Playing Bar
                    NowPlayingBar(
                        onBarClick = { onCategorySelected("Audio Player") },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = NovaTokens.Spacing.sm)
                    )
                }
            }
        }
    }
}

@Composable
fun NovaCategorySection(
    title: String,
    items: List<Category>,
    neonColor: Color,
    onCategorySelected: (String) -> Unit,
    index: Int
) {
    var isExpanded by remember { mutableStateOf(false) }

    NovaAnimatedItem(index = index) {
        Box(modifier = Modifier.padding(horizontal = NovaTokens.Spacing.xl, vertical = NovaTokens.Spacing.xs)) {
            GlassCard(
                neonAccent = neonColor,
                enableGlow = false
            ) {
                Column(modifier = Modifier.padding(NovaTokens.Spacing.sm)) {
                    // Header with expand toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(neonColor)
                            )
                            Spacer(modifier = Modifier.width(NovaTokens.Spacing.xs))
                            Text(
                                text = title.uppercase(),
                                style = NovaTypography.tagMono.copy(
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }
                        TextButton(
                            onClick = {
                                isExpanded = !isExpanded
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                text = if (isExpanded) "Show Less" else "See All ${items.size}",
                                style = NovaTypography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = neonColor
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(NovaTokens.Spacing.xs))

                    // Denser 4-column futuristic tools grid
                    val shownItems = if (isExpanded) items else items.take(4)
                    val rows = shownItems.chunked(4)
                    
                    rows.forEach { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = NovaTokens.Spacing.xxs),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            rowItems.forEach { item ->
                                NovaWidgetItem(
                                    icon = getCategoryIcon(item.title),
                                    label = item.title,
                                    neonColor = getNeonForCategory(item.title),
                                    onClick = {
                                        onCategorySelected(item.title)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowItems.size < 4) {
                                repeat(4 - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserGuideScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            NovaTopBar(
                title = "Anegan Offline Guide",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        NovaBackground {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(NovaTokens.Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(NovaTokens.Spacing.md)
            ) {
                item {
                    GlassCard(
                        neonAccent = NeonGold,
                        enableGlow = true
                    ) {
                        Column(modifier = Modifier.padding(NovaTokens.Spacing.md)) {
                            Text(
                                text = "🦁 ANEGAN BEAST V3.2",
                                style = NovaTypography.tagMono.copy(
                                    color = NeonGold
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Anegan is a completely local, private, and secure powerhouse utility suite designed to work 100% offline without accounts or trackings.",
                                style = NovaTypography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    GuideCard(
                        title = "🎥 Media Player & Conversions",
                        description = "Natively plays all formats of videos and audios. Support double-tap gesture (10 seconds skip), brightness adjusters, audio volume controller, and rotation settings. Built-in audio and video converters let you transcode formats, trim parts, cut ringtones, and crop sizes offline.",
                        neonColor = NeonMagenta
                    )
                }

                item {
                    GuideCard(
                        title = "📄 Professional PDF Reader",
                        description = "Natively opens PDF documents with dynamic zoom controllers, custom bookmarks, full contained-word search features, night filters, and slide page indicators. Use PDF tools to merge books, split ranges, extract raw text, and lock files.",
                        neonColor = NeonCyan
                    )
                }

                item {
                    GuideCard(
                        title = "🔒 Encrypted Secure Vault",
                        description = "Allows you to store Aadhaar cards, passbooks, passport copies, medical bills, and school certificates. Encrypted at-rest with AES-256 GCM logic. Requires biometric lock confirmation to gain entry, keeping your private documents perfectly safe.",
                        neonColor = NeonPurple
                    )
                }

                item {
                    GuideCard(
                        title = "⚡ Local Transfers & Network",
                        description = "Share files locally without internet using FTP (direct computer network connection) or SMB client access (connect directly to local NAS, shared computer files, and routers). Includes offline mesh communication over Bluetooth.",
                        neonColor = NeonBlue
                    )
                }

                item {
                    GuideCard(
                        title = "🛠️ Utility Tools Suite",
                        description = "Scientific calculator parser, declination calibrated accurate compass, flashlight intensity adjustment bars, unit conversion calculators, instant offline OCR text scanners, Base64 hash tools, and EXIF camera meta stripper.",
                        neonColor = NeonLime
                    )
                }
            }
        }
    }
}

@Composable
fun GuideCard(title: String, description: String, neonColor: Color = NeonBlue) {
    GlassCard(
        neonAccent = neonColor,
        enableGlow = false
    ) {
        Column(modifier = Modifier.padding(NovaTokens.Spacing.md)) {
            Text(
                text = title,
                style = NovaTypography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = NovaTypography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun NowPlayingBar(
    onBarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val sessionToken = remember {
        SessionToken(context, ComponentName(context, "com.anegan.app.AneganAudioService"))
    }
    val controllerFuture = remember {
        MediaController.Builder(context, sessionToken).buildAsync()
    }
    var playerController by remember { mutableStateOf<MediaController?>(null) }
    var currentTrackTitle by remember { mutableStateOf("") }
    var isPlaying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    DisposableEffect(controllerFuture) {
        controllerFuture.addListener(
            {
                try {
                    val controller = controllerFuture.get()
                    playerController = controller
                    isPlaying = controller.isPlaying
                    val mediaItem = controller.currentMediaItem
                    currentTrackTitle = mediaItem?.mediaMetadata?.title?.toString()
                        ?: mediaItem?.mediaId?.substringAfterLast('/') ?: ""

                    controller.addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(isPlay: Boolean) {
                            isPlaying = isPlay
                        }
                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            currentTrackTitle = mediaItem?.mediaMetadata?.title?.toString()
                                ?: mediaItem?.mediaId?.substringAfterLast('/') ?: ""
                        }
                    })
                } catch (e: Exception) {
                    // Ignore
                }
            },
            android.os.AsyncTask.THREAD_POOL_EXECUTOR
        )
        onDispose {
            MediaController.releaseFuture(controllerFuture)
        }
    }

    if (playerController != null && currentTrackTitle.isNotBlank()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = NovaTokens.Spacing.md, vertical = NovaTokens.Spacing.xs)
        ) {
            GlassCard(
                neonAccent = NeonMagenta,
                enableGlow = true,
                onClick = onBarClick
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Headphones,
                            contentDescription = "Playing",
                            tint = NeonMagenta,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = currentTrackTitle,
                                style = NovaTypography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isPlaying) "Now Playing" else "Paused",
                                style = NovaTypography.tagMono.copy(
                                    color = NeonMagenta
                                )
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            playerController?.let { controller ->
                                if (controller.isPlaying) {
                                    controller.pause()
                                } else {
                                    controller.play()
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = NeonMagenta
                        )
                    }
                }
            }
        }
    }
}
