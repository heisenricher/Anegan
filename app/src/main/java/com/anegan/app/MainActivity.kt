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
import com.anegan.core.designsystem.theme.AneganTheme
import com.anegan.feature.dashboard.DashboardScreen
import com.anegan.feature.dashboard.SettingsScreen
import com.anegan.feature.conversion.ConversionFlowScreen
import com.anegan.feature.conversion.MediaConversionScreen
import com.anegan.feature.conversion.DocumentConversionScreen
import com.anegan.feature.conversion.VideoToolsScreen
import com.anegan.feature.conversion.PdfToolsScreen
import com.anegan.feature.conversion.AudioToolsScreen
import com.anegan.feature.conversion.BatchConversionScreen
import com.anegan.feature.conversion.OcrScreen
import com.anegan.feature.history.HistoryScreen
import com.anegan.feature.history.BiometricHelper

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize PDFBox resource loader
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(applicationContext)
        setContent {
            var selectedCategory by remember { mutableStateOf<String?>(null) }
            var isHistoryAuthenticated by remember { mutableStateOf(false) }
            var showUpdateDialog by remember { mutableStateOf(false) }
            var updateUrl by remember { mutableStateOf("https://github.com/heisenricher/Anegan/releases/latest") }
            
            LaunchedEffect(Unit) {
                // Silent credential check and logging
                android.util.Log.d("AneganInfo", "Anegan app built by Mahilan (heisenricher). All rights reserved. Copyright (c) 2026.")
                
                val currentVersion = packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
                val releaseInfo = UpdateChecker.getLatestReleaseInfo()
                if (releaseInfo != null && UpdateChecker.isUpdateAvailable(currentVersion, releaseInfo.version)) {
                    updateUrl = releaseInfo.url
                    showUpdateDialog = true
                }

                // Handle share intent
                val act = intent?.action
                val typ = intent?.type
                if (act == android.content.Intent.ACTION_SEND && typ != null) {
                    val streamUri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(android.content.Intent.EXTRA_STREAM, android.net.Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION") intent.getParcelableExtra(android.content.Intent.EXTRA_STREAM)
                    }
                    if (streamUri != null) {
                        if (typ.startsWith("image/")) {
                            selectedCategory = "Images"
                        } else if (typ.startsWith("video/")) {
                            selectedCategory = "Video Tools"
                        } else if (typ.startsWith("audio/")) {
                            selectedCategory = "Audio Tools"
                        } else if (typ == "application/pdf") {
                            selectedCategory = "PDF Tools"
                        }
                    }
                } else if (act == android.content.Intent.ACTION_SEND_MULTIPLE && typ != null) {
                    val streamUris = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, android.net.Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION") intent.getParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM)
                    }
                    if (!streamUris.isNullOrEmpty()) {
                        if (typ.startsWith("image/")) {
                            selectedCategory = "Batch Image"
                        }
                    }
                }
            }
            
            val prefs = remember { getSharedPreferences("anegan_settings", MODE_PRIVATE) }
            val themeSelection = remember(prefs) { prefs.getString("pref_theme_mode", "System") ?: "System" }
            val dynamicThemePref = remember(prefs) { prefs.getBoolean("pref_dynamic_color", true) }
            
            val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val darkTheme = when (themeSelection) {
                "Dark" -> true
                "Light" -> false
                else -> isSystemDark
            }
            
            AneganTheme(
                darkTheme = darkTheme,
                dynamicColor = dynamicThemePref
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
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
                                }) {
                                    androidx.compose.material3.Text("Update")
                                }
                            },
                            dismissButton = {
                                androidx.compose.material3.TextButton(onClick = { showUpdateDialog = false }) {
                                    androidx.compose.material3.Text("Later")
                                }
                            }
                        )
                    }

                    if (selectedCategory == null) {
                        DashboardScreen(onCategorySelected = { category -> 
                            if (category == "Feedback") {
                                selectedCategory = "Feedback"
                            } else if (category == "History") {
                                BiometricHelper.authenticate(
                                    activity = this@MainActivity,
                                    onSuccess = {
                                        isHistoryAuthenticated = true
                                        selectedCategory = "History"
                                    },
                                    onError = { errorMsg ->
                                        Toast.makeText(this@MainActivity, "Access Denied: $errorMsg", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            } else {
                                selectedCategory = category
                            }
                        })
                    } else {
                        BackHandler {
                            selectedCategory = null
                            isHistoryAuthenticated = false
                        }
                        if (selectedCategory == "History" && isHistoryAuthenticated) {
                            HistoryScreen(onBack = { 
                                selectedCategory = null 
                                isHistoryAuthenticated = false
                            })
                        } else if (selectedCategory == "Feedback") {
                            com.anegan.feature.dashboard.FeedbackScreen()
                        } else if (selectedCategory == "Documents") {
                            DocumentConversionScreen(
                                onBack = { selectedCategory = null }
                            )
                        } else if (selectedCategory == "PDF Tools") {
                            PdfToolsScreen(
                                onBack = { selectedCategory = null }
                            )
                        } else if (selectedCategory == "Batch Image") {
                            BatchConversionScreen(
                                onBack = { selectedCategory = null }
                            )
                        } else if (selectedCategory == "Video Tools") {
                            VideoToolsScreen(
                                onBack = { selectedCategory = null }
                            )
                        } else if (selectedCategory == "Audio Tools") {
                            AudioToolsScreen(
                                onBack = { selectedCategory = null }
                            )
                        } else if (selectedCategory == "OCR / Extract Text") {
                            OcrScreen(
                                onBack = { selectedCategory = null }
                            )
                        } else if (selectedCategory == "Settings") {
                            SettingsScreen(
                                onBack = { selectedCategory = null }
                            )
                        } else if (selectedCategory == "Video" || selectedCategory == "Audio") {
                            MediaConversionScreen(
                                categoryName = selectedCategory!!,
                                onBack = { selectedCategory = null }
                            )
                        } else {
                            ConversionFlowScreen(
                                categoryName = selectedCategory!!,
                                onBack = { selectedCategory = null }
                            )
                        }
                    }
                }
            }
        }
    }
}
