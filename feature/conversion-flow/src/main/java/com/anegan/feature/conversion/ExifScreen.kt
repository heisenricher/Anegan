/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.conversion

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.conversion.ExifManager
import com.anegan.core.conversion.StorageManager
import com.anegan.core.database.DatabaseProvider
import com.anegan.core.database.ConversionHistoryEntity
import com.anegan.core.designsystem.theme.*
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExifScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileSize by remember { mutableStateOf<Long?>(null) }

    var isProcessing by remember { mutableStateOf(false) }
    var exifTags by remember { mutableStateOf<Map<String, Map<String, String>>?>(null) }

    // Comparative EXIF State
    var compareUri by remember { mutableStateOf<Uri?>(null) }
    var compareFileName by remember { mutableStateOf<String?>(null) }
    var compareExifTags by remember { mutableStateOf<Map<String, Map<String, String>>?>(null) }

    // Stripping Checkboxes State
    var stripLocation by remember { mutableStateOf(true) }
    var stripCamera by remember { mutableStateOf(false) }
    var stripDate by remember { mutableStateOf(false) }
    var stripAll by remember { mutableStateOf(false) }

    // Expand/Collapse state for categories
    val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }
    val isDark = isSystemInDarkTheme()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedUri = uri
        if (uri != null) {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (nameIndex != -1) selectedFileName = it.getString(nameIndex)
                    if (sizeIndex != -1) selectedFileSize = it.getLong(sizeIndex)
                }
            }
            exifTags = null
            compareUri = null
            compareFileName = null
            compareExifTags = null
            
            isProcessing = true
            coroutineScope.launch {
                try {
                    val tempFile = StorageManager.copyUriToTempFile(context, uri)
                    if (tempFile != null) {
                        val result = ExifManager().readExifMetadata(tempFile)
                        if (result.isSuccess) {
                            exifTags = result.getOrThrow()
                            // Expand all categories by default
                            exifTags?.keys?.forEach { expandedCategories[it] = true }
                            NovaHaptics.success(view)
                        } else {
                            Toast.makeText(context, "No EXIF metadata found", Toast.LENGTH_SHORT).show()
                            NovaHaptics.reject(view)
                        }
                        tempFile.delete()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    NovaHaptics.reject(view)
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    val compareImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        compareUri = uri
        if (uri != null) {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) compareFileName = it.getString(nameIndex)
                }
            }
            isProcessing = true
            coroutineScope.launch {
                try {
                    val tempFile = StorageManager.copyUriToTempFile(context, uri)
                    if (tempFile != null) {
                        val result = ExifManager().readExifMetadata(tempFile)
                        if (result.isSuccess) {
                            compareExifTags = result.getOrThrow()
                            NovaHaptics.success(view)
                        } else {
                            Toast.makeText(context, "No EXIF metadata found in comparison image", Toast.LENGTH_SHORT).show()
                            NovaHaptics.reject(view)
                        }
                        tempFile.delete()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    NovaHaptics.reject(view)
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    NovaBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                NovaTopBar(
                    title = "EXIF Metadata Viewer",
                    onBack = onBack,
                    neonAccent = NeonCyan
                )
            }
        ) { innerPadding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(scrollState)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // File Selector Cards Grid/Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        onClick = { imagePickerLauncher.launch("image/*") },
                        neonAccent = NeonCyan.copy(alpha = 0.4f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedFileName != null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🖼️ MAIN IMAGE", style = NovaTypography.tagMono, color = NeonCyan)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(selectedFileName!!, color = if (isDark) NovaFrostWhite else NovaDeepInk, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    val sizeMb = (selectedFileSize ?: 0L) / (1024f * 1024f)
                                    Text(String.format("%.2f MB", sizeMb), color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Rounded.PhotoLibrary,
                                        contentDescription = "Select Main Image",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("SELECT IMAGE", color = if (isDark) NovaFrostWhite else NovaDeepInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (selectedFileName != null) {
                        GlassCard(
                            modifier = Modifier.weight(1f),
                            onClick = { compareImagePickerLauncher.launch("image/*") },
                            neonAccent = if (compareFileName != null) NeonCyan else NeonCyan.copy(alpha = 0.2f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (compareFileName != null) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("⚖️ COMPARE IMAGE", style = NovaTypography.tagMono, color = NeonCyan)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(compareFileName!!, color = if (isDark) NovaFrostWhite else NovaDeepInk, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                        Text("COMPARE ACTIVE", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Rounded.PhotoLibrary,
                                            contentDescription = "Compare EXIF",
                                            tint = NeonCyan,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("COMPARE EXIF", color = if (isDark) NovaFrostWhite else NovaDeepInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                if (isProcessing) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NeonCyan)
                    }
                }

                // Comparative View Table
                if (compareExifTags != null && exifTags != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        neonAccent = NeonCyan,
                        enableGlow = true
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "EXIF COMPARISON",
                                    style = NovaTypography.displaySmall.copy(fontSize = 16.sp),
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "CLEAR COMPARE",
                                    color = if (isDark) NovaFrostWhite else NovaDeepInk,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable {
                                            NovaHaptics.click(view)
                                            compareUri = null
                                            compareFileName = null
                                            compareExifTags = null
                                        }
                                        .padding(4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            val allKeys = (exifTags!!.keys + compareExifTags!!.keys).distinct()
                            allKeys.forEach { cat ->
                                Text(
                                    text = cat.uppercase(),
                                    fontWeight = FontWeight.Black,
                                    fontFamily = SpaceGrotesk,
                                    fontSize = 13.sp,
                                    color = NeonCyan,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                                val catTags1 = exifTags!![cat] ?: emptyMap()
                                val catTags2 = compareExifTags!![cat] ?: emptyMap()
                                val allTags = (catTags1.keys + catTags2.keys).distinct()

                                allTags.forEach { tag ->
                                    val val1 = catTags1[tag] ?: "-"
                                    val val2 = catTags2[tag] ?: "-"
                                    val isDifferent = val1 != val2

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (isDifferent) {
                                                    if (isDark) NeonCyan.copy(alpha = 0.15f) else NeonCyan.copy(alpha = 0.08f)
                                                } else Color.Transparent,
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(vertical = 8.dp, horizontal = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = tag,
                                            color = if (isDark) NovaFrostWhite.copy(alpha = 0.7f) else NovaDeepInk.copy(alpha = 0.6f),
                                            fontSize = 11.sp,
                                            fontFamily = JetBrainsMono,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = val1,
                                            color = if (isDark) NovaFrostWhite else NovaDeepInk,
                                            fontSize = 11.sp,
                                            fontFamily = JetBrainsMono,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1.2f)
                                        )
                                        Text(
                                            text = val2,
                                            color = if (isDark) NovaFrostWhite else NovaDeepInk,
                                            fontSize = 11.sp,
                                            fontFamily = JetBrainsMono,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1.2f)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                } else {
                    // Standard Categorized View
                    exifTags?.let { tags ->
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        tags.forEach { (categoryName, catTags) ->
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                neonAccent = NeonCyan.copy(alpha = 0.3f)
                            ) {
                                Column {
                                    // Category Title Header
                                    val isExpanded = expandedCategories[categoryName] ?: false
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                NovaHaptics.toggle(view)
                                                expandedCategories[categoryName] = !isExpanded
                                            }
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = categoryName.uppercase(),
                                            fontWeight = FontWeight.Black,
                                            fontFamily = SpaceGrotesk,
                                            fontSize = 14.sp,
                                            color = NeonCyan
                                        )
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                                            contentDescription = "Toggle Section",
                                            tint = NeonCyan
                                        )
                                    }

                                    AnimatedVisibility(
                                        visible = isExpanded,
                                        enter = expandVertically(),
                                        exit = shrinkVertically()
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                            catTags.forEach { (label, value) ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = label,
                                                        color = if (isDark) NovaFrostWhite.copy(alpha = 0.7f) else NovaDeepInk.copy(alpha = 0.6f),
                                                        fontSize = 12.sp,
                                                        fontFamily = JetBrainsMono
                                                    )
                                                    Text(
                                                        text = value,
                                                        color = if (isDark) NovaFrostWhite else NovaDeepInk,
                                                        fontSize = 12.sp,
                                                        fontFamily = JetBrainsMono,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }

                                            // Special map mapping inside Location Metadata
                                            if (categoryName == "Location Metadata" && catTags.containsKey("Latitude") && catTags.containsKey("Longitude")) {
                                                Spacer(modifier = Modifier.height(12.dp))
                                                val lat = catTags["Latitude"]?.replace("°", "")?.trim() ?: "0.0"
                                                val lng = catTags["Longitude"]?.replace("°", "")?.trim() ?: "0.0"
                                                
                                                NovaPrimaryButton(
                                                    text = "View on Map",
                                                    neonColor = NeonCyan,
                                                    onClick = {
                                                        NovaHaptics.confirm(view)
                                                        val intentUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(Photo Location)")
                                                        val mapIntent = Intent(Intent.ACTION_VIEW, intentUri)
                                                        if (mapIntent.resolveActivity(context.packageManager) != null) {
                                                            context.startActivity(mapIntent)
                                                        } else {
                                                            // Try generic geo intent or web fallback
                                                            try {
                                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")))
                                                            } catch (e: Exception) {
                                                                Toast.makeText(context, "No map application found", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    },
                                                    icon = Icons.Default.Map,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (tags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Selective Stripping Switches Card
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                neonAccent = NeonCyan.copy(alpha = 0.3f)
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Text(
                                        text = "SELECT METADATA TO STRIP",
                                        style = NovaTypography.tagMono,
                                        color = NeonCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "GPS Location Coordinates",
                                            fontSize = 13.sp,
                                            color = if (isDark) NovaFrostWhite else NovaDeepInk
                                        )
                                        NovaSwitch(
                                            checked = stripLocation && !stripAll,
                                            onCheckedChange = {
                                                if (!stripAll) {
                                                    NovaHaptics.toggle(view)
                                                    stripLocation = it
                                                }
                                            },
                                            neonColor = NeonCyan
                                        )
                                    }
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Camera Model & Tech Info",
                                            fontSize = 13.sp,
                                            color = if (isDark) NovaFrostWhite else NovaDeepInk
                                        )
                                        NovaSwitch(
                                            checked = stripCamera && !stripAll,
                                            onCheckedChange = {
                                                if (!stripAll) {
                                                    NovaHaptics.toggle(view)
                                                    stripCamera = it
                                                }
                                            },
                                            neonColor = NeonCyan
                                        )
                                    }
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Date & Time Metadata",
                                            fontSize = 13.sp,
                                            color = if (isDark) NovaFrostWhite else NovaDeepInk
                                        )
                                        NovaSwitch(
                                            checked = stripDate && !stripAll,
                                            onCheckedChange = {
                                                if (!stripAll) {
                                                    NovaHaptics.toggle(view)
                                                    stripDate = it
                                                }
                                            },
                                            neonColor = NeonCyan
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Strip All (De-identify Completely)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) NovaFrostWhite else NovaDeepInk
                                        )
                                        NovaSwitch(
                                            checked = stripAll,
                                            onCheckedChange = {
                                                NovaHaptics.toggle(view)
                                                stripAll = it
                                            },
                                            neonColor = NeonCyan
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            NovaPrimaryButton(
                                text = "Strip EXIF & Save Copy",
                                neonColor = NeonCyan,
                                enabled = !isProcessing && (stripLocation || stripCamera || stripDate || stripAll),
                                isLoading = isProcessing,
                                onClick = {
                                    val uri = selectedUri
                                    if (uri == null) return@NovaPrimaryButton
                                    isProcessing = true
                                    coroutineScope.launch {
                                        try {
                                            val tempFile = StorageManager.copyUriToTempFile(context, uri)
                                            if (tempFile == null) {
                                                isProcessing = false
                                                Toast.makeText(context, "Failed to copy file", Toast.LENGTH_SHORT).show()
                                                return@launch
                                            }
                                            
                                            val categories = mutableSetOf<String>()
                                            if (stripAll) {
                                                categories.add("ALL")
                                            } else {
                                                if (stripLocation) categories.add("LOCATION")
                                                if (stripCamera) categories.add("CAMERA")
                                                if (stripDate) categories.add("DATE")
                                            }

                                            val result = ExifManager().stripSelectiveMetadata(tempFile, categories)
                                            isProcessing = false
                                            if (result.isSuccess) {
                                                val strippedFile = result.getOrThrow()
                                                Toast.makeText(context, "Selected EXIF Metadata Stripped Successfully!", Toast.LENGTH_LONG).show()
                                                NovaHaptics.success(view)
                                                
                                                val historyDao = DatabaseProvider.getDatabase(context).historyDao()
                                                historyDao.insertConversion(
                                                    ConversionHistoryEntity(
                                                        originalFileName = selectedFileName ?: tempFile.name,
                                                        outputFileName = strippedFile.name,
                                                        originalFormat = tempFile.extension.uppercase(),
                                                        outputFormat = strippedFile.extension.uppercase(),
                                                        status = "SUCCESS",
                                                        timestamp = System.currentTimeMillis(),
                                                        originalSize = selectedFileSize ?: tempFile.length(),
                                                        outputSize = strippedFile.length(),
                                                        outputPath = strippedFile.absolutePath
                                                    )
                                                )
                                                exifTags = null
                                                selectedUri = null
                                                selectedFileName = null
                                                selectedFileSize = null
                                            } else {
                                                val ex = result.exceptionOrNull()
                                                Toast.makeText(context, "Failed: ${ex?.message}", Toast.LENGTH_LONG).show()
                                                NovaHaptics.reject(view)
                                            }
                                            tempFile.delete()
                                        } catch (e: Exception) {
                                            isProcessing = false
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                            NovaHaptics.reject(view)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
