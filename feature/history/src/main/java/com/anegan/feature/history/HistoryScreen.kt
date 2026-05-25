package com.anegan.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
fun HistoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
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

            Text(
                text = "Conversion History",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp),
                color = MidnightIndigo
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Placeholder for History List mapped to Room Database
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(5) { index ->
                HistoryItemPlaceholder(index)
            }
        }
    }
}

@Composable
fun HistoryItemPlaceholder(index: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("File_$index.jpg -> File_$index.webp", fontWeight = FontWeight.Medium, color = MidnightIndigo)
            Text("Today, 10:45 AM • 1.2 MB", fontSize = 12.sp, color = Color.Gray)
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PlatinumSilver),
            contentAlignment = Alignment.Center
        ) {
            Text("✓", color = MidnightIndigo, fontWeight = FontWeight.Bold)
        }
    }
}
