/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.conversion

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.database.CurrencyRateEntity
import com.anegan.core.database.DatabaseProvider
import com.anegan.core.designsystem.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyConverterScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val isDark = isSystemInDarkTheme()

    // Baseline offline exchange rates (base = 1 USD)
    val defaultRates = remember {
        mapOf(
            "USD" to 1.0, "EUR" to 0.92, "GBP" to 0.79, "INR" to 83.50,
            "JPY" to 157.0, "AUD" to 1.53, "CAD" to 1.37, "CHF" to 0.91,
            "CNY" to 7.25, "SGD" to 1.35, "AED" to 3.67, "SAR" to 3.75,
            "MYR" to 4.72, "THB" to 35.80, "KRW" to 1370.0, "BRL" to 5.10,
            "ZAR" to 18.30, "RUB" to 92.0, "TRY" to 32.50, "MXN" to 17.20,
            "IDR" to 15800.0, "PHP" to 56.50, "PKR" to 278.0, "BDT" to 110.0,
            "LKR" to 305.0, "NGN" to 1550.0, "EGP" to 48.0, "VND" to 25400.0,
            "NZD" to 1.65, "SEK" to 10.85, "NOK" to 10.90, "DKK" to 6.88,
            "HKD" to 7.82, "TWD" to 32.20
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

    val db = remember { DatabaseProvider.getDatabase(context) }
    val currencyRateDao = remember { db.currencyRateDao() }

    var ratesList by remember { mutableStateOf<List<CurrencyRateEntity>>(emptyList()) }
    var isSyncing by remember { mutableStateOf(false) }

    var fromCurrency by remember { mutableStateOf("USD") }
    var toCurrency by remember { mutableStateOf("INR") }
    var inputAmount by remember { mutableStateOf("1") }

    // Dialog override states
    var showOverrideDialog by remember { mutableStateOf(false) }
    var overrideTargetCode by remember { mutableStateOf("") }
    var overrideInputValue by remember { mutableStateOf("") }

    val primaryAccent = NeonLime // Electric Lime for Utility tools

    fun loadRates() {
        scope.launch(Dispatchers.IO) {
            var dbRates = currencyRateDao.getAll()
            if (dbRates.isEmpty()) {
                val baseline = defaultRates.map { (code, rate) ->
                    CurrencyRateEntity(
                        code = code,
                        name = currencyNames[code] ?: code,
                        rateToUsd = rate,
                        manualRate = null,
                        lastUpdatedAt = System.currentTimeMillis()
                    )
                }
                baseline.forEach { currencyRateDao.upsert(it) }
                dbRates = baseline
            }
            withContext(Dispatchers.Main) {
                ratesList = dbRates
            }
        }
    }

    LaunchedEffect(Unit) {
        loadRates()
    }

    suspend fun fetchLatestRates(): Map<String, Double>? = withContext(Dispatchers.IO) {
        try {
            val response = URL("https://open.er-api.com/v6/latest/USD").readText()
            val json = JSONObject(response)
            if (json.getString("result") == "success") {
                val ratesJson = json.getJSONObject("rates")
                val ratesMap = mutableMapOf<String, Double>()
                ratesJson.keys().forEach { code ->
                    ratesMap[code] = ratesJson.getDouble(code)
                }
                ratesMap
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun syncRates() {
        if (isSyncing) return
        isSyncing = true
        NovaHaptics.click(view)
        scope.launch(Dispatchers.IO) {
            val fetched = fetchLatestRates()
            if (fetched != null) {
                ratesList.forEach { rateEntity ->
                    val newRate = fetched[rateEntity.code]
                    if (newRate != null) {
                        currencyRateDao.upsert(
                            rateEntity.copy(
                                rateToUsd = newRate,
                                lastUpdatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
                loadRates()
                withContext(Dispatchers.Main) {
                    isSyncing = false
                    NovaHaptics.success(view)
                    Toast.makeText(context, "Rates updated via Live API!", Toast.LENGTH_SHORT).show()
                }
            } else {
                withContext(Dispatchers.Main) {
                    isSyncing = false
                    NovaHaptics.warning(view)
                    Toast.makeText(context, "Update failed. Using offline cached rates.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val currencyList = remember(ratesList) { ratesList.map { it.code }.sorted() }

    val amount = inputAmount.toDoubleOrNull() ?: 0.0
    val fromRate = remember(ratesList, fromCurrency) {
        val r = ratesList.find { it.code == fromCurrency }
        r?.manualRate ?: r?.rateToUsd ?: 1.0
    }
    val toRate = remember(ratesList, toCurrency) {
        val r = ratesList.find { it.code == toCurrency }
        r?.manualRate ?: r?.rateToUsd ?: 1.0
    }
    
    val convertedAmount = (amount / fromRate) * toRate

    val formattedResult = if (convertedAmount > 1000) {
        String.format("%,.2f", convertedAmount)
    } else if (convertedAmount % 1.0 == 0.0) {
        String.format("%.0f", convertedAmount)
    } else {
        String.format("%.4f", convertedAmount).trimEnd('0').trimEnd('.')
    }

    fun swapCurrencies() {
        NovaHaptics.click(view)
        val temp = fromCurrency
        fromCurrency = toCurrency
        toCurrency = temp
    }

    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            NovaTopBar(
                title = "Currency Exchange",
                onBack = onBack,
                neonAccent = primaryAccent,
                actions = {
                    IconButton(
                        onClick = { syncRates() },
                        enabled = !isSyncing
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = primaryAccent,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Sync,
                                contentDescription = "Sync rates online",
                                tint = primaryAccent
                            )
                        }
                    }
                }
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
                // Live Status Card
                GlassCard(
                    neonAccent = primaryAccent,
                    enableGlow = true
                ) {
                    Row(
                        modifier = Modifier.padding(NovaTokens.Spacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📡", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Quantum Real-time Sync",
                                style = NovaTypography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = primaryAccent)
                            )
                            Text(
                                text = "Base rates are locally cached for offline utility. Tap the sync icon to fetch live bank rates, or set custom manual rate overrides below.",
                                style = NovaTypography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // FROM Card
                GlassCard(
                    neonAccent = primaryAccent,
                    enableGlow = false
                ) {
                    Column(modifier = Modifier.padding(NovaTokens.Spacing.md)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Source Asset", 
                                style = NovaTypography.tagMono.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            val r = ratesList.find { it.code == fromCurrency }
                            if (r?.manualRate != null) {
                                Text(
                                    text = "Override: ${r.manualRate}",
                                    style = NovaTypography.tagMono.copy(fontWeight = FontWeight.Black, color = NovaError),
                                    modifier = Modifier.clickable {
                                        NovaHaptics.click(view)
                                        overrideTargetCode = fromCurrency
                                        overrideInputValue = r.manualRate.toString()
                                        showOverrideDialog = true
                                    }
                                )
                            } else {
                                Text(
                                    text = "Set Custom Rate",
                                    style = NovaTypography.tagMono.copy(color = primaryAccent),
                                    modifier = Modifier.clickable {
                                        NovaHaptics.click(view)
                                        overrideTargetCode = fromCurrency
                                        overrideInputValue = (r?.rateToUsd ?: 1.0).toString()
                                        showOverrideDialog = true
                                    }
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(NovaTokens.Spacing.xs))
                        
                        CurrencyDropdown(
                            selected = fromCurrency,
                            currencies = currencyList,
                            currencyNames = currencyNames,
                            onSelected = { fromCurrency = it },
                            label = "Select Source Asset",
                            neonAccent = primaryAccent
                        )
                        
                        Spacer(modifier = Modifier.height(NovaTokens.Spacing.md))
                        
                        NovaTextField(
                            value = inputAmount,
                            onValueChange = { inputAmount = it },
                            placeholder = "Enter Amount",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            neonColor = primaryAccent
                        )
                    }
                }

                // Swap Button
                NovaSecondaryButton(
                    text = "⇅ Swap Currencies",
                    neonColor = primaryAccent,
                    onClick = { swapCurrencies() }
                )

                // TO Card
                GlassCard(
                    neonAccent = primaryAccent,
                    enableGlow = false
                ) {
                    Column(modifier = Modifier.padding(NovaTokens.Spacing.md)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Target Asset", 
                                style = NovaTypography.tagMono.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            val r = ratesList.find { it.code == toCurrency }
                            if (r?.manualRate != null) {
                                Text(
                                    text = "Override: ${r.manualRate}",
                                    style = NovaTypography.tagMono.copy(fontWeight = FontWeight.Black, color = NovaError),
                                    modifier = Modifier.clickable {
                                        NovaHaptics.click(view)
                                        overrideTargetCode = toCurrency
                                        overrideInputValue = r.manualRate.toString()
                                        showOverrideDialog = true
                                    }
                                )
                            } else {
                                Text(
                                    text = "Set Custom Rate",
                                    style = NovaTypography.tagMono.copy(color = primaryAccent),
                                    modifier = Modifier.clickable {
                                        NovaHaptics.click(view)
                                        overrideTargetCode = toCurrency
                                        overrideInputValue = (r?.rateToUsd ?: 1.0).toString()
                                        showOverrideDialog = true
                                    }
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(NovaTokens.Spacing.xs))
                        
                        CurrencyDropdown(
                            selected = toCurrency,
                            currencies = currencyList,
                            currencyNames = currencyNames,
                            onSelected = { toCurrency = it },
                            label = "Select Target Asset",
                            neonAccent = primaryAccent
                        )
                        
                        Spacer(modifier = Modifier.height(NovaTokens.Spacing.md))

                        // Result Box (Sci-fi holographic screen style)
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
                            Column {
                                Text(
                                    text = "Exchange Result", 
                                    style = NovaTypography.tagMono.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Spacer(modifier = Modifier.height(NovaTokens.Spacing.xxs))
                                Text(
                                    text = "$formattedResult $toCurrency",
                                    style = NovaTypography.headlineLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = primaryAccent,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 24.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(NovaTokens.Spacing.xxs))
                                Text(
                                    text = "Rate Reference: 1 $fromCurrency = ${String.format("%.4f", toRate / fromRate)} $toCurrency",
                                    style = NovaTypography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Override Dialogue
    if (showOverrideDialog) {
        AlertDialog(
            onDismissRequest = { showOverrideDialog = false },
            title = { 
                Text(
                    text = "Manual Rate Override for $overrideTargetCode", 
                    style = NovaTypography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = primaryAccent)
                ) 
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Specify a fixed conversion value relative to 1 USD. Setting an override blocks live API updates for this currency.",
                        style = NovaTypography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    NovaTextField(
                        value = overrideInputValue,
                        onValueChange = { overrideInputValue = it },
                        placeholder = "Base rate to 1 USD",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        neonColor = primaryAccent
                    )
                }
            },
            confirmButton = {
                NovaPrimaryButton(
                    text = "Save Override",
                    neonColor = primaryAccent,
                    onClick = {
                        val parsed = overrideInputValue.toDoubleOrNull()
                        if (parsed != null && parsed > 0.0) {
                            scope.launch(Dispatchers.IO) {
                                val currentEntity = currencyRateDao.getByCode(overrideTargetCode)
                                if (currentEntity != null) {
                                    currencyRateDao.upsert(
                                        currentEntity.copy(
                                            manualRate = parsed,
                                            lastUpdatedAt = System.currentTimeMillis()
                                        )
                                    )
                                }
                                loadRates()
                            }
                            showOverrideDialog = false
                        } else {
                            Toast.makeText(context, "Please enter a valid rate", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            },
            dismissButton = {
                NovaSecondaryButton(
                    text = "Clear Override",
                    neonColor = NovaError,
                    onClick = {
                        // Clear override
                        scope.launch(Dispatchers.IO) {
                            val currentEntity = currencyRateDao.getByCode(overrideTargetCode)
                            if (currentEntity != null) {
                                currencyRateDao.upsert(
                                    currentEntity.copy(
                                        manualRate = null,
                                        lastUpdatedAt = System.currentTimeMillis()
                                    )
                                )
                            }
                            loadRates()
                        }
                        showOverrideDialog = false
                    }
                )
            },
            containerColor = if (isDark) NovaMidnightBlue else Color.White,
            shape = RoundedCornerShape(NovaTokens.Radius.lg),
            modifier = Modifier.border(1.dp, primaryAccent.copy(alpha = 0.2f), RoundedCornerShape(NovaTokens.Radius.lg))
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyDropdown(
    selected: String,
    currencies: List<String>,
    currencyNames: Map<String, String>,
    onSelected: (String) -> Unit,
    label: String,
    neonAccent: Color
) {
    var expanded by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()
    val view = LocalView.current

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { 
            expanded = !expanded 
        }
    ) {
        OutlinedTextField(
            readOnly = true,
            value = "$selected — ${currencyNames[selected] ?: selected}",
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
            currencies.forEach { code ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            text = "$code — ${currencyNames[code] ?: code}",
                            style = NovaTypography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        ) 
                    },
                    onClick = {
                        NovaHaptics.click(view)
                        onSelected(code)
                        expanded = false
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
