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
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.MidnightIndigo
import com.anegan.core.designsystem.theme.PureWhite
import com.anegan.core.designsystem.theme.FiraCodeFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextCodeReaderScreen(
    filePath: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val prefs = remember { context.getSharedPreferences("anegan_doc_reader", Context.MODE_PRIVATE) }
    
    val file = remember { File(filePath) }
    val pathHash = filePath.hashCode().toString()
    val isMonospaceType = remember { 
        file.extension.equals("json", ignoreCase = true) || 
        file.extension.equals("log", ignoreCase = true) 
    }

    var fileContent by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    // Load file contents asynchronously
    LaunchedEffect(filePath) {
        withContext(Dispatchers.IO) {
            try {
                if (file.exists()) {
                    val text = file.readText(Charsets.UTF_8)
                    withContext(Dispatchers.Main) {
                        fileContent = text
                        isLoading = false
                        // Load saved position
                        val lastSavedScroll = prefs.getInt("pref_text_scroll_$pathHash", 0)
                        if (lastSavedScroll in 0 until fileContent.length) {
                            listState.scrollToItem(0, lastSavedScroll)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to read file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    onBack()
                }
            }
        }
    }

    // Auto-save scroll position
    LaunchedEffect(listState.firstVisibleItemScrollOffset) {
        val offset = listState.firstVisibleItemScrollOffset
        prefs.edit().putInt("pref_text_scroll_$pathHash", offset).apply()
    }

    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = file.name,
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
            // Search Input Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search keyword in document…", fontSize = 13.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MidnightIndigo,
                    unfocusedBorderColor = Color.LightGray
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = MidnightIndigo)
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            // Compute highlighting
                            val annotatedString = remember(fileContent, searchQuery) {
                                buildHighlightedString(fileContent, searchQuery)
                            }
                            Text(
                                text = annotatedString,
                                fontSize = 14.sp,
                                lineHeight = 22.sp,
                                color = Color.DarkGray,
                                fontFamily = if (isMonospaceType) FiraCodeFontFamily else FontFamily.SansSerif
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Builds standard Jetpack Compose AnnotatedString highlighting all search matches dynamically.
 */
private fun buildHighlightedString(content: String, query: String): AnnotatedString {
    val builder = AnnotatedString.Builder(content)
    if (query.isNotEmpty() && query.length >= 2) {
        var index = content.indexOf(query, ignoreCase = true)
        while (index >= 0) {
            builder.addStyle(
                SpanStyle(
                    background = Color(0xFFFFEB3B), // Premium golden highlight background
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.Bold
                ),
                index,
                index + query.length
            )
            index = content.indexOf(query, index + query.length, ignoreCase = true)
        }
    }
    return builder.toAnnotatedString()
}
