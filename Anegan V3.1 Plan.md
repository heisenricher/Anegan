# 🌑 ANEGAN V3.1 — ECLIPSE UPDATE
## Implementation Plan

> **Version**: 3.1 | **Codename**: Eclipse  
> **Base Version**: 3.0 (Beast Update) | **App ID**: com.anegan.app  
> **Architecture**: Kotlin + Jetpack Compose + Material Design 3  
> **Goal**: Polish, gap-fill, and elevate to professional-grade super-app

---

## Executive Summary

V3 "Beast" delivered a massive feature expansion — but many features were implemented at a structural level without deep runtime polish. V3.1 "Eclipse" is a **focused refinement update** that:

1. **Fixes real-world runtime issues** from V3 features
2. **Completes unfinished V3 promises** (Second Brain graph, audio MediaSession, etc.)
3. **Adds dark mode properly** (user choice, not forced light)
4. **Modernizes the SDK & build** (compileSdk 35, Kotlin 2.0, Compose BOM 2024)
5. **Introduces high-value new features** (voice recorder, QR generator, storage analyzer)
6. **Establishes quality assurance** foundations

### V3 Gap Analysis Summary

| Area | V3 Status | V3.1 Action |
|------|-----------|-------------|
| Dark Mode | Force-disabled (hack) | Proper dark/light/AMOLED toggle in Settings |
| Audio Background Playback | Code exists but no `MediaSessionService` | Full foreground service with notification controls |
| Second Brain | Basic note storage, no graph view | Graph visualization, backlinks, markdown preview |
| Splash Screen | None | Proper Android 12+ Splash API |
| Error Handling | Minimal try-catches | Comprehensive error states on all screens |
| Empty States | Missing on most screens | Beautiful empty state illustrations everywhere |
| Loading States | Inconsistent | Unified shimmer/skeleton loading pattern |
| Accessibility | No `contentDescription` audit | Full audit + semantic annotations |
| SDK Level | compileSdk 34, Compose BOM 2023 | compileSdk 35, Compose BOM 2024 |
| Version Naming | `2.6.0` / versionCode 18 | `3.1.0` / versionCode 20 |
| Onboarding | None | 3-screen welcome flow for first launch |
| ProGuard Rules | Basic rules, no exp4j keep | Add missing keep rules |
| Navigation Animations | None (instant cuts) | Shared element transitions |
| Haptic Feedback | Inconsistent | Systematic haptics on all interactions |
| App Shortcuts | None | Dynamic shortcuts for top 4 features |

---

## User Review Required

> [!IMPORTANT]
> **Dark Mode**: V3 force-disabled dark mode because it "caused crashes." V3.1 properly fixes those crashes and re-enables dark mode as a user preference in Settings. The app will default to **System Default** (follows Android theme). Is this acceptable, or do you want to keep force-light?

> [!IMPORTANT]
> **SDK Upgrade**: Upgrading from compileSdk 34 → 35 and Compose BOM 2023.10 → 2024.12.01 may require minor API adjustments. This gives us access to Predictive Back, new Material3 components, and Android 15 features. Approve?

> [!WARNING]
> **Version Naming**: The app currently reads `versionName = "2.6.0"` with `versionCode = 18`. V3.1 should bump this to `versionName = "3.1.0"` and `versionCode = 20`. Confirm?

## Open Questions

> [!IMPORTANT]
> 1. **Voice Recorder**: Should we add a new "Voice Recorder" tool to the home screen? It would use CameraX/MediaRecorder to capture audio with waveform visualization and save to `/Anegan/Recordings/`. Or is this scope creep for V3.1?

> [!IMPORTANT]  
> 2. **QR Code Generator**: V3 has a QR scanner. Should V3.1 add QR **generation** (text, URL, WiFi, contact card) with export-to-image? This is a natural complement.

> [!IMPORTANT]
> 3. **Storage Analyzer**: Should we add a "Storage Analyzer" feature in File Manager that shows treemap visualization of disk usage by folder? This helps users find space-wasting files.

---

# PHASE 1: BUILD & SDK MODERNIZATION
**Priority: Critical | Risk: Medium | Effort: Small**

This phase upgrades the build toolchain to modern standards, fixes version naming, and adds missing ProGuard rules.

---

### Build System

#### [MODIFY] [build.gradle.kts](file:///c:/Users/Srira/Desktop/Mahilan/2/Anegan/build.gradle.kts) (root)
- Upgrade Android Gradle Plugin to latest stable (if behind)
- Upgrade Kotlin plugin version to 2.0.x series

#### [MODIFY] [app/build.gradle.kts](file:///c:/Users/Srira/Desktop/Mahilan/2/Anegan/app/build.gradle.kts)
- `compileSdk = 34` → `compileSdk = 35`
- `targetSdk = 34` → `targetSdk = 35`
- `versionCode = 18` → `versionCode = 20`
- `versionName = "2.6.0"` → `versionName = "3.1.0"`
- Compose BOM: `"2023.10.01"` → `"2024.12.01"`
- Add `kotlinCompilerExtensionVersion = "1.5.14"` (or Compose compiler plugin for Kotlin 2.0)
- Add `buildFeatures { buildConfig = true }` for BuildConfig.VERSION_NAME access
- Add dependency: `implementation("androidx.core:core-splashscreen:1.0.1")`
- Add dependency: `implementation("androidx.compose.animation:animation")` for nav transitions

#### [MODIFY] [proguard-rules.pro](file:///c:/Users/Srira/Desktop/Mahilan/2/Anegan/app/proguard-rules.pro)
Add missing rules:
```proguard
# exp4j calculator engine
-keep class net.objecthunter.exp4j.** { *; }
-dontwarn net.objecthunter.exp4j.**

# Retrofit (currency API)
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**

# Markwon (markdown rendering)
-keep class io.noties.markwon.** { *; }
-dontwarn io.noties.markwon.**

# Compose stability
-keep class androidx.compose.** { *; }

# JCIFS SMB (smb-share module)  
-keep class jcifs.** { *; }
-dontwarn jcifs.**
```

#### [MODIFY] All feature module `build.gradle.kts` files
- Sync `compileSdk` and `minSdk` across all 11 feature modules + 3 core modules
- Ensure consistent Compose BOM version everywhere

---

# PHASE 2: DARK MODE — PROPER IMPLEMENTATION
**Priority: High | Risk: Medium | Effort: Medium**

V3 force-disabled dark mode with a comment "causes crashes." V3.1 properly fixes the root causes and re-enables dark mode as a user preference.

---

### Root Cause Analysis
The V3 dark mode crashes were likely caused by:
1. Hardcoded `Color.White` / `Color.Black` instead of using `MaterialTheme.colorScheme`
2. Icons with hardcoded tint colors not adapting to dark backgrounds
3. Status bar / navigation bar color mismatches

### Theme System

#### [MODIFY] [Theme.kt](file:///c:/Users/Srira/Desktop/Mahilan/2/Anegan/core/designsystem/src/main/java/com/anegan/core/designsystem/theme/Theme.kt)
- Remove `// V3: Force light mode always` logic
- Implement proper theme selection based on `ThemePreference` enum:
  - `SYSTEM_DEFAULT` → follows `isSystemInDarkTheme()`
  - `LIGHT` → always light
  - `DARK` → dark color scheme
  - `AMOLED` → AMOLED black scheme
- Add `CompositionLocalProvider` for `LocalThemePreference` so any screen can read current mode
- Keep Material You dynamic colors for Android 12+, apply `dynamicDarkColorScheme` when in dark mode

#### [NEW] `ThemePreference.kt` in `core/designsystem/`
```kotlin
enum class ThemePreference { SYSTEM_DEFAULT, LIGHT, DARK, AMOLED }
```
- Store preference in `SharedPreferences` (key: `"theme_mode"`)
- Provide a `rememberThemePreference()` composable

### Screen Audit — Hardcoded Colors
Every screen that uses hardcoded colors needs to be updated:

#### [MODIFY] All Screen Files (Batch)
Systematic find-and-replace across all `.kt` files:
| Find | Replace With |
|------|-------------|
| `Color.White` (background usage) | `MaterialTheme.colorScheme.background` |
| `Color.Black` (text color usage) | `MaterialTheme.colorScheme.onBackground` |
| `Color(0xFF...)` hardcoded grays | `MaterialTheme.colorScheme.surface` / `.surfaceVariant` |
| Hardcoded status bar white | `colorScheme.background.toArgb()` |

Key files that need dark-mode hardcoded color audit:
- `HomeScreen.kt` (dashboard module)
- `FileManagerScreen.kt` (file-manager module)
- `CalculatorScreen.kt`, `DevToolsScreen.kt`, all tool screens (conversion-flow module)
- `SecureVaultScreen.kt` (vault module)
- `NotesScreen.kt` (notes module)
- `HistoryScreen.kt` (history module)

### Settings Integration

#### [MODIFY] `SettingsScreen.kt`
- Add "Appearance" section at top of settings
- Theme picker: radio buttons for System / Light / Dark / AMOLED
- Live preview swatch showing selected theme colors

---

# PHASE 3: AUDIO PLAYER — MEDIASERVICE ARCHITECTURE
**Priority: High | Risk: High | Effort: Large**

V3 added audio player UI but lacks a proper background playback service. When the user leaves the app, audio stops. This phase adds a full `MediaSessionService` implementation.

---

#### [NEW] `AneganAudioService.kt` in `app/src/main/java/com/anegan/app/`
Full `MediaSessionService` implementation:
- Extends `MediaSessionService`
- Creates and manages `ExoPlayer` instance
- Publishes `MediaSession` for system media controls
- Shows persistent notification with:
  - Album art (or default gradient icon)
  - Track title & artist
  - Play/Pause, Previous, Next, Close buttons
- Handles audio focus properly (`AudioManager.AUDIOFOCUS_GAIN`)
- Handles becoming noisy (headphone disconnect → pause)
- Saves playback state to `AudioPlaybackStateEntity` on pause/stop/track change

#### [MODIFY] [AndroidManifest.xml](file:///c:/Users/Srira/Desktop/Mahilan/2/Anegan/app/src/main/AndroidManifest.xml)
Add service declaration:
```xml
<service
    android:name=".AneganAudioService"
    android:foregroundServiceType="mediaPlayback"
    android:exported="true">
    <intent-filter>
        <action android:name="androidx.media3.session.MediaSessionService" />
    </intent-filter>
</service>
```
Add permission:
```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
```

#### [MODIFY] `AudioPlayerScreen.kt` (in conversion-flow or dashboard)
- Connect to `AneganAudioService` via `MediaController` + `SessionToken`
- All playback operations go through the service (not direct ExoPlayer calls)
- Show "Now Playing" mini-bar on HomeScreen when audio is active
- Lock screen controls work automatically via MediaSession

#### [NEW] `NowPlayingBar.kt` in `core/designsystem/`
- Compact floating bar at bottom of HomeScreen
- Shows: album art thumbnail, title (marquee), play/pause button
- Taps → navigate to full AudioPlayerScreen
- Swipe up → expand to mini-player
- Only visible when AneganAudioService has active playback

---

# PHASE 4: UI/UX POLISH & MISSING STATES
**Priority: High | Risk: Low | Effort: Medium**

This phase adds the polish that separates amateur apps from professional ones.

---

### Splash Screen

#### [NEW] `SplashActivity.kt` in `app/`
- Use Android 12+ `SplashScreen` API (`installSplashScreen()`)
- Animated icon: Anegan logo with fade-in
- Keep splash visible while checking initial app state (permissions, first launch)
- Route to onboarding (first launch) or MainActivity

#### [MODIFY] [AndroidManifest.xml](file:///c:/Users/Srira/Desktop/Mahilan/2/Anegan/app/src/main/AndroidManifest.xml)
- Set `SplashActivity` as the new launcher activity
- Move intent-filters for file opening to `MainActivity` (keep as non-launcher)

### Onboarding Flow

#### [NEW] `OnboardingScreen.kt` in `feature/dashboard/`
3-screen welcome flow (only on first install):
1. **"Welcome to Anegan"** — App logo + tagline "Your All-in-One File & Utility Hub"
2. **"Powerful Tools"** — Icon grid showing key features
3. **"Grant Permissions"** — Storage & notification permission requests
- Dot indicators at bottom, skip button, "Get Started" CTA
- Stores `has_onboarded = true` in SharedPreferences

### Empty States

#### [NEW] `EmptyStateComponent.kt` in `core/designsystem/`
Reusable empty state composable:
```kotlin
@Composable
fun AneganEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
)
```
- Large icon with 0.3 alpha
- Title in `headlineSmall`
- Subtitle in `bodyMedium` with secondary color
- Optional action button

Apply to screens:
| Screen | Empty State Message |
|--------|-------------------|
| File Manager (empty folder) | "This folder is empty" + "Create File" button |
| Notes (no notes) | "No notes yet" + "Create Note" button |
| History (no history) | "No activity recorded yet" |
| Audio Library (no music found) | "No audio files found" + "Open File Manager" |
| Vault (empty) | "Your vault is empty" + "Add Files" |
| Playlists (none) | "No playlists created" + "Create Playlist" |
| Bookmarks (none) | "No bookmarks saved" |
| Search (no results) | "No results for '{query}'" |

### Loading States

#### [NEW] `ShimmerEffect.kt` in `core/designsystem/`
Reusable shimmer/skeleton loading animation:
- Rounded rectangle placeholders with animated gradient sweep
- Pre-built variants: `ShimmerListItem`, `ShimmerCard`, `ShimmerGrid`
- Apply to: File Manager (scanning folders), Audio Library (indexing), Document Hub (loading docs)

### Navigation Transitions

#### [MODIFY] `MainActivity.kt` / Navigation setup
- Add `enterTransition` / `exitTransition` / `popEnterTransition` / `popExitTransition` to all NavGraph composable routes
- Use `fadeIn() + slideInHorizontally()` for forward navigation
- Use `fadeOut() + slideOutHorizontally()` for back navigation
- Transition duration: 300ms with `FastOutSlowInEasing`

### Haptic Feedback System

#### [NEW] `HapticHelper.kt` in `core/designsystem/`
```kotlin
object HapticHelper {
    fun click(view: View) = view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    fun longPress(view: View) = view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    fun success(view: View) = view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
}
```
- Wire into all `WidgetItem` clicks, button presses, toggle switches, slider snaps

### Dynamic App Shortcuts

#### [NEW] `ShortcutManager.kt` in `app/`
- Register 4 dynamic shortcuts on app launch:
  1. 📁 File Manager
  2. 🎬 Video Player
  3. 📝 Notes
  4. 🧮 Calculator
- Use `ShortcutManagerCompat.setDynamicShortcuts()`
- Each shortcut deep-links to the respective route

#### [MODIFY] [AndroidManifest.xml](file:///c:/Users/Srira/Desktop/Mahilan/2/Anegan/app/src/main/AndroidManifest.xml)
Add shortcut metadata:
```xml
<meta-data
    android:name="android.app.shortcuts"
    android:resource="@xml/shortcuts" />
```

#### [NEW] `res/xml/shortcuts.xml`
Static shortcut definitions as fallback

---

# PHASE 5: SECOND BRAIN — COMPLETION
**Priority: Medium | Risk: Medium | Effort: Large**

V3 created `SecondBrainNoteEntity` in the database but the actual Second Brain UI (graph view, backlinks, markdown editing) needs completion.

---

#### [MODIFY/VERIFY] `SecondBrainScreen.kt` (locate in notes or dashboard module)
Ensure these sub-features work:
1. **Markdown Editor**: Live preview using Markwon (side-by-side or toggle mode)
2. **Tags System**: Color-coded tags, filter notes by tag, tag management screen
3. **Note Linking**: `[[note-title]]` syntax to create backlinks between notes
4. **Backlinks Panel**: Bottom section showing "Referenced by" — all notes linking to current note
5. **Inbox/Active/Archive** status filters
6. **Priority Levels**: Visual priority badges (P0–P5)
7. **Attachments**: Attach images/files from File Manager to notes
8. **Search**: Full-text search across all Second Brain notes

#### [NEW] `NoteGraphScreen.kt` in notes module
Knowledge graph visualization:
- Canvas-based node graph using Compose `Canvas` + `drawCircle`/`drawLine`
- Each note = a node (circle with title truncated)
- Links between notes = lines connecting nodes
- Pinch-to-zoom on the graph
- Tap node → navigate to that note
- Color nodes by tag or status
- Force-directed layout algorithm (simple spring simulation):
  ```
  - Connected nodes attract (spring force)
  - All nodes repel (electrostatic repulsion)
  - Iterate 100 frames to settle layout
  ```
- "Graph View" button in SecondBrainScreen toolbar opens this

---

# PHASE 6: PERFORMANCE & STABILITY
**Priority: High | Risk: Low | Effort: Medium**

---

### Memory & Leak Prevention

#### [MODIFY] `AudioPlayerScreen.kt`
- Ensure `ExoPlayer.release()` is called in `DisposableEffect` cleanup
- Use `remember { }` for expensive objects, not recreate on recomposition

#### [MODIFY] `VideoPlayerScreen.kt`  
- Same `DisposableEffect` cleanup pattern
- Release surface view on dispose
- Cancel any pending coroutine jobs

#### [MODIFY] `ImageViewerScreen.kt`
- Use `SubcomposeAsyncImage` (Coil) with memory cache
- Downscale large images (>4000px) before display to avoid OOM
- Implement proper bitmap recycling

### Startup Optimization

#### [MODIFY] `MainActivity.kt`
- Move heavy initialization (database, FFmpegKit) to background threads
- Use `Dispatchers.IO` for initial file scanning
- Lazy-load feature modules (don't initialize all at once)

### ANR Prevention

#### Audit all `withContext(Dispatchers.IO)` usage
Ensure NO file I/O, database queries, or network calls happen on `Dispatchers.Main`:
- File listing in FileManagerScreen → must be `Dispatchers.IO`
- Audio library indexing → must be `Dispatchers.IO`  
- PDF text extraction → must be `Dispatchers.IO`
- Archive content listing → must be `Dispatchers.IO`
- EXIF reading → must be `Dispatchers.IO`

### Crash Reporting

#### [NEW] `CrashHandler.kt` in `app/`
- Set `Thread.setDefaultUncaughtExceptionHandler`
- Log crash stack trace to `/Anegan/crash_logs/crash_TIMESTAMP.txt`
- On next app launch, detect crash log and show "App crashed last time. Send report?" dialog
- Crash log includes: stack trace, device info, Android version, app version, available RAM

---

# PHASE 7: NEW FEATURES (Conditional — Based on User Answers)
**Priority: Medium | Risk: Low | Effort: Medium-Large**

These features are proposed based on gap analysis. Implementation depends on user approval.

---

### 7A. QR Code Generator (if approved)

#### [MODIFY] `DevToolsScreen.kt`
Add new tab "QR Generator" alongside existing Scanner:
- Input modes: Text, URL, WiFi (SSID + password), Contact (vCard), Email, SMS
- Live QR preview generated using ZXing's `BarcodeEncoder`
- Color customization (foreground/background)
- Size selector (small/medium/large)
- "Save to Gallery" button → exports PNG to `/Anegan/QR Codes/`
- "Share" button → share intent

### 7B. Voice Recorder (if approved)

#### [NEW] `VoiceRecorderScreen.kt` in `feature/conversion-flow/`
- `MediaRecorder` based recording (M4A/AAC format)
- Real-time waveform visualization using `Visualizer` API
- Timer display (HH:MM:SS)
- Pause/Resume support (Android 24+)
- Save to `/Anegan/Recordings/`
- Recording quality selector: Low (64kbps), Normal (128kbps), High (256kbps)
- List of past recordings with playback
- Add as new widget on HomeScreen under "Utility Tools"

### 7C. Storage Analyzer (if approved)

#### [NEW] `StorageAnalyzerScreen.kt` in `feature/file-manager/`
- Scans entire internal storage in background
- Shows top 10 largest folders as sorted bar chart
- Category breakdown: Images, Videos, Audio, Documents, APKs, Other
- Pie chart visualization using Compose `Canvas`
- "Find Large Files" → list files > 100MB
- "Find Duplicates" → hash-based duplicate detection
- "Clean Temp Files" → clear cache directories

### 7D. Bookmark System for PDF Reader

#### [MODIFY] `PdfReaderScreen.kt`
- Bookmark button in toolbar → saves current page to `BookmarkEntity`
- Bookmark panel (drawer) showing all bookmarks for current file
- Tap bookmark → jump to page
- Delete bookmark with swipe

### 7E. Calculator History Persistence

#### [MODIFY] `CalculatorScreen.kt`
- Save every calculation result to `CalculatorHistoryEntity`
- History panel (expandable bottom sheet)
- Tap history item → populate expression
- Clear history button
- Show last 50 calculations

### 7F. Reading Progress Tracking

#### [MODIFY] `PdfReaderScreen.kt`, `EpubReaderScreen.kt`
- On every page change, save to `ReadingProgressEntity`
- On app reopen, auto-jump to last read page
- Continue Reading widget on HomeScreen reads from this table
- Show progress percentage in Document Hub

---

# PHASE 8: ACCESSIBILITY & QUALITY
**Priority: Medium | Risk: Low | Effort: Medium**

---

### Accessibility Audit

#### [MODIFY] All Screen Files
Systematic content description additions:
```kotlin
// BEFORE
Icon(Icons.Rounded.Settings, contentDescription = null)

// AFTER
Icon(Icons.Rounded.Settings, contentDescription = "Settings")
```

Key items:
- All `Icon()` calls must have descriptive `contentDescription`
- All `Image()` calls must have descriptive `contentDescription`
- All clickable elements need `semantics { contentDescription = "..." }`
- Ensure minimum touch target size of 48dp on all interactive elements
- Test with TalkBack enabled

### Font Scaling Support

#### [MODIFY] `Theme.kt` typography setup
- Ensure all `TextStyle` sizes use `sp` (they should already)
- Test with font scale 1.0, 1.5, 2.0
- Ensure no text overflow/clipping at 2.0x scale

### Landscape Support (Tablets)

#### [MODIFY] Key screens
- `HomeScreen`: 4 widgets per row in landscape vs 3 in portrait
- `FileManagerScreen`: Two-pane layout (folder tree + file list)
- `CalculatorScreen`: Scientific mode default in landscape
- `PdfReaderScreen`: Dual-page view in landscape

---

## Proposed Changes Summary

### Files by Module

| Module | Files Modified | Files Created |
|--------|---------------|--------------|
| `app/` | 3 (build.gradle.kts, AndroidManifest.xml, MainActivity.kt) | 4 (SplashActivity, AneganAudioService, CrashHandler, ShortcutManager) |
| `core/designsystem/` | 2 (Theme.kt, Color.kt) | 5 (ThemePreference, EmptyState, ShimmerEffect, HapticHelper, NowPlayingBar) |
| `core/database/` | 1 (migration) | 0 |
| `feature/dashboard/` | 1 (HomeScreen.kt) | 1 (OnboardingScreen) |
| `feature/conversion-flow/` | 3 (Calculator, DevTools, AudioPlayer) | 1 (VoiceRecorder — conditional) |
| `feature/document-reader/` | 2 (PdfReader, EpubReader) | 0 |
| `feature/notes/` | 1 (SecondBrainScreen) | 1 (NoteGraphScreen) |
| `feature/file-manager/` | 1 (FileManagerScreen) | 1 (StorageAnalyzer — conditional) |
| `feature/history/` | 1 (HistoryScreen) | 0 |
| `feature/vault/` | 1 (SecureVaultScreen) | 0 |
| `res/xml/` | 0 | 1 (shortcuts.xml) |
| **Total** | **~16** | **~14** |

---

## Verification Plan

### Automated Tests
```bash
# Full debug build to verify all changes compile
./gradlew assembleDebug

# Lint check for accessibility issues
./gradlew lint

# Check for unused resources
./gradlew lintDebug --check UnusedResources
```

### Manual Verification
1. **Dark Mode**: Toggle between all 4 modes (System/Light/Dark/AMOLED), verify no crashes or illegible text on every screen
2. **Audio Service**: Play audio → leave app → verify notification controls work → verify playback continues
3. **Splash Screen**: Cold start → verify splash shows → transitions to home/onboarding
4. **Navigation Transitions**: Navigate between 5+ screens, verify smooth slide animations
5. **Second Brain Graph**: Create 5+ linked notes → open graph view → verify nodes & connections render
6. **Empty States**: Verify every list screen shows proper empty state when data is empty
7. **Crash Handler**: Force a crash → reopen app → verify crash dialog appears
8. **App Shortcuts**: Long-press app icon → verify 4 shortcuts appear → tap each one
9. **Tablet/Landscape**: Rotate to landscape → verify HomeScreen & FileManager adapt layout
10. **Font Scale**: Set system font to 2.0x → verify no text clipping on key screens

### Build Verification
- Full `assembleRelease` must pass with R8/ProGuard enabled (no missing class warnings)
- APK size comparison: V3 vs V3.1 (should not increase >5MB)

---

## Execution Order

| Phase | Name | Dependencies | Estimated Effort |
|-------|------|-------------|-----------------|
| 1 | Build & SDK Modernization | None | Small |
| 2 | Dark Mode | Phase 1 | Medium |
| 3 | Audio MediaSession | Phase 1 | Large |
| 4 | UI/UX Polish | Phase 1, 2 | Medium |
| 5 | Second Brain Completion | Phase 1 | Large |
| 6 | Performance & Stability | Phase 1 | Medium |
| 7 | New Features (conditional) | Phase 1, 4 | Medium-Large |
| 8 | Accessibility & Quality | Phase 2, 4 | Medium |

**Total estimated phases**: 8  
**Critical path**: Phase 1 → Phase 2 → Phase 4 → Phase 8
