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
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import com.anegan.core.designsystem.theme.*
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class ApkMetadata(
    val label: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val fileSize: Long,
    val permissions: List<String>,
    val icon: Drawable?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApkInfoScreen(
    apkPath: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val file = remember { File(apkPath) }
    
    var metadata by remember { mutableStateOf<ApkMetadata?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(apkPath) {
        withContext(Dispatchers.IO) {
            try {
                if (file.exists()) {
                    val pm = context.packageManager
                    val packageInfo = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_PERMISSIONS)
                    
                    if (packageInfo != null) {
                        val appInfo = packageInfo.applicationInfo
                        if (appInfo != null) {
                            appInfo.sourceDir = apkPath
                            appInfo.publicSourceDir = apkPath
                            
                            val label = try {
                                appInfo.loadLabel(pm).toString()
                            } catch (e: Exception) {
                                file.nameWithoutExtension
                            }
                            
                            val icon = try {
                                appInfo.loadIcon(pm)
                            } catch (e: Exception) {
                                null
                            }

                            val minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                appInfo.minSdkVersion
                            } else {
                                19
                            }
                            
                            val targetSdk = appInfo.targetSdkVersion
                            val versionC = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                packageInfo.longVersionCode
                            } else {
                                @Suppress("DEPRECATION") packageInfo.versionCode.toLong()
                            }

                            val perms = packageInfo.requestedPermissions?.toList() ?: emptyList()

                            withContext(Dispatchers.Main) {
                                metadata = ApkMetadata(
                                    label = label,
                                    packageName = packageInfo.packageName,
                                    versionName = packageInfo.versionName ?: "1.0",
                                    versionCode = versionC,
                                    minSdk = minSdk,
                                    targetSdk = targetSdk,
                                    fileSize = file.length(),
                                    permissions = perms,
                                    icon = icon
                                )
                                isLoading = false
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Unable to parse APK application details", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Unable to parse APK package", Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "APK file not found", Toast.LENGTH_SHORT).show()
                        onBack()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error loading APK: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    onBack()
                }
            }
        }
    }

    fun installApk() {
        try {
            val apkFile = File(apkPath)
            val uri = FileProvider.getUriForFile(
                context,
                "com.anegan.app.fileprovider",
                apkFile
            )
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to initiate installation: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    BackHandler {
        onBack()
    }

    NovaBackground {
        Scaffold(
            topBar = {
                NovaTopBar(
                    title = "APK Package Inspector",
                    onBack = onBack,
                    neonAccent = NeonCyan
                )
            },
            containerColor = Color.Transparent,
            bottomBar = {
                metadata?.let {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(NovaTokens.Spacing.md),
                        neonAccent = NeonCyan
                    ) {
                        NovaPrimaryButton(
                            text = "Install Application",
                            neonColor = NeonCyan,
                            onClick = { installApk() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
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
                    metadata?.let { data ->
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = NovaTokens.Spacing.xl),
                            verticalArrangement = Arrangement.spacedBy(NovaTokens.Spacing.md),
                            contentPadding = PaddingValues(vertical = NovaTokens.Spacing.md)
                        ) {
                            item {
                                GlassCard(
                                    neonAccent = NeonCyan,
                                    enableGlow = true
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(NovaTokens.Spacing.xl),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(88.dp)
                                                .drawBehind {
                                                    drawCircle(
                                                        color = NeonCyan.copy(alpha = 0.2f),
                                                        radius = this.size.minDimension / 2 + 6.dp.toPx()
                                                    )
                                                    drawCircle(
                                                        color = NeonCyan,
                                                        radius = this.size.minDimension / 2,
                                                        style = Stroke(width = 2.dp.toPx())
                                                    )
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (data.icon != null) {
                                                Image(
                                                    bitmap = data.icon.toBitmap(80, 80).asImageBitmap(),
                                                    contentDescription = "App Icon",
                                                    modifier = Modifier
                                                        .size(76.dp)
                                                        .clip(RoundedCornerShape(NovaTokens.Radius.md))
                                                )
                                            } else {
                                                Text("📦", fontSize = 40.sp)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(NovaTokens.Spacing.md))
                                        Text(
                                            text = data.label,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = NeonCyan,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = data.packageName,
                                            style = NovaTypography.tagMono,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            item {
                                GlassCard(
                                    neonAccent = NeonCyan
                                ) {
                                    Column(modifier = Modifier.padding(NovaTokens.Spacing.md)) {
                                        Text(
                                            text = "PACKAGE DETAILS",
                                            style = NovaTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                            color = NeonCyan
                                        )
                                        Spacer(modifier = Modifier.height(NovaTokens.Spacing.sm))
                                        DetailRow("Version Name", data.versionName)
                                        DetailRow("Version Code", data.versionCode.toString())
                                        DetailRow("Min SDK", "API ${data.minSdk}")
                                        DetailRow("Target SDK", "API ${data.targetSdk}")
                                        val sizeMb = data.fileSize / (1024f * 1024f)
                                        DetailRow("File Size", String.format(Locale.US, "%.2f MB", sizeMb))
                                    }
                                }
                            }

                            if (data.permissions.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Permissions Requested (${data.permissions.size})".uppercase(Locale.US),
                                        style = NovaTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                        color = NeonCyan,
                                        modifier = Modifier.padding(top = NovaTokens.Spacing.xs)
                                    )
                                }
                                items(data.permissions.size) { idx ->
                                    val perm = data.permissions[idx]
                                    val cleanPermName = perm.substringAfterLast('.')
                                    NovaAnimatedItem(index = idx) {
                                        GlassCard(
                                            neonAccent = Color.Transparent
                                        ) {
                                            Column(modifier = Modifier.padding(NovaTokens.Spacing.sm)) {
                                                Text(
                                                    text = cleanPermName,
                                                    style = NovaTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = NeonCyan
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = perm,
                                                    style = NovaTypography.tagMono,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                )
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
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = NovaTypography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = NovaTypography.tagMono.copy(fontWeight = FontWeight.Bold),
            color = NeonCyan
        )
    }
}
