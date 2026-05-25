package com.aleph.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.aleph.core.designsystem.theme.MidnightIndigo

data class Category(val title: String, val description: String)

val categories = listOf(
    Category("Documents", "PDF, TXT, DOCX"),
    Category("Images", "JPG, PNG, WEBP"),
    Category("Video", "MP4, Resolution"),
    Category("Audio", "MP3, M4A, FLAC"),
    Category("Tools", "Split, Merge PDF"),
    Category("History", "Recent Conversions"),
    Category("Settings", "App Config"),
    Category("Feedback", "Report bugs to GitHub")
)

@Composable
fun DashboardScreen(
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        // App Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MidnightIndigo),
                contentAlignment = Alignment.Center
            ) {
                Text("A", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Aleph",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 32.sp)
                )
                Text(
                    text = "What would you like to convert?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(categories) { category ->
                CategoryCard(category, onClick = { onCategorySelected(category.title) })
            }
        }
    }
}

@Composable
fun CategoryCard(category: Category, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = category.title,
                style = MaterialTheme.typography.titleLarge,
                color = MidnightIndigo
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = category.description,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 12.sp),
                color = Color.Gray
            )
        }
    }
}
