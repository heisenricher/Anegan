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
fun DocumentConversionScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTool by remember { mutableStateOf("Convert to PDF") }

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
                text = "Document Tools",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp),
                color = MidnightIndigo
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Document Picker
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Text("Tap to Select Document (DOCX, TXT, PDF)", color = MidnightIndigo)
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Select Tool", style = MaterialTheme.typography.titleLarge, color = MidnightIndigo)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Convert to PDF", "Merge", "Unlock").forEach { tool ->
                Button(
                    onClick = { selectedTool = tool },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTool == tool) MidnightIndigo else MaterialTheme.colorScheme.surface,
                        contentColor = if (selectedTool == tool) PureWhite else MidnightIndigo
                    )
                ) {
                    Text(tool)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { /* Execute Document Flow */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MidnightIndigo, contentColor = PureWhite)
        ) {
            Text("Execute $selectedTool", style = MaterialTheme.typography.titleLarge)
        }
    }
}
