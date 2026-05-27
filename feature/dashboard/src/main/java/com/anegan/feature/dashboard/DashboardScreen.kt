/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.dashboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.MidnightIndigo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

data class Category(val title: String, val description: String)

data class QuickPreset(
    val title: String,
    val description: String,
    val category: String,
    val params: Map<String, String>,
    val emoji: String,
    val startColor: Color,
    val endColor: Color
)

val quickPresets = listOf(
    QuickPreset(
        title = "WhatsApp",
        description = "Compress for chat share",
        category = "Images",
        params = mapOf("quality" to "60", "targetFormat" to "JPG", "resizeWidth" to "1280"),
        emoji = "💬",
        startColor = Color(0xFF128C7E),
        endColor = Color(0xFF25D366)
    ),
    QuickPreset(
        title = "Instagram",
        description = "Optimized size & quality",
        category = "Images",
        params = mapOf("quality" to "85", "targetFormat" to "JPG", "resizeWidth" to "1080", "resizeHeight" to "1080"),
        emoji = "📸",
        startColor = Color(0xFFC13584),
        endColor = Color(0xFFF77737)
    ),
    QuickPreset(
        title = "Lossless PNG",
        description = "Convert preserving details",
        category = "Images",
        params = mapOf("quality" to "100", "targetFormat" to "PNG"),
        emoji = "💎",
        startColor = Color(0xFF0052D4),
        endColor = Color(0xFF4364F7)
    ),
    QuickPreset(
        title = "Compress Video",
        description = "Target 720p resolution",
        category = "Video Tools",
        params = mapOf("tab" to "Compress", "crf" to "28", "resolution" to "720p"),
        emoji = "🎥",
        startColor = Color(0xFF6A11CB),
        endColor = Color(0xFF2575FC)
    ),
    QuickPreset(
        title = "Video to GIF",
        description = "Extract 5s loop",
        category = "Video Tools",
        params = mapOf("tab" to "To GIF", "gifStart" to "0.0", "gifDuration" to "5.0", "gifFps" to "10"),
        emoji = "🎞️",
        startColor = Color(0xFFF857A6),
        endColor = Color(0xFFFF5858)
    ),
    QuickPreset(
        title = "Extract MP3",
        description = "Quick audio rip",
        category = "Audio Tools",
        params = mapOf("tab" to "Trim"),
        emoji = "🎵",
        startColor = Color(0xFF11998E),
        endColor = Color(0xFF38EF7D)
    ),
    QuickPreset(
        title = "Secure PDF",
        description = "Add page encrypt protection",
        category = "PDF Tools",
        params = mapOf("tab" to "Encrypt"),
        emoji = "🔒",
        startColor = Color(0xFFEB5757),
        endColor = Color(0xFF3F2B96)
    )
)

val categories = listOf(
    Category("Notes",             "Quick notes, reminders, checklists"),
    Category("Vault",             "Encrypted secure document storage"),
    Category("File Manager",      "Browse, zip, manage your files"),
    Category("Documents",         "Merge, text-to-pdf, unlock"),
    Category("PDF Tools",         "Split, compress, encrypt, images"),
    Category("Images",            "JPG, PNG, WEBP, quality"),
    Category("Batch Image",       "Process multiple images"),
    Category("Video",             "Convert MP4, MKV, AVI"),
    Category("Video Tools",       "Trim, compress, speed, GIF"),
    Category("Audio",             "Convert MP3, M4A, FLAC"),
    Category("Audio Tools",       "Cut audio, ringtones"),
    Category("OCR / Extract Text","Image to Text (Offline)"),
    Category("EXIF Metadata",     "View & strip photo metadata"),
    Category("Developer Tools",   "Hash, Base64, QR codes"),
    Category("AI Background Remover", "Isolate subjects offline"),
    Category("Image Watermark",   "Add text watermarks"),
    Category("PDF Reader & Editor", "Read, draw, sign and edit pages"),
    Category("Compass",           "Offline bearing & orientation sensor"),
    Category("Color Picker",      "Analyze and save palettes"),
    Category("Unit Converter",    "Offline length, mass, data size conversion"),
    Category("History",           "Recent Conversions"),
    Category("Settings",          "App Config"),
    Category("Feedback",          "Report bugs to GitHub")
)

@Composable
fun DashboardScreen(
    onCategorySelected: (String) -> Unit,
    onPresetSelected: (String, Map<String, String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("anegan_favorites", Context.MODE_PRIVATE) }
    var favoriteTitles by remember {
        mutableStateOf(prefs.getStringSet("favorite_categories", emptySet()) ?: emptySet())
    }

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
            Column {
                Text(
                    text = "Anegan",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 32.sp)
                )
                Text(
                    text = "Your offline utility suite — v2.5",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1B5E20).copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "🛡️ 100% Offline",
                            fontSize = 10.sp,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0D47A1).copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "🔒 No Account",
                            fontSize = 10.sp,
                            color = Color(0xFF1565C0),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Favorites ──────────────────────────────────────────────
        if (favoriteTitles.isNotEmpty()) {
            Text(
                text = "Favorites ❤️",
                style = MaterialTheme.typography.titleMedium,
                color = MidnightIndigo
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val favoriteCats = categories.filter { favoriteTitles.contains(it.title) }
                items(favoriteCats) { category ->
                    FavoriteCategoryCard(
                        category = category,
                        onClick = { onCategorySelected(category.title) },
                        onUnfavorite = {
                            val newFavorites = favoriteTitles - category.title
                            favoriteTitles = newFavorites
                            prefs.edit().putStringSet("favorite_categories", newFavorites).apply()
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // ── Quick Presets ──────────────────────────────────────────
        Text(
            text = "Quick Presets",
            style = MaterialTheme.typography.titleMedium,
            color = MidnightIndigo
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(quickPresets) { preset ->
                QuickPresetCard(preset = preset, onClick = { onPresetSelected(preset.category, preset.params) })
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(categories) { category ->
                val isFav = favoriteTitles.contains(category.title)
                CategoryCard(
                    category = category,
                    isFavorite = isFav,
                    onFavoriteToggle = {
                        val newFavorites = if (isFav) {
                            favoriteTitles - category.title
                        } else {
                            favoriteTitles + category.title
                        }
                        favoriteTitles = newFavorites
                        prefs.edit().putStringSet("favorite_categories", newFavorites).apply()
                    },
                    onClick = { onCategorySelected(category.title) }
                )
            }
        }
    }
}

@Composable
fun FavoriteCategoryCard(
    category: Category,
    onClick: () -> Unit,
    onUnfavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(100.dp)
            .semantics {
                contentDescription = "Favorite Category: ${category.title}. ${category.description}"
            }
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category.title,
                    color = MidnightIndigo,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onUnfavorite() }
                        .wrapContentSize(Alignment.Center)
                ) {
                    Text(
                        text = "❤️",
                        fontSize = 14.sp
                    )
                }
            }
            Text(
                text = category.description,
                color = Color.Gray,
                fontSize = 10.sp,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CategoryCard(
    category: Category,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .semantics {
                contentDescription = "Launch ${category.title} converter: ${category.description}"
            }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MidnightIndigo,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { onFavoriteToggle() }
                        .wrapContentSize(Alignment.Center)
                ) {
                    Text(
                        text = if (isFavorite) "⭐" else "☆",
                        fontSize = 18.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = category.description,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 12.sp),
                color = Color.Gray
            )
        }
    }
}



@Composable
fun QuickPresetCard(preset: QuickPreset, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(100.dp)
            .semantics {
                contentDescription = "Quick Preset for ${preset.title}: ${preset.description}"
            }
            .clickable { onClick() },

        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(preset.startColor, preset.endColor)
                    )
                )
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = preset.emoji,
                        fontSize = 20.sp
                    )
                    Text(
                        text = "→",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text(
                        text = preset.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = preset.description,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
