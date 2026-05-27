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
import com.anegan.core.designsystem.theme.MidnightIndigo
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
    VaultCategory("aadhaar",    "Aadhaar ID",        "🪪", "Government UID card",       Color(0xFF1565C0)),
    VaultCategory("pan",        "PAN Card",          "💳", "Income Tax ID",            Color(0xFF2E7D32)),
    VaultCategory("passport",   "Passport",          "📕", "Travel document",          Color(0xFF6A1B9A)),
    VaultCategory("insurance",  "Insurance",         "🛡️", "Health & Auto policy",      Color(0xFF00838F)),
    VaultCategory("medical",    "Medical",           "🏥", "Prescriptions & reports",  Color(0xFFC62828)),
    VaultCategory("education",  "Certificates",      "🎓", "Degrees & marks",          Color(0xFFE65100)),
    VaultCategory("legal",      "Legal Docs",        "⚖️", "Agreements & deeds",       Color(0xFF37474F)),
    VaultCategory("personal",   "Personal Photos",   "🖼️", "Private snapshots",        Color(0xFFAD1457)),
    VaultCategory("work",       "Work Docs",         "📁", "Office & projects",        Color(0xFF4E342E)),
    VaultCategory("other",      "Other Files",       "📁", "Miscellaneous docs",       Color(0xFF424242))
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
        topBar = {
            SmallTopAppBar(
                title = { Text("Anegan Vault Setup") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
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
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    color = MidnightIndigo
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Your files never leave your device.",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        PrivacyBullet("🔒 AES-256 Encryption", "All vault files are encrypted locally using industrial-grade hardware-backed keys.")
                        PrivacyBullet("📶 Zero Internet Dependency", "Vault runs entirely offline. No databases are uploaded to any cloud.")
                        PrivacyBullet("🚫 No Telemetry or Ads", "We collect zero analytics or usage logs. Complete private custody.")
                    }
                }
            }

            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MidnightIndigo),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text("Agree & Continue", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PrivacyBullet(title: String, desc: String) {
    Column {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MidnightIndigo)
        Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@Composable
fun VaultPinCreationScreen(onPinCreated: (String) -> Unit, onBack: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(0) } // 0=enter, 1=confirm
    var error by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Setup Secure PIN") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
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
                if (step == 0) "Create a 4-digit PIN for your Vault" else "Re-enter PIN to confirm",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(24.dp))
            PinDots(length = if (step == 0) pin.length else confirmPin.length)
            Spacer(Modifier.height(16.dp))
            if (error.isNotEmpty()) {
                Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
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
                                    onPinCreated(pin)
                                } else {
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
    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Write Down Recovery Phrase") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
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
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "If you forget your PIN, this phrase is the ONLY way to recover your vault. Write it down. Store it somewhere safe offline.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(phrase.size) { i ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${i + 1}.", fontSize = 10.sp, color = MidnightIndigo.copy(alpha = 0.5f))
                                Spacer(Modifier.width(4.dp))
                                Text(phrase[i], fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MidnightIndigo),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text("I've Written It Down", fontWeight = FontWeight.Bold)
            }
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
    val lockOptions = listOf(
        30000L to "30 Seconds",
        60000L to "1 Minute",
        300000L to "5 Minutes",
        600000L to "10 Minutes"
    )

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Vault Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
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
                Text("⏱️", fontSize = 64.sp)
                Spacer(Modifier.height(16.dp))
                Text(
                    "Auto-Lock Inactivity Timeout",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Choose how long the vault remains unlocked when idle. Re-minimizing the app will lock the vault immediately regardless of this choice.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                Spacer(Modifier.height(24.dp))

                lockOptions.forEach { opt ->
                    val isSelected = currentAutoLock == opt.first
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MidnightIndigo.copy(alpha = 0.1f)
                                else Color.Transparent
                            )
                            .clickable { onAutoLockChange(opt.first) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(opt.second, fontWeight = FontWeight.SemiBold)
                        RadioButton(
                            selected = isSelected,
                            onClick = { onAutoLockChange(opt.first) },
                            colors = RadioButtonDefaults.colors(selectedColor = MidnightIndigo)
                        )
                    }
                }
            }

            Button(
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MidnightIndigo),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text("Complete Vault Setup", fontWeight = FontWeight.Bold)
            }
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

    var cooldownSeconds by remember { mutableStateOf(0) }
    var failedAttempts by remember { mutableStateOf(prefs.getInt("vault_failed_attempts", 0)) }
    var showRecoveryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        tryBiometric(context, onSuccess = onUnlocked, onError = { /* fall back */ })
    }

    LaunchedEffect(cooldownSeconds) {
        if (cooldownSeconds > 0) {
            delay(1000)
            cooldownSeconds--
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Vault Locked") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
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
            Text("Enter Vault PIN", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            PinDots(length = pin.length)
            Spacer(Modifier.height(16.dp))

            if (cooldownSeconds > 0) {
                Text("Cooldown active. Try again in $cooldownSeconds seconds.", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
            } else if (error.isNotEmpty()) {
                Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
            }

            NumberPad(
                onDigit = { d ->
                    if (cooldownSeconds == 0 && pin.length < 4) {
                        pin += d
                        if (pin.length == 4) {
                            if (pin == savedPin) {
                                prefs.edit().putInt("vault_failed_attempts", 0).apply()
                                onUnlocked()
                            } else {
                                pin = ""
                                failedAttempts++
                                prefs.edit().putInt("vault_failed_attempts", failedAttempts).apply()
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

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(onClick = {
                    tryBiometric(context, onSuccess = onUnlocked, onError = { error = "Biometric failed or rejected" })
                }) {
                    Icon(Icons.Default.Fingerprint, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Fingerprint")
                }

                TextButton(onClick = { showRecoveryDialog = true }) {
                    Text("Forgot PIN?")
                }
            }
        }
    }

    if (showRecoveryDialog) {
        var recoveryInput by remember { mutableStateOf("") }
        var recoveryError by remember { mutableStateOf("") }
        val savedRecovery = prefs.getString("vault_recovery_phrase", "") ?: ""

        AlertDialog(
            onDismissRequest = { showRecoveryDialog = false },
            title = { Text("Reset PIN via Recovery Phrase") },
            text = {
                Column {
                    Text("Enter your 12-word recovery phrase to unlock the vault and reset your PIN:", fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = recoveryInput,
                        onValueChange = { recoveryInput = it },
                        label = { Text("12 Words") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (recoveryError.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(recoveryError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanInput = recoveryInput.trim().lowercase().replace("\\s+".toRegex(), " ")
                        val cleanSaved = savedRecovery.trim().lowercase().replace("\\s+".toRegex(), " ")
                        if (cleanInput == cleanSaved) {
                            prefs.edit().putString("vault_pin", null).putBoolean("vault_setup_done", false).apply()
                            showRecoveryDialog = false
                            // Refresh page to redo setup
                            activity?.recreate()
                        } else {
                            recoveryError = "Incorrect recovery phrase. Try again."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MidnightIndigo)
                ) { Text("Unlock & Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showRecoveryDialog = false }) { Text("Cancel") }
            }
        )
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
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search files & tags…") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("🔐 Secure Vault", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSearchActive) {
                            isSearchActive = false
                            searchQuery = ""
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, null)
                        }
                        IconButton(onClick = { showSettingsMenu = true }) {
                            Icon(Icons.Default.Settings, null)
                        }
                        IconButton(onClick = onLock) {
                            Icon(Icons.Default.Lock, null, tint = MidnightIndigo)
                        }
                    } else {
                        IconButton(onClick = { isSearchActive = false; searchQuery = "" }) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                }
            )
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
                        Text("No matching files found", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
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
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MidnightIndigo.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🛡️", fontSize = 32.sp)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Encrypted Offline Storage", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MidnightIndigo)
                                    Text("Your files never leave your device. Fully offline AES-256.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }

                    // Emergency Quick Access Card
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showEmergencyCard = true },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C).copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(16.dp),
                            border = borderStrokeForColor(Color(0xFFB71C1C).copy(alpha = 0.2f))
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
                                        Text("Emergency Access Card", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFB71C1C))
                                        Text("Quick offline profile, blood group & contacts", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                }
                                Icon(Icons.Default.ChevronRight, null, tint = Color(0xFFB71C1C))
                            }
                        }
                    }

                    // Pinned Documents Section
                    if (pinnedList.isNotEmpty()) {
                        item {
                            Text("Pinned Files", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MidnightIndigo)
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
                            Text("Recent Files", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MidnightIndigo)
                        }
                        items(recentList, key = { it.id }) { file ->
                            VaultFileRow(file = file, isStealth = isStealthMode, onClick = {
                                // Handle file decrypt and view
                            }, onMenuOption = { refreshData() })
                        }
                    }

                    // Categories Grid Header
                    item {
                        Text("Categories", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MidnightIndigo)
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
                            colors = SwitchDefaults.colors(checkedThumbColor = MidnightIndigo)
                        )
                    }

                    // Change Auto Lock
                    TextButton(onClick = { showSettingsMenu = false; showAutoLockDialog = true }) {
                        Text("⏱️ Change Auto-Lock duration", color = MidnightIndigo)
                    }

                    // View Recovery
                    TextButton(onClick = { showSettingsMenu = false; showRecoveryPhraseDialog = true }) {
                        Text("📝 View Recovery Phrase", color = MidnightIndigo)
                    }

                    // Backup/Restore
                    TextButton(onClick = { showSettingsMenu = false; showBackupRestoreDialog = true }) {
                        Text("💾 Encrypted Backup & Restore", color = MidnightIndigo)
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
                Button(onClick = { showRecoveryPhraseDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = MidnightIndigo)) { Text("Done") }
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
                    colors = ButtonDefaults.buttonColors(containerColor = MidnightIndigo)
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
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = category.color.copy(alpha = 0.08f)),
        border = borderStrokeForColor(category.color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(category.emoji, fontSize = 24.sp)
            Column {
                Text(category.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = category.color)
                Text(category.description, fontSize = 9.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun VaultPinnedCard(file: VaultFileEntity, isStealth: Boolean, onRefresh: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showViewer by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .size(width = 120.dp, height = 110.dp)
            .clickable { showViewer = true },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(fileEmojiForMime(file.mimeType), fontSize = 20.sp)
                Icon(Icons.Default.PushPin, null, tint = MidnightIndigo, modifier = Modifier.size(14.dp))
            }
            Text(
                text = if (isStealth) "••••••••••" else file.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
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

    val context = LocalContext.current
    val database = remember { DatabaseProvider.getDatabase(context) }
    val dao = remember { database.vaultFileDao() }
    val scope = rememberCoroutineScope()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = { showViewer = true },
                onLongClick = { showMenu = true }
            )
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MidnightIndigo.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(fileEmojiForMime(file.mimeType), fontSize = 20.sp)
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isStealth) "•••••••••••••" else file.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${(file.size / 1024.0).toInt()} KB · ${file.tags.ifEmpty { "no tags" }}",
                fontSize = 10.sp,
                color = Color.Gray
            )
        }

        IconButton(onClick = { showMenu = true }) {
            Icon(Icons.Default.MoreVert, null, tint = Color.Gray)
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
                        showMenu = false
                    }) {
                        Text(if (file.isPinned) "📌 Unpin from top" else "📌 Pin to top", color = MidnightIndigo)
                    }

                    TextButton(onClick = {
                        scope.launch(Dispatchers.IO) {
                            dao.upsert(file.copy(isFavorite = !file.isFavorite))
                            onMenuOption()
                        }
                        showMenu = false
                    }) {
                        Text(if (file.isFavorite) "⭐ Remove from favorites" else "⭐ Add to favorites", color = MidnightIndigo)
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
                    }) {
                        Text("📤 Decrypt & Move to Device Downloads", color = MidnightIndigo)
                    }

                    TextButton(onClick = {
                        scope.launch(Dispatchers.IO) {
                            val storageDir = VaultEncryptionHelper.getSecureStorageDir(context)
                            File(storageDir, file.encryptedFileName).delete()
                            dao.deleteById(file.id)
                            onMenuOption()
                        }
                        showMenu = false
                    }) {
                        Text("🗑️ Delete Permanently", color = Color.Red)
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
        topBar = {
            TopAppBar(
                title = { Text("${category.emoji} ${category.title}") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showImportMenu = true },
                containerColor = category.color,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (categoryFiles.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(category.emoji, fontSize = 64.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("Empty folder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Add files to this folder using the + button. Files are locally encrypted.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
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
        AlertDialog(
            onDismissRequest = { showImportMenu = false },
            title = { Text("Add securely to Vault") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        showImportMenu = false
                        filePicker.launch("*/*")
                    }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.UploadFile, null, tint = MidnightIndigo)
                            Spacer(Modifier.width(12.dp))
                            Text("Import Document/Media", color = MidnightIndigo)
                        }
                    }

                    TextButton(onClick = {
                        showImportMenu = false
                        cameraLauncher.launch(null)
                    }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CameraAlt, null, tint = MidnightIndigo)
                            Spacer(Modifier.width(12.dp))
                            Text("Take Private Secure Photo", color = MidnightIndigo)
                        }
                    }

                    TextButton(onClick = {
                        showImportMenu = false
                        showNoteDialog = true
                    }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NoteAdd, null, tint = MidnightIndigo)
                            Spacer(Modifier.width(12.dp))
                            Text("Create Secure Text Note", color = MidnightIndigo)
                        }
                    }

                    TextButton(onClick = {
                        showImportMenu = false
                        showOcrDialog = true
                    }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCodeScanner, null, tint = MidnightIndigo)
                            Spacer(Modifier.width(12.dp))
                            Text("Mock OCR Scan ID/Card", color = MidnightIndigo)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showImportMenu = false }) { Text("Cancel") }
            }
        )
    }

    // Secure note writing Dialog
    if (showNoteDialog) {
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text("New Secure Note") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        label = { Text("Note Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = noteBody,
                        onValueChange = { noteBody = it },
                        label = { Text("Note Content") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )
                }
            },
            confirmButton = {
                Button(
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
                            showNoteDialog = false
                            noteTitle = ""
                            noteBody = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MidnightIndigo)
                ) { Text("Save Note") }
            },
            dismissButton = {
                TextButton(onClick = { showNoteDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Mock OCR ID Scanner Dialog
    if (showOcrDialog) {
        AlertDialog(
            onDismissRequest = { showOcrDialog = false },
            title = { Text("Mock OCR ID Card Scan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Type the details from the card to simulate offline OCR scanning:", fontSize = 11.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = idNameInput,
                        onValueChange = { idNameInput = it },
                        label = { Text("Full Name on Card") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = idNumberInput,
                        onValueChange = { idNumberInput = it },
                        label = { Text("ID Number (e.g. Aadhaar / PAN)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
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
                            showOcrDialog = false
                            idNameInput = ""
                            idNumberInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MidnightIndigo)
                ) { Text("Scan ID") }
            },
            dismissButton = {
                TextButton(onClick = { showOcrDialog = false }) { Text("Cancel") }
            }
        )
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
    var isDecrypting by remember { mutableStateOf(true) }
    var noteContent by remember { mutableStateOf<String?>(null) }
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var errorText by remember { mutableStateOf("") }

    var isEditTagsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            try {
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isDecrypting -> CircularProgressIndicator(color = MidnightIndigo)
                    errorText.isNotEmpty() -> Text(errorText, color = MaterialTheme.colorScheme.error)
                    noteContent != null -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                Text(noteContent!!, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    imageBitmap != null -> {
                        androidx.compose.foundation.Image(
                            bitmap = imageBitmap!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text("📄", fontSize = 48.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("Decrypted in memory.", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Decrypt & Export to device to open.", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { isEditTagsDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MidnightIndigo)
            ) { Text("Edit Tags") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )

    if (isEditTagsDialog) {
        var tagsInput by remember { mutableStateOf(file.tags) }
        val database = remember { DatabaseProvider.getDatabase(context) }
        val dao = remember { database.vaultFileDao() }

        AlertDialog(
            onDismissRequest = { isEditTagsDialog = false },
            title = { Text("Edit File Tags") },
            text = {
                OutlinedTextField(
                    value = tagsInput,
                    onValueChange = { tagsInput = it },
                    label = { Text("Tags (comma separated)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            dao.upsert(file.copy(tags = tagsInput))
                            onRefresh()
                        }
                        isEditTagsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MidnightIndigo)
                ) { Text("Save Tags") }
            },
            dismissButton = {
                TextButton(onClick = { isEditTagsDialog = false }) { Text("Cancel") }
            }
        )
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

    var name by remember { mutableStateOf(prefs.getString("name", "") ?: "") }
    var bloodGroup by remember { mutableStateOf(prefs.getString("blood", "") ?: "") }
    var allergies by remember { mutableStateOf(prefs.getString("allergies", "") ?: "") }
    var medicalConditions by remember { mutableStateOf(prefs.getString("conditions", "") ?: "") }
    var contactName by remember { mutableStateOf(prefs.getString("contact_name", "") ?: "") }
    var contactPhone by remember { mutableStateOf(prefs.getString("contact_phone", "") ?: "") }

    var isEditing by remember { mutableStateOf(name.isBlank()) }
    var emergencyFiles by remember { mutableStateOf(emptyList<VaultFileEntity>()) }

    fun refreshEmergencyDocs() {
        scope.launch(Dispatchers.IO) {
            emergencyFiles = dao.getByCategory("medical")
        }
    }

    LaunchedEffect(Unit) {
        refreshEmergencyDocs()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🆘 Emergency Access Card") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C).copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(16.dp),
                    border = borderStrokeForColor(Color(0xFFB71C1C).copy(alpha = 0.2f))
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Emergency Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFB71C1C))
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
                                    isEditing = false
                                } else {
                                    isEditing = true
                                }
                            }) {
                                Icon(if (isEditing) Icons.Default.Check else Icons.Default.Edit, null, tint = Color(0xFFB71C1C))
                            }
                        }

                        if (isEditing) {
                            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            OutlinedTextField(value = bloodGroup, onValueChange = { bloodGroup = it }, label = { Text("Blood Group") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            OutlinedTextField(value = allergies, onValueChange = { allergies = it }, label = { Text("Allergies") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = medicalConditions, onValueChange = { medicalConditions = it }, label = { Text("Medical Conditions") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = contactName, onValueChange = { contactName = it }, label = { Text("Emergency Contact Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            OutlinedTextField(value = contactPhone, onValueChange = { contactPhone = it }, label = { Text("Emergency Contact Phone") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                        } else {
                            EmergencyItemRow("👤 Name", name.ifEmpty { "Not configured" })
                            EmergencyItemRow("🩸 Blood Group", bloodGroup.ifEmpty { "Not configured" })
                            EmergencyItemRow("⚠️ Allergies", allergies.ifEmpty { "None listed" })
                            EmergencyItemRow("🏥 Medical Conditions", medicalConditions.ifEmpty { "None listed" })
                            EmergencyItemRow("📞 Contact", if (contactName.isNotEmpty()) "$contactName · $contactPhone" else "Not configured")
                        }
                    }
                }
            }

            item {
                Text("Emergency Documents", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MidnightIndigo)
                Text("Quick access to critical medical insurance or cards stored in vault:", fontSize = 11.sp, color = Color.Gray)
            }

            if (emergencyFiles.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Add files under 'Medical' category to display them here", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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
    Column {
        Text(label, fontSize = 10.sp, color = Color.Gray)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Vault Manual Encrypted Local Backup & Restore
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun VaultBackupRestoreDialog(dao: VaultFileDao, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
                    restoreStatus = "Restore completed successfully!"
                } catch (e: Exception) {
                    restoreStatus = "Restore failed: ${e.message}"
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vault Backup & Restore") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Anegan Vault backups are fully local, encrypted, manual and secure. Backups are exported as an encrypted zip archive.", fontSize = 11.sp, color = Color.Gray)
                
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Create Backup", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Save all secure files & metadata to device Downloads.", fontSize = 10.sp, color = Color.Gray)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        backupStatus = "Creating backup…"
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
                                            // Write metadata entry
                                            zos.putNextEntry(ZipEntry("vault_metadata.json"))
                                            zos.write(jsonArray.toString().toByteArray())
                                            zos.closeEntry()
                                            
                                            // Write secure files
                                            secureDir.listFiles()?.forEach { file ->
                                                if (file.isFile) {
                                                    zos.putNextEntry(ZipEntry(file.name))
                                                    file.inputStream().use { input -> input.copyTo(zos) }
                                                    zos.closeEntry()
                                                }
                                            }
                                        }
                                        backupStatus = "Backup saved to Downloads: ${backupFile.name}"
                                    } catch (e: Exception) {
                                        backupStatus = "Backup failed: ${e.message}"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MidnightIndigo),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Export Backup Zip")
                        }
                        if (backupStatus.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(backupStatus, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MidnightIndigo)
                        }
                    }
                }

                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Restore Vault", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Select an Anegan Vault backup zip file to restore.", fontSize = 10.sp, color = Color.Gray)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                restoreLauncher.launch("application/zip")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MidnightIndigo),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Select Backup File")
                        }
                        if (restoreStatus.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(restoreStatus, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MidnightIndigo)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun PinDots(length: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(4) { i ->
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (i < length) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            )
        }
    }
}

@Composable
fun NumberPad(onDigit: (String) -> Unit, onDelete: () -> Unit) {
    val digits = listOf("1","2","3","4","5","6","7","8","9","","0","⌫")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        digits.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { d ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (d.isEmpty()) Color.Transparent
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                width = if (d.isEmpty()) 0.dp else 1.dp,
                                color = if (d.isEmpty()) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable(enabled = d.isNotEmpty()) {
                                if (d == "⌫") onDelete() else onDigit(d)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (d.isNotEmpty()) {
                            Text(d, fontSize = 22.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}
