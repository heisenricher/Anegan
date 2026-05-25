package com.anegan.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.database.DatabaseProvider
import com.anegan.core.database.ConversionHistoryEntity
import com.anegan.core.designsystem.theme.MidnightIndigo
import com.anegan.core.designsystem.theme.PlatinumSilver
import com.anegan.core.designsystem.theme.PureWhite
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val historyDao = remember { DatabaseProvider.getDatabase(context).historyDao() }
    val historyList by historyDao.getAllHistory().collectAsState(initial = emptyList())

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Conversion History",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp),
                color = MidnightIndigo
            )
            
            if (historyList.isNotEmpty()) {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            historyDao.clearHistory()
                        }
                    }
                ) {
                    Text("Clear All", color = MidnightIndigo, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No conversion history found", color = Color.Gray, fontSize = 16.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(historyList) { item ->
                    HistoryItemRow(item)
                }
            }
        }
    }
}

@Composable
fun HistoryItemRow(item: ConversionHistoryEntity) {
    val dateString = remember(item.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
        sdf.format(Date(item.timestamp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${item.originalFileName} → ${item.outputFileName.ifEmpty { "Failed" }}",
                fontWeight = FontWeight.Medium,
                color = MidnightIndigo,
                fontSize = 14.sp
            )
            val sizeMb = item.originalSize / (1024f * 1024f)
            val outputSizeMb = item.outputSize / (1024f * 1024f)
            val sizeText = if (item.status == "SUCCESS") {
                String.format("%.1f MB → %.1f MB", sizeMb, outputSizeMb)
            } else {
                String.format("%.1f MB", sizeMb)
            }
            Text("$dateString • $sizeText • ${item.outputFormat}", fontSize = 12.sp, color = Color.Gray)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (item.status == "SUCCESS") PlatinumSilver else MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center
        ) {
            val mark = if (item.status == "SUCCESS") "✓" else "✗"
            val color = if (item.status == "SUCCESS") MidnightIndigo else MaterialTheme.colorScheme.error
            Text(mark, color = color, fontWeight = FontWeight.Bold)
        }
    }
}
