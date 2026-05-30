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
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.*
import java.util.Calendar
import java.util.Locale
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompassScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }
    val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    var azimuth by remember { mutableStateOf(0f) }
    var pitch by remember { mutableStateOf(0f) }
    var roll by remember { mutableStateOf(0f) }
    var sensorAccuracy by remember { mutableStateOf(SensorManager.SENSOR_STATUS_ACCURACY_HIGH) }
    var hasSensors by remember { mutableStateOf(accelerometer != null && magnetometer != null) }

    // Compass controls
    var activeTab by remember { mutableStateOf("COMPASS") } // "COMPASS", "LEVEL"
    var declination by remember { mutableStateOf(0f) } // offset in degrees

    // Sunrise/Sunset location coordinates
    var latitude by remember { mutableStateOf(13.0827f) } // default Chennai, India
    var longitude by remember { mutableStateOf(80.2707f) }
    var showLocationDialog by remember { mutableStateOf(false) }

    // Haptic feedback logic for Level alignment
    var lastVibrationTime by remember { mutableStateOf(0L) }
    val isLevel = abs(pitch) <= 1.0f && abs(roll) <= 1.0f

    val primaryAccent = NeonLime // Electric Lime for Utility tools

    LaunchedEffect(isLevel) {
        if (isLevel && activeTab == "LEVEL") {
            val now = System.currentTimeMillis()
            if (now - lastVibrationTime > 2000) { // Throttle vibration to every 2 seconds
                lastVibrationTime = now
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(80L, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(80L)
                    }
                }
            }
        }
    }

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
                        
                        // Azimuth
                        val azInRadians = orientation[0]
                        var azInDegrees = Math.toDegrees(azInRadians.toDouble()).toFloat()
                        azInDegrees = (azInDegrees + 360) % 360
                        azimuth = azInDegrees

                        // Pitch (-180 to 180) & Roll (-90 to 90)
                        pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
                        roll = Math.toDegrees(orientation[2].toDouble()).toFloat()
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    sensorAccuracy = accuracy
                }
            }
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

    // Adjust compass heading with declination
    val heading = remember(azimuth, declination) {
        (azimuth + declination + 360f) % 360f
    }

    // Animate compass rotation smoothly
    var targetRotation by remember { mutableStateOf(0f) }
    LaunchedEffect(heading) {
        val diff = heading - targetRotation
        val normalizedDiff = ((diff + 180) % 360) - 180
        targetRotation += normalizedDiff
    }

    val animatedRotation by animateFloatAsState(
        targetValue = -targetRotation,
        animationSpec = tween(durationMillis = 150),
        label = "compass_dial_rotation"
    )

    // Calculate heading direction text
    val directionText = remember(heading) {
        when (heading) {
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

    // Calculate Sunrise & Sunset
    val sunriseSunsetTimes = remember(latitude, longitude) {
        calculateSunriseSunset(latitude.toDouble(), longitude.toDouble())
    }

    BackHandler {
        onBack()
    }

    val isDark = isSystemInDarkTheme()

    Scaffold(
        topBar = {
            NovaTopBar(
                title = "Compass & Level",
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
                    .padding(horizontal = NovaTokens.Spacing.md, vertical = NovaTokens.Spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!hasSensors) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        GlassCard(
                            neonAccent = NovaError,
                            enableGlow = true
                        ) {
                            Column(
                                modifier = Modifier.padding(NovaTokens.Spacing.xl),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🧭", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Sensors Not Detected",
                                    style = NovaTypography.headlineLarge.copy(
                                        color = NovaError,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Your device lacks an accelerometer or magnetic field sensor required to operate the compass offline.",
                                    style = NovaTypography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    // Futuristic glass segmented tab control
                    NovaSegmentedControl(
                        items = listOf("Compass Instrument", "Bubble Inclinometer"),
                        selectedIndex = if (activeTab == "COMPASS") 0 else 1,
                        onIndexSelected = { activeTab = if (it == 0) "COMPASS" else "LEVEL" },
                        neonColor = primaryAccent,
                        modifier = Modifier.padding(vertical = NovaTokens.Spacing.xs)
                    )

                    Spacer(modifier = Modifier.height(NovaTokens.Spacing.sm))

                    if (activeTab == "COMPASS") {
                        // COMPASS VIEW
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(NovaTokens.Spacing.md),
                            contentPadding = PaddingValues(bottom = NovaTokens.Spacing.lg)
                        ) {
                            // Heading Values Box
                            item {
                                GlassCard(
                                    neonAccent = primaryAccent,
                                    enableGlow = true
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(NovaTokens.Spacing.md),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = directionText,
                                                style = NovaTypography.displayLarge.copy(
                                                    fontSize = 40.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = primaryAccent,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Text(
                                                text = "${heading.toInt()}°",
                                                style = NovaTypography.displayLarge.copy(
                                                    fontSize = 40.sp,
                                                    fontWeight = FontWeight.Light,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            )
                                        }
                                        
                                        val accText = when (sensorAccuracy) {
                                            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "HIGH PRECISION"
                                            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "MEDIUM PRECISION"
                                            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "LOW PRECISION"
                                            else -> "ACCURACY UNRELIABLE"
                                        }
                                        val accColor = when (sensorAccuracy) {
                                            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> primaryAccent
                                            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> NeonGold
                                            else -> NovaError
                                        }
                                        
                                        Spacer(modifier = Modifier.height(NovaTokens.Spacing.xxs))
                                        Text(
                                            text = accText,
                                            style = NovaTypography.tagMono.copy(
                                                color = accColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }

                            // Rotatable Dial face
                            item {
                                Box(
                                    modifier = Modifier
                                        .size(240.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.02f)
                                        )
                                        .border(
                                            width = 2.dp,
                                            color = primaryAccent.copy(alpha = 0.3f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Rotation layer
                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer(rotationZ = animatedRotation)
                                    ) {
                                        val center = this.center
                                        val radius = size.minDimension / 2f
                                        
                                        // Dial graduation marks
                                        for (angle in 0 until 360 step 5) {
                                            val angleRad = Math.toRadians(angle.toDouble())
                                            val lineLength = if (angle % 30 == 0) radius * 0.12f else radius * 0.06f
                                            val startRadius = radius - lineLength
                                            
                                            val startX = center.x + (startRadius * sin(angleRad)).toFloat()
                                            val startY = center.y - (startRadius * cos(angleRad)).toFloat()
                                            
                                            val endX = center.x + (radius * sin(angleRad)).toFloat()
                                            val endY = center.y - (radius * cos(angleRad)).toFloat()
                                            
                                            drawLine(
                                                color = if (angle % 30 == 0) primaryAccent else (if (isDark) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.25f)),
                                                start = androidx.compose.ui.geometry.Offset(startX, startY),
                                                end = androidx.compose.ui.geometry.Offset(endX, endY),
                                                strokeWidth = if (angle % 30 == 0) 2.5f else 1f
                                            )
                                        }

                                        // Directions markers
                                        val textDirections = listOf("N" to 0f, "E" to 90f, "S" to 180f, "W" to 270f)
                                        textDirections.forEach { (dir, angle) ->
                                            rotate(angle) {
                                                val lineLength = radius * 0.15f
                                                val startX = center.x
                                                val startY = center.y - radius + lineLength
                                                
                                                val path = Path().apply {
                                                    moveTo(startX, startY)
                                                    lineTo(startX - 5f, startY + 7f)
                                                    lineTo(startX + 5f, startY + 7f)
                                                    close()
                                                }
                                                drawPath(
                                                    path = path,
                                                    color = if (dir == "N") NovaError else primaryAccent
                                                )
                                            }
                                        }
                                    }

                                    // Static pointer needle (North points up, South points down)
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val center = this.center
                                        val radius = size.minDimension / 2f
                                        
                                        drawCircle(color = primaryAccent, radius = 8f, center = center)
                                        drawCircle(color = if (isDark) NovaMidnightBlue else Color.White, radius = 3f, center = center)

                                        val needleLength = radius * 0.7f
                                        val northPath = Path().apply {
                                            moveTo(center.x, center.y - needleLength)
                                            lineTo(center.x - 7f, center.y)
                                            lineTo(center.x, center.y - 5f)
                                            close()
                                        }
                                        drawPath(path = northPath, color = NovaError)

                                        val southPath = Path().apply {
                                            moveTo(center.x, center.y + needleLength)
                                            lineTo(center.x + 7f, center.y)
                                            lineTo(center.x, center.y + 5f)
                                            close()
                                        }
                                        drawPath(path = southPath, color = if (isDark) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.3f))
                                    }
                                }
                            }

                            // Magnetic Declination Adjustment Card
                            item {
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
                                                text = "Magnetic Declination Offset",
                                                style = NovaTypography.headlineSmall.copy(
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                            Text(
                                                text = String.format(Locale.getDefault(), "%+.1f°", declination),
                                                style = NovaTypography.tagMono.copy(
                                                    fontWeight = FontWeight.Black,
                                                    color = primaryAccent,
                                                    fontSize = 12.sp
                                                )
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(NovaTokens.Spacing.xxs))
                                        NovaSlider(
                                            value = declination,
                                            onValueChange = { declination = it },
                                            valueRange = -20f..20f,
                                            neonColor = primaryAccent
                                        )
                                        Text(
                                            text = "Compensates for the local angular variance between True Geographic North and Magnetic North.",
                                            style = NovaTypography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Sunrise / Sunset Estimation Card
                            item {
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
                                                text = "Astronomical Solar Calculator",
                                                style = NovaTypography.headlineSmall.copy(
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                            IconButton(
                                                onClick = {
                                                    NovaHaptics.click(view)
                                                    showLocationDialog = true
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.EditLocation,
                                                    contentDescription = "Edit Location",
                                                    tint = primaryAccent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(NovaTokens.Spacing.xs))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("☀️ SOLAR SUNRISE", style = NovaTypography.tagMono.copy(fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = sunriseSunsetTimes.first,
                                                    style = NovaTypography.headlineLarge.copy(
                                                        fontWeight = FontWeight.Black,
                                                        color = primaryAccent,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("🌙 SOLAR SUNSET", style = NovaTypography.tagMono.copy(fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = sunriseSunsetTimes.second,
                                                    style = NovaTypography.headlineLarge.copy(
                                                        fontWeight = FontWeight.Black,
                                                        color = primaryAccent,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(NovaTokens.Spacing.xs))
                                        Text(
                                            text = String.format(Locale.getDefault(), "Coordinates: %.3f° N, %.3f° E (100%% Offline calculation)", latitude, longitude),
                                            style = NovaTypography.bodySmall.copy(fontSize = 10.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // BUBBLE LEVEL VIEW
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            GlassCard(
                                neonAccent = if (isLevel) primaryAccent else NovaError,
                                enableGlow = isLevel
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(NovaTokens.Spacing.md),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (isLevel) "ALIGNMENT ALIGNED" else "TILT OFFSET",
                                        style = NovaTypography.tagMono.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isLevel) primaryAccent else NovaError,
                                            letterSpacing = 1.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(NovaTokens.Spacing.xs))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("PITCH (Y-AXIS)", style = NovaTypography.tagMono.copy(fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = String.format(Locale.getDefault(), "%+.1f°", pitch),
                                                style = NovaTypography.headlineMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isLevel) primaryAccent else MaterialTheme.colorScheme.onSurface,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("ROLL (X-AXIS)", style = NovaTypography.tagMono.copy(fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = String.format(Locale.getDefault(), "%+.1f°", roll),
                                                style = NovaTypography.headlineMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isLevel) primaryAccent else MaterialTheme.colorScheme.onSurface,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(NovaTokens.Spacing.lg))

                            // Inclinometer level graphical Canvas widget
                            Box(
                                modifier = Modifier
                                    .size(240.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.02f)
                                    )
                                    .border(
                                        width = 2.dp,
                                        color = if (isLevel) primaryAccent.copy(alpha = 0.6f) else NovaBorderDark.copy(alpha = 0.2f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                // Drawing circles on canvas
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val center = this.center
                                    val radius = size.minDimension / 2f
                                    
                                    // Draw Target Crosshairs
                                    drawLine(
                                        color = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.15f),
                                        start = androidx.compose.ui.geometry.Offset(center.x - radius, center.y),
                                        end = androidx.compose.ui.geometry.Offset(center.x + radius, center.y),
                                        strokeWidth = 1.5f
                                    )
                                    drawLine(
                                        color = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.15f),
                                        start = androidx.compose.ui.geometry.Offset(center.x, center.y - radius),
                                        end = androidx.compose.ui.geometry.Offset(center.x, center.y + radius),
                                        strokeWidth = 1.5f
                                    )
                                    
                                    // Draw target center circles
                                    drawCircle(
                                        color = if (isLevel) primaryAccent.copy(alpha = 0.4f) else (if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.12f)),
                                        radius = radius * 0.25f,
                                        center = center,
                                        style = Stroke(width = 1.5f)
                                    )
                                    drawCircle(
                                        color = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.12f),
                                        radius = radius * 0.5f,
                                        center = center,
                                        style = Stroke(width = 1.5f)
                                    )

                                    // Calculate bubble offsets from Pitch and Roll
                                    // Roll maps to X, Pitch maps to Y
                                    val maxAngle = 20f // degrees for edge of dial
                                    val pixelScale = (radius * 0.85f) / maxAngle
                                    
                                    val bubbleX = (center.x + (roll * pixelScale)).coerceIn(center.x - radius * 0.9f, center.x + radius * 0.9f)
                                    val bubbleY = (center.y + (pitch * pixelScale)).coerceIn(center.y - radius * 0.9f, center.y + radius * 0.9f)

                                    // Draw the bubble
                                    drawCircle(
                                        color = if (isLevel) primaryAccent else (if (isDark) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.5f)),
                                        radius = 22f,
                                        center = androidx.compose.ui.geometry.Offset(bubbleX, bubbleY)
                                    )
                                    drawCircle(
                                        color = Color.White.copy(alpha = 0.7f),
                                        radius = 7f,
                                        center = androidx.compose.ui.geometry.Offset(bubbleX - 6f, bubbleY - 6f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(NovaTokens.Spacing.lg))
                            
                            // Tip box
                            GlassCard(
                                neonAccent = primaryAccent,
                                enableGlow = false
                            ) {
                                Row(
                                    modifier = Modifier.padding(NovaTokens.Spacing.md),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("📱", fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Lay your device completely flat on any surface to calibrate and use the bubble level alignment instrument.",
                                        style = NovaTypography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Coordinates Customization Input Dialog
    if (showLocationDialog) {
        var latInput by remember { mutableStateOf(latitude.toString()) }
        var lngInput by remember { mutableStateOf(longitude.toString()) }

        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = { 
                Text(
                    text = "Update Location Coords", 
                    style = NovaTypography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = primaryAccent)
                ) 
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter decimal coordinates to accurately calibrate offline sunrise and sunset estimations.", 
                        style = NovaTypography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    NovaTextField(
                        value = latInput,
                        onValueChange = { latInput = it },
                        placeholder = "Latitude (e.g. 13.0827)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        neonColor = primaryAccent
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    NovaTextField(
                        value = lngInput,
                        onValueChange = { lngInput = it },
                        placeholder = "Longitude (e.g. 80.2707)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        neonColor = primaryAccent
                    )
                }
            },
            confirmButton = {
                NovaPrimaryButton(
                    text = "Save",
                    neonColor = primaryAccent,
                    onClick = {
                        val latVal = latInput.toFloatOrNull()
                        val lngVal = lngInput.toFloatOrNull()
                        if (latVal != null && lngVal != null) {
                            latitude = latVal.coerceIn(-90f, 90f)
                            longitude = lngVal.coerceIn(-180f, 180f)
                            showLocationDialog = false
                        } else {
                            Toast.makeText(context, "Please enter valid decimal coordinates.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            },
            dismissButton = {
                NovaSecondaryButton(
                    text = "Cancel",
                    neonColor = primaryAccent,
                    onClick = { showLocationDialog = false }
                )
            },
            containerColor = if (isDark) NovaMidnightBlue else Color.White,
            shape = RoundedCornerShape(NovaTokens.Radius.lg),
            modifier = Modifier.border(1.dp, primaryAccent.copy(alpha = 0.2f), RoundedCornerShape(NovaTokens.Radius.lg))
        )
    }
}

/**
 * Solar Declination Equation offline calculation for Sunrise/Sunset times approximation
 */
private fun calculateSunriseSunset(lat: Double, lng: Double, timezone: Double = 5.5): Pair<String, String> {
    try {
        val calendar = Calendar.getInstance()
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        
        val radiansLat = Math.toRadians(lat)
        
        // Solar Declination (degrees)
        val declination = 23.45 * sin(Math.toRadians(360.0 * (284.0 + dayOfYear) / 365.0))
        val radiansDec = Math.toRadians(declination)
        
        // Hour angle (approximate for sunset/sunrise, where altitude = -0.833 degrees)
        val zenith = Math.toRadians(-0.833)
        val cosHourAngle = (sin(zenith) - sin(radiansLat) * sin(radiansDec)) / (cos(radiansLat) * cos(radiansDec))
        
        if (cosHourAngle > 1.0) return "Polar Night" to "Polar Night"
        if (cosHourAngle < -1.0) return "Midnight Sun" to "Midnight Sun"
        
        val hourAngle = Math.toDegrees(acos(cosHourAngle))
        
        // Solar Noon (hours)
        // Equation of time approximation
        val b = Math.toRadians(360.0 * (dayOfYear - 81) / 365.0)
        val equationOfTime = 9.87 * sin(2.0 * b) - 7.53 * cos(b) - 1.5 * sin(b) // in minutes
        
        val solarNoon = 12.0 - (lng / 15.0) + timezone - (equationOfTime / 60.0)
        
        val sunriseTime = solarNoon - (hourAngle / 15.0)
        val sunsetTime = solarNoon + (hourAngle / 15.0)
        
        fun formatTime(time: Double): String {
            val totalMinutes = (time * 60).roundToInt()
            val h = (totalMinutes / 60 + 24) % 24
            val m = abs(totalMinutes % 60)
            
            val amPm = if (h >= 12) "PM" else "AM"
            val formattedHour = if (h % 12 == 0) 12 else h % 12
            return String.format(Locale.getDefault(), "%02d:%02d %s", formattedHour, m, amPm)
        }
        
        return formatTime(sunriseTime) to formatTime(sunsetTime)
    } catch (e: Exception) {
        return "06:00 AM" to "06:00 PM"
    }
}
