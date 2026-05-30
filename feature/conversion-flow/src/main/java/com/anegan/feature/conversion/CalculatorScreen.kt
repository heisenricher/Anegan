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
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.*
import net.objecthunter.exp4j.ExpressionBuilder
import com.anegan.core.database.DatabaseProvider
import com.anegan.core.database.CalculatorHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.filled.Delete

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var isScientificMode by remember { mutableStateOf(false) }
    var expression by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("0") }

    val historyItems = remember { mutableStateListOf<CalculatorHistoryEntity>() }
    var showHistorySheet by remember { mutableStateOf(false) }

    val primaryAccent = NeonLime // Electric Lime for Utility tools

    LaunchedEffect(showHistorySheet) {
        if (showHistorySheet) {
            scope.launch(Dispatchers.IO) {
                try {
                    val db = DatabaseProvider.getDatabase(context)
                    val items = db.calculatorHistoryDao().getRecent(50)
                    withContext(Dispatchers.Main) {
                        historyItems.clear()
                        historyItems.addAll(items)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun evaluate(expr: String): String {
        if (expr.isBlank()) return "0"
        try {
            var cleanedExpr = expr
                .replace("×", "*")
                .replace("÷", "/")
                .replace("π", "pi")
                .replace("√", "sqrt")
                .replace("e", "2.718281828459045")
            
            // Auto close brackets if they are unbalanced at evaluation
            val openBrackets = cleanedExpr.count { it == '(' }
            val closeBrackets = cleanedExpr.count { it == ')' }
            if (openBrackets > closeBrackets) {
                cleanedExpr += ")".repeat(openBrackets - closeBrackets)
            }

            val builder = ExpressionBuilder(cleanedExpr).build()
            val evalResult = builder.evaluate()
            return if (evalResult.isNaN()) {
                "Error"
            } else if (evalResult % 1.0 == 0.0) {
                evalResult.toLong().toString()
            } else {
                // Limit decimal places to avoid scientific notation
                String.format("%.8f", evalResult).trimEnd('0').trimEnd('.')
            }
        } catch (e: Exception) {
            return "Error"
        }
    }

    // Try evaluating in real-time as user types
    LaunchedEffect(expression) {
        if (expression.isNotBlank()) {
            val lastChar = expression.lastOrNull()
            if (lastChar != null && lastChar !in listOf('+', '-', '×', '÷', '^', '(')) {
                val realTimeResult = evaluate(expression)
                if (realTimeResult != "Error") {
                    resultText = realTimeResult
                }
            }
        } else {
            resultText = "0"
        }
    }

    fun handleButtonClick(label: String) {
        when (label) {
            "C" -> {
                NovaHaptics.reject(view)
                expression = ""
                resultText = "0"
            }
            "⌫" -> {
                NovaHaptics.swipeSnap(view)
                if (expression.isNotEmpty()) {
                    expression = if (expression.endsWith("sin(") || expression.endsWith("cos(") || expression.endsWith("tan(") || expression.endsWith("log(") || expression.endsWith("log10(")) {
                        expression.substring(0, expression.lastIndexOf('n') - 2)
                    } else if (expression.endsWith("ln(") || expression.endsWith("sqrt(")) {
                        expression.substring(0, expression.lastIndexOf('(') - 1)
                    } else {
                        expression.dropLast(1)
                    }
                }
            }
            "=" -> {
                val finalResult = evaluate(expression)
                if (finalResult != "Error") {
                    NovaHaptics.confirm(view)
                    val prevExpr = expression
                    expression = finalResult
                    resultText = finalResult

                    // Save to database
                    scope.launch(Dispatchers.IO) {
                        try {
                            val db = DatabaseProvider.getDatabase(context)
                            db.calculatorHistoryDao().upsert(
                                CalculatorHistoryEntity(
                                    expression = prevExpr,
                                    result = finalResult,
                                    mode = if (isScientificMode) "scientific" else "basic",
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } else {
                    NovaHaptics.warning(view)
                    Toast.makeText(context, "Invalid Expression", Toast.LENGTH_SHORT).show()
                }
            }
            "sin", "cos", "tan", "log", "ln" -> {
                NovaHaptics.click(view)
                expression += "$label("
            }
            "√" -> {
                NovaHaptics.click(view)
                expression += "sqrt("
            }
            else -> {
                NovaHaptics.click(view)
                expression += label
            }
        }
    }

    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            NovaTopBar(
                title = "Calculator",
                onBack = onBack,
                neonAccent = primaryAccent,
                actions = {
                    IconButton(onClick = { 
                        NovaHaptics.click(view)
                        showHistorySheet = true 
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = "Calculation History",
                            tint = primaryAccent
                        )
                    }
                    TextButton(onClick = { 
                        NovaHaptics.toggle(view)
                        isScientificMode = !isScientificMode 
                    }) {
                        Text(
                            text = if (isScientificMode) "Basic" else "Scientific",
                            color = primaryAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
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
                    .padding(NovaTokens.Spacing.md),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // High-tech Monospaced Displays (JetBrains Mono)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = NovaTokens.Spacing.md),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = expression.ifEmpty { "0" },
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        style = NovaTypography.dataMedium.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        textAlign = TextAlign.Right,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(NovaTokens.Spacing.xs))
                    Text(
                        text = resultText,
                        color = if (resultText == "Error") NovaError else primaryAccent,
                        style = NovaTypography.dataLarge.copy(
                            fontSize = if (resultText.length > 8) 32.sp else 44.sp,
                            fontWeight = FontWeight.Black
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Keyboard
                if (!isScientificMode) {
                    // Basic buttons (4 columns)
                    val rows = listOf(
                        listOf("C", "⌫", "%", "÷"),
                        listOf("7", "8", "9", "×"),
                        listOf("4", "5", "6", "-"),
                        listOf("1", "2", "3", "+"),
                        listOf("0", ".", "=")
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(NovaTokens.Spacing.sm)
                    ) {
                        rows.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(NovaTokens.Spacing.sm)
                            ) {
                                row.forEach { btn ->
                                    val isOperator = btn in listOf("÷", "×", "-", "+")
                                    val isEqual = btn == "="
                                    val isClear = btn in listOf("C", "⌫")
                                    
                                    val keyColor = when {
                                        isEqual -> primaryAccent
                                        isClear -> NovaError
                                        isOperator -> primaryAccent
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }

                                    val interactionSource = remember { MutableInteractionSource() }
                                    val isPressed by interactionSource.collectIsPressedAsState()
                                    val scale by animateFloatAsState(
                                        targetValue = if (isPressed) NovaTokens.Motion.pressScaleButton else 1f,
                                        animationSpec = NovaTokens.Motion.springSnappy,
                                        label = "key_scale"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .weight(if (btn == "0") 2f else 1f)
                                            .height(68.dp)
                                            .scale(scale)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(
                                                when {
                                                    isEqual -> primaryAccent.copy(alpha = if (isPressed) 0.8f else 1f)
                                                    isOperator -> primaryAccent.copy(alpha = 0.15f)
                                                    isClear -> NovaError.copy(alpha = 0.12f)
                                                    else -> if (isSystemInDarkTheme()) NovaMidnightBlue.copy(alpha = 0.4f) else NovaPureWhite.copy(alpha = 0.5f)
                                                }
                                            )
                                            .clickable(
                                                interactionSource = interactionSource,
                                                indication = null
                                            ) { handleButtonClick(btn) }
                                            .then(
                                                if (isEqual) Modifier
                                                else Modifier.border(
                                                    BorderStroke(
                                                        width = 1.dp,
                                                        color = when {
                                                            isOperator -> primaryAccent.copy(alpha = 0.3f)
                                                            isClear -> NovaError.copy(alpha = 0.3f)
                                                            else -> if (isSystemInDarkTheme()) NovaBorderDark.copy(alpha = 0.2f) else NovaBorderLight.copy(alpha = 0.2f)
                                                        }
                                                    ),
                                                    shape = RoundedCornerShape(20.dp)
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = btn,
                                            style = NovaTypography.dataMedium.copy(
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isEqual) NovaDeepInk else keyColor
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Scientific buttons (5 columns)
                    val rows = listOf(
                        listOf("sin", "cos", "tan", "log", "ln"),
                        listOf("(", ")", "√", "^", "π"),
                        listOf("C", "⌫", "%", "÷", "e"),
                        listOf("7", "8", "9", "×", "="),
                        listOf("4", "5", "6", "-", "."),
                        listOf("1", "2", "3", "+", "0")
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(NovaTokens.Spacing.xs)
                    ) {
                        rows.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(NovaTokens.Spacing.xs)
                            ) {
                                row.forEach { btn ->
                                    val isOperator = btn in listOf("÷", "×", "-", "+")
                                    val isEqual = btn == "="
                                    val isTrigFunc = btn in listOf("sin", "cos", "tan", "log", "ln", "√", "^", "(", ")", "π", "e", "%")
                                    val isClear = btn in listOf("C", "⌫")

                                    val keyColor = when {
                                        isEqual -> primaryAccent
                                        isClear -> NovaError
                                        isOperator -> primaryAccent
                                        isTrigFunc -> primaryAccent.copy(alpha = 0.8f)
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }

                                    val interactionSource = remember { MutableInteractionSource() }
                                    val isPressed by interactionSource.collectIsPressedAsState()
                                    val scale by animateFloatAsState(
                                        targetValue = if (isPressed) NovaTokens.Motion.pressScaleButton else 1f,
                                        animationSpec = NovaTokens.Motion.springSnappy,
                                        label = "key_scale_sci"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(54.dp)
                                            .scale(scale)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                when {
                                                    isEqual -> primaryAccent.copy(alpha = if (isPressed) 0.8f else 1f)
                                                    isOperator -> primaryAccent.copy(alpha = 0.15f)
                                                    isTrigFunc -> primaryAccent.copy(alpha = 0.08f)
                                                    isClear -> NovaError.copy(alpha = 0.12f)
                                                    else -> if (isSystemInDarkTheme()) NovaMidnightBlue.copy(alpha = 0.4f) else NovaPureWhite.copy(alpha = 0.5f)
                                                }
                                            )
                                            .clickable(
                                                interactionSource = interactionSource,
                                                indication = null
                                            ) { handleButtonClick(btn) }
                                            .then(
                                                if (isEqual) Modifier
                                                else Modifier.border(
                                                    BorderStroke(
                                                        width = 1.dp,
                                                        color = when {
                                                            isOperator -> primaryAccent.copy(alpha = 0.3f)
                                                            isTrigFunc -> primaryAccent.copy(alpha = 0.15f)
                                                            isClear -> NovaError.copy(alpha = 0.3f)
                                                            else -> if (isSystemInDarkTheme()) NovaBorderDark.copy(alpha = 0.2f) else NovaBorderLight.copy(alpha = 0.2f)
                                                        }
                                                    ),
                                                    shape = RoundedCornerShape(16.dp)
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = btn,
                                            style = NovaTypography.dataMedium.copy(
                                                fontSize = if (btn.length > 3) 13.sp else 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isEqual) NovaDeepInk else keyColor
                                            )
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

    if (showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            containerColor = if (isSystemInDarkTheme()) NovaDeepSpace else NovaPureWhite,
            dragHandle = { BottomSheetDefaults.DragHandle(color = primaryAccent.copy(alpha = 0.5f)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = NovaTokens.Spacing.xl, vertical = NovaTokens.Spacing.xs)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Calculation History",
                        style = NovaTypography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = primaryAccent
                        )
                    )
                    IconButton(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val db = DatabaseProvider.getDatabase(context)
                                    db.calculatorHistoryDao().clearAll()
                                    withContext(Dispatchers.Main) {
                                        historyItems.clear()
                                        NovaHaptics.reject(view)
                                        Toast.makeText(context, "History cleared", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            tint = NovaError,
                            contentDescription = "Clear History"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(NovaTokens.Spacing.md))

                if (historyItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No history recorded yet",
                            style = NovaTypography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(NovaTokens.Spacing.xs)
                    ) {
                        items(historyItems) { item ->
                            GlassCard(
                                neonAccent = primaryAccent,
                                onClick = {
                                    expression = item.expression
                                    resultText = item.result
                                    showHistorySheet = false
                                }
                            ) {
                                Column(modifier = Modifier.padding(NovaTokens.Spacing.md)) {
                                    Text(
                                        text = item.expression,
                                        style = NovaTypography.dataSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "= ${item.result}",
                                        style = NovaTypography.dataMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = primaryAccent
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(NovaTokens.Spacing.xxl))
            }
        }
    }
}
