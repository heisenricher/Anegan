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
import android.util.Xml
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import com.anegan.core.designsystem.theme.*
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile

data class Chapter(val title: String, val content: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficeBookReaderScreen(
    filePath: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val prefs = remember { context.getSharedPreferences("anegan_doc_reader", Context.MODE_PRIVATE) }
    
    val file = remember { File(filePath) }
    val isEpub = remember { file.extension.equals("epub", ignoreCase = true) }
    val pathHash = filePath.hashCode().toString()

    var docTitle by remember { mutableStateOf(file.name) }
    var paragraphs by remember { mutableStateOf(listOf<String>()) }
    var chapters by remember { mutableStateOf(listOf<Chapter>()) }
    var currentChapterIndex by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var showChapterPicker by remember { mutableStateOf(false) }

    // Parse the file asynchronously on IO threads
    LaunchedEffect(filePath) {
        withContext(Dispatchers.IO) {
            try {
                if (isEpub) {
                    val parsedChapters = parseEpubFile(file)
                    withContext(Dispatchers.Main) {
                        chapters = parsedChapters
                        isLoading = false
                        // Load saved position
                        val lastSavedChapter = prefs.getInt("pref_book_chapter_$pathHash", 0)
                        if (lastSavedChapter in chapters.indices) {
                            currentChapterIndex = lastSavedChapter
                        }
                    }
                } else {
                    // DOCX parsing
                    val docText = parseDocxFile(file)
                    withContext(Dispatchers.Main) {
                        paragraphs = docText
                        isLoading = false
                        // Load saved position
                        val lastSavedParaIndex = prefs.getInt("pref_docx_para_$pathHash", 0)
                        if (lastSavedParaIndex in paragraphs.indices) {
                            scope.launch {
                                listState.scrollToItem(lastSavedParaIndex)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error parsing file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    onBack()
                }
            }
        }
    }

    // Auto-save DOCX position on scroll
    if (!isEpub && paragraphs.isNotEmpty()) {
        LaunchedEffect(listState.firstVisibleItemIndex) {
            val index = listState.firstVisibleItemIndex
            if (index in paragraphs.indices) {
                prefs.edit().putInt("pref_docx_para_$pathHash", index).apply()
            }
        }
    }

    // Auto-save EPUB chapter index on swap
    if (isEpub && chapters.isNotEmpty()) {
        LaunchedEffect(currentChapterIndex) {
            prefs.edit().putInt("pref_book_chapter_$pathHash", currentChapterIndex).apply()
        }
    }

    BackHandler {
        onBack()
    }

    NovaBackground {
        Scaffold(
            topBar = {
                NovaTopBar(
                    title = docTitle,
                    onBack = onBack,
                    neonAccent = NeonCyan,
                    actions = {
                        if (isEpub && chapters.isNotEmpty()) {
                            NovaChip(
                                text = "Chapters",
                                selected = showChapterPicker,
                                onClick = { showChapterPicker = true },
                                neonColor = NeonCyan
                            )
                        }
                    }
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = NeonCyan)
                } else {
                    if (isEpub && chapters.isNotEmpty()) {
                        // EPUB Chapter Display layout
                        val currentChapter = chapters[currentChapterIndex]
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(NovaTokens.Spacing.xl)
                        ) {
                            Text(
                                text = currentChapter.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = NeonCyan
                            )
                            Spacer(modifier = Modifier.height(NovaTokens.Spacing.sm))
                            Divider(color = NeonCyan.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(NovaTokens.Spacing.sm))
                            
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                item {
                                    Text(
                                        text = currentChapter.content,
                                        style = NovaTypography.bodyMedium,
                                        lineHeight = 24.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            
                            // Chapter navigation footer
                            Spacer(modifier = Modifier.height(NovaTokens.Spacing.md))
                            GlassCard(
                                neonAccent = NeonCyan
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(NovaTokens.Spacing.sm),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    NovaSecondaryButton(
                                        text = "Previous",
                                        neonColor = NeonCyan,
                                        enabled = currentChapterIndex > 0,
                                        onClick = { if (currentChapterIndex > 0) currentChapterIndex-- }
                                    )
                                    Text(
                                        text = "${currentChapterIndex + 1} of ${chapters.size}",
                                        style = NovaTypography.tagMono,
                                        color = NeonCyan
                                    )
                                    NovaPrimaryButton(
                                        text = "Next",
                                        neonColor = NeonCyan,
                                        enabled = currentChapterIndex < chapters.size - 1,
                                        onClick = { if (currentChapterIndex < chapters.size - 1) currentChapterIndex++ }
                                    )
                                }
                            }
                        }
                    } else if (!isEpub && paragraphs.isNotEmpty()) {
                        // DOCX Layout
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = NovaTokens.Spacing.xl, vertical = NovaTokens.Spacing.sm),
                            verticalArrangement = Arrangement.spacedBy(NovaTokens.Spacing.sm)
                        ) {
                            items(paragraphs.size) { idx ->
                                val para = paragraphs[idx]
                                NovaAnimatedItem(index = idx) {
                                    GlassCard(
                                        neonAccent = Color.Transparent
                                    ) {
                                        Text(
                                            text = para,
                                            style = NovaTypography.bodyMedium,
                                            lineHeight = 22.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                            modifier = Modifier.padding(NovaTokens.Spacing.sm)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text("This document is empty", style = NovaTypography.bodyMedium, color = Color.Gray)
                    }
                }
            }
        }
 
        // Chapter Selection dialog overlay
        if (showChapterPicker && chapters.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { showChapterPicker = false },
                title = { Text("Select Chapter", style = MaterialTheme.typography.titleLarge, color = NeonCyan) },
                text = {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(chapters.size) { idx ->
                            val chap = chapters[idx]
                            NovaAnimatedItem(index = idx) {
                                GlassCard(
                                    neonAccent = if (idx == currentChapterIndex) NeonCyan else Color.Transparent,
                                    onClick = {
                                        currentChapterIndex = idx
                                        showChapterPicker = false
                                    }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(NovaTokens.Spacing.sm)
                                    ) {
                                        Text(
                                            text = chap.title,
                                            style = NovaTypography.bodyMedium.copy(fontWeight = if (idx == currentChapterIndex) FontWeight.Bold else FontWeight.Normal),
                                            color = if (idx == currentChapterIndex) NeonCyan else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showChapterPicker = false }) {
                        Text("Close", color = NeonCyan, style = NovaTypography.labelLarge)
                    }
                }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Custom Offline Document Parsers
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Strips XML and parses DOCX zip to extract raw paragraphs.
 */
private fun parseDocxFile(file: File): List<String> {
    val zipFile = ZipFile(file)
    val entry = zipFile.getEntry("word/document.xml") ?: return emptyList()
    val inputStream = zipFile.getInputStream(entry)
    
    val paragraphs = mutableListOf<String>()
    
    try {
        val parser = Xml.newPullParser()
        parser.setInput(inputStream, "utf-8")
        
        var eventType = parser.eventType
        var currentParagraph = StringBuilder()
        
        while (eventType != XmlPullParser.END_DOCUMENT) {
            val name = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (name == "p") {
                        currentParagraph = StringBuilder()
                    }
                }
                XmlPullParser.TEXT -> {
                    currentParagraph.append(parser.text)
                }
                XmlPullParser.END_TAG -> {
                    if (name == "p") {
                        val text = currentParagraph.toString().trim()
                        if (text.isNotEmpty()) {
                            paragraphs.add(text)
                        }
                    }
                }
            }
            eventType = parser.next()
        }
    } finally {
        inputStream.close()
        zipFile.close()
    }
    return paragraphs
}

/**
 * Scans an EPUB zip and parses standard spine and manifests into high-fidelity book flows.
 */
private fun parseEpubFile(file: File): List<Chapter> {
    val zipFile = ZipFile(file)
    val chapters = mutableListOf<Chapter>()

    try {
        // Step 1: Scan ZIP to find any container or manifest file
        val entries = zipFile.entries()
        var manifestEntryName: String? = null
        
        while (entries.hasMoreElements()) {
            val element = entries.nextElement()
            if (element.name.endsWith(".opf", ignoreCase = true)) {
                manifestEntryName = element.name
                break
            }
        }
        
        if (manifestEntryName == null) return emptyList()
        
        val opfInputStream = zipFile.getInputStream(zipFile.getEntry(manifestEntryName))
        val parser = Xml.newPullParser()
        parser.setInput(opfInputStream, "utf-8")
        
        // Manifest elements map
        val manifestIdMap = mutableMapOf<String, String>()
        val spineItems = mutableListOf<String>()
        
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                val name = parser.name
                if (name == "item") {
                    val id = parser.getAttributeValue(null, "id")
                    val href = parser.getAttributeValue(null, "href")
                    if (id != null && href != null) {
                        manifestIdMap[id] = href
                    }
                } else if (name == "itemref") {
                    val idref = parser.getAttributeValue(null, "idref")
                    if (idref != null) {
                        spineItems.add(idref)
                    }
                }
            }
            eventType = parser.next()
        }
        opfInputStream.close()

        // Resolve absolute folder path for resources
        val parentPath = File(manifestEntryName).parent ?: ""
        
        // Step 2: Read each Spine chapter HTML and strip tags
        for (idref in spineItems) {
            val href = manifestIdMap[idref] ?: continue
            val relativePath = if (parentPath.isNotEmpty()) "$parentPath/$href" else href
            
            val zipEntry = zipFile.getEntry(relativePath) ?: continue
            val chapterStream = zipFile.getInputStream(zipEntry)
            val rawHtmlText = chapterStream.bufferedReader().use { it.readText() }
            chapterStream.close()
            
            // Extract chapter title or fallback
            val titleRegex = "<title>(.*?)</title>".toRegex(RegexOption.IGNORE_CASE)
            val title = titleRegex.find(rawHtmlText)?.groupValues?.get(1)?.trim() ?: "Chapter ${chapters.size + 1}"
            
            // Clean XML/HTML markup to raw readable string
            val cleanBody = stripHtmlTags(rawHtmlText)
            if (cleanBody.isNotBlank()) {
                chapters.add(Chapter(title, cleanBody))
            }
        }
    } finally {
        zipFile.close()
    }
    
    // Fallback if parsing spine resolved nothing
    if (chapters.isEmpty()) {
        chapters.add(Chapter("Introduction", "The book parsing yielded no readable chapters. Standard EPUB layout error."))
    }
    
    return chapters
}

/**
 * Custom fast regex html tags stripper
 */
private fun stripHtmlTags(html: String): String {
    // 1. Extract content between body tag
    val bodyRegex = "<body[^>]*>(.*?)</body>".toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    val bodyText = bodyRegex.find(html)?.groupValues?.get(1) ?: html
    
    // 2. Format paragraphs and clean layout
    var clean = bodyText
        .replace("<p[^>]*>", "\n\n")
        .replace("<br[^>]*>", "\n")
        .replace("<li[^>]*>", "\n• ")
        .replace("<h1[^>]*>|<h2[^>]*>|<h3[^>]*>", "\n\n")
        
    // 3. Strip remaining tags
    clean = clean.replace("<[^>]*>".toRegex(), "")
    
    // 4. Resolve core html entities
    clean = clean
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        
    // 5. Trim lines
    return clean.lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n\n")
}
