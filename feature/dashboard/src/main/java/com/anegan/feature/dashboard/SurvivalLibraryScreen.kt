/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 *
 * This source code is licensed under the custom Anegan Attribution License.
 */

package com.anegan.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurvivalLibraryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedLibrary by remember { mutableStateOf<String?>("Offline_Survival_Manual") }
    var selectedFile by remember { mutableStateOf<String?>(null) }
    var fileContent by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    // List assets inside the selected library
    val files = remember(selectedLibrary) {
        try {
            context.assets.list(selectedLibrary ?: "")
                ?.filter { it.endsWith(".md") && it != "Index.md" }
                ?.sorted() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    LaunchedEffect(selectedFile, selectedLibrary) {
        if (selectedFile != null && selectedLibrary != null) {
            try {
                val inputStream = context.assets.open("$selectedLibrary/$selectedFile")
                val reader = BufferedReader(InputStreamReader(inputStream))
                fileContent = reader.use { it.readText() }
            } catch (e: Exception) {
                fileContent = "Failed to load document: ${e.message}"
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Custom TopBar
        TopAppBar(
            title = {
                Text(
                    text = if (selectedFile != null) selectedFile!!.replace(".md", "").replace("_", " ") else "Survival Library 🌲",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MidnightIndigo
                )
            },
            navigationIcon = {
                TextButton(onClick = {
                    if (selectedFile != null) {
                        selectedFile = null
                    } else {
                        onBack()
                    }
                }) {
                    Text("◀ Back", color = MidnightIndigo, fontWeight = FontWeight.Bold)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        if (selectedFile == null) {
            // Library Selector Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LibraryTab("Survival 🌲", selectedLibrary == "Offline_Survival_Manual") {
                    selectedLibrary = "Offline_Survival_Manual"
                }
                LibraryTab("General ⚙️", selectedLibrary == "General_Offline_Things") {
                    selectedLibrary = "General_Offline_Things"
                }
                LibraryTab("Medical 🩺", selectedLibrary == "Medical_Hub") {
                    selectedLibrary = "Medical_Hub"
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Chapters...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // File List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val filteredFiles = files.filter {
                    it.replace("_", " ").contains(searchQuery, ignoreCase = true)
                }

                items(filteredFiles) { fileName ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFile = fileName },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = fileName.replace(".md", "").replace("_", " "),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MidnightIndigo
                                )
                                Text(
                                    text = "Offline Technical Reference Manual",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            Text("▶", color = Color.LightGray)
                        }
                    }
                }
            }
        } else {
            // Document Reader
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val lines = fileContent.split("\n")
                items(lines) { line ->
                    when {
                        line.startsWith("# ") -> {
                            Text(
                                text = line.removePrefix("# "),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MidnightIndigo,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                        line.startsWith("## ") -> {
                            Text(
                                text = line.removePrefix("## "),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MidnightIndigo,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        line.startsWith("### ") -> {
                            Text(
                                text = line.removePrefix("### "),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MidnightIndigo,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                        line.startsWith("> ") -> {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = line.removePrefix("> "),
                                    fontSize = 13.sp,
                                    color = Color.DarkGray,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                        line.startsWith("|") -> {
                            // Simple table row renderer
                            val cells = line.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                cells.forEach { cell ->
                                    Text(
                                        text = cell,
                                        fontSize = 12.sp,
                                        modifier = Modifier.weight(1f).padding(4.dp),
                                        fontWeight = if (line.contains("---")) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                        else -> {
                            Text(
                                text = line,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.LibraryTab(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MidnightIndigo else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.weight(1f)
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.White else Color.Black,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
