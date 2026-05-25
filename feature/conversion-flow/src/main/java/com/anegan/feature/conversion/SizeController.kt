package com.anegan.feature.conversion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.MidnightIndigo
import com.anegan.core.designsystem.theme.PlatinumSilver

@Composable
fun SizeController(
    modifier: Modifier = Modifier,
    initialQuality: Float = 0.8f,
    onQualityChanged: (Float) -> Unit
) {
    var quality by remember { mutableStateOf(initialQuality) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Quality & Size",
                    style = MaterialTheme.typography.titleLarge,
                    color = MidnightIndigo
                )
                Text(
                    text = "Adjust the compression level",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 12.sp),
                    color = Color.Gray
                )
            }
            // Displays exact percentage
            Text(
                text = "${(quality * 100).toInt()}%",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MidnightIndigo
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Slider(
            value = quality,
            onValueChange = { 
                quality = it
                onQualityChanged(it)
            },
            valueRange = 0.01f..1f,
            colors = SliderDefaults.colors(
                thumbColor = MidnightIndigo,
                activeTrackColor = MidnightIndigo,
                inactiveTrackColor = PlatinumSilver
            )
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Smallest File", fontSize = 12.sp, color = Color.Gray)
            Text("Best Quality", fontSize = 12.sp, color = Color.Gray)
        }
    }
}
