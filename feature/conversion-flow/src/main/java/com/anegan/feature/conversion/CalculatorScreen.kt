/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.conversion

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.MidnightIndigo
import com.anegan.core.designsystem.theme.PureWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var displayValue by remember { mutableStateOf("0") }
    var expressionValue by remember { mutableStateOf("") }
    
    // Internal evaluation states
    var lastOperator by remember { mutableStateOf("") }
    var operandValue by remember { mutableStateOf(0.0) }
    var isNewNumberStarted by remember { mutableStateOf(true) }

    fun calculate(num1: Double, num2: Double, op: String): Double {
        return when (op) {
            "+" -> num1 + num2
            "-" -> num1 - num2
            "*" -> num1 * num2
            "/" -> if (num2 != 0.0) num1 / num2 else 0.0
            else -> num2
        }
    }

    fun handleButtonClick(label: String) {
        when {
            label in "0123456789." -> {
                if (isNewNumberStarted || displayValue == "0") {
                    displayValue = if (label == ".") "0." else label
                    isNewNumberStarted = false
                } else {
                    if (label != "." || !displayValue.contains(".")) {
                        displayValue += label
                    }
                }
            }
            label == "C" -> {
                displayValue = "0"
                expressionValue = ""
                operandValue = 0.0
                lastOperator = ""
                isNewNumberStarted = true
            }
            label == "⌫" -> {
                if (displayValue.length > 1) {
                    displayValue = displayValue.dropLast(1)
                } else {
                    displayValue = "0"
                    isNewNumberStarted = true
                }
            }
            label == "%" -> {
                val current = displayValue.toDoubleOrNull() ?: 0.0
                displayValue = (current / 100.0).toString()
                isNewNumberStarted = true
            }
            label in listOf("+", "-", "*", "/") -> {
                val currentNum = displayValue.toDoubleOrNull() ?: 0.0
                if (lastOperator.isNotEmpty() && !isNewNumberStarted) {
                    operandValue = calculate(operandValue, currentNum, lastOperator)
                    displayValue = if (operandValue % 1.0 == 0.0) operandValue.toInt().toString() else operandValue.toString()
                } else {
                    operandValue = currentNum
                }
                lastOperator = label
                expressionValue = "${if (operandValue % 1.0 == 0.0) operandValue.toInt().toString() else operandValue.toString()} $label"
                isNewNumberStarted = true
            }
            label == "=" -> {
                val currentNum = displayValue.toDoubleOrNull() ?: 0.0
                if (lastOperator.isNotEmpty()) {
                    val finalResult = calculate(operandValue, currentNum, lastOperator)
                    expressionValue = ""
                    displayValue = if (finalResult % 1.0 == 0.0) finalResult.toInt().toString() else finalResult.toString()
                    lastOperator = ""
                    operandValue = finalResult
                    isNewNumberStarted = true
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
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
                Text(
                    text = "←",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp),
                    color = MidnightIndigo
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Calculator",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp),
                color = MidnightIndigo
            )
        }

        // Display Screen Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = expressionValue,
                color = Color.Gray,
                fontSize = 18.sp,
                maxLines = 1,
                textAlign = TextAlign.Right
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = displayValue,
                color = MidnightIndigo,
                fontSize = if (displayValue.length > 8) 32.sp else 48.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Sleek grid layout of buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val rows = listOf(
                listOf("C", "⌫", "%", "/"),
                listOf("7", "8", "9", "*"),
                listOf("4", "5", "6", "-"),
                listOf("1", "2", "3", "+"),
                listOf("0", ".", "=")
            )

            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { buttonLabel ->
                        val isOperator = buttonLabel in listOf("/", "*", "-", "+", "=")
                        val isClear = buttonLabel in listOf("C", "⌫")
                        val isZero = buttonLabel == "0"

                        Box(
                            modifier = Modifier
                                .weight(if (isZero) 2f else 1f)
                                .height(64.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    when {
                                        isOperator -> MidnightIndigo
                                        isClear -> Color(0xFFFFEAEA)
                                        else -> MaterialTheme.colorScheme.surface
                                    }
                                )
                                .border(
                                    width = 0.5.dp,
                                    color = if (isOperator) Color.Transparent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { handleButtonClick(buttonLabel) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = buttonLabel,
                                fontSize = if (buttonLabel == "⌫") 18.sp else 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isOperator -> PureWhite
                                    isClear -> Color.Red
                                    else -> MidnightIndigo
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
