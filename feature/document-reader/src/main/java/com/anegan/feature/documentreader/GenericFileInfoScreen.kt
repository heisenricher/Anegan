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
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenericFileInfoScreen(
    filePath: String,
    onBack: () -> Unit,
    onOpenAsText: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val file = remember { File(filePath) }

    var md5Hash by remember { mutableStateOf("Calculating...") }
    var sha256Hash by remember { mutableStateOf("Calculating...") }
    var mimeType by remember { mutableStateOf("unknown") }
    var isCalculatingHashes by remember { mutableStateOf(true) }

    LaunchedEffect(filePath) {
        withContext(Dispatchers.IO) {
            val calculatedMd5 = try {
                val digest = MessageDigest.getInstance("MD5")
                file.inputStream().use { input ->
                    val buffer = ByteArray(8192)
                    var bytesRead = input.read(buffer)
                    while (bytesRead != -1) {
                        digest.update(buffer, 0, bytesRead)
                        bytesRead = input.read(buffer)
                    }
                }
                digest.digest().joinToString("") { "%02x".format(it) }
            } catch (e: Exception) {
                "Error calculating MD5"
            }

            val calculatedSha256 = try {
                val digest = MessageDigest.getInstance("SHA-256")
                file.inputStream().use { input ->
                    val buffer = ByteArray(8192)
                    var bytesRead = input.read(buffer)
                    while (bytesRead != -1) {
                        digest.update(buffer, 0, bytesRead)
                        bytesRead = input.read(buffer)
                    }
                }
                digest.digest().joinToString("") { "%02x".format(it) }
            } catch (e: Exception) {
                "Error calculating SHA-256"
            }

            val calculatedMime = try {
                val extension = file.extension.lowercase(Locale.US)
                android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
            } catch (e: Exception) {
                "application/octet-stream"
            }

            withContext(Dispatchers.Main) {
                md5Hash = calculatedMd5
                sha256Hash = calculatedSha256
                mimeType = calculatedMime
                isCalculatingHashes = false
            }
        }
    }

    BackHandler {
        onBack()
    }

    NovaBackground {
        Scaffold(
            topBar = {
                NovaTopBar(
                    title = "File Info",
                    onBack = onBack,
                    neonAccent = NeonCyan
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = NovaTokens.Spacing.xl, vertical = NovaTokens.Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(NovaTokens.Spacing.md)
                ) {
                    // Summary Card
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            neonAccent = NeonCyan.copy(alpha = 0.2f),
                            cornerRadius = NovaTokens.Radius.lg
                        ) {
                            Row(
                                modifier = Modifier.padding(NovaTokens.Spacing.md),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(NeonCyan.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Description,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(NovaTokens.Spacing.md))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.name,
                                        style = NovaTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSystemInDarkTheme()) NovaFrostWhite else NovaDeepInk,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val sizeMb = file.length() / (1024f * 1024f)
                                    Text(
                                        text = if (sizeMb >= 0.1f) String.format(Locale.US, "%.2f MB", sizeMb) else "${file.length()} Bytes",
                                        style = NovaTypography.tagMono,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }

                    // File Details List Card
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            neonAccent = NeonCyan.copy(alpha = 0.15f),
                            cornerRadius = NovaTokens.Radius.lg
                        ) {
                            Column(
                                modifier = Modifier.padding(NovaTokens.Spacing.md),
                                verticalArrangement = Arrangement.spacedBy(NovaTokens.Spacing.sm)
                            ) {
                                Text(
                                    text = "Details",
                                    style = NovaTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = NeonCyan
                                )

                                DetailRow(label = "Extension", value = file.extension.ifBlank { "none" })
                                DetailRow(label = "MIME Type", value = mimeType)
                                
                                val lastModStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(file.lastModified()))
                                DetailRow(label = "Last Modified", value = lastModStr)
                                
                                DetailRow(label = "Full Path", value = file.absolutePath, isMonospace = true)
                            }
                        }
                    }

                    // Integrity Hashes Card
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            neonAccent = NeonCyan.copy(alpha = 0.15f),
                            cornerRadius = NovaTokens.Radius.lg
                        ) {
                            Column(
                                modifier = Modifier.padding(NovaTokens.Spacing.md),
                                verticalArrangement = Arrangement.spacedBy(NovaTokens.Spacing.sm)
                            ) {
                                Text(
                                    text = "Checksum Integrity",
                                    style = NovaTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = NeonCyan
                                )

                                DetailRow(
                                    label = "MD5 Hash",
                                    value = md5Hash,
                                    isMonospace = true
                                )
                                
                                DetailRow(
                                    label = "SHA-256 Hash",
                                    value = sha256Hash,
                                    isMonospace = true
                                )
                            }
                        }
                    }
                }

                // Bottom Buttons Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(NovaTokens.Spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(NovaTokens.Spacing.xs)
                ) {
                    NovaPrimaryButton(
                        text = "Open as Text File",
                        neonColor = NeonCyan,
                        onClick = onOpenAsText,
                        icon = Icons.Rounded.Article,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(NovaTokens.Spacing.xs)
                    ) {
                        NovaSecondaryButton(
                            text = "Share",
                            neonColor = NeonCyan,
                            icon = Icons.Rounded.Share,
                            onClick = {
                                try {
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = mimeType
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share file"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error sharing file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        NovaSecondaryButton(
                            text = "Open With",
                            neonColor = NeonCyan,
                            icon = Icons.Rounded.OpenInNew,
                            onClick = {
                                try {
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, mimeType)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(viewIntent, "Open file with..."))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error opening file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    isMonospace: Boolean = false
) {
    val isDark = isSystemInDarkTheme()
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(Locale.US),
            style = NovaTypography.tagMono,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(NovaTokens.Spacing.xxs))
        Text(
            text = value,
            style = if (isMonospace) NovaTypography.codeMono else NovaTypography.bodySmall,
            color = if (isDark) NovaFrostWhite else NovaDeepInk
        )
    }
}
