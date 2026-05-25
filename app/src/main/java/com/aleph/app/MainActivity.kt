package com.aleph.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.aleph.core.designsystem.theme.AlephTheme
import com.aleph.feature.dashboard.DashboardScreen
import com.aleph.feature.conversion.ConversionFlowScreen
import com.aleph.feature.conversion.MediaConversionScreen
import com.aleph.feature.conversion.DocumentConversionScreen
import com.aleph.feature.history.HistoryScreen
import com.aleph.feature.history.BiometricHelper

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var selectedCategory by remember { mutableStateOf<String?>(null) }
            var isHistoryAuthenticated by remember { mutableStateOf(false) }
            
            AlephTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (selectedCategory == null) {
                        DashboardScreen(onCategorySelected = { category -> 
                            if (category == "History") {
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
                        if (selectedCategory == "History" && isHistoryAuthenticated) {
                            HistoryScreen(onBack = { 
                                selectedCategory = null 
                                isHistoryAuthenticated = false
                            })
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
