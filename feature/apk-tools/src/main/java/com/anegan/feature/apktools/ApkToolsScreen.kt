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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.anegan.core.designsystem.theme.MidnightIndigo
import com.anegan.core.designsystem.theme.PureWhite
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
    val haptic = LocalHapticFeedback.current
    
    var appsList by remember { mutableStateOf(listOf<InstalledApp>()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("User Apps") } // "User Apps", "System Apps", "All"
    
    // Asynchronous extract loader
    var extractingAppName by remember { mutableStateOf<String?>(null) }

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
    }

    BackHandler {
        onBack()
    }

    fun extractApk(app: InstalledApp) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        extractingAppName = app.name
        
        scope.launch(Dispatchers.IO) {
            try {
                val sourceFile = File(app.sourceDir)
                val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetDir = File(publicDir, "Anegan/ApkBackups")
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
                    Toast.makeText(context, "Extracted successfully to Downloads/Anegan/ApkBackups!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    extractingAppName = null
                    Toast.makeText(context, "Extraction failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun shareApk(app: InstalledApp) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch(Dispatchers.IO) {
            try {
                val sourceFile = File(app.sourceDir)
                val tempDir = File(context.cacheDir, "shared_apks")
                if (!tempDir.exists()) tempDir.mkdirs()
                
                val cleanName = app.name.replace("[^a-zA-Z0-9]".toRegex(), "_")
                val targetFile = File(tempDir, "${cleanName}_${app.versionName}.apk")
                
                FileInputStream(sourceFile).use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                
                withContext(Dispatchers.Main) {
                    val authority = "${context.packageName}.fileprovider"
                    val fileUri: Uri = FileProvider.getUriForFile(context, authority, targetFile)
                    
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "APK Extractor & Backup",
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 22.sp),
                            color = MidnightIndigo
                        )
                        Text("Extract or backup your installed apps instantly", color = Color.Gray, fontSize = 10.sp)
                    }
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
            // Search Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MidnightIndigo.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔍", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text("Search installed apps...", color = Color.Gray, fontSize = 14.sp)
                    }
                    androidx.compose.foundation.text.BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clickable { 
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                searchQuery = "" 
                            }
                            .padding(4.dp)
                    ) {
                        Text("❌", fontSize = 11.sp)
                    }
                }
            }

            // Filter Row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val filters = listOf("User Apps", "System Apps", "All")
                items(filters) { filter ->
                    val isSelected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) MidnightIndigo else MaterialTheme.colorScheme.surface)
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter,
                            color = if (isSelected) PureWhite else MidnightIndigo,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Divider(color = Color.LightGray.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))

            Box(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = MidnightIndigo)
                } else if (filteredApps.isEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text("📦", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No apps found",
                            fontWeight = FontWeight.Bold,
                            color = MidnightIndigo,
                            fontSize = 16.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredApps) { app ->
                            val appIcon = remember(app.drawable) {
                                app.drawable.toBitmap().asImageBitmap()
                            }
                            val sdf = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
                            val dateString = remember(app.installDate) { sdf.format(Date(app.installDate)) }
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
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
                                            color = MidnightIndigo,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "v${app.versionName} • ${app.packageName}",
                                            color = Color.Gray,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val sizeMb = app.size / (1024f * 1024f)
                                        Text(
                                            text = String.format(Locale.ROOT, "%.2f MB • Installed: %s", sizeMb, dateString),
                                            color = Color.Gray,
                                            fontSize = 9.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = { extractApk(app) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Text("📥", fontSize = 18.sp)
                                        }
                                        IconButton(
                                            onClick = { shareApk(app) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Text("📤", fontSize = 18.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Global loading dialog during extraction (placed in ColumnScope)
            AnimatedVisibility(visible = extractingAppName != null) {
                val name = extractingAppName ?: ""
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text("Extracting Backup", fontWeight = FontWeight.Bold, color = MidnightIndigo) },
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(color = MidnightIndigo, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Backing up $name...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MidnightIndigo
                            )
                        }
                    },
                    confirmButton = {}
                )
            }
        }
    }
}
