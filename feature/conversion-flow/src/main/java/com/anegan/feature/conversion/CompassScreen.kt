/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.conversion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.MidnightIndigo
import com.anegan.core.designsystem.theme.PureWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompassScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    var azimuth by remember { mutableStateOf(0f) }
    var hasSensors by remember { mutableStateOf(accelerometer != null && magnetometer != null) }

    // Sensor Listener
    val sensorEventListener = remember {
        object : SensorEventListener {
            private var gravity = FloatArray(3)
            private var geomagnetic = FloatArray(3)
            private var hasGravity = false
            private var hasGeomagnetic = false

            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    System.arraycopy(event.values, 0, gravity, 0, event.values.size)
                    hasGravity = true
                } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    System.arraycopy(event.values, 0, geomagnetic, 0, event.values.size)
                    hasGeomagnetic = true
                }

                if (hasGravity && hasGeomagnetic) {
                    val r = FloatArray(9)
                    val i = FloatArray(9)
                    if (SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)) {
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(r, orientation)
                        val azInRadians = orientation[0]
                        var azInDegrees = Math.toDegrees(azInRadians.toDouble()).toFloat()
                        azInDegrees = (azInDegrees + 360) % 360
                        azimuth = azInDegrees
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }

    DisposableEffect(hasSensors) {
        if (hasSensors) {
            sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(sensorEventListener, magnetometer, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose {
            if (hasSensors) {
                sensorManager.unregisterListener(sensorEventListener)
            }
        }
    }

    // Animate compass rotation smoothly
    // Standardize rotation angle to prevent wrap-around spinning (e.g. from 359 to 0)
    var targetRotation by remember { mutableStateOf(0f) }
    LaunchedEffect(azimuth) {
        val diff = azimuth - targetRotation
        // Find shortest path to rotate
        val normalizedDiff = ((diff + 180) % 360) - 180
        targetRotation += normalizedDiff
    }

    val animatedRotation by animateFloatAsState(
        targetValue = -targetRotation, // Negative because dial rotates in opposite direction of heading
        animationSpec = tween(durationMillis = 150),
        label = "compass_dial_rotation"
    )

    // Calculate heading direction text
    val directionText = remember(azimuth) {
        when (azimuth) {
            in 337.5..360.0, in 0.0..22.5 -> "N"
            in 22.5..67.5 -> "NE"
            in 67.5..112.5 -> "E"
            in 112.5..157.5 -> "SE"
            in 157.5..202.5 -> "S"
            in 202.5..247.5 -> "SW"
            in 247.5..292.5 -> "W"
            in 292.5..337.5 -> "NW"
            else -> "N"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
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
                text = "Compass",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp),
                color = MidnightIndigo
            )
        }

        if (!hasSensors) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = borderStroke()
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🧭", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Sensors Not Detected",
                            color = MidnightIndigo,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your device lacks an accelerometer or magnetic field sensor required to operate the compass offline.",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.height(32.dp))

            // Premium Compass Value Box
            Card(
                modifier = Modifier.fillMaxWidth(0.8f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = borderStroke()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = directionText,
                        fontSize = 54.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MidnightIndigo,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${azimuth.toInt()}° Heading",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.1f))

            // Interactive Compass Dial Component
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(140.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White,
                                Color(0xFFF1F5F9)
                            )
                        )
                    )
                    .border(0.5.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(140.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Rotatable Dial face
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(rotationZ = animatedRotation)
                ) {
                    val center = this.center
                    val radius = size.minDimension / 2f
                    
                    // Draw outer graduation markers
                    for (angle in 0 until 360 step 5) {
                        val angleRad = Math.toRadians(angle.toDouble())
                        val lineLength = if (angle % 30 == 0) radius * 0.12f else radius * 0.06f
                        val startRadius = radius - lineLength
                        
                        val startX = center.x + (startRadius * Math.sin(angleRad)).toFloat()
                        val startY = center.y - (startRadius * Math.cos(angleRad)).toFloat()
                        
                        val endX = center.x + (radius * Math.sin(angleRad)).toFloat()
                        val endY = center.y - (radius * Math.cos(angleRad)).toFloat()
                        
                        drawLine(
                            color = if (angle % 30 == 0) MidnightIndigo else Color.LightGray,
                            start = androidx.compose.ui.geometry.Offset(startX, startY),
                            end = androidx.compose.ui.geometry.Offset(endX, endY),
                            strokeWidth = if (angle % 30 == 0) 2.5f else 1f
                        )
                    }

                    // Render N S E W Text
                    val textDirections = listOf("N" to 0f, "E" to 90f, "S" to 180f, "W" to 270f)
                    // Draw custom graphic indicators for N, S, E, W
                    textDirections.forEach { (dir, angle) ->
                        rotate(angle) {
                            // Draw direction indicator lines or labels
                            val lineLength = radius * 0.16f
                            val startX = center.x
                            val startY = center.y - radius + lineLength
                            
                            val path = Path().apply {
                                moveTo(startX, startY)
                                lineTo(startX - 6f, startY + 8f)
                                lineTo(startX + 6f, startY + 8f)
                                close()
                            }
                            
                            drawPath(
                                path = path,
                                color = if (dir == "N") Color.Red else MidnightIndigo
                            )
                        }
                    }
                }

                // Top Static Needle Pointer (Always points straight up)
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val center = this.center
                    val radius = size.minDimension / 2f
                    
                    // Center pivot point
                    drawCircle(
                        color = MidnightIndigo,
                        radius = 12f,
                        center = center
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4f,
                        center = center
                    )

                    // Standard Static direction marker pointing to actual N indicator on dial
                    val needleLength = radius * 0.75f
                    
                    // Upper half (North)
                    val northPath = Path().apply {
                        moveTo(center.x, center.y - needleLength)
                        lineTo(center.x - 10f, center.y)
                        lineTo(center.x, center.y - 8f)
                        close()
                    }
                    drawPath(
                        path = northPath,
                        color = Color.Red
                    )

                    // Lower half (South)
                    val southPath = Path().apply {
                        moveTo(center.x, center.y + needleLength)
                        lineTo(center.x + 10f, center.y)
                        lineTo(center.x, center.y + 8f)
                        close()
                    }
                    drawPath(
                        path = southPath,
                        color = Color.LightGray
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.1f))

            // Calibration Guidance Tip
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                border = borderStroke()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💡", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "If direction feels inaccurate, calibrate by waving your device in a figure-8 pattern.",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun borderStroke() = androidx.compose.foundation.BorderStroke(
    width = 0.5.dp,
    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
)
