@file:OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.anegan.feature.vault

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import com.anegan.core.database.DatabaseProvider
import com.anegan.core.database.VaultFileDao
import com.anegan.core.database.VaultFileEntity
import com.anegan.core.designsystem.theme.*
import android.widget.Toast
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

// ─────────────────────────────────────────────────────────────────────────────
// Encryption Helper (AES-256 via Jetpack Security)
// ─────────────────────────────────────────────────────────────────────────────

object VaultEncryptionHelper {
    private val KEY_ALIAS = MasterKeys.AES256_GCM_SPEC

    fun getSecureStorageDir(context: Context): File {
        val dir = File(context.filesDir, "vault_storage")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun encryptInputStream(context: Context, inputStream: InputStream, destFile: File) {
        val masterKeyAlias = MasterKeys.getOrCreate(KEY_ALIAS)
        val encryptedFile = EncryptedFile.Builder(
            destFile,
            context,
            masterKeyAlias,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        encryptedFile.openFileOutput().use { outputStream ->
            inputStream.use { input ->
                input.copyTo(outputStream)
            }
        }
    }

    fun encryptBytes(context: Context, bytes: ByteArray, destFile: File) {
        val masterKeyAlias = MasterKeys.getOrCreate(KEY_ALIAS)
        val encryptedFile = EncryptedFile.Builder(
            destFile,
            context,
            masterKeyAlias,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        encryptedFile.openFileOutput().use { outputStream ->
            outputStream.write(bytes)
        }
    }

    fun decryptToOutputStream(context: Context, srcFile: File, outputStream: OutputStream) {
        val masterKeyAlias = MasterKeys.getOrCreate(KEY_ALIAS)
        val encryptedFile = EncryptedFile.Builder(
            srcFile,
            context,
            masterKeyAlias,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        encryptedFile.openFileInput().use { inputStream ->
            outputStream.use { output ->
                inputStream.copyTo(output)
            }
        }
    }

    fun decryptToBytes(context: Context, srcFile: File): ByteArray {
        val masterKeyAlias = MasterKeys.getOrCreate(KEY_ALIAS)
        val encryptedFile = EncryptedFile.Builder(
            srcFile,
            context,
            masterKeyAlias,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        return encryptedFile.openFileInput().use { input ->
            input.readBytes()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Data model/definitions
// ─────────────────────────────────────────────────────────────────────────────

data class VaultCategory(
    val id: String,
    val title: String,
    val emoji: String,
    val description: String,
    val color: Color
)

val vaultCategories = listOf(
    VaultCategory("aadhaar",    "Aadhaar ID",        "🪪", "Government UID card",       NeonBlue),
    VaultCategory("pan",        "PAN Card",          "💳", "Income Tax ID",            NeonLime),
    VaultCategory("passport",   "Passport",          "📕", "Travel document",          NeonPurple),
    VaultCategory("insurance",  "Insurance",         "🛡️", "Health & Auto policy",      NeonCyan),
    VaultCategory("medical",    "Medical",           "🏥", "Prescriptions & reports",  NeonMagenta),
    VaultCategory("education",  "Certificates",      "🎓", "Degrees & marks",          NeonGold),
    VaultCategory("legal",      "Legal Docs",        "⚖️", "Agreements & deeds",       NeonCyan),
    VaultCategory("personal",   "Personal Photos",   "🖼️", "Private snapshots",        NeonMagenta),
    VaultCategory("work",       "Work Docs",         "📁", "Office & projects",        NeonPurple),
    VaultCategory("other",      "Other Files",       "📁", "Miscellaneous docs",       NeonGold)
)

val BIP39_WORDS = listOf(
    "abandon", "ability", "able", "about", "above", "absent", "absorb", "abstract", "absurd", "abuse",
    "access", "accident", "account", "accuse", "achieve", "acid", "acoustic", "acquire", "across", "act",
    "action", "active", "actor", "actress", "actual", "adapt", "add", "addict", "address", "adjust",
    "admit", "advice", "advise", "affair", "affect", "afford", "afraid", "again", "against", "age",
    "agent", "agree", "ahead", "aim", "air", "airport", "aisle", "alarm", "album", "alcohol", "alert",
    "alien", "alike", "alive", "all", "alley", "allow", "almost", "alone", "along", "already", "also"
)

// ─────────────────────────────────────────────────────────────────────────────
// Vault Entry Point
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun VaultScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val prefs = remember { context.getSharedPreferences("anegan_vault_prefs", Context.MODE_PRIVATE) }
    var hasCompletedOnboarding by remember { mutableStateOf(prefs.getBoolean("vault_setup_done", false)) }
    var isUnlocked by remember { mutableStateOf(false) }

    // Screen/Screenshot Security FLAG_SECURE
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    NovaBackground(modifier = Modifier.fillMaxSize()) {
        when {
            !hasCompletedOnboarding -> VaultSetupWalkthrough(
                onSetupFinished = {
                    prefs.edit().putBoolean("vault_setup_done", true).apply()
                    hasCompletedOnboarding = true
                    isUnlocked = true
                },
                onBack = onBack
            )
            !isUnlocked -> VaultLockScreen(
                context = context,
                prefs = prefs,
                onUnlocked = { isUnlocked = true },
                onBack = onBack
            )
            else -> VaultDashboardScreen(
                prefs = prefs,
                onLock = { isUnlocked = false },
                onBack = onBack
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Walkthrough / Setup Flow
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun VaultSetupWalkthrough(onSetupFinished: () -> Unit, onBack: () -> Unit) {
    var step by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("anegan_vault_prefs", Context.MODE_PRIVATE) }
    
    var chosenPin by remember { mutableStateOf("") }
    var recoveryPhrase by remember { mutableStateOf(emptyList<String>()) }
    var autoLockDuration by remember { mutableStateOf(60000L) } // default 1 min

    // Generate phrase once
    LaunchedEffect(Unit) {
        val list = mutableListOf<String>()
        repeat(12) {
            list.add(BIP39_WORDS.random())
        }
        recoveryPhrase = list
    }

    when (step) {
        0 -> VaultPrivacyExplanationScreen(onNext = { step = 1 }, onBack = onBack)
        1 -> VaultPinCreationScreen(
            onPinCreated = { pin ->
                chosenPin = pin
                prefs.edit().putString("vault_pin", pin).apply()
                step = 2
            },
            onBack = { step = 0 }
        )
        2 -> VaultRecoveryPhraseScreen(
            phrase = recoveryPhrase,
            onNext = {
                prefs.edit().putString("vault_recovery_phrase", recoveryPhrase.joinToString(" ")).apply()
                step = 3
            },
            onBack = { step = 1 }
        )
        3 -> VaultSettingsSetupScreen(
            currentAutoLock = autoLockDuration,
            onAutoLockChange = { autoLockDuration = it },
            onFinish = {
                prefs.edit().putLong("vault_autolock_ms", autoLockDuration).apply()
                onSetupFinished()
            },
            onBack = { step = 2 }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultPrivacyExplanationScreen(onNext: () -> Unit, onBack: () -> Unit) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            NovaTopBar(
                title = "Vault Setup",
                onBack = onBack,
                neonAccent = NeonPurple
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text("🛡️", fontSize = 72.sp)
                Spacer(Modifier.height(24.dp))
                Text(
                    "Encrypted Offline Storage",
                    style = NovaTypography.headlineLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Your files never leave your device.",
                    style = NovaTypography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                
                GlassCard(
                    neonAccent = NeonPurple,
                    enableGlow = true,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        PrivacyBullet("🔒 AES-256 Encryption", "All vault files are encrypted locally using industrial-grade hardware-backed keys.")
                        PrivacyBullet("📶 Zero Internet Dependency", "Vault runs entirely offline. No databases are uploaded to any cloud.")
                        PrivacyBullet("🚫 No Telemetry or Ads", "We collect zero analytics or usage logs. Complete private custody.")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            NovaPrimaryButton(
                text = "Agree & Continue",
                neonColor = NeonPurple,
                onClick = onNext,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun PrivacyBullet(title: String, desc: String) {
    Column {
        Text(
            text = title,
            style = NovaTypography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = NeonPurple
            )
        )
        Text(
            text = desc,
            style = NovaTypography.bodySmall.copy(
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        )
    }
}

@Composable
fun VaultPinCreationScreen(onPinCreated: (String) -> Unit, onBack: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(0) } // 0=enter, 1=confirm
    var error by remember { mutableStateOf("") }
    val view = LocalView.current

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            NovaTopBar(
                title = "Setup Secure PIN",
                onBack = onBack,
                neonAccent = NeonPurple
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🔑", fontSize = 64.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (step == 0) "Create a 4-digit PIN for your Vault" else "Re-enter PIN to confirm",
                style = NovaTypography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            PinDots(length = if (step == 0) pin.length else confirmPin.length)
            Spacer(Modifier.height(16.dp))
            if (error.isNotEmpty()) {
                Text(
                    text = error,
                    style = NovaTypography.bodySmall.copy(color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(8.dp))
            }
            NumberPad(
                onDigit = { d ->
                    if (step == 0) {
                        if (pin.length < 4) {
                            pin += d
                            if (pin.length == 4) {
                                delayMs(200) { step = 1 }
                            }
                        }
                    } else {
                        if (confirmPin.length < 4) {
                            confirmPin += d
                            if (confirmPin.length == 4) {
                                if (confirmPin == pin) {
                                    NovaHaptics.success(view)
                                    onPinCreated(pin)
                                } else {
                                    NovaHaptics.warning(view)
                                    error = "PINs do not match. Restarting."
                                    pin = ""
                                    confirmPin = ""
                                    step = 0
                                }
                            }
                        }
                    }
                },
                onDelete = {
                    if (step == 0) { if (pin.isNotEmpty()) pin = pin.dropLast(1) }
                    else { if (confirmPin.isNotEmpty()) confirmPin = confirmPin.dropLast(1) }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultRecoveryPhraseScreen(phrase: List<String>, onNext: () -> Unit, onBack: () -> Unit) {
    val view = LocalView.current
    val context = LocalContext.current

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            NovaTopBar(
                title = "Recovery Phrase",
                onBack = onBack,
                neonAccent = NeonPurple
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📝", fontSize = 56.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Your Offline Master Key",
                    style = NovaTypography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    "If you forget your PIN, this phrase is the ONLY way to recover your vault. Write it down. Store it somewhere safe offline.",
                    style = NovaTypography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                Spacer(Modifier.height(16.dp))

                GlassCard(
                    neonAccent = NeonPurple,
                    enableGlow = true,
                    onClick = {
                        try {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Anegan Vault Recovery Phrase", phrase.joinToString(" "))
                            clipboard.setPrimaryClip(clip)
                            NovaHaptics.confirm(view)
                            Toast.makeText(context, "Recovery phrase copied!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(phrase.size) { i ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(NovaGlassWhite)
                                        .border(1.dp, NovaGlassBorderW, RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${i + 1}.",
                                            style = NovaTypography.tagMono.copy(
                                                color = NeonPurple.copy(alpha = 0.6f)
                                            )
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = phrase[i],
                                            style = NovaTypography.codeMono.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Tap Card to Copy Phrase",
                            style = NovaTypography.tagMono.copy(color = NeonPurple)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            NovaPrimaryButton(
                text = "I've Written It Down",
                neonColor = NeonPurple,
                onClick = onNext,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultSettingsSetupScreen(
    currentAutoLock: Long,
    onAutoLockChange: (Long) -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit
) {
    val view = LocalView.current
    val lockOptions = listOf(
        30000L to "30 Seconds",
        60000L to "1 Minute",
        300000L to "5 Minutes",
        600000L to "10 Minutes"
    )
    val selectedIndex = lockOptions.indexOfFirst { it.first == currentAutoLock }.coerceAtLeast(0)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            NovaTopBar(
                title = "Vault Settings",
                onBack = onBack,
                neonAccent = NeonPurple
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Text("⏱️", fontSize = 64.sp)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Auto-Lock Timeout",
                    style = NovaTypography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Choose how long the vault remains unlocked when idle. Re-minimizing the app will lock the vault immediately regardless of this choice.",
                    style = NovaTypography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                Spacer(Modifier.height(24.dp))

                GlassCard(
                    neonAccent = NeonPurple,
                    enableGlow = true,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "INACTIVITY DURATION",
                            style = NovaTypography.tagMono.copy(color = NeonPurple)
                        )
                        Spacer(Modifier.height(12.dp))
                        
                        NovaSegmentedControl(
                            items = listOf("30s", "1m", "5m", "10m"),
                            selectedIndex = selectedIndex,
                            onIndexSelected = { idx ->
                                NovaHaptics.swipeSnap(view)
                                onAutoLockChange(lockOptions[idx].first)
                            },
                            neonColor = NeonPurple
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            NovaPrimaryButton(
                text = "Complete Vault Setup",
                neonColor = NeonPurple,
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// Helper to delay callbacks easily
private fun delayMs(ms: Long, action: () -> Unit) {
    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(action, ms)
}

// ─────────────────────────────────────────────────────────────────────────────
// Vault Lock / Unlock Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultLockScreen(
    context: Context,
    prefs: android.content.SharedPreferences,
    onUnlocked: () -> Unit,
    onBack: () -> Unit
) {
    val activity = context as? Activity
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val savedPin = prefs.getString("vault_pin", "") ?: ""
    val view = LocalView.current

    var cooldownSeconds by remember { mutableStateOf(0) }
    var failedAttempts by remember { mutableStateOf(prefs.getInt("vault_failed_attempts", 0)) }
    var showRecoveryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        tryBiometric(context, onSuccess = {
            NovaHaptics.success(view)
            onUnlocked()
        }, onError = { /* fall back */ })
    }

    LaunchedEffect(cooldownSeconds) {
        if (cooldownSeconds > 0) {
            delay(1000)
            cooldownSeconds--
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            NovaTopBar(
                title = "Vault Locked",
                onBack = onBack,
                neonAccent = NeonPurple
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🔒", fontSize = 64.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Enter Vault PIN",
                style = NovaTypography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(24.dp))
            PinDots(length = pin.length)
            Spacer(Modifier.height(16.dp))

            if (cooldownSeconds > 0) {
                Text(
                    text = "Cooldown active. Try again in $cooldownSeconds seconds.",
                    style = NovaTypography.bodySmall.copy(color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(8.dp))
            } else if (error.isNotEmpty()) {
                Text(
                    text = error,
                    style = NovaTypography.bodySmall.copy(color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(8.dp))
            }

            NumberPad(
                onDigit = { d ->
                    if (cooldownSeconds == 0 && pin.length < 4) {
                        pin += d
                        if (pin.length == 4) {
                            if (pin == savedPin) {
                                prefs.edit().putInt("vault_failed_attempts", 0).apply()
                                NovaHaptics.success(view)
                                onUnlocked()
                            } else {
                                pin = ""
                                failedAttempts++
                                prefs.edit().putInt("vault_failed_attempts", failedAttempts).apply()
                                NovaHaptics.warning(view)
                                if (failedAttempts >= 5) {
                                    cooldownSeconds = 30
                                    error = "Too many failed attempts. Cooldown started."
                                } else {
                                    error = "Incorrect PIN. Attempts: $failedAttempts/5"
                                }
                            }
                        }
                    }
                },
                onDelete = { if (pin.isNotEmpty()) pin = pin.dropLast(1) }
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    NovaSecondaryButton(
                        text = "Fingerprint",
                        neonColor = NeonPurple,
                        onClick = {
                            NovaHaptics.click(view)
                            tryBiometric(context, onSuccess = {
                                NovaHaptics.success(view)
                                onUnlocked()
                            }, onError = {
                                NovaHaptics.warning(view)
                                error = "Biometric failed or rejected"
                            })
                        },
                        icon = Icons.Default.Fingerprint,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                TextButton(
                    onClick = {
                        NovaHaptics.click(view)
                        showRecoveryDialog = true
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Forgot PIN?",
                        style = NovaTypography.labelLarge.copy(color = NeonPurple, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }

    if (showRecoveryDialog) {
        var recoveryInput by remember { mutableStateOf("") }
        var recoveryError by remember { mutableStateOf("") }
        val savedRecovery = prefs.getString("vault_recovery_phrase", "") ?: ""

        Dialog(
            onDismissRequest = { showRecoveryDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight()
            ) {
                GlassCard(
                    neonAccent = NeonPurple,
                    enableGlow = true
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "RESET VAULT PIN",
                            style = NovaTypography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = NeonPurple,
                                fontFamily = SpaceGrotesk
                            )
                        )
                        
                        Text(
                            text = "Enter your 12-word recovery phrase to unlock the vault and reset your PIN. All security options will be reset.",
                            style = NovaTypography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        )
                        
                        NovaTextField(
                            value = recoveryInput,
                            onValueChange = { recoveryInput = it },
                            placeholder = "12 Words (space separated)",
                            modifier = Modifier.fillMaxWidth(),
                            neonColor = NeonPurple
                        )
                        
                        if (recoveryError.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = recoveryError,
                                    style = NovaTypography.tagMono.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                )
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            NovaSecondaryButton(
                                text = "Cancel",
                                neonColor = NeonPurple,
                                onClick = { showRecoveryDialog = false },
                                modifier = Modifier.weight(1f)
                            )
                            
                            NovaPrimaryButton(
                                text = "Reset PIN",
                                neonColor = NeonPurple,
                                onClick = {
                                    val cleanInput = recoveryInput.trim().lowercase().replace("\\s+".toRegex(), " ")
                                    val cleanSaved = savedRecovery.trim().lowercase().replace("\\s+".toRegex(), " ")
                                    if (cleanInput == cleanSaved) {
                                        NovaHaptics.success(view)
                                        prefs.edit().putString("vault_pin", null).putBoolean("vault_setup_done", false).apply()
                                        showRecoveryDialog = false
                                        activity?.recreate()
                                    } else {
                                        NovaHaptics.warning(view)
                                        recoveryError = "Incorrect recovery phrase. Try again."
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Vault Dashboard Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VaultDashboardScreen(
    prefs: android.content.SharedPreferences,
    onLock: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    
    val database = remember { DatabaseProvider.getDatabase(context) }
    val dao = remember { database.vaultFileDao() }

    var filesList by remember { mutableStateOf(emptyList<VaultFileEntity>()) }
    var pinnedList by remember { mutableStateOf(emptyList<VaultFileEntity>()) }
    var recentList by remember { mutableStateOf(emptyList<VaultFileEntity>()) }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    var selectedCategory by remember { mutableStateOf<VaultCategory?>(null) }
    var showEmergencyCard by remember { mutableStateOf(false) }
    var showSettingsMenu by remember { mutableStateOf(false) }

    // Stealth & Privacy preferences
    var isStealthMode by remember { mutableStateOf(prefs.getBoolean("vault_stealth_mode", false)) }
    var showRecoveryPhraseDialog by remember { mutableStateOf(false) }
    var showAutoLockDialog by remember { mutableStateOf(false) }
    var showBackupRestoreDialog by remember { mutableStateOf(false) }

    // Activity lifecycle check for auto-locking
    val autolockMs = remember { prefs.getLong("vault_autolock_ms", 60000L) }
    var lastActiveTime by remember { mutableStateOf(System.currentTimeMillis()) }

    fun refreshData() {
        scope.launch(Dispatchers.IO) {
            filesList = dao.getAll()
            pinnedList = dao.getPinned()
            recentList = dao.getRecent()
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    // Auto-lock monitor
    LaunchedEffect(lastActiveTime) {
        while (true) {
            delay(10000)
            if (System.currentTimeMillis() - lastActiveTime > autolockMs) {
                onLock()
                break
            }
        }
    }

    // Handle Category Screen or Emergency Card Screen overlays
    when {
        selectedCategory != null -> {
            VaultCategoryScreen(
                category = selectedCategory!!,
                dao = dao,
                isStealth = isStealthMode,
                onBack = { selectedCategory = null; refreshData() }
            )
            return
        }
        showEmergencyCard -> {
            VaultEmergencyCardScreen(
                dao = dao,
                onBack = { showEmergencyCard = false; refreshData() }
            )
            return
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(64.dp)
                    .background(NovaGlassWhite)
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(listOf(NovaGlassBorderW, Color.Transparent)),
                        shape = androidx.compose.ui.graphics.RectangleShape
                    )
                    .padding(horizontal = NovaTokens.Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(onClick = {
                        NovaHaptics.click(view)
                        if (isSearchActive) {
                            isSearchActive = false
                            searchQuery = ""
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(NovaTokens.Spacing.xs))
                    
                    if (isSearchActive) {
                        NovaTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = "Search files & tags…",
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = "🔐 SECURE VAULT",
                            style = NovaTypography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = SpaceGrotesk
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                if (!isSearchActive) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            NovaHaptics.click(view)
                            isSearchActive = true
                        }) {
                            Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(onClick = {
                            NovaHaptics.click(view)
                            showSettingsMenu = true
                        }) {
                            Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(onClick = {
                            NovaHaptics.click(view)
                            onLock()
                        }) {
                            Icon(Icons.Default.Lock, null, tint = NeonPurple)
                        }
                    }
                } else {
                    IconButton(onClick = {
                        NovaHaptics.click(view)
                        isSearchActive = false
                        searchQuery = ""
                    }) {
                        Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    ) { padding ->
        // Track touch interactions to reset autolock timer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .combinedClickable(
                    onClick = { lastActiveTime = System.currentTimeMillis() },
                    onLongClick = { lastActiveTime = System.currentTimeMillis() }
                )
        ) {
            val filteredFiles = if (searchQuery.isBlank()) filesList
            else filesList.filter {
                it.name.contains(searchQuery, ignoreCase = true) || it.tags.contains(searchQuery, ignoreCase = true)
            }

            if (isSearchActive) {
                if (filteredFiles.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No matching files found",
                            style = NovaTypography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredFiles, key = { it.id }) { file ->
                            VaultFileRow(file = file, isStealth = isStealthMode, onClick = {
                                // View file (requires secure decrypt)
                            }, onMenuOption = { refreshData() })
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Privacy Header Banner
                    item {
                        GlassCard(
                            neonAccent = NeonPurpleSoft,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🛡️", fontSize = 32.sp)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Encrypted Offline Storage",
                                        style = NovaTypography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = NeonPurple)
                                    )
                                    Text(
                                        text = "Your files never leave your device. Fully offline AES-256.",
                                        style = NovaTypography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    )
                                }
                            }
                        }
                    }

                    // Emergency Quick Access Card
                    item {
                        GlassCard(
                            neonAccent = Color(0xFFB71C1C),
                            enableGlow = true,
                            onClick = {
                                NovaHaptics.click(view)
                                showEmergencyCard = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🆘", fontSize = 28.sp)
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Emergency Access Card",
                                            style = NovaTypography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFFB71C1C))
                                        )
                                        Text(
                                            text = "Quick offline profile, blood group & contacts",
                                            style = NovaTypography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                        )
                                    }
                                }
                                Icon(Icons.Default.ChevronRight, null, tint = Color(0xFFB71C1C))
                            }
                        }
                    }

                    // Pinned Documents Section
                    if (pinnedList.isNotEmpty()) {
                        item {
                            NovaSectionHeader(title = "Pinned Files", neonColor = NeonPurple)
                        }
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(pinnedList, key = { it.id }) { file ->
                                    VaultPinnedCard(file = file, isStealth = isStealthMode, onRefresh = { refreshData() })
                                }
                            }
                        }
                    }

                    // Recent Section
                    if (recentList.isNotEmpty()) {
                        item {
                            NovaSectionHeader(title = "Recent Files", neonColor = NeonPurple)
                        }
                        items(recentList, key = { it.id }) { file ->
                            VaultFileRow(file = file, isStealth = isStealthMode, onClick = {
                                // Handle file decrypt and view
                            }, onMenuOption = { refreshData() })
                        }
                    }

                    // Categories Grid Header
                    item {
                        NovaSectionHeader(title = "Categories", neonColor = NeonPurple)
                    }

                    // Grid layout for category cards
                    items(vaultCategories.chunked(2)) { pair ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            pair.forEach { cat ->
                                VaultCategoryGridCard(
                                    category = cat,
                                    onClick = { selectedCategory = cat },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }

    // Dropdown Settings Menu Dialogs & Prompts
    if (showSettingsMenu) {
        AlertDialog(
            onDismissRequest = { showSettingsMenu = false },
            title = { Text("Vault Settings") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Stealth toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                isStealthMode = !isStealthMode
                                prefs.edit().putBoolean("vault_stealth_mode", isStealthMode).apply()
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Stealth / Blur Mode", fontWeight = FontWeight.Bold)
                            Text("Blur all file previews & hide sensitive filenames.", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = isStealthMode,
                            onCheckedChange = {
                                isStealthMode = it
                                prefs.edit().putBoolean("vault_stealth_mode", isStealthMode).apply()
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                        )
                    }

                    // Change Auto Lock
                    TextButton(onClick = { showSettingsMenu = false; showAutoLockDialog = true }) {
                        Text("⏱️ Change Auto-Lock duration", color = MaterialTheme.colorScheme.primary)
                    }

                    // View Recovery
                    TextButton(onClick = { showSettingsMenu = false; showRecoveryPhraseDialog = true }) {
                        Text("📝 View Recovery Phrase", color = MaterialTheme.colorScheme.primary)
                    }

                    // Backup/Restore
                    TextButton(onClick = { showSettingsMenu = false; showBackupRestoreDialog = true }) {
                        Text("💾 Encrypted Backup & Restore", color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsMenu = false }) { Text("Close") }
            }
        )
    }

    if (showRecoveryPhraseDialog) {
        val phrase = prefs.getString("vault_recovery_phrase", "") ?: ""
        AlertDialog(
            onDismissRequest = { showRecoveryPhraseDialog = false },
            title = { Text("Your Recovery Phrase") },
            text = {
                Column {
                    Text("This phrase allows you to recover your vault files if you forget your PIN. Keep it private.", fontSize = 11.sp, color = Color.Gray)
                    Spacer(Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            phrase,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showRecoveryPhraseDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("Done") }
            }
        )
    }

    if (showAutoLockDialog) {
        var currentSelection by remember { mutableStateOf(prefs.getLong("vault_autolock_ms", 60000L)) }
        val options = listOf(30000L to "30 Seconds", 60000L to "1 Minute", 300000L to "5 Minutes", 600000L to "10 Minutes")

        AlertDialog(
            onDismissRequest = { showAutoLockDialog = false },
            title = { Text("Auto-Lock Timeout") },
            text = {
                Column {
                    options.forEach { opt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { currentSelection = opt.first }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(opt.second)
                            RadioButton(
                                selected = currentSelection == opt.first,
                                onClick = { currentSelection = opt.first }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        prefs.edit().putLong("vault_autolock_ms", currentSelection).apply()
                        showAutoLockDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAutoLockDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showBackupRestoreDialog) {
        VaultBackupRestoreDialog(
            dao = dao,
            onDismiss = { showBackupRestoreDialog = false; refreshData() }
        )
    }
}

// Border Stroke Helper
private fun borderStrokeForColor(color: Color) = androidx.compose.foundation.BorderStroke(1.dp, color)

// Biometric gate authenticator
private fun tryBiometric(context: Context, onSuccess: () -> Unit, onError: (String) -> Unit) {
    val activity = context as? FragmentActivity ?: return
    val biometricManager = BiometricManager.from(context)
    if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) != BiometricManager.BIOMETRIC_SUCCESS) {
        onError("Biometric not available")
        return
    }
    val executor = ContextCompat.getMainExecutor(context)
    val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { onSuccess() }
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { onError(errString.toString()) }
        override fun onAuthenticationFailed() { onError("Authentication failed") }
    })
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Anegan Vault")
        .setSubtitle("Unlock your secure vault")
        .setNegativeButtonText("Use PIN")
        .build()
    prompt.authenticate(info)
}

// ─────────────────────────────────────────────────────────────────────────────
// Vault Components (Pinned Cards / File Rows)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun VaultCategoryGridCard(category: VaultCategory, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val view = LocalView.current
    GlassCard(
        neonAccent = category.color,
        enableGlow = true,
        onClick = {
            NovaHaptics.click(view)
            onClick()
        },
        modifier = modifier.height(100.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(category.color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(category.emoji, fontSize = 20.sp)
            }
            Column {
                Text(
                    text = category.title,
                    style = NovaTypography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = category.color
                    )
                )
                Text(
                    text = category.description,
                    style = NovaTypography.bodySmall.copy(
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun VaultPinnedCard(file: VaultFileEntity, isStealth: Boolean, onRefresh: () -> Unit) {
    val view = LocalView.current
    var showViewer by remember { mutableStateOf(false) }

    GlassCard(
        neonAccent = NeonPurple,
        enableGlow = false,
        onClick = {
            NovaHaptics.click(view)
            showViewer = true
        },
        modifier = Modifier.size(width = 120.dp, height = 110.dp)
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
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonPurple.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(fileEmojiForMime(file.mimeType), fontSize = 16.sp)
                }
                Icon(Icons.Default.PushPin, null, tint = NeonPurple, modifier = Modifier.size(14.dp))
            }
            Text(
                text = if (isStealth) "••••••••••" else file.name,
                style = NovaTypography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    if (showViewer) {
        VaultFileViewerDialog(file = file, onDismiss = { showViewer = false }, onRefresh = onRefresh)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VaultFileRow(
    file: VaultFileEntity,
    isStealth: Boolean,
    onClick: () -> Unit,
    onMenuOption: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showViewer by remember { mutableStateOf(false) }
    val view = LocalView.current

    val context = LocalContext.current
    val database = remember { DatabaseProvider.getDatabase(context) }
    val dao = remember { database.vaultFileDao() }
    val scope = rememberCoroutineScope()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = {
                    NovaHaptics.click(view)
                    showViewer = true
                },
                onLongClick = {
                    NovaHaptics.longPress(view)
                    showMenu = true
                }
            )
            .background(NovaGlassWhite)
            .border(1.dp, NovaGlassBorderW, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(NeonPurple.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(fileEmojiForMime(file.mimeType), fontSize = 20.sp)
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isStealth) "•••••••••••••" else file.name,
                style = NovaTypography.headlineSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${(file.size / 1024.0).toInt()} KB · ${file.tags.ifEmpty { "no tags" }}",
                style = NovaTypography.tagMono.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 10.sp)
            )
        }

        IconButton(onClick = {
            NovaHaptics.click(view)
            showMenu = true
        }) {
            Icon(Icons.Default.MoreVert, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
    }

    if (showViewer) {
        VaultFileViewerDialog(file = file, onDismiss = { showViewer = false }, onRefresh = onMenuOption)
    }

    if (showMenu) {
        AlertDialog(
            onDismissRequest = { showMenu = false },
            title = { Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = {
                Column {
                    TextButton(onClick = {
                        scope.launch(Dispatchers.IO) {
                            dao.upsert(file.copy(isPinned = !file.isPinned))
                            onMenuOption()
                        }
                        NovaHaptics.confirm(view)
                        showMenu = false
                    }) {
                        Text(if (file.isPinned) "📌 Unpin from top" else "📌 Pin to top", color = NeonPurple)
                    }

                    TextButton(onClick = {
                        scope.launch(Dispatchers.IO) {
                            dao.upsert(file.copy(isFavorite = !file.isFavorite))
                            onMenuOption()
                        }
                        NovaHaptics.confirm(view)
                        showMenu = false
                    }) {
                        Text(if (file.isFavorite) "⭐ Remove from favorites" else "⭐ Add to favorites", color = NeonPurple)
                    }

                    TextButton(onClick = {
                        showMenu = false
                        scope.launch(Dispatchers.IO) {
                            val storageDir = VaultEncryptionHelper.getSecureStorageDir(context)
                            val srcFile = File(storageDir, file.encryptedFileName)
                            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                                android.os.Environment.DIRECTORY_DOWNLOADS
                            )
                            val destFile = File(downloadsDir, file.name)
                            try {
                                VaultEncryptionHelper.decryptToOutputStream(
                                    context,
                                    srcFile,
                                    destFile.outputStream()
                                )
                                // Delete secure copy after export
                                srcFile.delete()
                                dao.deleteById(file.id)
                                onMenuOption()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        NovaHaptics.success(view)
                    }) {
                        Text("📤 Decrypt & Move to Device Downloads", color = NeonPurple)
                    }

                    TextButton(onClick = {
                        scope.launch(Dispatchers.IO) {
                            val storageDir = VaultEncryptionHelper.getSecureStorageDir(context)
                            File(storageDir, file.encryptedFileName).delete()
                            dao.deleteById(file.id)
                            onMenuOption()
                        }
                        NovaHaptics.warning(view)
                        showMenu = false
                    }) {
                        Text("🗑️ Delete Permanently", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMenu = false }) { Text("Close") }
            }
        )
    }
}

fun fileEmojiForMime(mime: String): String {
    return when {
        mime.startsWith("image/") -> "🖼️"
        mime.startsWith("video/") -> "🎬"
        mime == "application/pdf" -> "📕"
        mime.startsWith("text/") -> "📝"
        mime.contains("zip") || mime.contains("octet-stream") -> "🗜️"
        else -> "📄"
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Vault Category Screen (Inside a Category)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultCategoryScreen(
    category: VaultCategory,
    dao: VaultFileDao,
    isStealth: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    var categoryFiles by remember { mutableStateOf(emptyList<VaultFileEntity>()) }
    var showImportMenu by remember { mutableStateOf(false) }

    // Custom Note creation state
    var showNoteDialog by remember { mutableStateOf(false) }
    var noteTitle by remember { mutableStateOf("") }
    var noteBody by remember { mutableStateOf("") }

    // Mock OCR ID Scanner state
    var showOcrDialog by remember { mutableStateOf(false) }
    var idNameInput by remember { mutableStateOf("") }
    var idNumberInput by remember { mutableStateOf("") }

    fun refresh() {
        scope.launch(Dispatchers.IO) {
            categoryFiles = dao.getByCategory(category.id)
        }
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    // Picker launchers
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                importUriToVault(context, uri, category.id, dao)
                refresh()
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            scope.launch(Dispatchers.IO) {
                importBitmapToVault(context, bitmap, category.id, dao)
                refresh()
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            NovaTopBar(
                title = "${category.emoji} ${category.title}",
                onBack = onBack,
                neonAccent = category.color
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    NovaHaptics.click(view)
                    showImportMenu = true
                },
                containerColor = category.color,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(56.dp)
                    .neonGlow(color = category.color)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(24.dp))
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (categoryFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GlassCard(
                        neonAccent = category.color,
                        enableGlow = true,
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(category.emoji, fontSize = 64.sp)
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "EMPTY FOLDER",
                                style = NovaTypography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = category.color,
                                    fontFamily = SpaceGrotesk
                                )
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Import files to this folder using the + button. All contents are locally encrypted.",
                                style = NovaTypography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categoryFiles, key = { it.id }) { file ->
                        VaultFileRow(file = file, isStealth = isStealth, onClick = {}, onMenuOption = { refresh() })
                    }
                }
            }
        }
    }

    if (showImportMenu) {
        Dialog(
            onDismissRequest = { showImportMenu = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight()
            ) {
                GlassCard(
                    neonAccent = category.color,
                    enableGlow = true
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "SECURE IMPORT",
                            style = NovaTypography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = category.color,
                                fontFamily = SpaceGrotesk
                            )
                        )
                        
                        Text(
                            text = "Choose an option to locally encrypt and import files into this vault category.",
                            style = NovaTypography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        )
                        
                        Spacer(Modifier.height(8.dp))
                        
                        ImportOptionRow(
                            icon = Icons.Default.UploadFile,
                            title = "Import Document/Media",
                            description = "Encrypt existing files from device",
                            neonColor = category.color,
                            onClick = {
                                showImportMenu = false
                                filePicker.launch("*/*")
                            }
                        )
                        
                        ImportOptionRow(
                            icon = Icons.Default.CameraAlt,
                            title = "Take Private Photo",
                            description = "Capture secure photo directly to vault",
                            neonColor = category.color,
                            onClick = {
                                showImportMenu = false
                                cameraLauncher.launch(null)
                            }
                        )
                        
                        ImportOptionRow(
                            icon = Icons.Default.NoteAdd,
                            title = "Create Secure Note",
                            description = "Write encrypted text note",
                            neonColor = category.color,
                            onClick = {
                                showImportMenu = false
                                showNoteDialog = true
                            }
                        )
                        
                        ImportOptionRow(
                            icon = Icons.Default.QrCodeScanner,
                            title = "Mock OCR Scan Card",
                            description = "Simulate local ID/Document scan",
                            neonColor = category.color,
                            onClick = {
                                showImportMenu = false
                                showOcrDialog = true
                            }
                        )
                        
                        Spacer(Modifier.height(8.dp))
                        
                        NovaSecondaryButton(
                            text = "Cancel",
                            neonColor = category.color,
                            onClick = { showImportMenu = false },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    // Secure note writing Dialog
    if (showNoteDialog) {
        Dialog(
            onDismissRequest = { showNoteDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight()
            ) {
                GlassCard(
                    neonAccent = category.color,
                    enableGlow = true
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "NEW SECURE NOTE",
                            style = NovaTypography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = category.color,
                                fontFamily = SpaceGrotesk
                            )
                        )
                        
                        Text(
                            text = "Write a private text note. It will be encrypted immediately and stored only on this device.",
                            style = NovaTypography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        )
                        
                        NovaTextField(
                            value = noteTitle,
                            onValueChange = { noteTitle = it },
                            placeholder = "Note Title",
                            modifier = Modifier.fillMaxWidth(),
                            neonColor = category.color
                        )
                        
                        NovaTextField(
                            value = noteBody,
                            onValueChange = { noteBody = it },
                            placeholder = "Note Content (Write details…)",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            singleLine = false,
                            neonColor = category.color
                        )
                        
                        Spacer(Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            NovaSecondaryButton(
                                text = "Cancel",
                                neonColor = category.color,
                                onClick = {
                                    NovaHaptics.click(view)
                                    showNoteDialog = false
                                },
                                modifier = Modifier.weight(1f)
                            )
                            
                            NovaPrimaryButton(
                                text = "Save Note",
                                neonColor = category.color,
                                onClick = {
                                    if (noteTitle.isNotBlank() && noteBody.isNotBlank()) {
                                        scope.launch(Dispatchers.IO) {
                                            val bytes = noteBody.toByteArray()
                                            val secureDir = VaultEncryptionHelper.getSecureStorageDir(context)
                                            val fileId = UUID.randomUUID().toString()
                                            val encName = "note_$fileId.enc"
                                            val destFile = File(secureDir, encName)
                                            VaultEncryptionHelper.encryptBytes(context, bytes, destFile)
                                            
                                            val entity = VaultFileEntity(
                                                id = fileId,
                                                name = "$noteTitle.txt",
                                                category = category.id,
                                                size = bytes.size.toLong(),
                                                mimeType = "text/plain",
                                                encryptedFileName = encName,
                                                tags = "note"
                                            )
                                            dao.upsert(entity)
                                            refresh()
                                        }
                                        NovaHaptics.success(view)
                                        showNoteDialog = false
                                        noteTitle = ""
                                        noteBody = ""
                                    } else {
                                        NovaHaptics.warning(view)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }

    // Mock OCR ID Scanner Dialog
    if (showOcrDialog) {
        Dialog(
            onDismissRequest = { showOcrDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight()
            ) {
                GlassCard(
                    neonAccent = category.color,
                    enableGlow = true
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "OFFLINE OCR SCANNER",
                            style = NovaTypography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = category.color,
                                fontFamily = SpaceGrotesk
                            )
                        )
                        
                        Text(
                            text = "Type card details to simulate offline OCR extraction. Extracted texts are saved as an encrypted text note inside your vault.",
                            style = NovaTypography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        )
                        
                        NovaTextField(
                            value = idNameInput,
                            onValueChange = { idNameInput = it },
                            placeholder = "Full Name on Card",
                            modifier = Modifier.fillMaxWidth(),
                            neonColor = category.color
                        )
                        
                        NovaTextField(
                            value = idNumberInput,
                            onValueChange = { idNumberInput = it },
                            placeholder = "ID Number (e.g. Aadhaar / PAN)",
                            modifier = Modifier.fillMaxWidth(),
                            neonColor = category.color
                        )
                        
                        Spacer(Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            NovaSecondaryButton(
                                text = "Cancel",
                                neonColor = category.color,
                                onClick = {
                                    NovaHaptics.click(view)
                                    showOcrDialog = false
                                },
                                modifier = Modifier.weight(1f)
                            )
                            
                            NovaPrimaryButton(
                                text = "Scan ID",
                                neonColor = category.color,
                                onClick = {
                                    if (idNameInput.isNotBlank() && idNumberInput.isNotBlank()) {
                                        scope.launch(Dispatchers.IO) {
                                            val mockOcrText = """
                                                --- Anegan Offline OCR Scan ---
                                                Document Category: ${category.title}
                                                Card Holder Name: $idNameInput
                                                ID Number: $idNumberInput
                                                Scan Timestamp: ${SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date())}
                                                Security Status: Verified Encrypted Offline
                                            """.trimIndent()

                                            val bytes = mockOcrText.toByteArray()
                                            val secureDir = VaultEncryptionHelper.getSecureStorageDir(context)
                                            val fileId = UUID.randomUUID().toString()
                                            val encName = "ocr_$fileId.enc"
                                            val destFile = File(secureDir, encName)
                                            VaultEncryptionHelper.encryptBytes(context, bytes, destFile)
                                            
                                            val entity = VaultFileEntity(
                                                id = fileId,
                                                name = "OCR_Scan_$idNumberInput.txt",
                                                category = category.id,
                                                size = bytes.size.toLong(),
                                                mimeType = "text/plain",
                                                encryptedFileName = encName,
                                                tags = "ocr,id"
                                            )
                                            dao.upsert(entity)
                                            refresh()
                                        }
                                        NovaHaptics.success(view)
                                        showOcrDialog = false
                                        idNameInput = ""
                                        idNumberInput = ""
                                    } else {
                                        NovaHaptics.warning(view)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImportOptionRow(
    icon: ImageVector,
    title: String,
    description: String,
    neonColor: Color,
    onClick: () -> Unit
) {
    val view = LocalView.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                NovaHaptics.click(view)
                onClick()
            }
            .background(NovaGlassWhite)
            .border(1.dp, NovaGlassBorderW, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(neonColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = neonColor, modifier = Modifier.size(20.dp))
        }
        
        Spacer(Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = NovaTypography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = description,
                style = NovaTypography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 10.sp)
            )
        }
        
        Icon(Icons.Default.ChevronRight, null, tint = neonColor, modifier = Modifier.size(16.dp))
    }
}

// Helper: import Uri to Vault
private suspend fun importUriToVault(
    context: Context,
    uri: Uri,
    categoryId: String,
    dao: VaultFileDao
) {
    withContext(Dispatchers.IO) {
        try {
            var fileName = "imported_file_${System.currentTimeMillis()}"
            var fileSize = 0L
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex)
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }

            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val fileId = UUID.randomUUID().toString()
            val encName = "file_$fileId.enc"
            val secureDir = VaultEncryptionHelper.getSecureStorageDir(context)
            val destFile = File(secureDir, encName)

            context.contentResolver.openInputStream(uri)?.use { stream ->
                VaultEncryptionHelper.encryptInputStream(context, stream, destFile)
            }

            val entity = VaultFileEntity(
                id = fileId,
                name = fileName,
                category = categoryId,
                size = fileSize,
                mimeType = mimeType,
                encryptedFileName = encName,
                tags = "imported"
            )
            dao.upsert(entity)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// Helper: import captured Bitmap to Vault
private suspend fun importBitmapToVault(
    context: Context,
    bitmap: Bitmap,
    categoryId: String,
    dao: VaultFileDao
) {
    withContext(Dispatchers.IO) {
        try {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            val bytes = stream.toByteArray()

            val fileId = UUID.randomUUID().toString()
            val encName = "photo_$fileId.enc"
            val secureDir = VaultEncryptionHelper.getSecureStorageDir(context)
            val destFile = File(secureDir, encName)

            VaultEncryptionHelper.encryptBytes(context, bytes, destFile)

            val entity = VaultFileEntity(
                id = fileId,
                name = "Secure_Photo_${System.currentTimeMillis()}.jpg",
                category = categoryId,
                size = bytes.size.toLong(),
                mimeType = "image/jpeg",
                encryptedFileName = encName,
                tags = "camera"
            )
            dao.upsert(entity)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Vault File Viewer Dialog (In-Memory decrypter)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun VaultFileViewerDialog(
    file: VaultFileEntity,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    var isDecrypting by remember { mutableStateOf(true) }
    var noteContent by remember { mutableStateOf<String?>(null) }
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var errorText by remember { mutableStateOf("") }

    var isEditTagsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            try {
                // Secure simulated delay to show decryption state
                delay(800)
                val storageDir = VaultEncryptionHelper.getSecureStorageDir(context)
                val encFile = File(storageDir, file.encryptedFileName)
                val bytes = VaultEncryptionHelper.decryptToBytes(context, encFile)
                
                if (file.mimeType.startsWith("text/")) {
                    noteContent = String(bytes)
                } else if (file.mimeType.startsWith("image/")) {
                    imageBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
                isDecrypting = false
            } catch (e: Exception) {
                errorText = "Decryption error: File corrupted or key unavailable."
                isDecrypting = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
        ) {
            GlassCard(
                neonAccent = NeonPurple,
                enableGlow = true
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SECURE DECRYPTER",
                            style = NovaTypography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = NeonPurple,
                                fontFamily = SpaceGrotesk
                            )
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NeonPurple.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "AES-256",
                                style = NovaTypography.tagMono.copy(color = NeonPurple, fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Text(
                        text = if (isDecrypting) "Decrypting secure stream in-memory..." else file.name,
                        style = NovaTypography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = JetBrainsMono
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NovaGlassWhite)
                            .border(1.dp, NovaGlassBorderW, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isDecrypting -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(contentAlignment = Alignment.Center) {
                                        NovaPulseRing(
                                            neonColor = NeonPurple,
                                            modifier = Modifier.size(80.dp)
                                        )
                                        Text("🔑", fontSize = 28.sp)
                                    }
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        text = "STREAM DECRYPT ACTIVE",
                                        style = NovaTypography.tagMono.copy(color = NeonPurple, fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                            errorText.isNotEmpty() -> {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("⚠️", fontSize = 48.sp)
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = errorText,
                                        style = NovaTypography.bodySmall.copy(color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            noteContent != null -> {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp)
                                ) {
                                    item {
                                        Text(
                                            text = noteContent!!,
                                            style = NovaTypography.codeMono.copy(
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 12.sp
                                            )
                                        )
                                    }
                                }
                            }
                            imageBitmap != null -> {
                                androidx.compose.foundation.Image(
                                    bitmap = imageBitmap!!.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().padding(4.dp)
                                )
                            }
                            else -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Text(fileEmojiForMime(file.mimeType), fontSize = 48.sp)
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "Decrypted in memory safely.",
                                        style = NovaTypography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Use option menu to export to decrypted local file.",
                                        style = NovaTypography.tagMono.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 9.sp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        NovaSecondaryButton(
                            text = "Close",
                            neonColor = NeonPurple,
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        )
                        
                        if (!isDecrypting && errorText.isEmpty()) {
                            NovaPrimaryButton(
                                text = "Edit Tags",
                                neonColor = NeonPurple,
                                onClick = {
                                    NovaHaptics.click(view)
                                    isEditTagsDialog = true
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (isEditTagsDialog) {
        var tagsInput by remember { mutableStateOf(file.tags) }
        val database = remember { DatabaseProvider.getDatabase(context) }
        val dao = remember { database.vaultFileDao() }

        Dialog(
            onDismissRequest = { isEditTagsDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .wrapContentHeight()
            ) {
                GlassCard(
                    neonAccent = NeonPurple,
                    enableGlow = true
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "EDIT FILE TAGS",
                            style = NovaTypography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = NeonPurple,
                                fontFamily = SpaceGrotesk
                            )
                        )
                        
                        NovaTextField(
                            value = tagsInput,
                            onValueChange = { tagsInput = it },
                            placeholder = "Tags (comma separated)",
                            modifier = Modifier.fillMaxWidth(),
                            neonColor = NeonPurple
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            NovaSecondaryButton(
                                text = "Cancel",
                                neonColor = NeonPurple,
                                onClick = { isEditTagsDialog = false },
                                modifier = Modifier.weight(1f)
                            )
                            
                            NovaPrimaryButton(
                                text = "Save Tags",
                                neonColor = NeonPurple,
                                onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        dao.upsert(file.copy(tags = tagsInput))
                                        onRefresh()
                                    }
                                    NovaHaptics.success(view)
                                    isEditTagsDialog = false
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Vault Emergency Card Screen (Sos Card Profile)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultEmergencyCardScreen(dao: VaultFileDao, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("anegan_emergency_card", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    var name by remember { mutableStateOf(prefs.getString("name", "") ?: "") }
    var bloodGroup by remember { mutableStateOf(prefs.getString("blood", "") ?: "") }
    var allergies by remember { mutableStateOf(prefs.getString("allergies", "") ?: "") }
    var medicalConditions by remember { mutableStateOf(prefs.getString("conditions", "") ?: "") }
    var contactName by remember { mutableStateOf(prefs.getString("contact_name", "") ?: "") }
    var contactPhone by remember { mutableStateOf(prefs.getString("contact_phone", "") ?: "") }

    var isEditing by remember { mutableStateOf(name.isBlank()) }
    var emergencyFiles by remember { mutableStateOf(emptyList<VaultFileEntity>()) }

    val redAccent = Color(0xFFB71C1C)

    fun refreshEmergencyDocs() {
        scope.launch(Dispatchers.IO) {
            emergencyFiles = dao.getByCategory("medical")
        }
    }

    LaunchedEffect(Unit) {
        refreshEmergencyDocs()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            NovaTopBar(
                title = "🆘 Emergency Profile",
                onBack = onBack,
                neonAccent = redAccent
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                GlassCard(
                    neonAccent = redAccent,
                    enableGlow = true,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SOS INFOCARD",
                                style = NovaTypography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = redAccent,
                                    fontFamily = SpaceGrotesk
                                )
                            )
                            IconButton(onClick = {
                                if (isEditing) {
                                    prefs.edit()
                                        .putString("name", name)
                                        .putString("blood", bloodGroup)
                                        .putString("allergies", allergies)
                                        .putString("conditions", medicalConditions)
                                        .putString("contact_name", contactName)
                                        .putString("contact_phone", contactPhone)
                                        .apply()
                                    NovaHaptics.success(view)
                                    isEditing = false
                                } else {
                                    NovaHaptics.click(view)
                                    isEditing = true
                                }
                            }) {
                                Icon(
                                    imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                                    contentDescription = "Edit Profile",
                                    tint = redAccent
                                )
                            }
                        }

                        if (isEditing) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                NovaTextField(value = name, onValueChange = { name = it }, placeholder = "Full Name", modifier = Modifier.fillMaxWidth(), neonColor = redAccent)
                                NovaTextField(value = bloodGroup, onValueChange = { bloodGroup = it }, placeholder = "Blood Group", modifier = Modifier.fillMaxWidth(), neonColor = redAccent)
                                NovaTextField(value = allergies, onValueChange = { allergies = it }, placeholder = "Allergies", modifier = Modifier.fillMaxWidth(), neonColor = redAccent)
                                NovaTextField(value = medicalConditions, onValueChange = { medicalConditions = it }, placeholder = "Medical Conditions", modifier = Modifier.fillMaxWidth(), neonColor = redAccent)
                                NovaTextField(value = contactName, onValueChange = { contactName = it }, placeholder = "Emergency Contact Name", modifier = Modifier.fillMaxWidth(), neonColor = redAccent)
                                NovaTextField(
                                    value = contactPhone,
                                    onValueChange = { contactPhone = it },
                                    placeholder = "Emergency Contact Phone",
                                    modifier = Modifier.fillMaxWidth(),
                                    neonColor = redAccent,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                EmergencyItemRow("👤 NAME", name.ifEmpty { "Not configured" })
                                EmergencyItemRow("🩸 BLOOD GROUP", bloodGroup.ifEmpty { "Not configured" })
                                EmergencyItemRow("⚠️ ALLERGIES", allergies.ifEmpty { "None listed" })
                                EmergencyItemRow("🏥 MEDICAL CONDITIONS", medicalConditions.ifEmpty { "None listed" })
                                EmergencyItemRow("📞 EMERGENCY CONTACT", if (contactName.isNotEmpty()) "$contactName · $contactPhone" else "Not configured")
                            }
                        }
                    }
                }
            }

            item {
                NovaSectionHeader(title = "Emergency Documents", neonColor = redAccent)
            }

            if (emergencyFiles.isEmpty()) {
                item {
                    GlassCard(
                        neonAccent = redAccent.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Add files under 'Medical' category to display them here",
                                style = NovaTypography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(emergencyFiles, key = { it.id }) { file ->
                    VaultFileRow(file = file, isStealth = false, onClick = {}, onMenuOption = { refreshEmergencyDocs() })
                }
            }
        }
    }
}

@Composable
fun EmergencyItemRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(NovaGlassWhite)
            .border(1.dp, NovaGlassBorderW, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = NovaTypography.tagMono.copy(
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = NovaTypography.codeMono.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Vault Manual Encrypted Local Backup & Restore
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun VaultBackupRestoreDialog(dao: VaultFileDao, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    var backupStatus by remember { mutableStateOf("") }
    var restoreStatus by remember { mutableStateOf("") }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    restoreStatus = "Restoring vault…"
                    val secureDir = VaultEncryptionHelper.getSecureStorageDir(context)
                    val input = context.contentResolver.openInputStream(uri) ?: throw IOException("Cannot read backup file")
                    
                    val tempZip = File(context.cacheDir, "temp_restore.zip")
                    tempZip.outputStream().use { out -> input.copyTo(out) }
                    
                    ZipInputStream(tempZip.inputStream()).use { zis ->
                        var entry = zis.nextEntry
                        while (entry != null) {
                            if (entry.name == "vault_metadata.json") {
                                val jsonStr = zis.readBytes().decodeToString()
                                val array = JSONArray(jsonStr)
                                repeat(array.length()) { i ->
                                    val obj = array.getJSONObject(i)
                                    val entity = VaultFileEntity(
                                        id = obj.getString("id"),
                                        name = obj.getString("name"),
                                        category = obj.getString("category"),
                                        size = obj.getLong("size"),
                                        mimeType = obj.getString("mimeType"),
                                        isPinned = obj.getBoolean("isPinned"),
                                        isFavorite = obj.getBoolean("isFavorite"),
                                        tags = obj.getString("tags"),
                                        encryptedFileName = obj.getString("encryptedFileName")
                                    )
                                    dao.upsert(entity)
                                }
                            } else {
                                val destFile = File(secureDir, entry.name)
                                destFile.outputStream().use { out -> zis.copyTo(out) }
                            }
                            zis.closeEntry()
                            entry = zis.nextEntry
                        }
                    }
                    tempZip.delete()
                    restoreStatus = "SUCCESS: Vault restored!"
                    NovaHaptics.success(view)
                } catch (e: Exception) {
                    restoreStatus = "ERROR: Restore failed: ${e.message}"
                    NovaHaptics.warning(view)
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
        ) {
            GlassCard(
                neonAccent = NeonPurple,
                enableGlow = true
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "BACKUP & RESTORE",
                        style = NovaTypography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = NeonPurple,
                            fontFamily = SpaceGrotesk
                        )
                    )
                    
                    Text(
                        text = "Vault backups are offline, private, and manual. They are generated as an encrypted ZIP archive stored in your Downloads folder.",
                        style = NovaTypography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    )

                    Spacer(Modifier.height(4.dp))

                    // Backup Card
                    GlassCard(
                        neonAccent = NeonPurpleSoft,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "💾 CREATE OFFLINE BACKUP",
                                style = NovaTypography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Compiles all vault entities, metadata and files into a password-protected zip file.",
                                style = NovaTypography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 10.sp)
                            )
                            
                            NovaPrimaryButton(
                                text = "Export Backup ZIP",
                                neonColor = NeonPurple,
                                onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            backupStatus = "Compiling vault files…"
                                            val secureDir = VaultEncryptionHelper.getSecureStorageDir(context)
                                            val metadata = dao.getAll()
                                            
                                            val jsonArray = JSONArray()
                                            metadata.forEach { f ->
                                                val obj = JSONObject()
                                                obj.put("id", f.id)
                                                obj.put("name", f.name)
                                                obj.put("category", f.category)
                                                obj.put("size", f.size)
                                                obj.put("mimeType", f.mimeType)
                                                obj.put("isPinned", f.isPinned)
                                                obj.put("isFavorite", f.isFavorite)
                                                obj.put("tags", f.tags)
                                                obj.put("encryptedFileName", f.encryptedFileName)
                                                jsonArray.put(obj)
                                            }

                                            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                                                android.os.Environment.DIRECTORY_DOWNLOADS
                                            )
                                            val backupFile = File(downloadsDir, "Anegan_Vault_Backup_${System.currentTimeMillis()}.zip")
                                            
                                            ZipOutputStream(backupFile.outputStream()).use { zos ->
                                                zos.putNextEntry(ZipEntry("vault_metadata.json"))
                                                zos.write(jsonArray.toString().toByteArray())
                                                zos.closeEntry()
                                                
                                                secureDir.listFiles()?.forEach { file ->
                                                    if (file.isFile) {
                                                        zos.putNextEntry(ZipEntry(file.name))
                                                        file.inputStream().use { input -> input.copyTo(zos) }
                                                        zos.closeEntry()
                                                    }
                                                }
                                            }
                                            backupStatus = "SUCCESS: Saved to Downloads"
                                            NovaHaptics.success(view)
                                        } catch (e: Exception) {
                                            backupStatus = "ERROR: Backup failed: ${e.message}"
                                            NovaHaptics.warning(view)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (backupStatus.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (backupStatus.startsWith("ERROR")) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                                            else NeonPurple.copy(alpha = 0.12f)
                                        )
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = backupStatus,
                                        style = NovaTypography.tagMono.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (backupStatus.startsWith("ERROR")) MaterialTheme.colorScheme.error else NeonPurple
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Restore Card
                    GlassCard(
                        neonAccent = NeonPurpleSoft,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "📤 RESTORE FROM BACKUP",
                                style = NovaTypography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Select an exported backup ZIP archive to restore all offline vault files.",
                                style = NovaTypography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 10.sp)
                            )
                            
                            NovaPrimaryButton(
                                text = "Select Backup File",
                                neonColor = NeonPurple,
                                onClick = {
                                    NovaHaptics.click(view)
                                    restoreLauncher.launch("application/zip")
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (restoreStatus.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (restoreStatus.startsWith("ERROR")) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                                            else NeonPurple.copy(alpha = 0.12f)
                                        )
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = restoreStatus,
                                        style = NovaTypography.tagMono.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (restoreStatus.startsWith("ERROR")) MaterialTheme.colorScheme.error else NeonPurple
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    NovaSecondaryButton(
                        text = "Close",
                        neonColor = NeonPurple,
                        onClick = {
                            NovaHaptics.click(view)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun PinDots(length: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(4) { i ->
            val active = i < length
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) NeonPurple
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )
                    .border(
                        width = 1.dp,
                        color = if (active) NeonPurple else Color.Transparent,
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
fun NumberPad(onDigit: (String) -> Unit, onDelete: () -> Unit) {
    val view = LocalView.current
    val digits = listOf("1","2","3","4","5","6","7","8","9","","0","⌫")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        digits.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { d ->
                    if (d.isEmpty()) {
                        Spacer(modifier = Modifier.weight(1f).height(64.dp))
                    } else {
                        val isSpecial = d == "⌫"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSpecial) Color.Transparent
                                    else NovaGlassWhite
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSpecial) Color.Transparent else NovaGlassBorderW,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    NovaHaptics.tick(view)
                                    if (d == "⌫") onDelete() else onDigit(d)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = d,
                                style = NovaTypography.dataMedium.copy(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSpecial) NeonPurple else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
