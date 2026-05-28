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
import com.anegan.core.designsystem.theme.MidnightIndigo
import com.anegan.core.designsystem.theme.PureWhite
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = docTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MidnightIndigo,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = 24.sp, color = MidnightIndigo, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    if (isEpub && chapters.isNotEmpty()) {
                        // Chapter Selection dialog button
                        Button(
                            onClick = { showChapterPicker = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MidnightIndigo, contentColor = PureWhite),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Chapters", fontSize = 11.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MidnightIndigo)
            } else {
                if (isEpub && chapters.isNotEmpty()) {
                    // EPUB Chapter Display layout
                    val currentChapter = chapters[currentChapterIndex]
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = currentChapter.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MidnightIndigo
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = Color.LightGray)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            item {
                                Text(
                                    text = currentChapter.content,
                                    fontSize = 15.sp,
                                    lineHeight = 26.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }
                        
                        // Chapter navigation footer
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { if (currentChapterIndex > 0) currentChapterIndex-- },
                                enabled = currentChapterIndex > 0,
                                colors = ButtonDefaults.buttonColors(containerColor = MidnightIndigo, contentColor = PureWhite)
                            ) {
                                Text("Previous")
                            }
                            Text(
                                text = "${currentChapterIndex + 1} of ${chapters.size}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MidnightIndigo
                            )
                            Button(
                                onClick = { if (currentChapterIndex < chapters.size - 1) currentChapterIndex++ },
                                enabled = currentChapterIndex < chapters.size - 1,
                                colors = ButtonDefaults.buttonColors(containerColor = MidnightIndigo, contentColor = PureWhite)
                            ) {
                                Text("Next")
                            }
                        }
                    }
                } else if (!isEpub && paragraphs.isNotEmpty()) {
                    // DOCX Layout
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(paragraphs) { para ->
                            Text(
                                text = para,
                                fontSize = 15.sp,
                                lineHeight = 24.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                } else {
                    Text("This document is empty", color = Color.Gray, fontSize = 14.sp)
                }
            }
        }

        // Chapter Selection dialog overlay
        if (showChapterPicker && chapters.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { showChapterPicker = false },
                title = { Text("Select Chapter", fontWeight = FontWeight.Bold, color = MidnightIndigo) },
                text = {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(chapters.size) { idx ->
                            val chap = chapters[idx]
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        currentChapterIndex = idx
                                        showChapterPicker = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp)
                            ) {
                                Text(
                                    text = chap.title,
                                    fontSize = 14.sp,
                                    fontWeight = if (idx == currentChapterIndex) FontWeight.Bold else FontWeight.Normal,
                                    color = if (idx == currentChapterIndex) MidnightIndigo else Color.DarkGray
                                )
                            }
                            Divider()
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showChapterPicker = false }) {
                        Text("Close", color = MidnightIndigo)
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
