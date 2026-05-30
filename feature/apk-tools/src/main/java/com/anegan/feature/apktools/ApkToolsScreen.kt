/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.apktools

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.anegan.core.designsystem.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class InstalledApp(
    val name: String,
    val packageName: String,
    val versionName: String,
    val sourceDir: String,
    val size: Long,
    val installDate: Long,
    val isSystem: Boolean,
    val drawable: Drawable
)

// Helper to convert Drawable to Bitmap cleanly for Jetpack Compose
fun Drawable.toBitmap(): Bitmap {
    val w = if (intrinsicWidth > 0) intrinsicWidth else 96
    val h = if (intrinsicHeight > 0) intrinsicHeight else 96
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, w, h)
    draw(canvas)
    return bitmap
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApkToolsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    
    val prefs = remember { context.getSharedPreferences("anegan_doc_reader", Context.MODE_PRIVATE) }
    
    var appsList by remember { mutableStateOf(listOf<InstalledApp>()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("User Apps") } // "User Apps", "System Apps", "All"
    var currentTab by remember { mutableStateOf(0) } // 0 = Installed Apps, 1 = Extracted Backups
    var showHowItWorks by remember { mutableStateOf(false) }

    // Asynchronous extract loader
    var extractingAppName by remember { mutableStateOf<String?>(null) }

    data class ExtractedApk(
        val name: String,
        val file: File,
        val size: Long
    )
    
    var extractedApks by remember { mutableStateOf(listOf<ExtractedApk>()) }

    fun loadExtractedApks() {
        scope.launch(Dispatchers.IO) {
            val targetDir = File(Environment.getExternalStorageDirectory(), "Anegan/APKs")
            if (!targetDir.exists()) targetDir.mkdirs()
            val files = targetDir.listFiles()?.filter { it.isFile && it.extension.lowercase(Locale.ROOT) == "apk" } ?: emptyList()
            val temp = files.map { file ->
                ExtractedApk(
                    name = file.name,
                    file = file,
                    size = file.length()
                )
            }.sortedByDescending { it.file.lastModified() }
            
            withContext(Dispatchers.Main) {
                extractedApks = temp
            }
        }
    }

    fun loadApps() {
        isLoading = true
        scope.launch(Dispatchers.IO) {
            val pm = context.packageManager
            val rawApps = pm.getInstalledPackages(PackageManager.GET_META_DATA)
            val tempApps = mutableListOf<InstalledApp>()
            
            for (pkg in rawApps) {
                val appInfo = pkg.applicationInfo ?: continue
                
                // Get app name
                val name = pm.getApplicationLabel(appInfo).toString()
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                
                val apkFile = File(appInfo.sourceDir)
                if (!apkFile.exists()) continue
                
                tempApps.add(
                    InstalledApp(
                        name = name,
                        packageName = pkg.packageName,
                        versionName = pkg.versionName ?: "1.0",
                        sourceDir = appInfo.sourceDir,
                        size = apkFile.length(),
                        installDate = pkg.firstInstallTime,
                        isSystem = isSystem,
                        drawable = pm.getApplicationIcon(appInfo)
                    )
                )
            }
            
            // Sort alphabetically
            val sorted = tempApps.sortedBy { it.name.lowercase(Locale.ROOT) }
            withContext(Dispatchers.Main) {
                appsList = sorted
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadApps()
        loadExtractedApks()
    }

    BackHandler {
        onBack()
    }

    fun extractApk(app: InstalledApp) {
        NovaHaptics.longPress(view)
        extractingAppName = app.name
        
        scope.launch(Dispatchers.IO) {
            try {
                val sourceFile = File(app.sourceDir)
                val targetDir = File(Environment.getExternalStorageDirectory(), "Anegan/APKs")
                if (!targetDir.exists()) targetDir.mkdirs()
                
                val cleanName = app.name.replace("[^a-zA-Z0-9]".toRegex(), "_")
                val targetFile = File(targetDir, "${cleanName}_${app.versionName}.apk")
                
                FileInputStream(sourceFile).use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                
                withContext(Dispatchers.Main) {
                    extractingAppName = null
                    Toast.makeText(context, "Extracted to Anegan/APKs!", Toast.LENGTH_SHORT).show()
                    NovaHaptics.success(view)
                    loadExtractedApks()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    extractingAppName = null
                    Toast.makeText(context, "Extraction failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    NovaHaptics.reject(view)
                }
            }
        }
    }

    fun shareExtractedApk(file: File) {
        NovaHaptics.longPress(view)
        scope.launch(Dispatchers.IO) {
            try {
                val authority = "${context.packageName}.fileprovider"
                val fileUri: Uri = FileProvider.getUriForFile(context, authority, file)
                
                withContext(Dispatchers.Main) {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/vnd.android.package-archive"
                        putExtra(Intent.EXTRA_STREAM, fileUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share APK via"))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Sharing failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun deleteExtractedApk(file: File) {
        NovaHaptics.longPress(view)
        if (file.delete()) {
            Toast.makeText(context, "Deleted successfully", Toast.LENGTH_SHORT).show()
            NovaHaptics.success(view)
            loadExtractedApks()
        } else {
            Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show()
            NovaHaptics.reject(view)
        }
    }

    val filteredApps = remember(appsList, selectedFilter, searchQuery) {
        appsList.filter { app ->
            val matchesFilter = when (selectedFilter) {
                "User Apps" -> !app.isSystem
                "System Apps" -> app.isSystem
                else -> true
            }
            val matchesSearch = app.name.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    val isDark = isSystemInDarkTheme()

    NovaBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                NovaTopBar(
                    title = "APK Extractor & Backup",
                    onBack = onBack,
                    neonAccent = NeonPurple,
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
                // Futuristic NovaSegmentedControl replacing native TabRow
                val tabs = remember { listOf("INSTALLED APPS", "EXTRACTED APKS") }
                NovaSegmentedControl(
                    items = tabs,
                    selectedIndex = currentTab,
                    onIndexSelected = { idx ->
                        NovaHaptics.click(view)
                        currentTab = idx
                    },
                    neonColor = NeonPurple,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (currentTab == 0) {
                    // Futuristic Search Input
                    NovaTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Search installed apps...",
                        neonColor = NeonPurple,
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

                    // Categories/Filters Row
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val filters = listOf("User Apps", "System Apps", "All")
                        items(filters) { filter ->
                            val isSelected = selectedFilter == filter
                            NovaChip(
                                text = filter,
                                selected = isSelected,
                                onClick = {
                                    NovaHaptics.toggle(view)
                                    selectedFilter = filter
                                },
                                neonColor = NeonPurple
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = NeonPurple)
                        } else if (filteredApps.isEmpty()) {
                            NovaEmptyState(
                                icon = Icons.Rounded.GetApp,
                                title = "No Apps Found",
                                subtitle = "Adjust your filters or query to locate installed applications.",
                                neonColor = NeonPurple
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(filteredApps) { index, app ->
                                    val appIcon = remember(app.drawable) {
                                        app.drawable.toBitmap().asImageBitmap()
                                    }
                                    val sdf = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
                                    val dateString = remember(app.installDate) { sdf.format(Date(app.installDate)) }
                                    
                                    NovaAnimatedItem(index = index) {
                                        GlassCard(
                                            modifier = Modifier.fillMaxWidth(),
                                            neonAccent = NeonPurple.copy(alpha = 0.3f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Image(
                                                    bitmap = appIcon,
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = app.name,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isDark) NovaFrostWhite else NovaDeepInk,
                                                        fontSize = 13.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "v${app.versionName} • ${app.packageName}",
                                                        color = if (isDark) NovaFrostWhite.copy(alpha = 0.6f) else NovaDeepInk.copy(alpha = 0.5f),
                                                        fontSize = 10.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    val sizeMb = app.size / (1024f * 1024f)
                                                    Text(
                                                        text = String.format(Locale.ROOT, "%.2f MB • Installed: %s", sizeMb, dateString),
                                                        color = if (isDark) NovaFrostWhite.copy(alpha = 0.5f) else NovaDeepInk.copy(alpha = 0.4f),
                                                        fontFamily = JetBrainsMono,
                                                        fontSize = 9.sp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                IconButton(
                                                    onClick = { extractApk(app) },
                                                    modifier = Modifier.size(44.dp)
                                                ) {
                                                    Text("📥", fontSize = 20.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (extractedApks.isEmpty()) {
                            NovaEmptyState(
                                icon = Icons.Rounded.FolderZip,
                                title = "No Extracted APKs",
                                subtitle = "Extracted backups will appear here for sharing or reinstalling.",
                                neonColor = NeonPurple
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(extractedApks) { index, extracted ->
                                    NovaAnimatedItem(index = index) {
                                        GlassCard(
                                            modifier = Modifier.fillMaxWidth(),
                                            neonAccent = NeonPurple.copy(alpha = 0.3f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(CircleShape)
                                                        .background(NeonPurple.copy(alpha = 0.12f))
                                                        .border(BorderStroke(1.dp, NeonPurple.copy(alpha = 0.3f)), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("📦", fontSize = 20.sp)
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = extracted.name,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isDark) NovaFrostWhite else NovaDeepInk,
                                                        fontSize = 13.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    val sizeMb = extracted.size / (1024f * 1024f)
                                                    Text(
                                                        text = String.format(Locale.ROOT, "%.2f MB", sizeMb),
                                                        color = if (isDark) NovaFrostWhite.copy(alpha = 0.6f) else NovaDeepInk.copy(alpha = 0.5f),
                                                        fontFamily = JetBrainsMono,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    IconButton(
                                                        onClick = { shareExtractedApk(extracted.file) },
                                                        modifier = Modifier.size(44.dp)
                                                    ) {
                                                        Text("📤", fontSize = 20.sp)
                                                    }
                                                    IconButton(
                                                        onClick = { deleteExtractedApk(extracted.file) },
                                                        modifier = Modifier.size(44.dp)
                                                    ) {
                                                        Text("🗑️", fontSize = 20.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Global loading dialog during extraction styled elegantly in V3.2
            AnimatedVisibility(visible = extractingAppName != null) {
                val name = extractingAppName ?: ""
                AlertDialog(
                    onDismissRequest = {},
                    title = { 
                        Text(
                            text = "Extracting Backup", 
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold, 
                            color = if (isDark) NovaFrostWhite else NovaDeepInk
                        ) 
                    },
                    text = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(color = NeonPurple, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Backing up $name...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) NovaFrostWhite else NovaDeepInk
                            )
                        }
                    },
                    confirmButton = {},
                    containerColor = if (isDark) NovaMidnightBlue else NovaPureWhite,
                    shape = RoundedCornerShape(24.dp)
                )
            }
        }
    }

    if (showHowItWorks) {
        com.anegan.core.designsystem.theme.HowItWorksDialog(
            title = "App Extractor",
            description = "Extracts installed apps on your device into standalone APK installer files for sharing or backing up offline.",
            steps = listOf(
                "In the 'Installed Apps' tab, browse or search for the app you want to back up.",
                "Tap the download icon (📥) next to the app to start extraction.",
                "The extracted APK file will be saved to your device's internal storage under /Anegan/APKs/.",
                "Switch to the 'Extracted APKs' tab to see all your extracted backups.",
                "From the 'Extracted APKs' tab, tap the share icon (📤) to send the APK to another device, or tap the delete icon (🗑️) to remove it."
            ),
            tips = listOf(
                "System apps are listed in a separate filter and can also be extracted, though some pre-installed system apps may require their dependencies to work elsewhere.",
                "Sharing large APKs might take longer. Use Wifi Transfer or Local Transfer for faster speeds."
            ),
            faq = listOf(
                "Where are my extracted APKs stored?" to "They are stored in the public folder '/Anegan/APKs/' on your internal storage so they can be easily moved or copied.",
                "Will extracting an app uninstall it?" to "No. Extraction only copies the app's base package to create a backup; the installed app remains intact on your device."
            ),
            onDismiss = { showHowItWorks = false }
        )
    }
}
