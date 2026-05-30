/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.conversion

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitConverterScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scrollState = rememberScrollState()
    val isDark = isSystemInDarkTheme()

    val categories = listOf("Length", "Mass", "Temperature", "Data Size", "Area")
    var activeCategoryIndex by remember { mutableStateOf(0) }
    val activeCategory = categories[activeCategoryIndex]

    val unitsMap = mapOf(
        "Length" to listOf("Millimeter (mm)", "Centimeter (cm)", "Meter (m)", "Kilometer (km)", "Inch (in)", "Foot (ft)", "Yard (yd)", "Mile (mi)"),
        "Mass" to listOf("Milligram (mg)", "Gram (g)", "Kilogram (kg)", "Ounce (oz)", "Pound (lb)"),
        "Temperature" to listOf("Celsius (°C)", "Fahrenheit (°F)", "Kelvin (K)"),
        "Data Size" to listOf("Byte (B)", "Kilobyte (KB)", "Megabyte (MB)", "Gigabyte (GB)", "Terabyte (TB)"),
        "Area" to listOf("Square Meter (m²)", "Square Kilometer (km²)", "Square Foot (ft²)", "Acre (ac)", "Hectare (ha)")
    )

    val currentUnits = unitsMap[activeCategory] ?: emptyList()

    var fromUnit by remember(activeCategory) { mutableStateOf(currentUnits.firstOrNull() ?: "") }
    var toUnit by remember(activeCategory) { mutableStateOf(if (currentUnits.size > 1) currentUnits[1] else currentUnits.firstOrNull() ?: "") }

    var inputValue by remember { mutableStateOf("1") }

    val doubleValue = inputValue.toDoubleOrNull() ?: 0.0
    val resultValue = convert(doubleValue, fromUnit, toUnit, activeCategory)

    // Formatted result: strip trailing decimals if whole number, or show up to 6 decimal places
    val formattedResult = if (resultValue % 1.0 == 0.0) {
        String.format("%.0f", resultValue)
    } else {
        String.format("%.6f", resultValue).trimEnd('0').trimEnd('.')
    }

    val primaryAccent = NeonLime // Electric Lime for Utility tools

    fun swapUnits() {
        NovaHaptics.click(view)
        val temp = fromUnit
        fromUnit = toUnit
        toUnit = temp
    }

    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            NovaTopBar(
                title = "Unit Converter",
                onBack = onBack,
                neonAccent = primaryAccent
            )
        }
    ) { innerPadding ->
        NovaBackground {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = NovaTokens.Spacing.md, vertical = NovaTokens.Spacing.sm)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(NovaTokens.Spacing.md)
            ) {
                // Horizontal Category Tabs (Sci-fi scrolling chips layout)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = NovaTokens.Spacing.xxs),
                    horizontalArrangement = Arrangement.spacedBy(NovaTokens.Spacing.xs)
                ) {
                    categories.forEachIndexed { index, name ->
                        NovaChip(
                            text = name,
                            selected = activeCategoryIndex == index,
                            onClick = { 
                                activeCategoryIndex = index
                            },
                            neonColor = primaryAccent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(NovaTokens.Spacing.xxs))

                // FROM Selection & Input Card
                GlassCard(
                    neonAccent = primaryAccent,
                    enableGlow = false
                ) {
                    Column(modifier = Modifier.padding(NovaTokens.Spacing.md)) {
                        Text(
                            text = "Source Asset Unit",
                            style = NovaTypography.tagMono.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(NovaTokens.Spacing.xs))

                        UnitSelectorDropdown(
                            selectedUnit = fromUnit,
                            units = currentUnits,
                            onUnitSelected = { fromUnit = it },
                            label = "Select Source Unit",
                            neonAccent = primaryAccent
                        )

                        Spacer(modifier = Modifier.height(NovaTokens.Spacing.md))

                        NovaTextField(
                            value = inputValue,
                            onValueChange = { inputValue = it },
                            placeholder = "Enter Input Value",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            neonColor = primaryAccent
                        )
                    }
                }

                // Swap Button
                NovaSecondaryButton(
                    text = "⇅ Swap Conversion Units",
                    neonColor = primaryAccent,
                    onClick = { swapUnits() }
                )

                // TO Selection & Result Card
                GlassCard(
                    neonAccent = primaryAccent,
                    enableGlow = false
                ) {
                    Column(modifier = Modifier.padding(NovaTokens.Spacing.md)) {
                        Text(
                            text = "Target Destination Unit",
                            style = NovaTypography.tagMono.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(NovaTokens.Spacing.xs))

                        UnitSelectorDropdown(
                            selectedUnit = toUnit,
                            units = currentUnits,
                            onUnitSelected = { toUnit = it },
                            label = "Select Target Unit",
                            neonAccent = primaryAccent
                        )

                        Spacer(modifier = Modifier.height(NovaTokens.Spacing.md))

                        // Result display box (Sci-fi holographic panel style)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(NovaTokens.Radius.md))
                                .background(
                                    if (isDark) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.02f)
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = primaryAccent.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(NovaTokens.Radius.md)
                                )
                                .padding(NovaTokens.Spacing.md)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Conversion Result",
                                        style = NovaTypography.tagMono.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$formattedResult ${getUnitAbbreviation(toUnit)}",
                                        style = NovaTypography.headlineLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = primaryAccent,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 24.sp
                                        )
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        NovaHaptics.success(view)
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Conversion Result", formattedResult)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Copied $formattedResult to clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ContentCopy,
                                        contentDescription = "Copy result",
                                        tint = primaryAccent
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitSelectorDropdown(
    selectedUnit: String,
    units: List<String>,
    onUnitSelected: (String) -> Unit,
    label: String,
    neonAccent: Color
) {
    var expanded by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()
    val view = LocalView.current

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            readOnly = true,
            value = selectedUnit,
            onValueChange = {},
            label = { 
                Text(
                    text = label,
                    style = NovaTypography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ) 
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = if (isDark) NovaMidnightBlue.copy(alpha = 0.5f) else NovaPureWhite.copy(alpha = 0.5f),
                unfocusedContainerColor = if (isDark) NovaMidnightBlue.copy(alpha = 0.3f) else NovaPureWhite.copy(alpha = 0.3f),
                focusedBorderColor = neonAccent,
                unfocusedBorderColor = if (isDark) NovaBorderDark.copy(alpha = 0.3f) else NovaBorderLight.copy(alpha = 0.3f),
                focusedLabelColor = neonAccent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(NovaTokens.Radius.md)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(if (isDark) NovaMidnightBlue else Color.White)
        ) {
            units.forEach { selectionOption ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            text = selectionOption,
                            style = NovaTypography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        ) 
                    },
                    onClick = {
                        NovaHaptics.click(view)
                        onUnitSelected(selectionOption)
                        expanded = false
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// Unit Abbreviation Parser Helper
fun getUnitAbbreviation(unitName: String): String {
    val startIndex = unitName.indexOf('(')
    val endIndex = unitName.indexOf(')')
    return if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
        unitName.substring(startIndex + 1, endIndex)
    } else {
        unitName
    }
}

// Offline Unit Math Converter
fun convert(value: Double, fromUnit: String, toUnit: String, category: String): Double {
    if (fromUnit == toUnit) return value

    return when (category) {
        "Temperature" -> {
            val celsiusValue = when (fromUnit) {
                "Celsius (°C)" -> value
                "Fahrenheit (°F)" -> (value - 32.0) * 5.0 / 9.0
                "Kelvin (K)" -> value - 273.15
                else -> value
            }
            when (toUnit) {
                "Celsius (°C)" -> celsiusValue
                "Fahrenheit (°F)" -> celsiusValue * 9.0 / 5.0 + 32.0
                "Kelvin (K)" -> celsiusValue + 273.15
                else -> celsiusValue
            }
        }
        else -> {
            val factors = when (category) {
                "Length" -> mapOf(
                    "Millimeter (mm)" to 0.001,
                    "Centimeter (cm)" to 0.01,
                    "Meter (m)" to 1.0,
                    "Kilometer (km)" to 1000.0,
                    "Inch (in)" to 0.0254,
                    "Foot (ft)" to 0.3048,
                    "Yard (yd)" to 0.9144,
                    "Mile (mi)" to 1609.344
                )
                "Mass" -> mapOf(
                    "Milligram (mg)" to 0.001,
                    "Gram (g)" to 1.0,
                    "Kilogram (kg)" to 1000.0,
                    "Ounce (oz)" to 28.349523125,
                    "Pound (lb)" to 453.59237
                )
                "Data Size" -> mapOf(
                    "Byte (B)" to 1.0,
                    "Kilobyte (KB)" to 1024.0,
                    "Megabyte (MB)" to 1048576.0,
                    "Gigabyte (GB)" to 1073741824.0,
                    "Terabyte (TB)" to 1099511627776.0
                )
                "Area" -> mapOf(
                    "Square Meter (m²)" to 1.0,
                    "Square Kilometer (km²)" to 1000000.0,
                    "Square Foot (ft²)" to 0.09290304,
                    "Acre (ac)" to 4046.8564224,
                    "Hectare (ha)" to 10000.0
                )
                else -> emptyMap()
            }
            val fromFactor = factors[fromUnit] ?: 1.0
            val toFactor = factors[toUnit] ?: 1.0
            (value * fromFactor) / toFactor
        }
    }
}
