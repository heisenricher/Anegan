/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 *
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.anegan.core.designsystem.theme.AneganTheme
import com.anegan.feature.dashboard.DashboardScreen
import com.anegan.feature.dashboard.SurvivalLibraryScreen
import com.anegan.feature.dashboard.SettingsScreen
import com.anegan.feature.dashboard.OnboardingScreen
import com.anegan.feature.conversion.ConversionFlowScreen
import com.anegan.feature.conversion.MediaConversionScreen
import com.anegan.feature.conversion.DocumentConversionScreen
import com.anegan.feature.conversion.VideoToolsScreen
import com.anegan.feature.conversion.PdfToolsScreen
import com.anegan.feature.conversion.AudioToolsScreen
import com.anegan.feature.conversion.BatchConversionScreen
import com.anegan.feature.conversion.OcrScreen
import com.anegan.feature.conversion.ExifScreen
import com.anegan.feature.conversion.DevToolsScreen
import com.anegan.feature.conversion.ColorPickerScreen
import com.anegan.feature.conversion.UnitConverterScreen
import com.anegan.feature.conversion.BackgroundRemoverScreen
import com.anegan.feature.conversion.ImageWatermarkScreen
import com.anegan.feature.conversion.PdfPageOrganizerScreen
import com.anegan.feature.conversion.PdfReaderEditorScreen
import com.anegan.feature.conversion.CompassScreen
import com.anegan.feature.conversion.CalculatorScreen
import com.anegan.feature.conversion.FlashlightScreen
import com.anegan.feature.conversion.CurrencyConverterScreen
import com.anegan.feature.conversion.OfflineCommScreen
import com.anegan.feature.history.HistoryScreen
import com.anegan.feature.history.BiometricHelper
import com.anegan.feature.notes.NoteListScreen
import com.anegan.feature.notes.NoteEditorScreen
import com.anegan.feature.vault.VaultScreen
import com.anegan.feature.filemanager.FileManagerScreen
import com.anegan.feature.documentreader.DocumentHubScreen
import com.anegan.feature.wifitransfer.WifiTransferScreen
import com.anegan.feature.apktools.ApkToolsScreen
import com.anegan.feature.saver.SmartSaverScreen
import com.anegan.feature.smbshare.SmbShareScreen

class MainActivity : FragmentActivity() {
    private val intentFlow = kotlinx.coroutines.flow.MutableSharedFlow<android.content.Intent>(extraBufferCapacity = 1)

    private fun saveSharedStreamToCache(uri: android.net.Uri, type: String?): String? {
        val contentResolver = contentResolver ?: return null
        val ext = when {
            type == "application/pdf" -> "pdf"
            type == "application/epub+zip" -> "epub"
            type == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx"
            type?.startsWith("text/") == true -> "txt"
            else -> {
                var inferredExt = "txt"
                try {
                    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1 && cursor.moveToFirst()) {
                            val name = cursor.getString(nameIndex)
                            val fileExt = name.substringAfterLast('.', "")
                            if (fileExt.isNotBlank()) {
                                inferredExt = fileExt
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore
                }
                inferredExt
            }
        }
        
        try {
            val cacheFile = java.io.File(cacheDir, "shared_document.$ext")
            contentResolver.openInputStream(uri)?.use { input ->
                cacheFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            return cacheFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intentFlow.tryEmit(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize PDFBox resource loader
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(applicationContext)

        // Setup dynamic launcher shortcuts
        ShortcutHelper.setupShortcuts(this)
        intent?.let { intentFlow.tryEmit(it) }

        setContent {
            var selectedCategory by remember { mutableStateOf<String?>(null) }
            var presetParams by remember { mutableStateOf<Map<String, String>?>(null) }
            var isHistoryAuthenticated by remember { mutableStateOf(false) }
            var showUpdateDialog by remember { mutableStateOf(false) }
            var updateUrl by remember { mutableStateOf("https://github.com/heisenricher/Anegan/releases/latest") }
            // Notes sub-navigation: null = list, non-null = editor (null string = new note, noteId = existing)
            var selectedNoteId by remember { mutableStateOf<String?>(null) }
            var openNoteEditor by remember { mutableStateOf(false) }

            val currentIntent by intentFlow.collectAsState(initial = intent)

            LaunchedEffect(currentIntent) {
                val intentVal = currentIntent ?: return@LaunchedEffect
                android.util.Log.d("AneganInfo", "Anegan v2.5 built by Mahilan (heisenricher). All rights reserved. Copyright (c) 2026.")

                val currentVersion = packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
                val releaseInfo = UpdateChecker.getLatestReleaseInfo()
                if (releaseInfo != null && UpdateChecker.isUpdateAvailable(currentVersion, releaseInfo.version)) {
                    updateUrl = releaseInfo.url
                    showUpdateDialog = true
                }

                // Handle launcher shortcuts and share intents
                val act = intentVal.action
                val typ = intentVal.type
                when {
                    act == "com.anegan.action.SHORTCUT_IMAGES"  -> selectedCategory = "Images"
                    act == "com.anegan.action.SHORTCUT_PDF"     -> selectedCategory = "PDF Tools"
                    act == "com.anegan.action.SHORTCUT_HISTORY" -> selectedCategory = "History"
                    act == "com.anegan.action.SHORTCUT_NOTES"   -> selectedCategory = "Notes"
                    act == "com.anegan.action.SHORTCUT_COMPASS" -> selectedCategory = "Compass"
                    act == android.content.Intent.ACTION_SEND && typ != null -> {
                        val streamUri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            intentVal.getParcelableExtra(android.content.Intent.EXTRA_STREAM, android.net.Uri::class.java)
                        } else {
                            @Suppress("DEPRECATION") intentVal.getParcelableExtra(android.content.Intent.EXTRA_STREAM)
                        }
                        if (streamUri != null) {
                            when {
                                typ.startsWith("image/") -> {
                                    selectedCategory = "Images"
                                }
                                typ.startsWith("video/") -> {
                                    selectedCategory = "Video Tools"
                                }
                                typ.startsWith("audio/") -> {
                                    selectedCategory = "Audio Tools"
                                }
                                typ == "application/pdf" || typ.startsWith("text/") || typ.contains("epub") || typ.contains("officedocument") || typ.contains("word") -> {
                                    val path = saveSharedStreamToCache(streamUri, typ)
                                    if (path != null) {
                                        presetParams = mapOf("initialFilePath" to path)
                                        selectedCategory = "Document Hub"
                                    } else {
                                        selectedCategory = "Document Hub"
                                    }
                                }
                            }
                        }
                    }
                    act == android.content.Intent.ACTION_SEND_MULTIPLE && typ != null -> {
                        val streamUris = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            intentVal.getParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, android.net.Uri::class.java)
                        } else {
                            @Suppress("DEPRECATION") intentVal.getParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM)
                        }
                        if (!streamUris.isNullOrEmpty() && typ.startsWith("image/")) {
                            selectedCategory = "Batch Image"
                        }
                    }
                }
            }

            val prefs = remember { getSharedPreferences("anegan_settings", MODE_PRIVATE) }
            var showOnboarding by remember {
                mutableStateOf(prefs.getBoolean("pref_show_onboarding", true))
            }

            // Instantly reactive personalization states
            var themeSelection by remember {
                mutableStateOf(prefs.getString("pref_theme_mode", "System") ?: "System")
            }
            var dynamicThemePref by remember {
                mutableStateOf(prefs.getBoolean("pref_dynamic_color", true))
            }
            var amoledThemePref by remember {
                mutableStateOf(prefs.getBoolean("pref_amoled_theme", false))
            }
            var customFontPref by remember {
                mutableStateOf(prefs.getString("pref_custom_font", "Default") ?: "Default")
            }

            DisposableEffect(prefs) {
                val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                    when (key) {
                        "pref_theme_mode" -> {
                            themeSelection = sharedPreferences.getString("pref_theme_mode", "System") ?: "System"
                        }
                        "pref_dynamic_color" -> {
                            dynamicThemePref = sharedPreferences.getBoolean("pref_dynamic_color", true)
                        }
                        "pref_amoled_theme" -> {
                            amoledThemePref = sharedPreferences.getBoolean("pref_amoled_theme", false)
                        }
                        "pref_custom_font" -> {
                            customFontPref = sharedPreferences.getString("pref_custom_font", "Default") ?: "Default"
                        }
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose {
                    prefs.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }

            LaunchedEffect(selectedCategory, presetParams) {
                if (selectedCategory == "Notes" && presetParams?.containsKey("noteId") == true) {
                    selectedNoteId = presetParams?.get("noteId")
                    openNoteEditor = true
                }
            }

            val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val darkTheme = when (themeSelection) {
                "Dark"  -> true
                "Light" -> false
                else    -> isSystemDark
            }

            val haptic = LocalHapticFeedback.current

            AneganTheme(
                darkTheme = darkTheme,
                dynamicColor = dynamicThemePref,
                amoledDark = amoledThemePref,
                fontName = customFontPref
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (showOnboarding) {
                        OnboardingScreen(
                            onFinished = {
                                showOnboarding = false
                                prefs.edit().putBoolean("pref_show_onboarding", false).apply()
                            }
                        )
                    } else {
                        if (showUpdateDialog) {
                            androidx.compose.material3.AlertDialog(
                                onDismissRequest = { showUpdateDialog = false },
                                title = { androidx.compose.material3.Text("Update Available!") },
                                text = { androidx.compose.material3.Text("A new version of Anegan is ready. Update now for the latest features.") },
                                confirmButton = {
                                    androidx.compose.material3.TextButton(onClick = {
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse(updateUrl)
                                        )
                                        startActivity(intent)
                                        showUpdateDialog = false
                                    }) { androidx.compose.material3.Text("Update") }
                                },
                                dismissButton = {
                                    androidx.compose.material3.TextButton(onClick = { showUpdateDialog = false }) {
                                        androidx.compose.material3.Text("Later")
                                    }
                                }
                            )
                        }

                        AnimatedContent(
                            targetState = selectedCategory,
                            transitionSpec = {
                                if (targetState != null) {
                                    // Opening a screen: slide in from right, fade in
                                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                        slideOutHorizontally { width -> -width / 4 } + fadeOut()
                                    )
                                } else {
                                    // Going back: slide out to right, fade out
                                    (slideInHorizontally { width -> -width / 4 } + fadeIn()).togetherWith(
                                        slideOutHorizontally { width -> width } + fadeOut()
                                    )
                                }
                            },
                            label = "CategoryTransition"
                        ) { targetCategory ->
                            if (targetCategory == null) {
                                DashboardScreen(
                                    onCategorySelected = { category ->
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        when (category) {
                                            "Feedback" -> selectedCategory = "Feedback"
                                            "History"  -> BiometricHelper.authenticate(
                                                activity = this@MainActivity,
                                                onSuccess = {
                                                    isHistoryAuthenticated = true
                                                    selectedCategory = "History"
                                                },
                                                onError = { errorMsg ->
                                                    Toast.makeText(this@MainActivity, "Access Denied: $errorMsg", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                            else -> selectedCategory = category
                                        }
                                    },
                                    onPresetSelected = { category, params ->
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        presetParams = params
                                        selectedCategory = category
                                    }
                                )
                            } else {
                                BackHandler {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedCategory = null
                                    presetParams = null
                                    isHistoryAuthenticated = false
                                }
                                when {
                                    targetCategory == "History" && isHistoryAuthenticated ->
                                        HistoryScreen(onBack = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedCategory = null; presetParams = null; isHistoryAuthenticated = false
                                        })
                                    targetCategory == "Document Hub" -> {
                                        val initialPath = presetParams?.get("initialFilePath")
                                        DocumentHubScreen(
                                            initialFilePath = initialPath,
                                            onBack = { 
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                selectedCategory = null
                                                presetParams = null 
                                            }
                                        )
                                    }
                                     targetCategory == "Wi-Fi & FTP Transfer" ->
                                        WifiTransferScreen(onBack = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedCategory = null })
                                    targetCategory == "SMB File Sharing" ->
                                        SmbShareScreen(onBack = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedCategory = null })
                                    targetCategory == "APK Extractor" ->
                                        ApkToolsScreen(onBack = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedCategory = null })
                                    targetCategory == "Smart Saver" ->
                                        SmartSaverScreen(
                                            onPresetSelected = { category, params ->
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                presetParams = params
                                                selectedCategory = category
                                            },
                                            onBack = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedCategory = null }
                                        )
                                    targetCategory == "Feedback" ->
                                        com.anegan.feature.dashboard.FeedbackScreen()
                                    targetCategory == "Documents" ->
                                        DocumentConversionScreen(onBack = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedCategory = null; presetParams = null })
                                    targetCategory == "PDF Tools" ->
                                        PdfToolsScreen(presetParams = presetParams, onBack = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedCategory = null; presetParams = null })
                                    targetCategory == "Batch Image" ->
                                        BatchConversionScreen(onBack = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedCategory = null; presetParams = null })
                                    targetCategory == "Video Tools" ->
                                        VideoToolsScreen(presetParams = presetParams, onBack = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedCategory = null; presetParams = null })
                                    targetCategory == "Audio Tools" ->
                                        AudioToolsScreen(presetParams = presetParams, onBack = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedCategory = null; presetParams = null })
                                    targetCategory == "OCR / Extract Text" ->
                                        OcrScreen(onBack = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedCategory = null; presetParams = null })
                                    targetCategory == "EXIF Metadata" ->
                                        ExifScreen(onBack = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedCategory = null; presetParams = null })
                                    targetCategory == "Developer Tools" ->
                                        DevToolsScreen(onBack = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedCategory = null; presetParams = null })
                                    targetCategory == "Color Picker" ->
                                        ColorPickerScreen(onBack = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedCategory = null; presetParams = null })
                                    targetCategory == "Unit Converter" ->
                                        UnitConverterScreen(onBack = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedCategory = null; presetParams = null })
                                    targetCategory == "AI Background Remover" ->
                                        BackgroundRemoverScreen(onBack = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedCategory = null; presetParams = null })
                                    targetCategory == "Image Watermark" ->
                                        ImageWatermarkScreen(onBack = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedCategory = null; presetParams = null })
                                     targetCategory == "PDF Organizer" || targetCategory == "PDF Reader & Editor" ->
                                         PdfReaderEditorScreen(onBack = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedCategory = null; presetParams = null })
                                     targetCategory == "Compass" ->
                                         CompassScreen(onBack = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedCategory = null; presetParams = null })
                                    targetCategory == "Calculator" ->
                                        CalculatorScreen(onBack = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedCategory = null; presetParams = null })
                                    targetCategory == "Flashlight" ->
                                        FlashlightScreen(onBack = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedCategory = null; presetParams = null })
                                    targetCategory == "Currency Converter" ->
                                        CurrencyConverterScreen(onBack = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedCategory = null; presetParams = null })
                                    targetCategory == "Offline Comm" ->
                                        OfflineCommScreen(onBack = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedCategory = null; presetParams = null })
                                     targetCategory == "Settings" ->
                                        SettingsScreen(onBack = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedCategory = null; presetParams = null })
                                    targetCategory == "Notes" && openNoteEditor ->
                                        NoteEditorScreen(
                                            noteId = selectedNoteId,
                                            onBack = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); openNoteEditor = false; selectedNoteId = null }
                                        )
                                    targetCategory == "Notes" ->
                                        NoteListScreen(
                                            onBack = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedCategory = null },
                                            onOpenNote = { noteId ->
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                selectedNoteId = noteId
                                                openNoteEditor = true
                                            }
                                        )
                                    targetCategory == "Vault" ->
                                        VaultScreen(onBack = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedCategory = null })
                                    targetCategory == "File Manager" ->
                                        FileManagerScreen(onBack = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedCategory = null })
                                    targetCategory == "Video" || targetCategory == "Audio" ->
                                        MediaConversionScreen(
                                            categoryName = targetCategory,
                                            onBack = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedCategory = null; presetParams = null }
                                        )
                                    else ->
                                        ConversionFlowScreen(
                                            categoryName = targetCategory,
                                            presetParams = if (targetCategory == "Images") presetParams else null,
                                            onBack = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedCategory = null; presetParams = null }
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
