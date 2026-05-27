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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.MidnightIndigo
import com.anegan.core.designsystem.theme.PureWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitConverterScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

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

    fun swapUnits() {
        val temp = fromUnit
        fromUnit = toUnit
        toUnit = temp
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(scrollState)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = "Go back to dashboard" }
            ) {
                Text(
                    text = "←",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp),
                    color = MidnightIndigo
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Unit Converter",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp),
                color = MidnightIndigo
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Horizontal Category Tabs
        ScrollableTabRow(
            selectedTabIndex = activeCategoryIndex,
            edgePadding = 0.dp,
            containerColor = Color.Transparent,
            contentColor = MidnightIndigo,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeCategoryIndex]),
                    color = MidnightIndigo
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            categories.forEachIndexed { index, name ->
                Tab(
                    selected = activeCategoryIndex == index,
                    onClick = { activeCategoryIndex = index },
                    text = {
                        Text(
                            text = name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier.height(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // FROM Selection & Input Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "From",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                UnitSelectorDropdown(
                    selectedUnit = fromUnit,
                    units = currentUnits,
                    onUnitSelected = { fromUnit = it },
                    label = "Select source unit"
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { inputValue = it },
                    label = { Text("Value to Convert") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Enter numeric value to convert from $fromUnit" },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MidnightIndigo,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Swap Button Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = { swapUnits() },
                colors = ButtonDefaults.buttonColors(containerColor = MidnightIndigo, contentColor = PureWhite),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .height(48.dp)
                    .semantics { contentDescription = "Swap source and target units" }
            ) {
                Text("⇅ Swap Units", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // TO Selection & Result Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "To",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                UnitSelectorDropdown(
                    selectedUnit = toUnit,
                    units = currentUnits,
                    onUnitSelected = { toUnit = it },
                    label = "Select target unit"
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Result display box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                        .semantics { contentDescription = "Conversion result is $formattedResult $toUnit" }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Result",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "$formattedResult ${getUnitAbbreviation(toUnit)}",
                                color = MidnightIndigo,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Conversion Result", formattedResult)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied $formattedResult to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .semantics { contentDescription = "Copy conversion result value to clipboard" }
                        ) {
                            Text("Copy", color = MidnightIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
    label: String
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            readOnly = true,
            value = selectedUnit,
            onValueChange = {},
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MidnightIndigo,
                unfocusedBorderColor = Color.LightGray
            ),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .semantics { contentDescription = "$label: active selection is $selectedUnit" },
            shape = RoundedCornerShape(16.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            units.forEach { selectionOption ->
                DropdownMenuItem(
                    text = { Text(text = selectionOption) },
                    onClick = {
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
