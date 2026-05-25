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
import com.anegan.feature.conversion.ConversionFlowScreen
import com.anegan.feature.conversion.MediaConversionScreen
import com.anegan.feature.conversion.DocumentConversionScreen
import com.anegan.feature.history.HistoryScreen
import com.anegan.feature.history.BiometricHelper

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var selectedCategory by remember { mutableStateOf<String?>(null) }
            var isHistoryAuthenticated by remember { mutableStateOf(false) }
            var showUpdateDialog by remember { mutableStateOf(false) }
            
            LaunchedEffect(Unit) {
                val currentVersion = packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
                val latestVersion = UpdateChecker.getLatestReleaseVersion()
                if (latestVersion != null && UpdateChecker.isUpdateAvailable(currentVersion, latestVersion)) {
                    showUpdateDialog = true
                }
            }
            
            AneganTheme {
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
                                        android.net.Uri.parse("https://github.com/heisenricher/Anegan/releases/latest")
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
