# 🦁 ANEGAN V3 — THE BEAST UPDATE
## Comprehensive Implementation Plan

> **Version**: 3.0 | **Codename**: Beast  
> **Current Version**: 2.1 | **App ID**: com.anegan.anegan  
> **Architecture**: Kotlin + Jetpack Compose + Material Design 3  
> **Target**: Complete overhaul of every nook and corner

---

# TABLE OF CONTENTS

1. [Executive Summary](#1-executive-summary)
2. [Architecture & Foundation Changes](#2-architecture--foundation-changes)
3. [Universal File Opening System](#3-universal-file-opening-system)
4. [Home Screen Redesign](#4-home-screen-redesign)
5. [File Manager — Complete Overhaul](#5-file-manager--complete-overhaul)
6. [Video Player — Beast Mode](#6-video-player--beast-mode)
7. [Audio Player — Full Music Experience](#7-audio-player--full-music-experience)
8. [PDF Viewer & Editor — Professional Grade](#8-pdf-viewer--editor--professional-grade)
9. [Video Tools — Massive Expansion](#9-video-tools--massive-expansion)
10. [Audio Tools — Complete Suite](#10-audio-tools--complete-suite)
11. [Notes — Dual Mode System](#11-notes--dual-mode-system)
12. [Calculator — Scientific Powerhouse](#12-calculator--scientific-powerhouse)
13. [Unit Converter — Live Currency & Manual Override](#13-unit-converter--live-currency--manual-override)
14. [Compass — Precision Instrument](#14-compass--precision-instrument)
15. [Flashlight — Intensity Control](#15-flashlight--intensity-control)
16. [Developer Tools — Professional Suite](#16-developer-tools--professional-suite)
17. [Secure Vault — Enterprise Security](#17-secure-vault--enterprise-security)
18. [Document Hub — Knowledge Center](#18-document-hub--knowledge-center)
19. [FTP Server — Complete Fix & Overhaul](#19-ftp-server--complete-fix--overhaul)
20. [Settings — Crash Fix & Premium Design](#20-settings--crash-fix--premium-design)
21. [History — Rich Activity Tracker](#21-history--rich-activity-tracker)
22. [Image Watermark — Full Image Editor](#22-image-watermark--full-image-editor)
23. [Batch Processing — Industrial Grade](#23-batch-processing--industrial-grade)
24. [App Extractor — Simplified](#24-app-extractor--simplified)
25. [Local Transfer Hub — Redesign](#25-local-transfer-hub--redesign)
26. [Smart Offline Saver — Vault Integration](#26-smart-offline-saver--vault-integration)
27. [OCR Text Extraction — Enhanced](#27-ocr-text-extraction--enhanced)
28. [EXIF Metadata — Premium Viewer](#28-exif-metadata--premium-viewer)
29. [Survival Guide — Crash Fix & Separation](#29-survival-guide--crash-fix--separation)
30. [Image Converter — Standalone](#30-image-converter--standalone)
31. [Offline Communication — User Guide](#31-offline-communication--user-guide)
32. [Continue Reading — Widget Conversion](#32-continue-reading--widget-conversion)
33. [Update Check — Widget Conversion](#33-update-check--widget-conversion)
34. [AI Background — Complete Deletion](#34-ai-background--complete-deletion)
35. [How It Works — Universal Guide System](#35-how-it-works--universal-guide-system)
36. [User Guide Widget — New](#36-user-guide-widget--new)
37. [Design System Overhaul](#37-design-system-overhaul)
38. [Back Button — Premium Redesign](#38-back-button--premium-redesign)
39. [Dark Mode — Force Light Mode](#39-dark-mode--force-light-mode)
40. [Scroll State Preservation](#40-scroll-state-preservation)
41. [Widget Icons & Text — Complete Redesign](#41-widget-icons--text--complete-redesign)
42. [File Logos in All Widgets](#42-file-logos-in-all-widgets)
43. [Anegan Folder — Root Storage](#43-anegan-folder--root-storage)
44. [File Converter Precision](#44-file-converter-precision)
45. [In-Place File Editing](#45-in-place-file-editing)
46. [Verification Plan](#46-verification-plan)

---

# 1. EXECUTIVE SUMMARY

## Current State Analysis

The Anegan app v2.1 is an all-in-one file manager and utility suite with **85 Kotlin files** across a monolithic `feature:home` module. While the foundation is solid with ExoPlayer, FFmpegKit, ML Kit, Room Database, and CameraX integrations, many features are at MVP level and need massive expansion.

### Critical Issues Found
| # | Issue | Severity |
|---|-------|----------|
| 1 | App cannot open files from other file managers (no intent-filters for file types) | 🔴 Critical |
| 2 | Settings widget completely crashed | 🔴 Critical |
| 3 | PDF Reader/Editor crashed | 🔴 Critical |
| 4 | Survival Guide showing Image Converter (wrong screen) | 🔴 Critical |
| 5 | FTP server not working (binding issues) | 🔴 Critical |
| 6 | FTP web portal not working | 🔴 Critical |
| 7 | Dark mode causes crashes | 🔴 Critical |
| 8 | Video player has no gesture controls | 🟡 Major |
| 9 | Audio player missing playlists, albums, equalizer | 🟡 Major |
| 10 | Notes has no Second Brain mode | 🟡 Major |
| 11 | Calculator has no scientific mode | 🟡 Major |
| 12 | Unit converter currency rates outdated | 🟡 Major |
| 13 | Home screen scroll position lost on back | 🟡 Major |
| 14 | Update check popup blocks UI | 🟡 Major |
| 15 | Continue Reading is fixed/pinned (should be widget) | 🟡 Major |
| 16 | AI Background feature exists (should be deleted) | 🟡 Major |
| 17 | Widget icons and text design is poor | 🟡 Major |
| 18 | Back button icon is bad | 🟢 Minor |
| 19 | Anegan folder in Documents/ instead of root | 🟢 Minor |
| 20 | File manager storage cards too large | 🟢 Minor |

### V3 Vision
Transform Anegan from an amateur utility app into a **professional, premium, beast-level** super-app that:
- Opens ANY file type natively
- Provides pro-grade media players with gesture controls
- Offers a dual-mode notes system (simple + Second Brain)
- Has flawless FTP/transfer capabilities
- Features stunning UI with premium icons and smooth animations
- Includes comprehensive "How It Works" guides for every feature
- Operates crash-free with optimized code throughout

---

# 2. ARCHITECTURE & FOUNDATION CHANGES

## 2.1 Module Structure Cleanup

### Current Structure (Monolithic)
```
app/
core/common/
core/designsystem/
core/conversion-engine/
core/database/
feature/home/          ← ALL 85 screens here
```

### Proposed Structure (No module splitting — maintain current but organize files)
We will keep the monolithic structure to avoid build complexity but organize files into clear sub-packages:

```
feature/home/src/main/java/com/anegan/home/
├── navigation/
│   └── AnegalNavigation.kt
├── home/
│   └── HomeScreen.kt
├── filemanager/
│   ├── FileManagerScreen.kt
│   └── FileViewerScreen.kt        [NEW]
├── media/
│   ├── VideoPlayerScreen.kt
│   ├── AudioPlayerScreen.kt
│   ├── VideoToolsScreen.kt
│   ├── AudioToolsScreen.kt
│   └── AudioCutterScreen.kt
├── documents/
│   ├── PdfReaderScreen.kt
│   ├── PdfToolsScreen.kt
│   └── DocumentHubScreen.kt
├── images/
│   ├── ImageConverterScreen.kt
│   ├── ImageWatermarkScreen.kt
│   ├── OcrScreen.kt
│   └── ExifMetadataScreen.kt
├── tools/
│   ├── DeveloperToolScreen.kt
│   ├── CalculatorScreen.kt
│   ├── UnitConverterScreen.kt
│   ├── CompassScreen.kt
│   └── FlashlightScreen.kt
├── security/
│   ├── SecureVaultScreen.kt
│   └── AppExtractorScreen.kt
├── productivity/
│   ├── NotesScreen.kt
│   ├── SecondBrainScreen.kt        [NEW]
│   ├── SettingsScreen.kt
│   └── HistoryScreen.kt
├── transfer/
│   ├── FtpServerScreen.kt
│   ├── LocalTransferScreen.kt
│   └── SmartOfflineSaverScreen.kt
├── learning/
│   ├── SurvivalGuideScreen.kt
│   ├── OfflineCommunicationScreen.kt
│   └── HowItWorksScreen.kt
├── batch/
│   └── BatchProcessingScreen.kt
├── widgets/
│   ├── ContinueReadingWidget.kt    [NEW — was fixed bar]
│   ├── UpdateCheckWidget.kt        [NEW — was popup]
│   └── UserGuideWidget.kt          [NEW]
└── common/
    ├── FileOpenerHelper.kt         [NEW]
    └── ScrollStateManager.kt       [NEW]
```

## 2.2 Database Schema Expansion

### Current Database (v3, 3 entities)
- `ConversionHistoryEntity`
- `NoteEntity`
- `VaultFileEntity`

### Proposed Database (v4, 15+ entities)

#### [NEW] `PlaylistEntity` (table: `playlists`)
| Column | Type | Notes |
|--------|------|-------|
| id | String | PK (UUID) |
| name | String | |
| description | String | |
| coverArtPath | String? | |
| createdAt | Long | |
| updatedAt | Long | |

#### [NEW] `PlaylistTrackEntity` (table: `playlist_tracks`)
| Column | Type | Notes |
|--------|------|-------|
| id | String | PK (UUID) |
| playlistId | String | FK → playlists |
| trackPath | String | File path |
| position | Int | Order in playlist |
| addedAt | Long | |

#### [NEW] `AudioTrackEntity` (table: `audio_tracks`)
| Column | Type | Notes |
|--------|------|-------|
| id | String | PK (UUID) |
| filePath | String | Unique |
| title | String | |
| artist | String | |
| album | String | |
| duration | Long | milliseconds |
| albumArtPath | String? | |
| genre | String? | |
| year | Int? | |
| playCount | Int | Default 0 |
| lastPlayedAt | Long? | |
| isFavorite | Boolean | Default false |
| addedAt | Long | |

#### [NEW] `AudioPlaybackStateEntity` (table: `audio_playback_state`)
| Column | Type | Notes |
|--------|------|-------|
| id | Int | PK (always 1 — singleton) |
| currentTrackPath | String? | |
| currentPosition | Long | milliseconds |
| currentPlaylistId | String? | |
| shuffleEnabled | Boolean | |
| repeatMode | Int | 0=off, 1=all, 2=one |

#### [NEW] `ReadingProgressEntity` (table: `reading_progress`)
| Column | Type | Notes |
|--------|------|-------|
| id | String | PK (UUID) |
| filePath | String | |
| fileName | String | |
| fileType | String | pdf, epub, txt, docx |
| currentPage | Int | |
| totalPages | Int | |
| lastReadAt | Long | |
| percentComplete | Float | |
| thumbnailPath | String? | |

#### [NEW] `BookmarkEntity` (table: `bookmarks`)
| Column | Type | Notes |
|--------|------|-------|
| id | String | PK (UUID) |
| filePath | String | |
| pageNumber | Int | |
| label | String | |
| createdAt | Long | |

#### [NEW] `SecondBrainNoteEntity` (table: `second_brain_notes`)
| Column | Type | Notes |
|--------|------|-------|
| id | String | PK (UUID) |
| title | String | |
| content | String | Markdown content |
| parentId | String? | For hierarchy |
| tags | String | Comma-separated |
| linkedNoteIds | String | Comma-separated UUIDs |
| sourceUrl | String? | Web clip source |
| attachmentPaths | String | Comma-separated paths |
| noteType | String | "note", "idea", "project", "reference", "journal" |
| status | String | "active", "archived", "inbox" |
| priority | Int | 0-5 |
| isPinned | Boolean | |
| createdAt | Long | |
| updatedAt | Long | |

#### [NEW] `SecondBrainTagEntity` (table: `second_brain_tags`)
| Column | Type | Notes |
|--------|------|-------|
| id | String | PK (UUID) |
| name | String | Unique |
| color | String | Hex color |
| noteCount | Int | Cached count |
| createdAt | Long | |

#### [NEW] `AppUsageHistoryEntity` (table: `app_usage_history`)
| Column | Type | Notes |
|--------|------|-------|
| id | String | PK (UUID) |
| featureName | String | "Video Player", "Calculator", etc. |
| actionType | String | "open", "convert", "edit", "create", "delete" |
| details | String | JSON details |
| filePath | String? | Related file |
| timestamp | Long | |
| durationMs | Long? | Time spent |

#### [NEW] `CurrencyRateEntity` (table: `currency_rates`)
| Column | Type | Notes |
|--------|------|-------|
| code | String | PK ("USD", "EUR", etc.) |
| name | String | Full name |
| rateToUsd | Double | Rate relative to USD |
| isManualOverride | Boolean | User-set rate |
| manualRate | Double? | User-provided rate |
| lastUpdatedAt | Long | |

#### [NEW] `CalculatorHistoryEntity` (table: `calculator_history`)
| Column | Type | Notes |
|--------|------|-------|
| id | String | PK (UUID) |
| expression | String | "sin(45) + 2^3" |
| result | String | "8.707..." |
| mode | String | "basic" or "scientific" |
| timestamp | Long | |

#### [MODIFY] Expand `NoteEntity` with new fields:
| New Column | Type | Notes |
|------------|------|-------|
| folderName | String | Default "General" |
| checklistItems | String | JSON array of {text, checked} |
| dueDate | Long? | For tasks |
| attachmentPaths | String | Comma-separated |
| wordCount | Int | Auto-calculated |

#### [MODIFY] Expand `VaultFileEntity` with new fields:
| New Column | Type | Notes |
|------------|------|-------|
| thumbnailPath | String? | Encrypted thumbnail |
| expiryDate | Long? | Document expiry tracking |
| notes | String | User notes about document |
| lastAccessedAt | Long | |
| accessCount | Int | |
| sharingHistory | String | JSON array of share events |

### Migration Strategy
- Database version bump: 3 → 4
- Use `addMigrations()` instead of `fallbackToDestructiveMigration()` to preserve user data
- Write explicit `Migration(3, 4)` with ALTER TABLE statements for existing tables
- CREATE TABLE for all new tables

---

## 2.3 Dependency Updates

### [MODIFY] `feature/home/build.gradle.kts`

#### New Dependencies to Add:
```kotlin
// Gesture detection for video player
implementation("androidx.compose.foundation:foundation")

// Advanced PDF rendering
implementation("com.github.barteksc:android-pdf-viewer:3.2.0-beta.1")

// Currency exchange rates API
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:okhttp:4.12.0")

// Markdown rendering for Second Brain
implementation("io.noties.markwon:core:4.6.2")
implementation("io.noties.markwon:ext-tables:4.6.2")
implementation("io.noties.markwon:ext-strikethrough:4.6.2")
implementation("io.noties.markwon:ext-tasklist:4.6.2")

// MediaMetadataRetriever for audio metadata + album art
implementation("androidx.media3:media3-session:1.2.1")

// Flashlight intensity control (already have CameraX)
// No new dep needed — use CameraManager API

// Scientific calculator expression parser
implementation("net.objecthunter:exp4j:0.4.8")
```

---

## 2.4 AndroidManifest.xml — Major Changes

### [MODIFY] `app/src/main/AndroidManifest.xml`

#### Add Intent Filters for File Opening

The app must register to handle ALL file types so it appears in "Open with" dialogs:

```xml
<!-- Universal File Opener -->
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    
    <!-- Video files -->
    <data android:mimeType="video/*" />
</intent-filter>

<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    
    <!-- Audio files -->
    <data android:mimeType="audio/*" />
</intent-filter>

<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    
    <!-- PDF files -->
    <data android:mimeType="application/pdf" />
</intent-filter>

<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    
    <!-- Image files -->
    <data android:mimeType="image/*" />
</intent-filter>

<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    
    <!-- Text files -->
    <data android:mimeType="text/*" />
</intent-filter>

<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    
    <!-- Document files -->
    <data android:mimeType="application/msword" />
    <data android:mimeType="application/vnd.openxmlformats-officedocument.wordprocessingml.document" />
    <data android:mimeType="application/vnd.ms-excel" />
    <data android:mimeType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" />
    <data android:mimeType="application/vnd.ms-powerpoint" />
    <data android:mimeType="application/vnd.openxmlformats-officedocument.presentationml.presentation" />
    <data android:mimeType="application/epub+zip" />
    <data android:mimeType="application/rtf" />
    <data android:mimeType="text/html" />
    <data android:mimeType="text/csv" />
</intent-filter>

<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    
    <!-- Archive files -->
    <data android:mimeType="application/zip" />
    <data android:mimeType="application/x-rar-compressed" />
    <data android:mimeType="application/x-7z-compressed" />
    <data android:mimeType="application/x-tar" />
    <data android:mimeType="application/gzip" />
</intent-filter>

<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    
    <!-- APK files -->
    <data android:mimeType="application/vnd.android.package-archive" />
</intent-filter>

<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    
    <!-- Any other file (catch-all) -->
    <data android:mimeType="application/octet-stream" />
    <data android:mimeType="*/*" />
</intent-filter>
```

#### File Extension Support (scheme-based)
```xml
<!-- File URI scheme for specific extensions -->
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="file" />
    <data android:scheme="content" />
    <data android:host="*" />
    <data android:pathPattern=".*\\.mp4" />
    <data android:pathPattern=".*\\.mkv" />
    <data android:pathPattern=".*\\.avi" />
    <data android:pathPattern=".*\\.mov" />
    <data android:pathPattern=".*\\.wmv" />
    <data android:pathPattern=".*\\.flv" />
    <data android:pathPattern=".*\\.webm" />
    <data android:pathPattern=".*\\.3gp" />
    <data android:pathPattern=".*\\.m4v" />
    <data android:pathPattern=".*\\.ts" />
    <data android:pathPattern=".*\\.mp3" />
    <data android:pathPattern=".*\\.wav" />
    <data android:pathPattern=".*\\.flac" />
    <data android:pathPattern=".*\\.aac" />
    <data android:pathPattern=".*\\.ogg" />
    <data android:pathPattern=".*\\.wma" />
    <data android:pathPattern=".*\\.m4a" />
    <data android:pathPattern=".*\\.opus" />
    <data android:pathPattern=".*\\.pdf" />
    <data android:pathPattern=".*\\.doc" />
    <data android:pathPattern=".*\\.docx" />
    <data android:pathPattern=".*\\.txt" />
    <data android:pathPattern=".*\\.epub" />
    <data android:pathPattern=".*\\.jpg" />
    <data android:pathPattern=".*\\.jpeg" />
    <data android:pathPattern=".*\\.png" />
    <data android:pathPattern=".*\\.gif" />
    <data android:pathPattern=".*\\.webp" />
    <data android:pathPattern=".*\\.bmp" />
    <data android:pathPattern=".*\\.svg" />
    <data android:pathPattern=".*\\.zip" />
    <data android:pathPattern=".*\\.rar" />
    <data android:pathPattern=".*\\.7z" />
    <data android:pathPattern=".*\\.apk" />
</intent-filter>
```

---

# 3. UNIVERSAL FILE OPENING SYSTEM

## 3.1 Overview
When a user opens ANY file with Anegan from another file manager or app, Anegan must:
1. Detect the file type (MIME type + extension)
2. Route to the appropriate native viewer
3. Provide a seamless, instant viewing experience

## 3.2 File Routing Logic

### [NEW] `FileOpenerHelper.kt`
```
Input: Intent with URI + MIME type
Output: Route to appropriate screen
```

| File Type | MIME Pattern | Route To | Features |
|-----------|-------------|----------|----------|
| Video | `video/*` | `VideoPlayerScreen` | Full player with gestures |
| Audio | `audio/*` | `AudioPlayerScreen` | Full player with album art |
| PDF | `application/pdf` | `PdfReaderScreen` | Zoom, search, goto page |
| Image | `image/*` | `ImageViewerScreen` [NEW] | Zoom, rotate, share, edit |
| Text | `text/plain`, `text/csv`, `text/html` | `TextViewerScreen` [NEW] | Syntax highlighting, word wrap |
| Word Doc | `application/msword`, `...wordprocessingml...` | `DocumentViewerScreen` [NEW] | Basic rendering |
| Excel | `application/vnd.ms-excel`, `...spreadsheetml...` | `DocumentViewerScreen` [NEW] | Table rendering |
| PowerPoint | `...presentationml...` | `DocumentViewerScreen` [NEW] | Slide view |
| EPUB | `application/epub+zip` | `EpubReaderScreen` [NEW] | Chapter navigation |
| Archive | `application/zip`, `x-rar`, `x-7z` | `ArchiveViewerScreen` [NEW] | List contents, extract |
| APK | `application/vnd.android.package-archive` | `ApkInfoScreen` [NEW] | Package info, install |
| Other/Unknown | `*/*` | `GenericFileInfoScreen` [NEW] | File details, share, open-with |

### [MODIFY] `MainActivity.kt`
Add intent handling in `onCreate()`:
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    val fileUri = intent?.data ?: intent?.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
    val mimeType = intent?.type ?: contentResolver.getType(fileUri ?: Uri.EMPTY)
    
    // Route to appropriate viewer
    val initialRoute = FileOpenerHelper.getRouteForFile(fileUri, mimeType)
    
    setContent {
        AnegalTheme(darkTheme = false) { // Always light mode
            AnegalNavigation(
                initialRoute = initialRoute,
                initialFileUri = fileUri
            )
        }
    }
}
```

## 3.3 New Viewer Screens

### [NEW] `ImageViewerScreen.kt`
Full-featured image viewer:
- **Pinch-to-zoom** with smooth animations (min 1x, max 10x)
- **Double-tap to zoom** (toggle between 1x and 3x)
- **Pan/scroll** when zoomed in
- **Swipe gestures** for next/previous image in same folder
- **Bottom toolbar**: Share, Edit (→ Image Watermark), Delete, Info, Rotate
- **Top toolbar**: Filename, file size, dimensions
- **Edit integration**: Direct link to crop, rotate, filter tools
- **Slideshow mode**: Auto-advance with configurable interval
- **Support formats**: JPG, JPEG, PNG, GIF (animated), WebP, BMP, SVG, HEIC, AVIF

### [NEW] `TextViewerScreen.kt`
Feature-rich text viewer:
- **Syntax highlighting** for code files (.kt, .java, .py, .js, .html, .css, .xml, .json, .yaml, .md)
- **Line numbers** (toggleable)
- **Word wrap** (toggleable)
- **Font size adjustment** (pinch-to-zoom or buttons)
- **Search** with highlight all matches
- **Go to line** number
- **Share** text content
- **Copy all** to clipboard
- **Encoding detection** (UTF-8, ASCII, ISO-8859-1)
- **Markdown preview** for .md files (rendered via Markwon)

### [NEW] `DocumentViewerScreen.kt`
Basic document rendering for Office files:
- **DOCX**: Parse XML content, render paragraphs with basic formatting
- **XLSX**: Parse as table, render in scrollable grid
- **PPTX**: Parse slides, show as carousel
- **RTF**: Basic text rendering
- **Toolbar**: Share, Open externally, File info

### [NEW] `EpubReaderScreen.kt`
EPUB reading experience:
- **Chapter navigation** (table of contents sidebar)
- **Reading progress** tracking (saved to database)
- **Font size** adjustment
- **Background color** themes (white, sepia, dark)
- **Bookmarks** system
- **Search** within book
- **Reading time estimate**
- **Continuous scroll** or page-by-page mode

### [NEW] `ArchiveViewerScreen.kt`
Archive file management:
- **List contents** of ZIP, RAR, 7z, TAR, GZ
- **Extract all** to chosen directory
- **Extract selected** files
- **Preview** individual files inside archive
- **File tree view** with nested folders
- **Progress indicator** during extraction
- **Compression info**: Original size, compressed size, ratio

### [NEW] `ApkInfoScreen.kt`
APK file inspector:
- **Package name**, version, min SDK, target SDK
- **Permissions** list with explanations
- **App icon** preview
- **File size**
- **Install button** (if from trusted source)
- **Activities, services, receivers** list
- **Signature info**

### [NEW] `GenericFileInfoScreen.kt`
For unknown file types:
- **File name, size, path, extension**
- **MIME type** detection attempt
- **Created/modified dates**
- **MD5/SHA256 hash**
- **Open with** other apps
- **Share** via intent
- **Copy to** Anegan folder
- **Move to** another location

---

# 4. HOME SCREEN REDESIGN

## 4.1 Current Problems
1. Widget icons are bad — poorly designed, inconsistent
2. Widget text labels are poorly formatted
3. Too many widgets visible at once — cluttered
4. Continue Reading is a fixed bar — should be a scrollable widget
5. Update check is a popup dialog — should be a widget
6. No scroll state preservation when navigating back
7. No file logo/link symbol on widgets
8. Grid layout is not premium — looks amateurish

## 4.2 New Home Screen Layout

### Structure (Top to Bottom)

```
┌─────────────────────────────────────┐
│  🔍 Search Bar (tap to search)      │
├─────────────────────────────────────┤
│  📁 FILE MANAGER (Hero Card)        │ ← Premium gradient card, tap to open
│  Internal: 45.2 GB / 128 GB        │
│  [████████░░░] 35%                  │
├─────────────────────────────────────┤
│  📖 CONTINUE READING (Widget)       │ ← Scrollable horizontal carousel
│  [Book1] [Book2] [PDF1] [→ More]   │
├─────────────────────────────────────┤
│  ⭐ QUICK ACCESS (3 per row)        │ ← Most used features (auto-detected)
│  [Video] [Audio] [PDF]              │
├─────────────────────────────────────┤
│  📂 CATEGORIES (Expandable cards)   │
│                                     │
│  ┌── Media Tools ──────────────┐   │
│  │ [🎬Video] [🎵Audio] [🎥Tools]│   │ ← 3 icons per row
│  │ [→ See All 6 items]         │   │ ← Expand to see more
│  └─────────────────────────────┘   │
│                                     │
│  ┌── Document Tools ───────────┐   │
│  │ [📄PDF] [📋Hub] [🔍OCR]     │   │
│  │ [→ See All 5 items]         │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌── Utility Tools ────────────┐   │
│  │ [🧮Calc] [🧭Compass] [🔦Light]│ │
│  │ [→ See All 8 items]         │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌── Security & Privacy ───────┐   │
│  │ [🔒Vault] [📦Extract] [🔄FTP]│  │
│  │ [→ See All 5 items]         │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌── Productivity ─────────────┐   │
│  │ [📝Notes] [⚙Settings] [📊Hist]│ │
│  │ [→ See All 4 items]         │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌── Learning & Offline ───────┐   │
│  │ [🏕Survival] [📡Offline] [💾Save]│
│  │ [→ See All 3 items]         │   │
│  └─────────────────────────────┘   │
│                                     │
│  📚 USER GUIDE (Widget)            │ ← New guide widget
│  "Tap to explore all features"      │
│                                     │
│  🔄 CHECK FOR UPDATE (Widget)      │ ← Moved from popup
│  Current: v2.1 | Latest: v3.0      │
└─────────────────────────────────────┘
```

### Key Design Principles
1. **3 icons per row maximum** — clean, not cluttered
2. **Category cards** are collapsible — show top 3, "See All" to expand
3. **File Manager is the TOP widget** — hero card with storage info
4. **Continue Reading** is a horizontal carousel widget — NOT fixed
5. **Update Check** is a widget at bottom — NOT a popup
6. **Every icon** has a file/link symbol overlay
7. **Premium rounded cards** with subtle shadows and gradient borders
8. **Micro-animations** on tap (scale down 0.95 → 1.0)
9. **Haptic feedback** on every interaction

## 4.3 Widget Icon Design

### Current Issue
Icons use basic Material Icons with no consistency. They look amateurish.

### New Design System for Icons

Each widget icon will be:
1. **Gradient background** circle (48dp) with the category's signature color
2. **White foreground icon** (24dp) — carefully chosen premium Material Icon
3. **File/link symbol** badge (12dp) at bottom-right corner
4. **Rounded label** below with Outfit Medium font, 12sp

#### Icon Mapping (New Premium Icons)

| Feature | Old Icon | New Icon | Gradient Colors |
|---------|----------|----------|-----------------|
| File Manager | Folder | `Icons.Rounded.FolderOpen` | Blue → Indigo |
| Video Player | PlayArrow | `Icons.Rounded.OndemandVideo` | Red → Orange |
| Audio Player | MusicNote | `Icons.Rounded.Headphones` | Purple → Pink |
| Video Tools | VideoSettings | `Icons.Rounded.MovieFilter` | Orange → Amber |
| Audio Tools | AudioFile | `Icons.Rounded.GraphicEq` | Teal → Cyan |
| Audio Cutter | ContentCut | `Icons.Rounded.WavingHand` | Indigo → Blue |
| PDF Reader | PictureAsPdf | `Icons.Rounded.MenuBook` | Red → Rose |
| PDF Tools | Build | `Icons.Rounded.PictureAsPdf` | Crimson → Red |
| Document Hub | LibraryBooks | `Icons.Rounded.Hub` | Brown → Amber |
| Image Converter | Image | `Icons.Rounded.PhotoFilter` | Green → Lime |
| Image Watermark | WaterDrop | `Icons.Rounded.BrandingWatermark` | Blue → Sky |
| OCR | TextFields | `Icons.Rounded.DocumentScanner` | Violet → Purple |
| EXIF Metadata | Info | `Icons.Rounded.PhotoCamera` | Gray → Slate |
| Developer Tools | Code | `Icons.Rounded.Terminal` | Emerald → Green |
| Calculator | Calculate | `Icons.Rounded.Calculate` | Sky → Cyan |
| Unit Converter | SwapHoriz | `Icons.Rounded.Straighten` | Amber → Yellow |
| Compass | Explore | `Icons.Rounded.NearMe` | Navy → Blue |
| Flashlight | FlashlightOn | `Icons.Rounded.Highlight` | Yellow → Amber |
| Secure Vault | Lock | `Icons.Rounded.Shield` | Indigo → Violet |
| App Extractor | Android | `Icons.Rounded.GetApp` | Green → Teal |
| Notes | StickyNote2 | `Icons.Rounded.EditNote` | Yellow → Lime |
| Settings | Settings | `Icons.Rounded.Tune` | Gray → BlueGray |
| History | History | `Icons.Rounded.Timeline` | Slate → Gray |
| FTP Server | Wifi | `Icons.Rounded.Dns` | Blue → Cyan |
| Local Transfer | ShareOutlined | `Icons.Rounded.WifiTethering` | Teal → Blue |
| Offline Communication | SignalWifiOff | `Icons.Rounded.SatelliteAlt` | Orange → Red |
| Survival Guide | LocalFireDepartment | `Icons.Rounded.Explore` | Green → Emerald |
| Batch Processing | DynamicFeed | `Icons.Rounded.LayersClear` | Purple → Indigo |
| Smart Offline Saver | CloudDownload | `Icons.Rounded.CloudDone` | Sky → Blue |
| Continue Reading | AutoStories | `Icons.Rounded.ChromeReaderMode` | Amber → Orange |

## 4.4 Scroll State Preservation

### Problem
When user is scrolled to a specific widget (e.g., Unit Converter), taps it, uses it, then presses back → the home screen scrolls back to the top instead of where they were.

### Solution

#### [NEW] `ScrollStateManager.kt`
```kotlin
object ScrollStateManager {
    // Store LazyColumn scroll position
    private var savedScrollIndex: Int = 0
    private var savedScrollOffset: Int = 0
    
    fun saveScrollPosition(state: LazyListState) {
        savedScrollIndex = state.firstVisibleItemIndex
        savedScrollOffset = state.firstVisibleItemScrollOffset
    }
    
    fun restoreScrollPosition(state: LazyListState, coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            state.scrollToItem(savedScrollIndex, savedScrollOffset)
        }
    }
}
```

#### [MODIFY] `HomeScreen.kt`
```kotlin
@Composable
fun HomeScreen(navController: NavController) {
    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Restore position when this composable re-enters composition
    LaunchedEffect(Unit) {
        ScrollStateManager.restoreScrollPosition(scrollState, coroutineScope)
    }
    
    // Save position before navigating away
    DisposableEffect(Unit) {
        onDispose {
            ScrollStateManager.saveScrollPosition(scrollState)
        }
    }
    
    LazyColumn(state = scrollState) {
        // ... all widgets
    }
}
```

Actually — even better approach: Use `rememberSaveable` with a custom saver or use Navigation Compose's built-in state saving. Since the app uses `AnimatedContent` (not NavHost), we need the manual approach above.

---

# 5. FILE MANAGER — COMPLETE OVERHAUL

## 5.1 Current Problems
1. Storage cards are too large — waste space
2. No SD card detection working properly
3. Small cards like Internal Storage, Downloads, Documents not needed
4. App cannot open files from other file managers
5. Not all file formats supported

## 5.2 Storage Info Redesign

### Current: Large individual cards
### New: Compact inline storage indicator

```
┌──────────────────────────────────┐
│ 📱 Internal Storage              │
│ [████████░░░░░] 64.2 / 128 GB   │
│ 📸 Photos: 12.3 GB  🎬 Videos: 8.1 GB │
│ 🎵 Audio: 2.4 GB   📄 Docs: 1.8 GB   │
│ 📦 Apps: 15.2 GB   💾 Other: 24.4 GB  │
└──────────────────────────────────┘
```

- **Small, compact card** — NOT huge
- Shows **breakdown by type** with mini color bars
- If SD card present, show **second small card** below
- Remove separate "Downloads", "Documents" cards — those are in Quick Access

## 5.3 Quick Access Section (Compact)
```
┌── Quick Access ─────────────────┐
│ [📥Downloads] [📄Documents] [🖼Pictures] │
│ [🎵Music] [🎬Videos] [📦APKs]          │
└─────────────────────────────────┘
```
- Small round icons with labels (not cards)
- One tap to navigate

## 5.4 File Listing Improvements

### Every file type opens natively:
| Extension | Opens In | Capability |
|-----------|----------|------------|
| .mp4, .mkv, .avi, .mov, .wmv, .flv, .webm, .3gp, .m4v, .ts | Video Player | Full gesture controls |
| .mp3, .wav, .flac, .aac, .ogg, .wma, .m4a, .opus, .amr, .mid | Audio Player | Full music experience |
| .pdf | PDF Reader | Search, zoom, annotations |
| .jpg, .jpeg, .png, .gif, .webp, .bmp, .svg, .heic, .avif, .tiff, .ico | Image Viewer | Zoom, slideshow |
| .txt, .log, .md, .json, .xml, .html, .css, .js, .py, .kt, .java, .c, .cpp, .h, .sh, .bat, .ps1, .yaml, .yml, .ini, .cfg, .conf, .properties | Text Viewer | Syntax highlighting |
| .doc, .docx | Document Viewer | Basic rendering |
| .xls, .xlsx | Document Viewer | Table rendering |
| .ppt, .pptx | Document Viewer | Slide view |
| .epub | EPUB Reader | Full reading experience |
| .zip, .rar, .7z, .tar, .gz, .bz2, .xz | Archive Viewer | List, extract |
| .apk | APK Info | Package details, install |
| .csv | CSV Viewer | Table with sorting |
| .rtf | Text Viewer | Basic formatting |
| Other | Generic File Info | Details, share, open-with |

## 5.5 File Operations Enhancements

### New Operations
1. **Bulk select** → compress all to ZIP
2. **Properties** → detailed file info (hash, permissions, EXIF)
3. **Open with Anegan tool** → route to specific tool (e.g., add watermark)
4. **Duplicate finder** (bonus feature for premium feel)
5. **Recent files** quick access panel
6. **Starred/Favorited** files section

### File Grid View Improvements
- Show **file type icon** overlay on thumbnails
- For videos: show **duration** badge
- For images: show **dimensions** badge
- For audio: show **duration** badge
- For documents: show **page count** badge

---

# 6. VIDEO PLAYER — BEAST MODE

## 6.1 Current State
- Basic ExoPlayer with play/pause and seek bar
- NO rotation, NO gestures, NO brightness/volume control
- NO double-tap forward/backward
- Limited format support

## 6.2 Complete Feature List

### Core Playback
| Feature | Implementation |
|---------|---------------|
| Play/Pause | Tap center of screen |
| Seek | Horizontal drag on progress bar |
| Fullscreen | Auto landscape for landscape videos |
| Picture-in-Picture | PiP mode support |
| Background playback | Continue audio when app minimized |
| Resume playback | Remember position per file |

### Gesture Controls (MX Player / VLC style)
| Gesture | Action | Area |
|---------|--------|------|
| Double-tap right half | Forward 10 seconds | Right side of screen |
| Double-tap left half | Rewind 10 seconds | Left side of screen |
| Triple-tap right | Forward 30 seconds | Right side |
| Triple-tap left | Rewind 30 seconds | Left side |
| Vertical swipe right | Volume up/down | Right side |
| Vertical swipe left | Brightness up/down | Left side |
| Horizontal swipe | Seek forward/backward | Center area |
| Pinch to zoom | Scale to fill/fit | Anywhere |
| Long press | 2x speed (release to normal) | Anywhere |

### Display Controls
| Feature | Details |
|---------|---------|
| Rotation lock | Lock to portrait/landscape/auto |
| Rotate button | Manual rotate 90° clockwise |
| Aspect ratio toggle | Fit, Fill, 16:9, 4:3, Stretch |
| Screen orientation | Force landscape, portrait, or sensor |

### Brightness & Audio
| Feature | Details |
|---------|---------|
| Brightness slider | Vertical gesture on left side + overlay indicator |
| Volume slider | Vertical gesture on right side + overlay indicator |
| Audio track selection | For multi-audio files (MKV) |
| Audio boost | 200% volume boost |
| Equalizer | 5-band + presets (Rock, Pop, Jazz, Classical, Bass Boost) |

### Subtitle Support
| Feature | Details |
|---------|---------|
| Built-in subtitles | SRT, ASS, SSA, VTT |
| External subtitles | Load .srt file from storage |
| Subtitle styling | Font size, color, background, position |
| Subtitle delay | Adjust sync +/- milliseconds |
| Subtitle encoding | UTF-8, ISO-8859-1, auto-detect |

### Playback Features
| Feature | Details |
|---------|---------|
| Speed control | 0.25x, 0.5x, 0.75x, 1x, 1.25x, 1.5x, 2x, 3x + custom |
| A-B repeat | Loop between two points |
| Sleep timer | Stop after X minutes |
| Playlist | Play all videos in folder |
| Next/Previous | Navigate between files in folder |
| Loop mode | Off, One, All |

### Format Support (via ExoPlayer + FFmpeg extension)
```
Video: MP4, MKV, AVI, MOV, WMV, FLV, WebM, 3GP, M4V, TS, MPEG, 
       VOB, OGV, ASF, RMVB, DIVX
Audio: MP3, WAV, FLAC, AAC, OGG, WMA, M4A, OPUS, AMR, APE, 
       DTS, AC3, EAC3, TrueHD
Subtitles: SRT, ASS, SSA, VTT, SUB, IDX, PGS, DVB
Container: MKV, MP4, AVI, WebM, TS
```

### UI Design
```
┌──────────────────────────────────┐
│  ← filename.mp4          🔒 🔊  │  ← Top bar (auto-hide)
│                                  │
│                                  │
│          [Brightness]   [Volume] │  ← Gesture overlays (when active)
│              ☀️            🔊    │
│              |||           |||   │
│                                  │
│           ▶️  (center)           │  ← Play/pause (tap to toggle)
│                                  │
│  ⏪ -10s        ▶️       +10s ⏩ │  ← Double-tap indicators (when active)
│                                  │
│  00:05:32 ═══════●══════ 01:32:15│  ← Progress bar
│  🔄 ⏪ ⏸️ ⏩ 🔄 │ 🔊 🔄 ⚙️    │  ← Bottom controls
│  Loop Prev Play Next│ Vol Rot Set│
└──────────────────────────────────┘
```

### Settings Panel (⚙️ slide-up)
- Speed: `[0.5x] [0.75x] [1x] [1.25x] [1.5x] [2x]`
- Aspect: `[Fit] [Fill] [16:9] [4:3]`
- Subtitles: `[None] [Track 1] [Load External]`
- Audio Track: `[Track 1 - English] [Track 2 - Japanese]`
- Sleep Timer: `[Off] [15min] [30min] [1hr] [Custom]`
- Equalizer: `[Flat] [Rock] [Pop] [Jazz] [Custom]`

## 6.3 Technical Implementation

### Gesture Detection System
```kotlin
@Composable
fun VideoGestureOverlay(
    player: ExoPlayer,
    onBrightnessChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit
) {
    var brightnessLevel by remember { mutableFloatStateOf(0.5f) }
    var volumeLevel by remember { mutableFloatStateOf(0.5f) }
    var showBrightnessOverlay by remember { mutableStateOf(false) }
    var showVolumeOverlay by remember { mutableStateOf(false) }
    var showForwardIndicator by remember { mutableStateOf(false) }
    var showRewindIndicator by remember { mutableStateOf(false) }
    
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val halfScreen = screenWidth / 2
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Left half — brightness gesture
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(halfScreen.dp)
                .align(Alignment.CenterStart)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        brightnessLevel = (brightnessLevel - dragAmount / 500f)
                            .coerceIn(0f, 1f)
                        showBrightnessOverlay = true
                        onBrightnessChange(brightnessLevel)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            // Rewind 10 seconds
                            player.seekTo(player.currentPosition - 10_000)
                            showRewindIndicator = true
                        }
                    )
                }
        )
        
        // Right half — volume gesture
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(halfScreen.dp)
                .align(Alignment.CenterEnd)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        volumeLevel = (volumeLevel - dragAmount / 500f)
                            .coerceIn(0f, 1f)
                        showVolumeOverlay = true
                        onVolumeChange(volumeLevel)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            // Forward 10 seconds
                            player.seekTo(player.currentPosition + 10_000)
                            showForwardIndicator = true
                        }
                    )
                }
        )
        
        // Overlay indicators
        AnimatedVisibility(showBrightnessOverlay) {
            BrightnessOverlay(brightnessLevel)
        }
        AnimatedVisibility(showVolumeOverlay) {
            VolumeOverlay(volumeLevel)
        }
    }
}
```

### Rotation Implementation
```kotlin
// In VideoPlayerScreen
var currentOrientation by remember { mutableIntStateOf(ActivityInfo.SCREEN_ORIENTATION_SENSOR) }

fun rotateScreen(activity: Activity) {
    currentOrientation = when (currentOrientation) {
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT -> 
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE -> 
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
        ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE -> 
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        else -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }
    activity.requestedOrientation = currentOrientation
}
```

### Brightness Control
```kotlin
fun setBrightness(activity: Activity, brightness: Float) {
    val params = activity.window.attributes
    params.screenBrightness = brightness // 0.0 to 1.0
    activity.window.attributes = params
}
```

---

# 7. AUDIO PLAYER — FULL MUSIC EXPERIENCE

## 7.1 Current State
- Basic play/pause with seek bar
- Placeholder album art
- No playlists, no album browsing, no equalizer
- No "most listened" tracking

## 7.2 Complete Feature List

### Library Organization
| View | Description |
|------|-------------|
| All Songs | Complete list sorted by title/artist/recently added |
| Albums | Grid of album covers with name and artist |
| Artists | List of artists with song count |
| Playlists | User-created playlists + auto-generated smart playlists |
| Folders | Browse by directory structure |
| Genres | Filter by genre |
| Most Listened | Top 25/50/100 most played tracks |
| Recently Played | Last 50 played tracks with timestamps |
| Recently Added | Tracks added in last 7/14/30 days |
| Favorites | Hearted/starred tracks |

### Now Playing Screen
```
┌──────────────────────────────────┐
│  ← Now Playing                   │
│                                  │
│  ┌──────────────────────────┐   │
│  │                          │   │
│  │     [Album Art Image]    │   │  ← Large album art (from file metadata)
│  │     (300dp × 300dp)      │   │     Falls back to gradient + music icon
│  │                          │   │
│  └──────────────────────────┘   │
│                                  │
│      Song Title Here             │  ← Marquee scroll if too long
│      Artist Name - Album Name    │
│                                  │
│  01:23 ═══════●════════ 04:56   │  ← Seek bar
│                                  │
│  🔀   ⏮   ⏪   ▶️   ⏩   ⏭   🔁 │  ← Controls
│ Shuf  Prev -10  Play +10 Next Rep│
│                                  │
│  ❤️        📋        🔊        ⏱  │  ← Actions
│ Fav    Playlist   Vol    Timer   │
│                                  │
│  ═══════════════════════════════ │
│  Up Next:                        │
│  • Next Song Title               │
│  • Another Song                  │
│  • More Song                     │
└──────────────────────────────────┘
```

### Player Features
| Feature | Details |
|---------|---------|
| Album Art | Extract from file metadata (ID3 tags), display prominently |
| Seek 10s Forward | Tap ⏩ button |
| Seek 10s Backward | Tap ⏪ button |
| Next/Previous | Skip to next/previous track in queue |
| Shuffle | Randomize queue order |
| Repeat | Off → Repeat All → Repeat One |
| Favorite | Heart/star toggle, saved to DB |
| Sleep Timer | Stop after 15/30/45/60/90 min or end of current track |
| Speed Control | 0.5x to 3x in 0.25 increments |
| Queue Management | View up-next list, reorder, add/remove |
| Notification Controls | Media notification with play/pause/next/previous |
| Lock Screen | Lock screen controls and album art |
| Audio Focus | Pause on call, duck on notification |
| Gapless Playback | Seamless transition between tracks |
| Crossfade | 0-12 second crossfade between tracks |

### Playlist Management
| Feature | Details |
|---------|---------|
| Create Playlist | Name, optional cover image, description |
| Edit Playlist | Rename, change cover, reorder tracks |
| Delete Playlist | With confirmation |
| Add to Playlist | From any song list, long-press menu |
| Smart Playlists | Auto-generated: Most Played, Recently Added, Favorites |
| Import Playlist | M3U, M3U8, PLS file support |
| Export Playlist | Save as M3U8 |

### Album Art Extraction
```kotlin
fun extractAlbumArt(filePath: String): Bitmap? {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(filePath)
        val art = retriever.embeddedPicture
        return art?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
    } finally {
        retriever.release()
    }
}
```

### Audio Format Support
```
Lossless: FLAC, WAV, ALAC, AIFF, APE, WV (WavPack), DSD
Lossy: MP3, AAC, OGG/Vorbis, Opus, WMA, M4A, AMR
Container: MKA (Matroska Audio)
Metadata: ID3v1, ID3v2, Vorbis Comment, APE tags
```

### Equalizer
| Feature | Details |
|---------|---------|
| 5-Band EQ | 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz |
| 10-Band EQ (Advanced) | Full spectrum control |
| Presets | Flat, Rock, Pop, Jazz, Classical, Bass Boost, Vocal Boost, Custom |
| Bass Boost | Dedicated slider |
| Virtualizer | 3D sound effect slider |
| Loudness Enhancer | Volume normalization |
| Save Custom Preset | User-named presets |

### Background Service
```kotlin
class AudioPlaybackService : MediaSessionService() {
    // Media3 session service for background playback
    // Handles notification, lock screen, Bluetooth controls
    // Saves playback state on pause/stop for resume
}
```

---

# 8. PDF VIEWER & EDITOR — PROFESSIONAL GRADE

## 8.1 Current State
- Basic rendering (currently crashed)
- Has search and goto page (buggy)
- Zoom exists but unreliable
- No editing capabilities

## 8.2 Crash Fix
The PDF reader crash needs to be diagnosed. Likely causes:
1. Null pointer on empty PDF
2. Memory overflow on large PDFs
3. Incorrect page rendering lifecycle
4. Missing error handling in PDFBox renderer

### Fix Approach
- Wrap all PDF operations in try-catch with user-friendly error messages
- Use `PdfRenderer` (Android native) for viewing instead of/alongside PDFBox
- Implement page-by-page loading (lazy rendering) for large PDFs
- Add OutOfMemory protection with bitmap recycling

## 8.3 Complete Viewer Features

### Navigation
| Feature | Details |
|---------|---------|
| Scroll modes | Vertical continuous, Horizontal page-by-page |
| Goto page | Input field: type page number → jump |
| Page thumbnails | Bottom strip with mini page previews |
| Table of Contents | Parse PDF bookmarks/outlines → sidebar |
| Bookmarks | User bookmarks with labels |
| Reading progress | Auto-save last page, show in Continue Reading widget |

### Zoom
| Feature | Details |
|---------|---------|
| Pinch to zoom | 50% to 500% |
| Double-tap zoom | Toggle between fit-width and 200% |
| Zoom indicator | Show current zoom percentage |
| Fit to width | Default view |
| Fit to page | See full page |

### Search (All Kinds)
| Search Type | Details |
|-------------|---------|
| Full word search | Find exact word matches |
| Contained word search | Find partial matches (substring) |
| Case-sensitive | Toggle case sensitivity |
| Whole word only | Match complete words only |
| Regex search | Advanced pattern matching |
| Search results count | "3 of 15 matches" |
| Navigate results | Previous/Next match buttons |
| Highlight all | Show all matches on page highlighted |

### Editing (Tap-to-Edit)
| Feature | Details |
|---------|---------|
| Text annotation | Add text boxes anywhere on page |
| Highlight text | Yellow, green, blue, pink, red highlight |
| Underline text | Colored underline |
| Strikethrough | Cross out text |
| Freehand drawing | Draw with finger/stylus |
| Shapes | Rectangle, circle, line, arrow |
| Sticky notes | Add comment notes |
| Signature | Draw/load signature image |
| Stamp | "Approved", "Draft", "Confidential", "Rejected", custom |
| Eraser | Remove annotations |
| Undo/Redo | Full history |
| Save annotations | Flatten into PDF or save separately |

### Page Management
| Feature | Details |
|---------|---------|
| Reorder pages | Drag & drop in thumbnail view |
| Delete pages | Remove individual pages |
| Rotate pages | 90°, 180°, 270° rotation per page |
| Insert blank page | Add empty page at position |
| Duplicate page | Copy a page |
| Extract pages | Save selected pages as new PDF |

### Toolbar Design
```
┌──────────────────────────────────┐
│ ← Document.pdf    🔍 📑 ⚙️ ⋮    │ ← Top bar
│══════════════════════════════════│
│                                  │
│        [PDF Page Content]        │
│                                  │
│                                  │
│══════════════════════════════════│
│ ✏️ Highlight │ Text │ Draw │ 📎 │ ← Edit toolbar (toggle)
│ Page 12 of 156  🔍 "search..."  │ ← Bottom bar
│ [◀ Prev] [Page #] [Next ▶]      │
└──────────────────────────────────┘
```

---

# 9. VIDEO TOOLS — MASSIVE EXPANSION

## 9.1 Current State
- Video conversion, compression, trim, merge
- Extract audio, add watermark, rotate, crop
- Change resolution, frame extraction
- Limited to preset formats

## 9.2 Missing: Video Editor with Visual Timeline

### [NEW] `VideoEditorScreen.kt`
A full video editing screen where the file is shown and all edit tools are overlaid:

```
┌──────────────────────────────────┐
│ ← Edit Video              💾 ⚙️  │
│══════════════════════════════════│
│                                  │
│     [Video Preview Area]         │ ← Live preview of edits
│     Shows current frame          │
│     with applied effects         │
│                                  │
│══════════════════════════════════│
│ ═══|████████████████|════════    │ ← Timeline with trimming handles
│ 00:00  00:15  00:30  00:45 01:00│
│══════════════════════════════════│
│ [✂️Trim] [📐Crop] [🔄Rotate]     │ ← Tool tabs
│ [🎨Filter] [📝Text] [🎵Audio]    │
│ [⚡Speed] [💧Watermark] [📸Frame]│
└──────────────────────────────────┘
```

### Complete Tool List

| Tool | Features |
|------|----------|
| **Trim** | Start/end markers, preview, precision frame-by-frame |
| **Crop** | Free crop, 16:9, 4:3, 1:1, 9:16 presets |
| **Rotate** | 90°, 180°, 270°, flip horizontal, flip vertical |
| **Speed** | 0.25x to 4x, reverse playback |
| **Filters** | 20+ video filters (B&W, Sepia, Vintage, Warm, Cool, Vivid, etc.) |
| **Text Overlay** | Add text with font, size, color, position, duration |
| **Audio Replace** | Replace video audio with another audio file |
| **Audio Remove** | Strip audio from video |
| **Audio Adjust** | Volume level 0-200% |
| **Watermark** | Text or image watermark with position and opacity |
| **Frame Extract** | Save specific frames as images |
| **GIF Creator** | Convert video segment to GIF with quality/size control |
| **Resolution** | Upscale/downscale: 4K, 1080p, 720p, 480p, 360p, custom |
| **Bitrate** | Custom bitrate for quality/size control |
| **Merge** | Combine multiple videos with transitions |
| **Split** | Split video at specific time points |

### Custom Format Conversion
User can enter ANY value:
```
┌── Custom Format ──────────────────┐
│ Container: [MP4 ▼] (or type any) │
│ Video Codec: [H.264 ▼]           │
│ Audio Codec: [AAC ▼]             │
│ Resolution: [1920] × [1080]       │ ← User types exact values
│ Bitrate (kbps): [5000]            │ ← User types exact value
│ Frame Rate: [30]                  │ ← User types exact value
│ Audio Bitrate: [192]              │
│ Audio Sample Rate: [44100]        │
│                                   │
│ Output: exactly what user entered │
│ NO .1% extra or lower!           │
└───────────────────────────────────┘
```

### FFmpeg Commands Used
```bash
# Trim video
ffmpeg -i input.mp4 -ss 00:00:30 -to 00:01:15 -c copy output.mp4

# Rotate 90° clockwise
ffmpeg -i input.mp4 -vf "transpose=1" output.mp4

# Add text overlay
ffmpeg -i input.mp4 -vf "drawtext=text='Hello':fontsize=48:fontcolor=white:x=10:y=10" output.mp4

# Custom format conversion with exact user values
ffmpeg -i input.mp4 -c:v libx264 -b:v 5000k -r 30 -s 1920x1080 
       -c:a aac -b:a 192k -ar 44100 output.mp4

# Speed change
ffmpeg -i input.mp4 -vf "setpts=0.5*PTS" -af "atempo=2.0" output.mp4

# Reverse
ffmpeg -i input.mp4 -vf reverse -af areverse output.mp4

# Apply filter (sepia)
ffmpeg -i input.mp4 -vf "colorchannelmixer=.393:.769:.189:0:.349:.686:.168:0:.272:.534:.131" output.mp4
```

---

# 10. AUDIO TOOLS — COMPLETE SUITE

## 10.1 Current State
- Audio conversion, compression, trim, merge
- Normalize, fade, reverse, speed/pitch change
- Noise reduction, extract vocals
- Basic implementation

## 10.2 Massive Expansion

### [NEW] `AudioEditorScreen.kt`
Full audio editing with waveform visualization:

```
┌──────────────────────────────────┐
│ ← Edit Audio              💾 ⚙️  │
│══════════════════════════════════│
│                                  │
│   [Waveform Visualization]      │ ← Zoomable waveform display
│   ┌┐ ┌┐┌┐  ┌┐┌┐ ┌┐┌┐┌┐        │    with selection handles
│   ██ ████  ████ ██████         │
│   ████████████████████████      │
│  |←──selected region──→|        │
│                                  │
│══════════════════════════════════│
│ 00:00:15 / 00:03:45              │
│ [▶ Play] [⏸ Pause] [⏹ Stop]     │
│══════════════════════════════════│
│ [✂️Cut] [📋Copy] [📎Paste]       │ ← Basic operations
│ [🔇Silence] [🎚️Vol] [📐Fade]    │
│ [🔄Reverse] [⚡Speed] [🎵Pitch]  │ ← Transform
│ [🔊Noise] [🎛️EQ] [🔈Compress]  │ ← Effects
│ [🎤Echo] [📡Reverb] [🔀Mix]     │ ← Advanced
└──────────────────────────────────┘
```

### Complete Tool List

| Tool | Features |
|------|----------|
| **Trim/Cut** | Select region on waveform, cut with crossfade |
| **Split** | Split audio at precise points |
| **Merge** | Combine multiple audio files with optional crossfade |
| **Volume Adjust** | 0-300% volume with normalization |
| **Fade In/Out** | Linear, logarithmic, exponential curves |
| **Reverse** | Play backwards |
| **Speed Change** | 0.25x to 4x without pitch change (time stretch) |
| **Pitch Change** | Shift pitch up/down without speed change |
| **Noise Reduction** | Remove background noise (FFmpeg anlmdn filter) |
| **Equalizer** | 10-band parametric EQ |
| **Compressor** | Dynamic range compression |
| **Echo/Delay** | Configurable delay and decay |
| **Reverb** | Room simulation (small room, hall, cathedral) |
| **Normalize** | Peak/RMS normalization |
| **Convert Format** | Any audio format to any other |
| **Extract Vocals** | Separate vocals from instruments |
| **Mix/Overlay** | Mix two audio files together |
| **Insert Silence** | Add silence at specific points |
| **Ringtone Maker** | Trim + fade + set as ringtone |
| **Voice Recorder** | Record audio with visualization |
| **Concatenate** | Join files end-to-end |

### Audio Cutter Improvements
- **Waveform display** with zoom (pinch) and scroll
- **Snap to zero-crossing** for click-free cuts
- **Selection handles** for start/end
- **Preview selection** before cutting
- **Undo/Redo** stack
- **Multiple markers** for batch splitting

---

# 11. NOTES — DUAL MODE SYSTEM

## 11.1 Current State
- Basic note-taking with folders and tags
- No reminders
- No checklist items working properly
- No "Second Brain" concept

## 11.2 Mode 1: Simple Notes

### Features
| Feature | Details |
|---------|---------|
| Quick Note | Tap + to create, start typing immediately |
| Folders | Organize notes into folders |
| Tags | Color-coded tags for cross-folder organization |
| Pin | Pin important notes to top |
| Archive | Archive old notes (not delete) |
| Search | Full-text search across all notes |
| Sort | By date created, modified, title, color |
| Color Labels | Red, Orange, Yellow, Green, Blue, Purple, None |
| Checklist Mode | Toggle between text and checklist |
| Reminder | Set date/time reminder for any note (notification) |
| Due Date | Task-style due date with overdue indicator |
| Word Count | Auto-calculated, shown in footer |
| Character Count | Shown alongside word count |
| Share Note | Share as text to other apps |
| Duplicate Note | Clone a note |
| Trash | Soft delete with 30-day recovery |
| Bulk Actions | Select multiple → archive, delete, move, tag |
| Attachments | Attach images and files to notes |
| Text Formatting | Bold, italic, underline, bullet list, numbered list, headers |

### Note Card Design
```
┌──────────────────────────────────┐
│ 📌 Shopping List        🔴 Red  │
│ • Milk ✓                         │
│ • Eggs                           │
│ • Bread ✓                        │
│ 📅 May 29 • 12 words • #grocery │
└──────────────────────────────────┘
```

## 11.3 Mode 2: Second Brain

### Concept
A Zettelkasten/PARA-inspired knowledge management system for power users who want to organize ALL their knowledge.

### Features

#### Note Types
| Type | Icon | Purpose |
|------|------|---------|
| Note | 📝 | General knowledge capture |
| Idea | 💡 | Creative ideas and brainstorms |
| Project | 🎯 | Project plans and progress |
| Reference | 📚 | Reference material, bookmarks |
| Journal | 📓 | Daily journal entries |

#### Core Features
| Feature | Details |
|---------|---------|
| **Markdown Editor** | Full markdown support with live preview |
| **Bi-directional Links** | `[[Note Title]]` to link notes, backlinks shown |
| **Tags** | Hierarchical tags (e.g., `#programming/kotlin/coroutines`) |
| **Graph View** | Visual map of all notes and their connections |
| **Inbox** | Quick capture → process later (GTD methodology) |
| **Daily Note** | Auto-created daily page for journaling |
| **Templates** | Predefined templates for meetings, projects, reviews |
| **Search** | Full-text search with filters by type, tag, date range |
| **Hierarchy** | Parent-child note relationships (tree structure) |
| **Attachments** | Images, PDFs, files attached to notes |
| **Web Clipper** | Paste URL → auto-extract title and save as reference |
| **Export** | Export to Markdown files, ZIP archive |
| **Import** | Import from Markdown files |
| **Priority System** | 0-5 priority levels with visual indicators |
| **Status Tracking** | Active, Archived, Inbox for workflow management |
| **Word Count** | Auto-calculated |
| **Last Modified** | Timestamp tracking |
| **Favorites** | Quick access to important notes |
| **Recent Notes** | Last accessed notes |
| **Orphan Finder** | Find notes with no links or tags |

#### Views
| View | Description |
|------|-------------|
| List View | Traditional list with preview |
| Card View | Pinterest-style cards |
| Graph View | Visual network of linked notes |
| Calendar View | Notes organized by date |
| Kanban View | Notes as cards in status columns |
| Tag Cloud | Visual tag hierarchy |

### Database Structure
Uses `SecondBrainNoteEntity` and `SecondBrainTagEntity` defined in Section 2.2.

---

# 12. CALCULATOR — SCIENTIFIC POWERHOUSE

## 12.1 Current State
- Basic arithmetic (add, subtract, multiply, divide)
- No scientific functions
- No history

## 12.2 Dual Mode Design

### Mode 1: Basic Calculator
```
┌──────────────────────────────────┐
│ ← Calculator                    │
│                                  │
│                    123,456.789   │ ← Display (right-aligned, large)
│                    45 + 678 × 2  │ ← Expression (smaller, above)
│══════════════════════════════════│
│  [C]  [( )]  [%]    [÷]        │
│  [7]  [8]    [9]    [×]        │
│  [4]  [5]    [6]    [−]        │
│  [1]  [2]    [3]    [+]        │
│  [±]  [0]    [.]    [=]        │
│══════════════════════════════════│
│  🔬 Switch to Scientific         │
│  📋 History (15 entries)          │
└──────────────────────────────────┘
```

### Mode 2: Scientific Calculator
```
┌──────────────────────────────────┐
│ ← Scientific Calculator         │
│                                  │
│           sin(45°) + 2³ = 8.707  │ ← Expression display
│                         8.707    │ ← Result display
│══════════════════════════════════│
│  [2nd] [DEG] [RAD] [π]  [e]    │ ← Mode row
│  [sin] [cos] [tan] [ln] [log]  │ ← Trig/Log
│  [sin⁻¹][cos⁻¹][tan⁻¹][eˣ][10ˣ]│ ← Inverse (2nd mode)
│  [x²]  [x³]  [xⁿ]  [√x] [∛x] │ ← Powers/Roots
│  [|x|] [n!]  [1/x] [mod] [( )] │ ← Advanced
│══════════════════════════════════│
│  [C]  [⌫]    [%]    [÷]        │
│  [7]  [8]    [9]    [×]        │
│  [4]  [5]    [6]    [−]        │
│  [1]  [2]    [3]    [+]        │
│  [±]  [0]    [.]    [=]        │
│══════════════════════════════════│
│  🧮 Switch to Basic              │
│  📋 History (23 entries)          │
└──────────────────────────────────┘
```

### Scientific Functions (using exp4j)
| Category | Functions |
|----------|-----------|
| Trigonometric | sin, cos, tan, asin, acos, atan, sinh, cosh, tanh |
| Logarithmic | ln (natural log), log (base 10), log₂ |
| Powers | x², x³, xⁿ, eˣ, 10ˣ, 2ˣ |
| Roots | √x, ∛x, ⁿ√x |
| Other | |x| (absolute), n! (factorial), 1/x (reciprocal), mod (modulus) |
| Constants | π (pi), e (Euler's number), φ (golden ratio) |
| Modes | DEG (degrees), RAD (radians), GRAD (gradians) |
| Memory | M+, M-, MR, MC |

### History
- Saved to `CalculatorHistoryEntity` in Room database
- Shows expression AND result
- Tap to recall result
- Long-press to copy
- Clear all option
- Separate history for Basic and Scientific modes

### Expression Parser
Use `exp4j` library for safe, accurate expression evaluation:
```kotlin
val result = ExpressionBuilder("sin(45) + 2^3")
    .build()
    .evaluate()
```

---

# 13. UNIT CONVERTER — LIVE CURRENCY & MANUAL OVERRIDE

## 13.1 Current State
- Supports length, weight, temperature, volume, speed, area, time, data, currency
- Currency rates are hardcoded/outdated
- No manual override option

## 13.2 Complete Unit Categories

| Category | Units |
|----------|-------|
| **Length** | mm, cm, m, km, inch, foot, yard, mile, nautical mile, μm, nm |
| **Weight** | mg, g, kg, tonne, oz, lb, stone, short ton, long ton |
| **Temperature** | Celsius, Fahrenheit, Kelvin, Rankine |
| **Volume** | mL, L, gallon (US), gallon (UK), quart, pint, cup, fl oz, tablespoon, teaspoon, m³, cm³ |
| **Speed** | m/s, km/h, mph, knots, ft/s, Mach |
| **Area** | mm², cm², m², km², hectare, acre, sq ft, sq yd, sq mi, sq in |
| **Time** | ms, s, min, hr, day, week, month, year, decade, century |
| **Data** | bit, byte, KB, MB, GB, TB, PB, Kbit, Mbit, Gbit |
| **Pressure** | Pa, kPa, MPa, bar, atm, psi, mmHg, inHg |
| **Energy** | J, kJ, cal, kcal, Wh, kWh, BTU, eV |
| **Power** | W, kW, MW, HP, BTU/h, cal/s |
| **Frequency** | Hz, kHz, MHz, GHz, rpm |
| **Fuel Economy** | km/L, L/100km, mpg (US), mpg (UK) |
| **Angle** | degree, radian, gradian, minute of arc, second of arc |
| **Currency** | 150+ world currencies with LIVE rates |

## 13.3 Currency Converter — Live + Manual

### Live Exchange Rates
```kotlin
// Retrofit API interface
interface ExchangeRateApi {
    @GET("v6/{apiKey}/latest/USD")
    suspend fun getLatestRates(
        @Path("apiKey") apiKey: String
    ): ExchangeRateResponse
}

// Use free API: exchangerate-api.com (1500 requests/month free)
// Alternative: open.er-api.com (free, no key needed)
// Alternative: frankfurter.app (free, open source)
```

### Manual Override Feature
```
┌── Currency Settings ─────────────┐
│                                   │
│ 💱 Exchange Rate Source:          │
│ ○ Live API (last updated: 5m ago)│
│ ○ Manual Entry                    │
│                                   │
│ ── Manual Rate Entry ──          │
│ Base Currency: [USD ▼]            │
│                                   │
│ 1 USD = [   83.45  ] INR         │
│ 1 USD = [    0.92  ] EUR         │
│ 1 USD = [    0.79  ] GBP         │
│ 1 USD = [  149.50  ] JPY         │
│ ... (all 150+ currencies)        │
│                                   │
│ [Save Manual Rates]               │
│ [Reset to Live Rates]             │
└───────────────────────────────────┘
```

### Database Storage
- Live rates cached in `CurrencyRateEntity`
- Manual overrides stored with `isManualOverride = true`
- Auto-refresh live rates every 4 hours
- Show "Last updated: X minutes ago" indicator
- Offline mode: use last cached rates

### Conversion Precision
- All conversions use `BigDecimal` for precision
- Show up to 10 decimal places for small values
- Show appropriate decimal places for large values
- **NO .1% extra or lower** — exact mathematical conversion

---

# 14. COMPASS — PRECISION INSTRUMENT

## 14.1 Current State
- Basic compass with heading and cardinal direction
- Shows coordinates
- No calibration guide
- No magnetic declination

## 14.2 Complete Feature List

### Core Compass
| Feature | Details |
|---------|---------|
| Heading | Degrees (0-360) with smooth animation |
| Cardinal Direction | N, NE, E, SE, S, SW, W, NW |
| True North | Adjust for magnetic declination |
| Magnetic North | Raw sensor reading |
| Accuracy Indicator | Show sensor accuracy (high/medium/low/unreliable) |
| Magnetic Field Strength | Show in μT (microtesla) |

### Calibration
| Feature | Details |
|---------|---------|
| Calibration Guide | Visual figure-8 animation showing how to calibrate |
| Auto-detect | Alert when calibration is needed (low accuracy) |
| Sensor Status | Show accelerometer and magnetometer health |
| Interference Warning | Alert when magnetic interference detected |

### Advanced Features
| Feature | Details |
|---------|---------|
| Magnetic Declination | Auto-calculate from GPS coordinates |
| Bearing to Location | Enter coordinates → show direction and distance |
| Location Display | Current lat/long in DD and DMS formats |
| Altitude | GPS altitude (if available) |
| Speed | Current speed (if moving) |
| Level/Inclinometer | Phone tilt angle (pitch and roll) |
| Sunrise/Sunset | Times based on location |
| Moon Phase | Current lunar phase |
| Map Integration | Show heading on mini-map |
| Lock Bearing | Lock to a specific heading |
| Waypoint Save | Save current position as waypoint |
| Night Mode | Red-tinted display for night use |

### UI Design
```
┌──────────────────────────────────┐
│ ← Compass              🎯 ⚙️    │
│                                  │
│        N                         │
│      ╱   ╲                       │
│    NW  ●  NE                     │
│    │ ╱ ▲ ╲ │    ← Compass Rose   │
│    W ─ + ─ E       with needle  │
│    │ ╲   ╱ │                     │
│    SW     SE                     │
│      ╲   ╱                       │
│        S                         │
│                                  │
│    Heading: 247° (WSW)           │
│    Magnetic: 252° | True: 247°   │
│    Declination: -5.2°            │
│                                  │
│  ─── Details ──────────────────  │
│  📍 12.9716° N, 77.5946° E      │
│  🏔 920m altitude                │
│  🧲 48.3 μT field strength      │
│  🎯 Accuracy: HIGH              │
│  🌅 Sunrise: 5:52 AM             │
│  🌇 Sunset: 6:45 PM             │
│  🌙 Moon: Waxing Gibbous (82%)  │
│                                  │
│  [📐 Level] [🎯 Bearing] [📍 Save]│
└──────────────────────────────────┘
```

### Implementation
```kotlin
// Magnetic declination calculation
val geoField = GeomagneticField(
    latitude.toFloat(),
    longitude.toFloat(),
    altitude.toFloat(),
    System.currentTimeMillis()
)
val declination = geoField.declination // degrees
val trueNorth = magneticHeading + declination
```

---

# 15. FLASHLIGHT — INTENSITY CONTROL

## 15.1 Current State
- Flashlight toggle
- SOS mode
- Frequency/strobe mode
- No intensity control

## 15.2 Intensity Control Implementation

### Android 13+ (API 33+) — Native Intensity
```kotlin
val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
val cameraId = cameraManager.cameraIdList[0]
val characteristics = cameraManager.getCameraCharacteristics(cameraId)

// Check if intensity control is supported
val maxLevel = characteristics.get(
    CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL
) ?: 1

if (maxLevel > 1) {
    // Device supports intensity control
    // Range: 1 to maxLevel
    cameraManager.turnOnTorchWithStrengthLevel(cameraId, desiredLevel)
}
```

### Below Android 13 — Simulated Intensity via PWM
```kotlin
// For devices without native intensity support
// Use rapid on/off cycling to simulate dimming
fun simulateIntensity(level: Float) { // 0.0 to 1.0
    val onTime = (level * 100).toLong() // ms
    val offTime = ((1 - level) * 100).toLong() // ms
    
    flashlightScope.launch {
        while (isActive) {
            cameraManager.setTorchMode(cameraId, true)
            delay(onTime)
            cameraManager.setTorchMode(cameraId, false)
            delay(offTime)
        }
    }
}
```

### UI Design
```
┌──────────────────────────────────┐
│ ← Flashlight                    │
│                                  │
│         ┌───────────┐            │
│         │   💡      │            │ ← Large power button
│         │  ON/OFF   │            │    (tap to toggle)
│         └───────────┘            │
│                                  │
│  ─── Brightness ─────────────── │
│  🔅 ═══════════●═══════════ 🔆  │ ← Swipe slider for intensity
│       Low        Medium    Max   │
│       Level: 7 / 10              │
│                                  │
│  ─── Modes ──────────────────── │
│  [💡 Steady]  [🆘 SOS]          │
│  [⚡ Strobe]  [🎵 Music Sync]   │
│                                  │
│  ─── Strobe Frequency ───────── │
│  🐌 ═══════●═══════════ ⚡      │
│     1 Hz         10 Hz    20 Hz  │
│                                  │
│  ─── Screen Light ───────────── │
│  [📱 White Screen] [🟡 Warm]    │ ← Use screen as ambient light
│  [🔴 Red Night]   [🟢 Green]    │
└──────────────────────────────────┘
```

### New Features
| Feature | Details |
|---------|---------|
| Intensity Slider | Swipe to control brightness (1 to max level) |
| SOS Mode | Automatic SOS pattern (···−−−···) |
| Strobe | Adjustable frequency 1-20 Hz |
| Music Sync | Flash synced to microphone input (beat detection) |
| Screen Light | Use phone screen as ambient light (white, warm, red, green, blue) |
| Timer | Auto-off after X minutes |
| Widget Support | Quick toggle from home screen widget |

---

# 16. DEVELOPER TOOLS — PROFESSIONAL SUITE

## 16.1 Current State
- JSON formatter, Base64, URL encoder
- Hash generator, Color picker, Regex tester
- Lorem Ipsum, QR generator, QR scanner (no camera)
- Timestamp converter

## 16.2 QR Scanner — Camera Mode

### Current Issue
QR scanner only works with image files — no live camera scanning.

### Fix: Add Camera-Based QR Scanner
```kotlin
@Composable
fun QrCameraScannerScreen() {
    val context = LocalContext.current
    
    // Request camera permission
    val cameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> /* handle */ }
    
    // CameraX Preview + ML Kit Analysis
    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    
                    val preview = Preview.Builder().build()
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .apply {
                            setAnalyzer(executor) { imageProxy ->
                                // ML Kit barcode scanning
                                val scanner = BarcodeScanning.getClient(
                                    BarcodeScannerOptions.Builder()
                                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                                        .build()
                                )
                                val image = InputImage.fromMediaImage(
                                    imageProxy.image!!,
                                    imageProxy.imageInfo.rotationDegrees
                                )
                                scanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        barcodes.firstOrNull()?.rawValue?.let { value ->
                                            // Handle scanned QR code
                                            onQrScanned(value)
                                        }
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            }
                        }
                    
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA,
                        preview, imageAnalysis
                    )
                    preview.setSurfaceProvider(surfaceProvider)
                }, ContextCompat.getMainExecutor(ctx))
            }
        }
    )
    
    // Overlay with scan area indicator
    Box(modifier = Modifier.fillMaxSize()) {
        // Transparent dark overlay with clear center rectangle
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw scan area frame
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(centerX - 150.dp.toPx(), centerY - 150.dp.toPx()),
                size = Size(300.dp.toPx(), 300.dp.toPx()),
                cornerRadius = CornerRadius(16.dp.toPx()),
                style = Stroke(width = 3.dp.toPx())
            )
        }
        
        // Scanning line animation
        ScanningLineAnimation()
    }
}
```

## 16.3 Expanded Tool List

### Existing Tools (Improved)
| Tool | Improvements |
|------|-------------|
| JSON Formatter | Add JSON validation, minify, convert to YAML/XML |
| Base64 | Add file-to-base64, image preview of base64 data |
| URL Encoder | Add URL parser (show protocol, host, path, params) |
| Hash Generator | Add file hash, SHA-512, CRC32, compare hashes |
| Color Picker | Add color from camera, palette generator, Material colors |
| Regex Tester | Add common regex library, explain regex, test against file |
| QR Generator | Add custom colors, logo overlay, export as PNG/SVG |
| QR Scanner | **ADD CAMERA MODE**, scan history, auto-detect URLs/phones/email |
| Timestamp Converter | Add ISO 8601, Unix epoch, relative time ("3 days ago") |

### New Tools
| Tool | Features |
|------|----------|
| **UUID Generator** | v1, v4, v5 UUID generation, bulk generate |
| **JWT Decoder** | Paste JWT → decode header, payload, verify |
| **Diff Checker** | Compare two texts, show differences |
| **Cron Expression** | Visual cron builder + next 5 run times |
| **Markdown Preview** | Live markdown editor with preview |
| **HTML Preview** | Live HTML/CSS/JS editor with WebView preview |
| **Network Info** | IP address, WiFi info, DNS lookup, ping |
| **Device Info** | Full device specs (CPU, RAM, display, sensors, battery) |
| **Text Counter** | Characters, words, sentences, paragraphs, reading time |
| **Number Base Converter** | Binary, Octal, Decimal, Hex converter |
| **ASCII Table** | Full ASCII reference with search |
| **HTTP Status Codes** | Reference list with descriptions |
| **Fake Data Generator** | Names, emails, addresses, phone numbers, lorem ipsum |

---

# 17. SECURE VAULT — ENTERPRISE SECURITY

## 17.1 Current State
- Biometric authentication
- File storage with categories (aadhaar, pan, passport, etc.)
- Basic encryption
- Encrypted file names on disk

## 17.2 Improvements

### Authentication Upgrades
| Feature | Details |
|---------|---------|
| Biometric (Fingerprint) | Using `BiometricPrompt` with `BIOMETRIC_STRONG` |
| PIN Code | 4-8 digit secondary auth |
| Pattern Lock | Custom pattern as alternative |
| Auto-Lock | Lock after X minutes of inactivity (1, 5, 15, 30 min) |
| Failed Attempt Limit | Lock for 30 seconds after 5 failed attempts |
| Panic Button | Quick triple-tap to lock immediately |
| Decoy Mode | Wrong PIN shows fake empty vault |

### File Management
| Feature | Details |
|---------|---------|
| File Preview | Thumbnails for images, first page for PDFs |
| Folder Organization | Create sub-folders within vault |
| Quick Add | Add files from file manager with one tap |
| Camera Capture | Take photo directly into vault |
| Bulk Import | Select multiple files to import |
| Bulk Export | Export selected files (with re-authentication) |
| File Details | Size, type, added date, last accessed |
| Sharing History | Track when and where files were shared |
| Document Expiry | Set expiry dates for IDs (passport, license) with notifications |
| Notes | Add text notes to each file |
| Tags | Custom tags for easy searching |

### New Categories
Expand beyond current 9 categories:
```
Identity: Aadhaar, PAN, Passport, Driving License, Voter ID
Financial: Bank Statements, Tax Returns, Insurance, Investments
Medical: Reports, Prescriptions, Insurance, Vaccination Records
Education: Certificates, Transcripts, ID Cards
Legal: Contracts, Agreements, Property Documents
Personal: Photos, Videos, Private Notes
Work: Employment Letters, Pay Slips, Offer Letters
Travel: Tickets, Visas, Hotel Bookings, Itineraries
Passwords: Secure text notes for passwords/keys
Other: Miscellaneous
```

### Encryption Improvements
| Feature | Details |
|---------|---------|
| AES-256-GCM | Upgrade from basic encryption |
| Hardware-backed keys | Use Android Keystore TEE |
| File-level encryption | Each file encrypted independently |
| Encrypted thumbnails | Thumbnails also encrypted |
| Secure delete | Overwrite deleted files with random data |
| Encrypted database | Vault metadata encrypted in Room |

### Smart Offline Saver Integration
- Option to save offline content directly to vault (encrypted)
- Vault files accessible in Smart Offline Saver's secure section

---

# 18. DOCUMENT HUB — KNOWLEDGE CENTER

## 18.1 Current State
- Basic document management
- Recent files, favorites, tags, search, preview
- Described as "very worst" by user

## 18.2 Complete Redesign

### Document Dashboard
```
┌──────────────────────────────────┐
│ ← Document Hub            🔍 ⚙️ │
│══════════════════════════════════│
│ 📊 Overview                      │
│ Total: 156 docs | 2.3 GB used   │
│ [PDF:78] [DOCX:34] [TXT:22] [Other:22] │
│                                  │
│ ── Recent Documents ─────────── │
│ ┌────┐ ┌────┐ ┌────┐ ┌────┐   │
│ │ 📄 │ │ 📄 │ │ 📄 │ │ 📄 │   │ ← Horizontal scroll
│ │Doc1│ │Doc2│ │Doc3│ │Doc4│   │
│ └────┘ └────┘ └────┘ └────┘   │
│                                  │
│ ── Favorites ⭐ ─────────────── │
│ • Annual Report 2025.pdf         │
│ • Resume.docx                    │
│ • Tax Filing.pdf                 │
│                                  │
│ ── By Category ─────────────── │
│ [📋 Work (23)] [📚 Study (15)] │
│ [💰 Finance (8)] [🏠 Personal (12)]│
│ [📦 Other (98)]                  │
│                                  │
│ ── Smart Collections ──────── │
│ [🕐 This Week (5)]              │
│ [📏 Large Files >10MB (3)]      │
│ [📤 Shared Recently (7)]        │
│ [⚠️ Expiring Soon (2)]          │
│                                  │
│ ── All Documents ─────────────  │
│ Sort: [Date ▼] Filter: [All ▼] │
│ ... (searchable, sortable list)  │
└──────────────────────────────────┘
```

### Features
| Feature | Details |
|---------|---------|
| **Smart Scanning** | Scan device for all documents (PDF, DOCX, XLSX, PPTX, TXT, EPUB) |
| **Auto-categorize** | Suggest categories based on file name and content |
| **Full-text search** | Search within PDF/TXT content, not just filenames |
| **Tags & Labels** | Multiple tags per document, color-coded |
| **Favorites** | Star important documents for quick access |
| **Collections** | Group related documents (e.g., "Tax 2025", "Project X") |
| **Preview** | Quick preview without opening full viewer |
| **Document Info** | File size, pages, word count, created/modified dates |
| **Share** | Share via any app, generate shareable link via FTP |
| **Duplicate Detection** | Find and manage duplicate documents |
| **Storage Analytics** | Breakdown by type, size, age |
| **Expiry Tracking** | Set expiry alerts for important documents |
| **Annotations Summary** | List all annotated PDFs with annotation count |
| **Print** | Direct print via Android print framework |
| **Convert** | Convert between formats (PDF ↔ DOCX, Images → PDF, etc.) |
| **Merge** | Combine multiple documents into one PDF |
| **Batch Operations** | Select multiple → compress, share, move, delete |

---

# 19. FTP SERVER — COMPLETE FIX & OVERHAUL

## 19.1 Current Problems
1. FTP server not starting properly
2. Web portal not accessible
3. Binding to wrong network interface
4. Home screen of FTP shows wrong file system

## 19.2 Root Cause Analysis & Fix

### Problem 1: Network Binding
```kotlin
// ISSUE: Server may bind to loopback (127.0.0.1) instead of WiFi IP
// FIX: Explicitly get WiFi IP address

fun getWifiIpAddress(context: Context): String {
    val wifiManager = context.applicationContext
        .getSystemService(Context.WIFI_SERVICE) as WifiManager
    val connectionInfo = wifiManager.connectionInfo
    val ipInt = connectionInfo.ipAddress
    
    // Convert integer IP to string
    return String.format(
        "%d.%d.%d.%d",
        ipInt and 0xff,
        (ipInt shr 8) and 0xff,
        (ipInt shr 16) and 0xff,
        (ipInt shr 24) and 0xff
    )
}

// Alternative for Android 12+
fun getWifiIpAddressModern(context: Context): String {
    val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    val network = connectivityManager.activeNetwork
    val linkProperties = connectivityManager.getLinkProperties(network)
    return linkProperties?.linkAddresses
        ?.firstOrNull { it.address is Inet4Address }
        ?.address?.hostAddress ?: "0.0.0.0"
}
```

### Problem 2: FTP Server Implementation
```kotlin
// Use Apache FtpServer properly
class AneganFtpServer(private val context: Context) {
    private var ftpServer: FtpServer? = null
    
    fun start(port: Int = 2121, username: String, password: String) {
        val userManagerFactory = PropertiesUserManagerFactory()
        val userManager = userManagerFactory.createUserManager()
        
        // Configure user with root directory
        val user = BaseUser().apply {
            name = username
            this.password = password
            homeDirectory = Environment.getExternalStorageDirectory().absolutePath
            authorities = listOf(
                WritePermission(),
                ConcurrentLoginPermission(10, 10)
            )
        }
        userManager.save(user)
        
        val factory = FtpServerFactory().apply {
            this.userManager = userManager
            addListener("default", ListenerFactory().apply {
                this.port = port
                // Bind to WiFi IP explicitly
                serverAddress = getWifiIpAddress(context)
            }.createListener())
        }
        
        ftpServer = factory.createServer()
        ftpServer?.start()
    }
    
    fun stop() {
        ftpServer?.stop()
    }
}
```

### Problem 3: Web Portal Fix
```kotlin
// NanoHTTPD web portal — complete rewrite
class AneganWebServer(
    private val context: Context,
    port: Int = 8080
) : NanoHTTPD(getWifiIpAddress(context), port) {
    
    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        
        return when {
            uri == "/" || uri == "/index.html" -> {
                // Serve file browser HTML
                newFixedLengthResponse(
                    Response.Status.OK,
                    "text/html",
                    generateFileBrowserHtml("/")
                )
            }
            uri.startsWith("/browse/") -> {
                // Browse directory
                val path = uri.removePrefix("/browse/")
                newFixedLengthResponse(
                    Response.Status.OK,
                    "text/html",
                    generateFileBrowserHtml(path)
                )
            }
            uri.startsWith("/download/") -> {
                // Download file
                val filePath = URLDecoder.decode(uri.removePrefix("/download/"), "UTF-8")
                val file = File(Environment.getExternalStorageDirectory(), filePath)
                if (file.exists() && file.isFile) {
                    val fis = FileInputStream(file)
                    val mimeType = getMimeType(file.name)
                    newChunkedResponse(Response.Status.OK, mimeType, fis)
                } else {
                    newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "File not found")
                }
            }
            uri.startsWith("/upload") && session.method == Method.POST -> {
                // Handle file upload
                handleFileUpload(session)
            }
            uri == "/api/files" -> {
                // JSON API for file listing
                val path = session.parms["path"] ?: "/"
                newFixedLengthResponse(
                    Response.Status.OK,
                    "application/json",
                    getFilesJson(path)
                )
            }
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not found")
        }
    }
    
    private fun generateFileBrowserHtml(relativePath: String): String {
        // Generate responsive, modern HTML file browser
        // With: file listing, upload button, download links
        // Breadcrumb navigation, file type icons
        // Mobile-friendly responsive design
        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Anegan File Server</title>
            <style>
                /* Modern, responsive CSS */
                * { margin: 0; padding: 0; box-sizing: border-box; }
                body { font-family: 'Segoe UI', system-ui, sans-serif; background: #f5f5f5; }
                .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); 
                          color: white; padding: 20px; }
                .file-list { max-width: 800px; margin: 20px auto; }
                .file-item { display: flex; align-items: center; padding: 12px 16px;
                            background: white; margin: 4px 0; border-radius: 8px;
                            box-shadow: 0 1px 3px rgba(0,0,0,0.1); cursor: pointer; }
                .file-item:hover { background: #f0f0ff; }
                .upload-zone { border: 2px dashed #667eea; border-radius: 12px;
                              padding: 40px; text-align: center; margin: 20px auto;
                              max-width: 800px; }
                /* ... more styles ... */
            </style>
        </head>
        <body>
            <div class="header">
                <h1>📁 Anegan File Server</h1>
                <p>Connected • ${getDeviceName()}</p>
            </div>
            <!-- Breadcrumbs, file list, upload zone -->
        </body>
        </html>
        """.trimIndent()
    }
}
```

### FTP Screen UI Redesign
```
┌──────────────────────────────────┐
│ ← File Transfer Server     ⚙️   │
│══════════════════════════════════│
│                                  │
│  ┌── Server Status ───────────┐ │
│  │ ● Active  (Tap to stop)    │ │
│  │                            │ │
│  │ 📡 FTP Server              │ │
│  │ ftp://192.168.1.5:2121     │ │ ← Tap to copy
│  │ User: anegan | Pass: ****  │ │
│  │                            │ │
│  │ 🌐 Web Portal              │ │
│  │ http://192.168.1.5:8080    │ │ ← Tap to copy
│  │ Open in any browser!       │ │
│  │                            │ │
│  │ 📊 Transfers: 3 active     │ │
│  └────────────────────────────┘ │
│                                  │
│  ─── Settings ─────────────────  │
│  FTP Port:    [2121]             │
│  HTTP Port:   [8080]             │
│  Username:    [anegan]           │
│  Password:    [●●●●] 👁️         │
│  Root Folder: [/storage/emul..] │
│  Anonymous:   [OFF]              │
│                                  │
│  ─── Connected Clients ────────  │
│  • 192.168.1.10 (Windows)       │
│  • 192.168.1.12 (iPhone)        │
│                                  │
│  ─── Transfer History ─────────  │
│  ↓ photo.jpg (2.3 MB) ✓         │
│  ↑ document.pdf (1.1 MB) ✓      │
│  ↓ video.mp4 (45%) ⏳            │
└──────────────────────────────────┘
```

### Run as Foreground Service
```kotlin
class FtpTransferService : Service() {
    private var ftpServer: AneganFtpServer? = null
    private var webServer: AneganWebServer? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        // Start both servers
        ftpServer = AneganFtpServer(this).apply { start(...) }
        webServer = AneganWebServer(this).apply { start() }
        
        return START_STICKY
    }
    
    override fun onDestroy() {
        ftpServer?.stop()
        webServer?.stop()
        super.onDestroy()
    }
}
```

---

# 20. SETTINGS — CRASH FIX & PREMIUM DESIGN

## 20.1 Current Problem
Settings screen is completely crashed and not working.

## 20.2 Crash Fix Approach
1. Read the current SettingsScreen.kt source code
2. Identify the crash cause (likely missing context, null prefs, or Compose lifecycle issue)
3. Rewrite with proper error handling
4. Test each section independently

## 20.3 Complete Settings Structure

```
┌──────────────────────────────────┐
│ ← Settings                      │
│══════════════════════════════════│
│                                  │
│ ── Appearance ─────────────────  │
│ 🎨 Theme            [Light ▼]   │ ← Always light (forced)
│ 🔤 Font             [Outfit ▼]  │
│ 🎯 Dynamic Color    [ON/OFF]    │
│ 📐 Font Size        [Medium ▼]  │
│                                  │
│ ── Storage ────────────────────  │
│ 📁 Anegan Folder    [/Anegan]   │
│ 💾 Cache Size       12.3 MB     │
│ 🗑️ Clear Cache      [Clear]     │
│ 📊 Storage Usage    [View]      │
│                                  │
│ ── File Manager ───────────────  │
│ 📂 Default View     [List ▼]    │
│ 🔢 Sort By          [Name ▼]    │
│ 👁️ Show Hidden      [OFF]       │
│ 📄 File Previews    [ON]        │
│                                  │
│ ── Media ──────────────────────  │
│ 🎬 Video Player Resume  [ON]    │
│ 🎵 Audio Player Crossfade [3s]  │
│ 🔊 Equalizer Preset [Flat ▼]   │
│                                  │
│ ── Security ───────────────────  │
│ 🔒 Vault Auto-Lock  [5 min ▼]  │
│ 🔐 Biometric Auth   [ON]        │
│ 📱 App Lock          [OFF]      │
│                                  │
│ ── Notifications ──────────────  │
│ 🔔 Note Reminders   [ON]        │
│ 📄 Document Expiry  [ON]        │
│ 🔄 Update Check     [Weekly ▼]  │
│                                  │
│ ── Advanced ───────────────────  │
│ 🔧 Developer Mode   [OFF]       │
│ 📋 Export Settings   [Export]    │
│ 📋 Import Settings   [Import]   │
│ 🔄 Reset to Default  [Reset]    │
│                                  │
│ ── About ──────────────────────  │
│ 📱 Version           3.0        │
│ 👨‍💻 Developer         Mahilan   │
│ 📜 License           MIT        │
│ 🐙 GitHub           [Open]      │
│ ⭐ Rate App          [Rate]     │
│ 📧 Feedback          [Send]     │
│ 📋 Changelog         [View]     │
│                                  │
│ ── How It Works ───────────────  │
│ 📖 [Tap to learn about Settings]│
└──────────────────────────────────┘
```

### Implementation Notes
- Each setting uses `SharedPreferences` with reactive listeners
- Use `remember { mutableStateOf(...) }` for each preference
- Wrap all preference reads in try-catch
- Theme setting is forced to "Light" (dark mode ignored)
- Add smooth expand/collapse animations for sections

---

# 21. HISTORY — RICH ACTIVITY TRACKER

## 21.1 Current State
- Basic usage history (conversion history only)
- Not enough features

## 21.2 Complete History System

### Track Everything
| Activity Type | What's Tracked |
|---------------|---------------|
| File Operations | Open, copy, move, delete, rename, compress, extract |
| Conversions | Format, size before/after, success/failure |
| Media Playback | Videos watched, songs played, duration |
| Documents | PDFs opened, pages viewed, annotations made |
| Tools Used | Calculator operations, unit conversions, compass readings |
| FTP Transfers | Files sent/received, client info |
| Notes | Created, edited, deleted |
| Vault | Files added, accessed, shared |
| Search | What was searched, in which tool |

### History UI
```
┌──────────────────────────────────┐
│ ← Activity History        🔍 🗑️ │
│══════════════════════════════════│
│                                  │
│ ── Today ──────────────────────  │
│ 🎬 Watched video.mp4 (45 min)   │
│ 📄 Opened report.pdf (12 pages) │
│ 🔄 Converted img.png → jpg      │
│ 📝 Edited "Shopping List" note   │
│ 🧮 Calculator: 45×67 = 3015     │
│                                  │
│ ── Yesterday ──────────────────  │
│ 🎵 Played 23 songs (1hr 42min)  │
│ 📡 FTP: Sent 5 files (234 MB)   │
│ 🔒 Vault: Added passport.pdf    │
│ 📐 Unit: 100 USD = 8,345 INR    │
│                                  │
│ ── This Week ──────────────────  │
│ ... (grouped by day)             │
│                                  │
│ ── Statistics ─────────────────  │
│ 📊 Most Used: File Manager (45×) │
│ 🎬 Videos Watched: 12 (8.5 hrs) │
│ 🎵 Songs Played: 156             │
│ 📄 Documents Opened: 23          │
│ 🔄 Conversions: 34               │
│ 📊 Total App Time: 14 hrs        │
└──────────────────────────────────┘
```

### Features
| Feature | Details |
|---------|---------|
| Timeline View | Chronological activity feed |
| Daily/Weekly/Monthly grouping | Filter by time period |
| Statistics Dashboard | Usage charts and metrics |
| Search History | Search within history entries |
| Filter by Type | Show only file operations, media, tools, etc. |
| Clear History | By type, by date range, or all |
| Export History | CSV export for analysis |
| Most Used Features | Ranking of most-used tools |
| Storage Impact | Track space saved by conversions/compressions |

---

# 22. IMAGE WATERMARK — FULL IMAGE EDITOR

## 22.1 Current State
- Only adds text watermarks
- No image editing
- User says "one of the worst ever — need image to edit"

## 22.2 Redesign as Image Editor with Watermark

### Rename: **Image Editor & Watermark**

```
┌──────────────────────────────────┐
│ ← Image Editor            💾 ↩️  │
│══════════════════════════════════│
│                                  │
│  ┌──────────────────────────┐   │
│  │                          │   │
│  │    [Image Preview]       │   │ ← Shows image with all edits applied
│  │    with all edits        │   │    Pinch to zoom, pan
│  │    applied live          │   │
│  │                          │   │
│  └──────────────────────────┘   │
│                                  │
│══════════════════════════════════│
│ [✂️Crop] [🔄Rotate] [🎨Filter]  │ ← Tool tabs (scrollable)
│ [📝Text] [✏️Draw] [💧Watermark] │
│ [🖼️Sticker] [🔧Adjust] [📐Frame]│
└──────────────────────────────────┘
```

### Tool Details

| Tool | Features |
|------|----------|
| **Crop** | Free, 1:1, 4:3, 16:9, 3:2, custom ratio |
| **Rotate** | 90° steps, free rotation with angle input |
| **Flip** | Horizontal, Vertical |
| **Resize** | Scale by percentage or exact dimensions |
| **Brightness** | -100 to +100 slider |
| **Contrast** | -100 to +100 slider |
| **Saturation** | -100 to +100 slider |
| **Exposure** | -100 to +100 slider |
| **Sharpness** | 0 to 100 slider |
| **Blur** | 0 to 50 radius |
| **Vignette** | 0 to 100 intensity |
| **Filters** | 20+ preset filters (B&W, Sepia, Vintage, Vivid, Cool, Warm, etc.) |
| **Text Overlay** | Font, size, color, shadow, outline, position, rotation |
| **Image Watermark** | Import image as watermark, opacity, position, tile mode |
| **Drawing** | Brush (size, color, opacity), eraser, line, rectangle, circle, arrow |
| **Stickers** | Emoji, shapes, stamps |
| **Frame** | Decorative borders and frames |
| **Undo/Redo** | Full edit history |
| **Compare** | Side-by-side or toggle original vs edited |
| **Save** | JPG, PNG, WEBP with quality slider |
| **Share** | Direct share to any app |

---

# 23. BATCH PROCESSING — INDUSTRIAL GRADE

## 23.1 Current State
- Basic batch file operations
- Not enough features
- Quality not equal across files

## 23.2 Complete Batch System

### Batch Operations
| Operation | Details |
|-----------|---------|
| **Image Conversion** | Convert 100+ images simultaneously: JPG→PNG, PNG→WebP, etc. |
| **Image Resize** | Resize all images to same dimensions/percentage |
| **Image Compress** | Equal quality compression across all files |
| **Video Compress** | Batch video compression with uniform quality |
| **Audio Convert** | Batch audio format conversion |
| **PDF Merge** | Merge multiple PDFs in order |
| **File Rename** | Batch rename with patterns (prefix, suffix, number, date) |
| **EXIF Strip** | Remove metadata from all selected files |
| **Watermark** | Apply same watermark to all images |
| **File Move** | Move multiple files to destination |
| **File Copy** | Copy multiple files to destination |
| **File Delete** | Bulk delete with confirmation |
| **Archive** | Compress all selected into ZIP/7z |

### Equal Quality Guarantee
```
For batch compression:
- User sets target quality percentage (e.g., 80%)
- OR user sets target file size (e.g., "each file ≤ 500KB")
- System processes EACH file individually
- Uses binary search to achieve exact target
- Verifies output matches specification within 1% tolerance
- NO file should be .1% extra or lower than target
```

### Batch UI
```
┌──────────────────────────────────┐
│ ← Batch Processing        ⚙️ ▶️  │
│══════════════════════════════════│
│                                  │
│ ── Selected Files (15) ───────  │
│ [✓ photo1.jpg 2.3MB]            │
│ [✓ photo2.jpg 3.1MB]            │
│ [✓ photo3.jpg 1.8MB]            │
│ ... +12 more [View All]          │
│                                  │
│ ── Operation ──────────────────  │
│ [Compress Images ▼]              │
│                                  │
│ ── Settings ───────────────────  │
│ Target Quality: [80%] slider     │
│ OR Target Size: [500 KB] each    │
│ Output Format: [Same as input ▼] │
│ Output Folder: [/Anegan/Batch/]  │
│ Preserve EXIF: [Yes / No]        │
│                                  │
│ ── Preview ────────────────────  │
│ Estimated output: ~8.2 MB total  │
│ Space saved: ~12.3 MB (60%)     │
│                                  │
│ [▶️ Start Batch Processing]       │
│                                  │
│ ── Progress ───────────────────  │
│ [████████████░░░░] 12/15 (80%)   │
│ Current: photo13.jpg             │
│ Speed: 2.3 files/sec             │
│ ETA: ~2 seconds                  │
└──────────────────────────────────┘
```

---

# 24. APP EXTRACTOR — SIMPLIFIED

## 24.1 Current State
- Extract and backup APKs
- Has features beyond just download

## 24.2 Simplification (per user request)
User wants: **"Only download option to make APK ready"**

### Simplified UI
```
┌──────────────────────────────────┐
│ ← App Extractor                 │
│══════════════════════════════════│
│ 🔍 Search apps...                │
│                                  │
│ ── Installed Apps (87) ────────  │
│ [📱 WhatsApp       v2.24.1  ⬇️] │
│ [📱 Instagram      v312.0   ⬇️] │
│ [📱 Chrome         v125.0   ⬇️] │
│ [📱 YouTube        v19.23   ⬇️] │
│ ... (all installed apps)         │
│                                  │
│ Tap ⬇️ to extract APK           │
│ Saved to: /Anegan/APKs/         │
│                                  │
│ ── Extracted APKs ─────────────  │
│ [✓ WhatsApp_v2.24.1.apk 45MB]  │
│ [✓ Chrome_v125.0.apk 120MB]    │
│   [📤 Share] [🗑️ Delete]        │
└──────────────────────────────────┘
```

### Only Two Actions Per App:
1. **⬇️ Extract APK** → Save to `/Anegan/APKs/`
2. **📤 Share extracted APK** → Share via any app

---

# 25. LOCAL TRANSFER HUB — REDESIGN

## 25.1 Redesign According to Previous Prompt
(Maintain existing WiFi Direct implementation but fix UI and reliability)

### Features
| Feature | Details |
|---------|---------|
| WiFi Direct Discovery | Scan for nearby devices |
| File Send | Select files → send to discovered device |
| File Receive | Accept incoming files |
| Transfer Progress | Real-time progress bar with speed |
| Transfer History | Log of all transfers |
| QR Code Connect | Generate/scan QR for quick pairing |
| Multiple Files | Batch file transfer |
| Folder Transfer | Send entire folders |
| Resume Support | Resume interrupted transfers |
| Encryption | Transfer files over encrypted channel |

---

# 26. SMART OFFLINE SAVER — VAULT INTEGRATION

## 26.1 Integration with Secure Vault
- Option to save offline content with encryption (vault-level security)
- "Save to Vault" toggle when saving offline content
- Vault-saved content requires biometric to access
- Encrypted thumbnails for vault-saved content

## 26.2 Feature Improvements
| Feature | Details |
|---------|---------|
| Save Web Pages | Download complete web page for offline reading |
| Save Text Content | Clipboard → offline storage |
| Save Media | Download images/videos from URLs |
| Organize by Category | Folders for different content types |
| Search Saved Content | Full-text search |
| Auto-cleanup | Remove old saved content (configurable) |
| Storage Usage | Show space used by offline content |
| Export | Export saved content as files |

---

# 27. OCR TEXT EXTRACTION — ENHANCED

## 27.1 Current State
- ML Kit Text Recognition (Latin only)
- Basic image → text

## 27.2 Improvements
| Feature | Details |
|---------|---------|
| **Multi-language** | Add support for: Hindi, Tamil, Telugu, Japanese, Chinese, Korean, Arabic |
| **Camera Mode** | Point camera → extract text live |
| **Document Mode** | Auto-detect document edges, perspective correction |
| **Handwriting** | Improved handwriting recognition |
| **Table Extraction** | Detect and extract tables from images |
| **Copy/Share** | One-tap copy all text, share to apps |
| **Edit** | Edit extracted text before copying |
| **PDF from OCR** | Create searchable PDF from scanned images |
| **Batch OCR** | Process multiple images at once |
| **History** | Keep history of extracted texts |
| **Accuracy Indicator** | Show confidence percentage |
| **Select Regions** | Draw box on image to OCR specific region |

---

# 28. EXIF METADATA — PREMIUM VIEWER

## 28.1 Current State
- Basic metadata reading (camera, GPS, dimensions, exposure)
- Can strip metadata
- Good foundation

## 28.2 Improvements
| Feature | Details |
|---------|---------|
| **Map View** | Show GPS coordinates on embedded map |
| **All Tags** | Show EVERY EXIF tag, grouped by category |
| **Edit Tags** | Modify date, GPS, camera info, description |
| **Batch Edit** | Edit metadata for multiple files |
| **Compare** | Compare metadata of two images side-by-side |
| **Export** | Export metadata as JSON/CSV/TXT |
| **Photo Details** | Lens info, ISO, aperture, shutter speed, flash |
| **Color Profile** | ICC profile, color space info |
| **Thumbnail** | Show embedded EXIF thumbnail |
| **Search by Metadata** | Find photos by camera model, date range, location |
| **Privacy Check** | Highlight privacy-sensitive tags (GPS, device ID) |
| **Copy Location** | Copy GPS coordinates to clipboard |
| **Privacy Strip** | One-tap remove all privacy-sensitive data |

---

# 29. SURVIVAL GUIDE — CRASH FIX & SEPARATION

## 29.1 Current Problem
- Shows Image Converter tools instead of survival content
- Crash when entering the feature

## 29.2 Fix
The navigation is likely routing to the wrong screen. Need to:
1. Check the route string in `AnegalNavigation.kt`
2. Verify the composable destination matches
3. Ensure `SurvivalGuideScreen` is not sharing a route with `ImageConverterScreen`
4. Completely separate the two features

## 29.3 Image Converter — Make Standalone
Image Converter should be its own separate widget/feature, not mixed with Survival Guide.

## 29.4 Survival Guide Content
| Category | Topics |
|----------|--------|
| **First Aid** | CPR, wounds, burns, fractures, choking, allergic reactions |
| **Water** | Purification methods, finding water sources, storage |
| **Fire** | Starting fire without matches, fire types, safety |
| **Shelter** | Emergency shelter types, insulation, location selection |
| **Navigation** | Compass use, star navigation, GPS basics, map reading |
| **Food** | Edible plants, hunting basics, food preservation |
| **Signals** | SOS signals, mirror signaling, smoke signals |
| **Weather** | Cloud reading, storm prediction, seasonal patterns |
| **Knots** | Essential knots with visual guides |
| **Medical** | Emergency medicines, natural remedies, wound treatment |
| **Emergency Numbers** | Worldwide emergency contacts |
| **Offline Maps** | Basic map concepts |

---

# 30. IMAGE CONVERTER — STANDALONE

## 30.1 Make Separate Feature
- Move out of Survival Guide
- Own widget in home screen under "Image Tools" category
- Full format conversion:

| Input → Output | Formats |
|---------------|---------|
| Input | JPG, JPEG, PNG, WebP, BMP, GIF, HEIC, AVIF, TIFF, SVG, ICO |
| Output | JPG, PNG, WebP, BMP, GIF, TIFF, ICO, PDF |

### Features
- Quality slider (1-100%)
- Resolution change
- Target file size option
- Batch conversion
- Preview before/after
- **Exact output** — no .1% extra or lower

---

# 31. OFFLINE COMMUNICATION — USER GUIDE

## 31.1 Current State
- Offline messaging tools
- Not enough user guidance

## 31.2 Improvements
| Feature | Details |
|---------|---------|
| **WiFi Direct Chat** | Text messaging over WiFi Direct |
| **Bluetooth Chat** | Text messaging over Bluetooth |
| **NFC Tap** | Quick data exchange via NFC |
| **Mesh Networking** | Multi-device relay messaging |
| **How to Use Guide** | Step-by-step visual guide for each method |
| **Connection Status** | Real-time connection quality indicator |
| **File Sharing** | Send small files over offline channels |
| **Group Chat** | Multi-device mesh chat |
| **Message History** | Saved conversation logs |
| **Auto-retry** | Retry failed messages automatically |

### "How to Do" Section
- Visual step-by-step instructions with screenshots
- Common troubleshooting tips
- FAQ section
- Video tutorial links (if online)

---

# 32. CONTINUE READING — WIDGET CONVERSION

## 32.1 Current: Fixed bar at top of home screen
## 32.2 Change: Regular scrollable widget

```
┌── Continue Reading ──────────────┐
│ 📖 Pick up where you left off    │
│                                  │
│ ┌────┐ ┌────┐ ┌────┐           │
│ │📄  │ │📕  │ │📄  │           │ ← Horizontal scroll carousel
│ │Rep │ │Book│ │Man │           │
│ │67% │ │34% │ │89% │           │
│ └────┘ └────┘ └────┘           │
│                                  │
│ [→ View All Reading History]     │
└──────────────────────────────────┘
```

- Only shows if there ARE items to continue
- Horizontal carousel of recent documents with progress
- Tapping a card opens the document at the last page
- "View All" navigates to full reading history

---

# 33. UPDATE CHECK — WIDGET CONVERSION

## 33.1 Current: Popup dialog on app launch
## 33.2 Change: Widget at bottom of home screen

```
┌── App Update ────────────────────┐
│ 🔄 Anegan v3.0                   │
│ Current: v2.1 → Latest: v3.0    │
│ [🔗 Download Update]             │ ← Only shows if update available
│                                  │
│ OR (if up to date):              │
│ ✅ You're up to date! (v3.0)    │
│ Last checked: 2 hours ago        │
└──────────────────────────────────┘
```

- NO popup on launch
- Check in background on app start (silent)
- Widget shows result
- User taps "Download" to go to GitHub releases page
- Auto-check interval: configurable in Settings (daily, weekly, monthly, never)

---

# 34. AI BACKGROUND — COMPLETE DELETION

## 34.1 Files to Delete
1. `feature/home/.../AiBackgroundScreen.kt` — DELETE entirely
2. `core/conversion-engine/.../SubjectSegmenterManager.kt` — DELETE entirely
3. Remove ML Kit Subject Segmentation dependency from `build.gradle.kts`
4. Remove route `"ai_background"` from `AnegalNavigation.kt`
5. Remove widget entry from `HomeScreen.kt`
6. Clean up any imports referencing these files

## 34.2 Checklist
- [ ] Delete AiBackgroundScreen.kt
- [ ] Delete SubjectSegmenterManager.kt
- [ ] Remove from navigation
- [ ] Remove from home screen widgets
- [ ] Remove ML Kit Subject Segmentation dependency
- [ ] Remove any related resources (strings, drawables)
- [ ] Verify build succeeds after deletion

---

# 35. HOW IT WORKS — UNIVERSAL GUIDE SYSTEM

## 35.1 Concept
Every widget, utility, and sub-feature should have a "How It Works" option accessible via an info icon (ℹ️).

## 35.2 Implementation

### [MODIFY] Every Screen
Add an info icon (ℹ️) in the top app bar:
```kotlin
@Composable
fun ScreenWithHowItWorks(
    title: String,
    guideContent: HowItWorksContent,
    content: @Composable () -> Unit
) {
    var showGuide by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                actions = {
                    IconButton(onClick = { showGuide = true }) {
                        Icon(Icons.Rounded.Info, "How It Works")
                    }
                }
            )
        }
    ) {
        content()
    }
    
    if (showGuide) {
        HowItWorksBottomSheet(
            content = guideContent,
            onDismiss = { showGuide = false }
        )
    }
}
```

### Guide Content Structure
```kotlin
data class HowItWorksContent(
    val title: String,
    val description: String,
    val steps: List<GuideStep>,
    val tips: List<String>,
    val faq: List<Pair<String, String>> // question, answer
)

data class GuideStep(
    val number: Int,
    val title: String,
    val description: String,
    val icon: ImageVector
)
```

### Every Feature Gets a Guide
| Feature | Guide Topics |
|---------|-------------|
| File Manager | Browse files, sort, filter, operations, quick access |
| Video Player | Gesture controls, settings, subtitles, rotation |
| Audio Player | Create playlists, equalizer, controls, library |
| PDF Reader | Search, zoom, annotations, bookmarks, edit |
| Video Tools | Convert, trim, merge, each tool explained |
| Audio Tools | Cut, merge, effects, each tool explained |
| Notes | Create notes, folders, tags, reminders, checklists |
| Second Brain | Links, tags, graph view, templates, inbox |
| Calculator | Basic vs scientific, memory, history |
| Unit Converter | Categories, currency, manual rates |
| Compass | Calibration, true north, features |
| Flashlight | Intensity, SOS, strobe |
| Developer Tools | Each tool individually explained |
| Secure Vault | Setup, add files, categories, security |
| Document Hub | Organize, search, collections |
| FTP Server | Setup, connect from PC/phone, web portal |
| Batch Processing | Select files, choose operation, settings |
| OCR | Camera mode, accuracy tips, languages |
| EXIF | View, edit, strip metadata |
| Image Editor | Each tool, tips for best results |
| Settings | Each setting explained |
| Offline Comm | WiFi Direct, Bluetooth, setup steps |
| Survival Guide | How to navigate each category |

---

# 36. USER GUIDE WIDGET — NEW

## 36.1 New Home Screen Widget

```
┌── 📚 Anegan Guide ──────────────┐
│ Your complete guide to mastering │
│ all features in Anegan           │
│                                  │
│ [🎬 Media Guide]                 │
│ [📄 Document Guide]              │
│ [🔧 Tools Guide]                │
│ [🔒 Security Guide]              │
│ [📡 Transfer Guide]              │
│ [⚙️ App Overview]               │
│                                  │
│ [→ Explore All Guides]           │
└──────────────────────────────────┘
```

### Guide Categories
Each opens a detailed page with:
- Feature list with descriptions
- Step-by-step tutorials
- Tips and tricks
- Common questions and answers
- Video demonstrations (screenshots)

---

# 37. DESIGN SYSTEM OVERHAUL

## 37.1 Current Issues
- Two different design systems (core/designsystem vs feature/home components)
- Inconsistent color usage
- Widget icons look amateurish
- Text styling not premium enough

## 37.2 Unified Design System

### Color Palette
```kotlin
// Primary gradient
val AneganPrimaryGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF667EEA), Color(0xFF764BA2))
)

// Category gradients
val MediaGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFFF6B6B), Color(0xFFFF8E53))
)
val DocumentGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF4FACFE), Color(0xFF00F2FE))
)
val ToolsGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF43E97B), Color(0xFF38F9D7))
)
val SecurityGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFA18CD1), Color(0xFFFBC2EB))
)
val ProductivityGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFFCB69F), Color(0xFFFFECD2))
)

// Surface colors
val AneganBackground = Color(0xFFF8FAFC)
val AneganSurface = Color(0xFFFFFFFF)
val AneganCardSurface = Color(0xFFF1F5F9)
```

### Typography
```kotlin
// Use Outfit as primary font (already bundled)
val AneganTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = outfitFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = outfitFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp
    ),
    titleLarge = TextStyle(
        fontFamily = outfitFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = outfitFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    labelMedium = TextStyle(
        fontFamily = outfitFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp
    )
)
```

### Premium Card Component
```kotlin
@Composable
fun AneganPremiumCard(
    modifier: Modifier = Modifier,
    gradient: Brush? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.97f else 1f)
    
    Card(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick ?: {}
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (gradient != null) Color.Transparent else AneganSurface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 1.dp
        )
    ) {
        Box(
            modifier = if (gradient != null) {
                Modifier.background(gradient)
            } else Modifier
        ) {
            content()
        }
    }
}
```

---

# 38. BACK BUTTON — PREMIUM REDESIGN

## 38.1 Current: `Icons.Default.ArrowBack`
Simple arrow that's "bad and hard to find"

## 38.2 New: Premium minimal back button

```kotlin
@Composable
fun AneganBackButton(onClick: () -> Unit) {
    IconButton(
        onClick = {
            HapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF1F5F9))
    ) {
        Icon(
            imageVector = Icons.Rounded.ArrowBackIosNew,  // iOS-style chevron
            contentDescription = "Back",
            modifier = Modifier.size(20.dp),
            tint = Color(0xFF1E293B)
        )
    }
}
```

### Design:
- **Icon**: `ArrowBackIosNew` (clean iOS-style left chevron `‹`)
- **Background**: Light gray rounded rectangle (12dp corners)
- **Size**: 40dp button, 20dp icon
- **Color**: Dark slate on light gray background
- **Haptic**: Subtle haptic feedback on tap
- **Easy to find**: Contrasts well against any background

---

# 39. DARK MODE — FORCE LIGHT MODE

## 39.1 Current Behavior
- Theme follows system setting (Light/Dark/System)
- Dark mode causes crashes
- When system dark mode is on, app turns dark

## 39.2 Fix: Always Force Light Mode

### [MODIFY] `MainActivity.kt`
```kotlin
setContent {
    // ALWAYS force light mode regardless of system setting
    AneganTheme(
        darkTheme = false,  // ALWAYS false
        dynamicColor = dynamicColorEnabled,
        amoledDark = false,  // ALWAYS false
        fontName = customFont
    ) {
        // ... app content
    }
}
```

### [MODIFY] `Theme.kt`
```kotlin
@Composable
fun AneganTheme(
    darkTheme: Boolean = false,  // Default to false
    dynamicColor: Boolean = true,
    amoledDark: Boolean = false,  // Default to false
    fontName: String = "Default",
    content: @Composable () -> Unit
) {
    // ALWAYS use light color scheme
    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicLightColorScheme(LocalContext.current)  // Always LIGHT dynamic
    } else {
        AneganLightColorScheme
    }
    
    // Force light status bar
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true // Always LIGHT
        }
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = getTypographyForFont(fontName),
        content = content
    )
}
```

### Remove Dark Mode Option from Settings
- Remove theme selector ("System", "Dark", "Light")
- Remove AMOLED toggle
- Keep only font and dynamic color options

---

# 40. SCROLL STATE PRESERVATION

Already covered in Section 4.4. Summary:

### Implementation for Every Screen with Lists
```kotlin
// Pattern for all scrollable screens
@Composable
fun AnyListScreen() {
    // Use rememberSaveable for scroll state
    val listState = rememberLazyListState()
    
    // This preserves scroll position across recomposition
    // and when navigating back
    LazyColumn(state = listState) {
        // ... items
    }
}
```

### For Home Screen (Custom Navigation)
Since the app uses `AnimatedContent` for navigation (not NavHost), we need the manual `ScrollStateManager` approach described in Section 4.4.

---

# 41. WIDGET ICONS & TEXT — COMPLETE REDESIGN

Already covered in Section 4.3. Summary:

### Design Rules
1. **3 icons per row** maximum on home screen
2. **Gradient circle** background (48dp) per icon
3. **White foreground icon** (24dp) — carefully selected
4. **File/link symbol** badge at bottom-right corner
5. **Outfit Medium** font, 12sp for labels
6. **Categories collapse** — show top 3, "See All" to expand
7. **Micro-animations** on tap (scale pulse 0.95→1.0)
8. **Haptic feedback** on every tap

### Widget Item Composable
```kotlin
@Composable
fun WidgetItem(
    icon: ImageVector,
    label: String,
    gradientColors: List<Color>,
    showFileBadge: Boolean = true,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f)
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(8.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Gradient circle background
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(gradientColors)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // File/link badge
            if (showFileBadge) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Link,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF475569),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
```

---

# 42. FILE LOGOS IN ALL WIDGETS

## Every widget that deals with files should show the file type logo

### Implementation
When a widget shows a file (in Document Hub, History, Continue Reading, etc.), show the file type icon:

```kotlin
@Composable
fun FileTypeBadge(extension: String) {
    val (icon, color) = when (extension.lowercase()) {
        "pdf" -> Icons.Rounded.PictureAsPdf to Color(0xFFE53935)
        "doc", "docx" -> Icons.Rounded.Description to Color(0xFF1565C0)
        "xls", "xlsx" -> Icons.Rounded.TableChart to Color(0xFF2E7D32)
        "ppt", "pptx" -> Icons.Rounded.Slideshow to Color(0xFFE65100)
        "jpg", "jpeg", "png", "gif", "webp" -> Icons.Rounded.Image to Color(0xFF7B1FA2)
        "mp4", "mkv", "avi", "mov" -> Icons.Rounded.OndemandVideo to Color(0xFFC62828)
        "mp3", "wav", "flac", "aac" -> Icons.Rounded.MusicNote to Color(0xFF6A1B9A)
        "zip", "rar", "7z" -> Icons.Rounded.FolderZip to Color(0xFF4E342E)
        "txt", "md" -> Icons.Rounded.TextSnippet to Color(0xFF546E7A)
        "apk" -> Icons.Rounded.Android to Color(0xFF388E3C)
        else -> Icons.Rounded.InsertDriveFile to Color(0xFF78909C)
    }
    
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = extension,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
    }
}
```

---

# 43. ANEGAN FOLDER — ROOT STORAGE

## 43.1 Current: `Documents/Anegan/`
## 43.2 Change: `/Anegan/` (root of internal storage)

### [MODIFY] `Constants.kt`
```kotlin
// OLD
const val ANEGAN_FOLDER = "Documents/Anegan"

// NEW
const val ANEGAN_FOLDER = "Anegan"
```

### [MODIFY] `StorageManager.kt`
```kotlin
fun getAneganOutputDirectory(subFolder: String): File {
    // Save to root of external storage: /storage/emulated/0/Anegan/
    val baseDir = File(
        Environment.getExternalStorageDirectory(),
        "Anegan" // Root level, not inside Documents
    )
    val outputDir = File(baseDir, subFolder)
    if (!outputDir.exists()) outputDir.mkdirs()
    return outputDir
}
```

### Migration
- On first launch of v3, check if `Documents/Anegan/` exists
- If yes, move all contents to `/Anegan/`
- Delete old `Documents/Anegan/` folder
- Show user a one-time notification about the move

---

# 44. FILE CONVERTER PRECISION

## 44.1 Rule
ALL file converters must output EXACTLY what the user specified:
- If target is 80% quality → output must be 80% quality (not 79.9% or 80.1%)
- If target is 500KB → output must be ≤ 500KB and ≥ 495KB
- If target is 1920×1080 → output must be exactly 1920×1080
- If target format is WebP → output must be .webp, not .webp with wrong encoding

## 44.2 Implementation Pattern
```kotlin
// Binary search for exact file size targeting
suspend fun compressToTargetSize(
    input: File,
    targetSizeBytes: Long,
    tolerance: Float = 0.01f // 1% tolerance max
): File {
    var low = 1
    var high = 100
    var bestOutput: File? = null
    var bestDiff = Long.MAX_VALUE
    
    repeat(10) { // Max 10 iterations
        val quality = (low + high) / 2
        val output = compressWithQuality(input, quality)
        val diff = output.length() - targetSizeBytes
        
        if (abs(diff) < bestDiff) {
            bestDiff = abs(diff)
            bestOutput = output
        }
        
        val withinTolerance = abs(diff.toFloat() / targetSizeBytes) <= tolerance
        if (withinTolerance) return output
        
        if (output.length() > targetSizeBytes) {
            high = quality - 1
        } else {
            low = quality + 1
        }
    }
    
    return bestOutput!!
}
```

### Same pattern applies to:
- Image compression (quality and file size targeting)
- Video compression (bitrate and file size targeting)
- Audio compression (bitrate targeting)
- PDF compression (quality targeting)

---

# 45. IN-PLACE FILE EDITING

## 45.1 Concept
When user wants to edit a PDF, video, image, or audio file:
1. Show the file first (preview)
2. Overlay edit tools ON TOP of the file
3. User can edit directly without navigating away
4. Tools are contextual — only relevant tools appear

## 45.2 Edit Toolbar Design
```
┌──────────────────────────────────┐
│ ← filename.ext        💾 ↩️ ⚙️   │
│══════════════════════════════════│
│                                  │
│     [FILE CONTENT PREVIEW]       │ ← The actual file is displayed here
│     (image, video frame,         │
│      PDF page, audio waveform)   │
│                                  │
│══════════════════════════════════│
│ [Tool 1] [Tool 2] [Tool 3] →   │ ← Scrollable tool bar at bottom
└──────────────────────────────────┘
```

### Per-File-Type Edit Tools

| File Type | Available Tools |
|-----------|----------------|
| **Image** | Crop, Rotate, Flip, Brightness, Contrast, Saturation, Filter, Text, Draw, Watermark, Resize |
| **Video** | Trim, Crop, Rotate, Speed, Filter, Text Overlay, Audio Replace, Compress, Frame Extract |
| **Audio** | Trim/Cut, Split, Fade, Speed, Pitch, EQ, Normalize, Reverse, Noise Reduction |
| **PDF** | Annotate, Highlight, Draw, Text, Sign, Stamp, Reorder Pages, Delete Pages, Rotate Pages |

---

# 46. VERIFICATION PLAN

## 46.1 Automated Testing

### Build Verification
```bash
./gradlew assembleDebug  # Must compile without errors
./gradlew lint           # No critical lint issues
```

### Feature Verification Checklist
Each feature must be tested for:
1. ✅ Opens without crash
2. ✅ Core functionality works
3. ✅ "How It Works" guide accessible
4. ✅ Back button works (returns to correct screen)
5. ✅ Scroll position preserved
6. ✅ File type icons display correctly
7. ✅ Dark mode doesn't affect appearance
8. ✅ No ANR (Application Not Responding) on any operation

## 46.2 Critical Path Testing

| Test | Steps | Expected |
|------|-------|----------|
| File opening from external app | Share any file from Files app → Open with Anegan | Routes to correct viewer |
| Video player gestures | Play video → double-tap right side | Forwards 10 seconds |
| Video rotation | Play video → tap rotate button | Screen rotates 90° |
| Audio playlist | Open Audio Player → create playlist → add songs | Playlist plays in order |
| PDF search | Open PDF → search for word | All matches highlighted |
| FTP server | Start FTP → connect from PC | Files accessible |
| FTP web portal | Start server → open URL in browser | Web file browser loads |
| Settings | Open settings → change any option | No crash, setting persists |
| Dark mode immunity | Enable system dark mode | App stays in light mode |
| Scroll preservation | Scroll down home → open feature → press back | Returns to scroll position |
| Survival Guide | Open Survival Guide | Shows survival content, NOT image converter |
| Calculator scientific | Switch to scientific mode → compute sin(45) | Returns 0.7071... |
| Currency manual | Enter manual currency rate → convert | Uses manual rate |
| Flashlight intensity | Open flashlight → drag brightness slider | Brightness changes |
| QR Camera scan | Open Developer Tools → QR Scanner → Camera mode | Live camera scanning works |
| Compass calibration | Open Compass → check accuracy | Shows calibration guide if needed |
| Notes reminder | Create note with reminder → wait for time | Notification appears |
| Vault biometric | Open Vault → authenticate | Biometric prompt appears |
| Batch compression | Select 10 images → compress to 80% | All files are exactly 80% quality |

## 46.3 Manual Verification
- Install APK on physical device
- Test every feature manually
- Verify UI looks premium and polished
- Check all animations are smooth (60fps)
- Verify haptic feedback works
- Test with different screen sizes
- Test with system dark mode ON (should not affect app)

---

# APPENDIX A: FILE COUNT ESTIMATE

| Area | New Files | Modified Files |
|------|-----------|---------------|
| Core/Common | 2 | 2 |
| Core/DesignSystem | 1 | 3 |
| Core/Database | 12 | 3 |
| Core/Conversion-Engine | 2 | 4 |
| App Module | 0 | 2 |
| Feature/Home - New Screens | 12 | 0 |
| Feature/Home - Modified Screens | 0 | 30+ |
| Feature/Home - New Components | 8 | 0 |
| Feature/Home - Navigation | 0 | 1 |
| **TOTAL** | **~37 new files** | **~45 modified files** |

---

# APPENDIX B: PRIORITY ORDER OF EXECUTION

### Phase 1: Critical Fixes (Must Do First)
1. Fix Settings crash
2. Fix PDF Reader crash
3. Fix Survival Guide crash (wrong screen)
4. Fix FTP server (binding + web portal)
5. Fix dark mode (force light)
6. Delete AI Background completely

### Phase 2: Foundation Changes
7. Database schema expansion (migration)
8. Anegan folder → root storage
9. Intent filters for file opening
10. File opener routing system
11. Back button redesign
12. Design system overhaul (colors, typography, icons)

### Phase 3: Home Screen Overhaul
13. Widget icon redesign (all 30+ widgets)
14. Home screen layout restructure (3 per row, categories)
15. Scroll state preservation
16. Continue Reading → widget
17. Update Check → widget (remove popup)
18. File logos on all widgets
19. User Guide widget (new)

### Phase 4: Media Players
20. Video Player — gesture system (brightness, volume, seek)
21. Video Player — double-tap forward/backward
22. Video Player — rotation controls
23. Video Player — subtitle support
24. Video Player — speed controls, sleep timer
25. Audio Player — library (albums, artists, playlists)
26. Audio Player — album art extraction
27. Audio Player — equalizer
28. Audio Player — most listened, recently played
29. Audio Player — 10s forward/backward
30. Audio Player — background playback service

### Phase 5: File Viewers
31. Image Viewer (new)
32. Text Viewer (new)
33. EPUB Reader (new)
34. Archive Viewer (new)
35. Document Viewer — DOCX/XLSX/PPTX (new)
36. APK Info screen (new)
37. Generic File Info screen (new)

### Phase 6: Tool Upgrades
38. Calculator — scientific mode
39. Unit Converter — live currency API + manual override
40. Compass — calibration, declination, sunrise/sunset, level
41. Flashlight — intensity slider
42. Developer Tools — camera QR scanner
43. Developer Tools — new tools (UUID, JWT, diff, etc.)

### Phase 7: Productivity
44. Notes — enhanced simple mode (reminders, checklists, formatting)
45. Notes — Second Brain mode (markdown, links, graph view)
46. Document Hub — complete redesign
47. History — rich activity tracking with statistics
48. Secure Vault — enhanced categories, encryption, expiry tracking

### Phase 8: Media Tools Enhancement
49. Video Tools — video editor with timeline
50. Video Tools — custom format entry
51. Audio Tools — audio editor with waveform
52. Audio Cutter — improved waveform UI
53. Image Watermark → Image Editor (full redesign)
54. Batch Processing — equal quality guarantee
55. Image Converter — standalone feature

### Phase 9: Transfer & Offline
56. FTP Server — web portal redesign
57. Local Transfer Hub — reliability fixes
58. Smart Offline Saver — vault integration
59. Offline Communication — user guide
60. App Extractor — simplified (download only)

### Phase 10: Polish & Guides
61. How It Works — add to every screen
62. User Guide — comprehensive guide widget
63. OCR — multi-language, camera mode
64. EXIF — map view, batch edit
65. Converter precision — verify all converters
66. In-place file editing
67. File Manager — compact storage cards
68. Final design polish across all screens

---

# APPENDIX C: ESTIMATED SCOPE

| Metric | Estimate |
|--------|----------|
| Total new Kotlin files | ~37 |
| Total modified Kotlin files | ~45 |
| New lines of code | ~25,000-35,000 |
| Modified lines of code | ~15,000-20,000 |
| New database entities | 12 |
| New UI screens | 12 |
| Features getting major overhaul | 25+ |
| Features getting minor improvements | 10+ |
| Features being deleted | 1 (AI Background) |
| New dependencies | ~8 |

---

> **This plan covers every nook and corner of the Anegan app v3 — "The Beast Update".**
> Every widget, utility, sub-feature, icon, animation, and pixel has been accounted for.
> The goal: transform Anegan from amateur to professional, from basic to beast.

---

*End of Implementation Plan*
*© 2026 Mahilan (heisenricher) — Anegan v3 Beast Update*
