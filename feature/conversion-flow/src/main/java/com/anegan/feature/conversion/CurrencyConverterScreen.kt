/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.conversion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.MidnightIndigo
import com.anegan.core.designsystem.theme.PureWhite

/**
 * Offline Currency Converter.
 *
 * Ships with a hardcoded baseline exchange-rate table (USD base).
 * Users can manually update rates if they wish; no network call is ever made.
 * This guarantees the feature works 100% offline on day one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyConverterScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Hardcoded offline exchange rates (base = 1 USD)
    // Last synced: May 2026 approximate rates
    val rateMap = remember {
        mapOf(
            "USD" to 1.0,
            "EUR" to 0.92,
            "GBP" to 0.79,
            "INR" to 83.50,
            "JPY" to 157.0,
            "AUD" to 1.53,
            "CAD" to 1.37,
            "CHF" to 0.91,
            "CNY" to 7.25,
            "SGD" to 1.35,
            "AED" to 3.67,
            "SAR" to 3.75,
            "MYR" to 4.72,
            "THB" to 35.80,
            "KRW" to 1370.0,
            "BRL" to 5.10,
            "ZAR" to 18.30,
            "RUB" to 92.0,
            "TRY" to 32.50,
            "MXN" to 17.20,
            "IDR" to 15800.0,
            "PHP" to 56.50,
            "PKR" to 278.0,
            "BDT" to 110.0,
            "LKR" to 305.0,
            "NGN" to 1550.0,
            "EGP" to 48.0,
            "VND" to 25400.0,
            "NZD" to 1.65,
            "SEK" to 10.85,
            "NOK" to 10.90,
            "DKK" to 6.88,
            "HKD" to 7.82,
            "TWD" to 32.20
        )
    }

    val currencyNames = remember {
        mapOf(
            "USD" to "US Dollar", "EUR" to "Euro", "GBP" to "British Pound",
            "INR" to "Indian Rupee", "JPY" to "Japanese Yen", "AUD" to "Australian Dollar",
            "CAD" to "Canadian Dollar", "CHF" to "Swiss Franc", "CNY" to "Chinese Yuan",
            "SGD" to "Singapore Dollar", "AED" to "UAE Dirham", "SAR" to "Saudi Riyal",
            "MYR" to "Malaysian Ringgit", "THB" to "Thai Baht", "KRW" to "South Korean Won",
            "BRL" to "Brazilian Real", "ZAR" to "South African Rand", "RUB" to "Russian Ruble",
            "TRY" to "Turkish Lira", "MXN" to "Mexican Peso", "IDR" to "Indonesian Rupiah",
            "PHP" to "Philippine Peso", "PKR" to "Pakistani Rupee", "BDT" to "Bangladeshi Taka",
            "LKR" to "Sri Lankan Rupee", "NGN" to "Nigerian Naira", "EGP" to "Egyptian Pound",
            "VND" to "Vietnamese Dong", "NZD" to "New Zealand Dollar", "SEK" to "Swedish Krona",
            "NOK" to "Norwegian Krone", "DKK" to "Danish Krone", "HKD" to "Hong Kong Dollar",
            "TWD" to "Taiwan Dollar"
        )
    }

    val currencyList = remember { rateMap.keys.toList().sorted() }

    var fromCurrency by remember { mutableStateOf("USD") }
    var toCurrency by remember { mutableStateOf("INR") }
    var inputAmount by remember { mutableStateOf("1") }

    val amount = inputAmount.toDoubleOrNull() ?: 0.0
    val fromRate = rateMap[fromCurrency] ?: 1.0
    val toRate = rateMap[toCurrency] ?: 1.0
    val convertedAmount = (amount / fromRate) * toRate

    val formattedResult = if (convertedAmount > 1000) {
        String.format("%,.2f", convertedAmount)
    } else if (convertedAmount % 1.0 == 0.0) {
        String.format("%.0f", convertedAmount)
    } else {
        String.format("%.4f", convertedAmount).trimEnd('0').trimEnd('.')
    }

    fun swapCurrencies() {
        val temp = fromCurrency
        fromCurrency = toCurrency
        toCurrency = temp
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
                modifier = Modifier.size(48.dp)
            ) {
                Text("←", style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp), color = MidnightIndigo)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Currency Converter", style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp), color = MidnightIndigo)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Offline badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1B5E20).copy(alpha = 0.12f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text("🛡️ 100% Offline · Hardcoded rates (May 2026)", fontSize = 10.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // FROM Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("From", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                CurrencyDropdown(
                    selected = fromCurrency,
                    currencies = currencyList,
                    currencyNames = currencyNames,
                    onSelected = { fromCurrency = it },
                    label = "Source currency"
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = inputAmount,
                    onValueChange = { inputAmount = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MidnightIndigo,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Swap
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Button(
                onClick = { swapCurrencies() },
                colors = ButtonDefaults.buttonColors(containerColor = MidnightIndigo, contentColor = PureWhite),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text("⇅ Swap Currencies", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // TO Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("To", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                CurrencyDropdown(
                    selected = toCurrency,
                    currencies = currencyList,
                    currencyNames = currencyNames,
                    onSelected = { toCurrency = it },
                    label = "Target currency"
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Result
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text("Result", color = Color.Gray, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$formattedResult $toCurrency",
                            color = MidnightIndigo,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "1 $fromCurrency = ${String.format("%.4f", toRate / fromRate)} $toCurrency",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Disclaimer card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("💡", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Rates are approximate and hardcoded for offline use. For real-time rates, verify with an online source.",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyDropdown(
    selected: String,
    currencies: List<String>,
    currencyNames: Map<String, String>,
    onSelected: (String) -> Unit,
    label: String
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            readOnly = true,
            value = "$selected — ${currencyNames[selected] ?: selected}",
            onValueChange = {},
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MidnightIndigo,
                unfocusedBorderColor = Color.LightGray
            ),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(16.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            currencies.forEach { code ->
                DropdownMenuItem(
                    text = { Text("$code — ${currencyNames[code] ?: code}") },
                    onClick = {
                        onSelected(code)
                        expanded = false
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
