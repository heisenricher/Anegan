package com.anegan.feature.conversion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.MidnightIndigo
import com.anegan.core.designsystem.theme.PureWhite

@Composable
fun MediaConversionScreen(
    categoryName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isVideo = categoryName == "Video"
    var selectedFormat by remember { mutableStateOf(if (isVideo) "MP4" else "MP3") }
    var selectedResolution by remember { mutableStateOf("Original") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MidnightIndigo)) {
                Text("Back")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "$categoryName Settings",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp),
                color = MidnightIndigo
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Placeholder for File Picker UI
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Text("Tap to Select Media File", color = MidnightIndigo)
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Target Format", style = MaterialTheme.typography.titleLarge, color = MidnightIndigo)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val formats = if (isVideo) listOf("MP4", "MKV", "AVI") else listOf("MP3", "M4A", "FLAC")
            formats.forEach { format ->
                Button(
                    onClick = { selectedFormat = format },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedFormat == format) MidnightIndigo else MaterialTheme.colorScheme.surface,
                        contentColor = if (selectedFormat == format) PureWhite else MidnightIndigo
                    )
                ) {
                    Text(format)
                }
            }
        }

        if (isVideo) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Resolution", style = MaterialTheme.typography.titleLarge, color = MidnightIndigo)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Original", "1080p", "720p").forEach { res ->
                    Button(
                        onClick = { selectedResolution = res },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedResolution == res) MidnightIndigo else MaterialTheme.colorScheme.surface,
                            contentColor = if (selectedResolution == res) PureWhite else MidnightIndigo
                        )
                    ) {
                        Text(res)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { /* TODO Execute FFmpeg */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MidnightIndigo, contentColor = PureWhite)
        ) {
            val action = if (categoryName == "Audio") "Extract" else "Convert"
            Text("$action to $selectedFormat", style = MaterialTheme.typography.titleLarge)
        }
    }
}
