/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 *
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.*
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurvivalLibraryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    var selectedLibrary by remember { mutableStateOf<String?>("Offline_Survival_Manual") }
    var selectedFile by remember { mutableStateOf<String?>(null) }
    var fileContent by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var showHowItWorks by remember { mutableStateOf(false) }

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

    val isDark = isSystemInDarkTheme()

    NovaBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                NovaTopBar(
                    title = if (selectedFile != null) selectedFile!!.replace(".md", "").replace("_", " ") else "Survival Library",
                    onBack = {
                        NovaHaptics.click(view)
                        if (selectedFile != null) {
                            selectedFile = null
                        } else {
                            onBack()
                        }
                    },
                    neonAccent = NeonGold,
                    showHowItWorks = true,
                    onHowItWorks = { showHowItWorks = true }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (selectedFile == null) {
                    // Library Selector Tabs replacing the custom Row with futuristic NovaSegmentedControl
                    val tabs = remember { listOf("SURVIVAL 🌲", "GENERAL ⚙️", "MEDICAL 🩺") }
                    val selectedIndex = when (selectedLibrary) {
                        "Offline_Survival_Manual" -> 0
                        "General_Offline_Things" -> 1
                        "Medical_Hub" -> 2
                        else -> 0
                    }
                    NovaSegmentedControl(
                        items = tabs,
                        selectedIndex = selectedIndex,
                        onIndexSelected = { idx ->
                            NovaHaptics.click(view)
                            selectedLibrary = when (idx) {
                                0 -> "Offline_Survival_Manual"
                                1 -> "General_Offline_Things"
                                2 -> "Medical_Hub"
                                else -> "Offline_Survival_Manual"
                            }
                        },
                        neonColor = NeonGold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Futuristic Search Input using NovaTextField
                    NovaTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Search chapters...",
                        neonColor = NeonGold,
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

                    Spacer(modifier = Modifier.height(6.dp))

                    val filteredFiles = files.filter {
                        it.replace("_", " ").contains(searchQuery, ignoreCase = true)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (filteredFiles.isEmpty()) {
                            NovaEmptyState(
                                icon = Icons.Rounded.AutoStories,
                                title = "No Chapters Found",
                                subtitle = "No articles match the current search filters.",
                                neonColor = NeonGold
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                itemsIndexed(filteredFiles) { index, fileName ->
                                    val cleanName = fileName.replace(".md", "").replace("_", " ")
                                    NovaAnimatedItem(index = index) {
                                        GlassCard(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    NovaHaptics.click(view)
                                                    selectedFile = fileName
                                                },
                                            neonAccent = NeonGold.copy(alpha = 0.3f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("📖", fontSize = 22.sp)
                                                    Spacer(modifier = Modifier.width(14.dp))
                                                    Column {
                                                        Text(
                                                            text = cleanName,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            color = if (isDark) NovaFrostWhite else NovaDeepInk
                                                        )
                                                        Spacer(modifier = Modifier.height(3.dp))
                                                        Text(
                                                            text = "OFFLINE MANUAL • TECHNICAL",
                                                            fontFamily = JetBrainsMono,
                                                            fontWeight = FontWeight.SemiBold,
                                                            fontSize = 9.sp,
                                                            color = NeonGold
                                                        )
                                                    }
                                                }
                                                Text("➡️", color = NeonGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Document Reader with reading progress indicator
                    val listState = rememberLazyListState()
                    val totalItems = fileContent.split("\n").size
                    val firstVisible by remember { derivedStateOf { listState.firstVisibleItemIndex } }
                    val progress = if (totalItems <= 1) 0f else firstVisible.toFloat() / (totalItems - 1).toFloat()

                    LinearProgressIndicator(
                        progress = progress.coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = NeonGold,
                        trackColor = Color.Transparent
                    )

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val lines = fileContent.split("\n")
                        items(lines) { line ->
                            val cleanLine = line.trim()
                            when {
                                cleanLine.startsWith("# ") -> {
                                    Text(
                                        text = cleanLine.removePrefix("# "),
                                        fontFamily = SpaceGrotesk,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonGold,
                                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                    )
                                }
                                cleanLine.startsWith("## ") -> {
                                    Text(
                                        text = cleanLine.removePrefix("## "),
                                        fontFamily = SpaceGrotesk,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) NovaFrostWhite else NovaDeepInk,
                                        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                                    )
                                }
                                cleanLine.startsWith("### ") -> {
                                    Text(
                                        text = cleanLine.removePrefix("### "),
                                        fontFamily = SpaceGrotesk,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) NovaFrostWhite.copy(alpha = 0.9f) else NovaDeepInk.copy(alpha = 0.9f),
                                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                    )
                                }
                                cleanLine.startsWith("> ") -> {
                                    GlassCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        neonAccent = NeonGold.copy(alpha = 0.4f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(4.dp)
                                                    .height(32.dp)
                                                    .background(NeonGold, RoundedCornerShape(2.dp))
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = cleanLine.removePrefix("> "),
                                                fontFamily = JetBrainsMono,
                                                fontSize = 12.sp,
                                                color = if (isDark) NovaFrostWhite.copy(alpha = 0.8f) else NovaDeepInk.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                                cleanLine.startsWith("|") -> {
                                    val cells = cleanLine.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                                    if (cleanLine.contains("---")) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                    } else {
                                        GlassCard(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp),
                                            neonAccent = NeonGold.copy(alpha = 0.15f)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                cells.forEach { cell ->
                                                    Text(
                                                        text = cell,
                                                        fontFamily = JetBrainsMono,
                                                        fontSize = 11.sp,
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .padding(4.dp),
                                                        fontWeight = FontWeight.Medium,
                                                        color = if (isDark) NovaFrostWhite else NovaDeepInk
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                cleanLine.isBlank() -> {
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                                else -> {
                                    Text(
                                        text = cleanLine,
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 13.sp,
                                        lineHeight = 20.sp,
                                        color = if (isDark) NovaFrostWhite.copy(alpha = 0.85f) else NovaDeepInk.copy(alpha = 0.85f),
                                        modifier = Modifier.padding(vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showHowItWorks) {
                com.anegan.core.designsystem.theme.HowItWorksDialog(
                    title = "Survival Library",
                    description = "An offline documentation hub and technical library containing survival, general, and medical instructions completely readable without an active internet connection.",
                    steps = listOf(
                        "Use the top tabs to switch between Survival, General, and Medical categories.",
                        "Type in the search bar to locate specific manual chapters quickly.",
                        "Tap on any chapter to open the offline reader.",
                        "Scroll to read chapters, and monitor reading progress via the gold bar at the top."
                    ),
                    tips = listOf(
                        "The entire contents of this library are stored locally inside the application package and operate 100% offline.",
                        "Medical instructions provided are reference materials only; always seek professional aid when available."
                    ),
                    faq = listOf(
                        "Can I add custom books to this library?" to "Currently, this library is bundled with curated emergency texts. Custom manual additions will be available in future releases."
                    ),
                    onDismiss = { showHowItWorks = false }
                )
            }
        }
    }
}
