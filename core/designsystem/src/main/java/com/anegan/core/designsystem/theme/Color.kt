/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 *
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════════
// ANEGAN V3.2 — NOVA COLOR PALETTE
// Design Philosophy: 60-30-10 Color Harmony + 6-Category Neon System
// ═══════════════════════════════════════════════════════════════════

// ─────────────────────────────────────────────────
// CORE BACKGROUNDS (60% — Dominant Layer)
// ─────────────────────────────────────────────────

// Light Mode Backgrounds
val NovaGhostWhite     = Color(0xFFF0F2F5)    // Primary background
val NovaPureWhite      = Color(0xFFFFFFFF)    // Surface cards
val NovaCoolGray50     = Color(0xFFF8F9FA)    // Substrate layer
val NovaCoolGray100    = Color(0xFFE9ECEF)    // Dividers, borders

// Dark Mode Backgrounds
val NovaVoidBlack      = Color(0xFF0A0A0F)    // AMOLED pure void
val NovaDeepSpace      = Color(0xFF0D1117)    // Dark mode background
val NovaMidnightBlue   = Color(0xFF161B22)    // Dark mode surface cards
val NovaDarkSlate      = Color(0xFF21262D)    // Dark mode elevated surface

// ─────────────────────────────────────────────────
// STRUCTURAL ELEMENTS (30% — Text, Borders, UI)
// ─────────────────────────────────────────────────

// Text Colors
val NovaDeepInk        = Color(0xFF1A1A2E)    // Light mode primary text
val NovaSlateGray      = Color(0xFF6E7681)    // Secondary text (both modes)
val NovaFrostWhite     = Color(0xFFE8ECF4)    // Dark mode primary text
val NovaMutedGray      = Color(0xFF8B949E)    // Tertiary/muted text

// Border Colors
val NovaBorderLight    = Color(0xFFD0D7DE)    // Light mode borders
val NovaBorderDark     = Color(0xFF30363D)    // Dark mode borders

// Glassmorphism Fills
val NovaGlassWhite     = Color(0x33FFFFFF)    // Glass card fill — 20% white
val NovaGlassBlack     = Color(0x33000000)    // Glass card fill — 20% black
val NovaGlassBorderW   = Color(0x1AFFFFFF)    // Glass border — 10% white
val NovaGlassBorderB   = Color(0x1A000000)    // Glass border — 10% black

// ─────────────────────────────────────────────────
// NEON ACCENTS (10% — Category-Coded Highlights)
// ─────────────────────────────────────────────────

// 🎬 MEDIA & CREATIVE — Electric Magenta
val NeonMagenta        = Color(0xFFFF006E)
val NeonMagentaGlow    = Color(0x40FF006E)    // 25% alpha glow
val NeonMagentaSoft    = Color(0x14FF006E)    // 8% alpha background tint
val NeonMagentaEnd     = Color(0xFFFF4D94)    // Gradient end

// 📄 DOCUMENTS & READING — Cyber Cyan
val NeonCyan           = Color(0xFF00D4FF)
val NeonCyanGlow       = Color(0x4000D4FF)
val NeonCyanSoft       = Color(0x1400D4FF)
val NeonCyanEnd        = Color(0xFF66E5FF)

// 🛠️ UTILITY TOOLKIT — Electric Lime
val NeonLime           = Color(0xFF39FF14)
val NeonLimeGlow       = Color(0x4039FF14)
val NeonLimeSoft       = Color(0x1439FF14)
val NeonLimeEnd        = Color(0xFF7FFF5C)

// 🔒 SECURITY & FILES — Plasma Purple
val NeonPurple         = Color(0xFFBF40FF)
val NeonPurpleGlow     = Color(0x40BF40FF)
val NeonPurpleSoft     = Color(0x14BF40FF)
val NeonPurpleEnd      = Color(0xFFD580FF)

// 🌐 TRANSFER & CONNECTION — Holo Blue
val NeonBlue           = Color(0xFF4D7CFF)
val NeonBlueGlow       = Color(0x404D7CFF)
val NeonBlueSoft       = Color(0x144D7CFF)
val NeonBlueEnd        = Color(0xFF80A4FF)

// 📋 PRODUCTIVITY & LEARNING — Solar Gold
val NeonGold           = Color(0xFFFFB800)
val NeonGoldGlow       = Color(0x40FFB800)
val NeonGoldSoft       = Color(0x14FFB800)
val NeonGoldEnd        = Color(0xFFFFCF4D)

// ─────────────────────────────────────────────────
// SEMANTIC COLORS
// ─────────────────────────────────────────────────
val NovaSuccess        = Color(0xFF00E676)    // Bright green — success
val NovaSuccessGlow    = Color(0x4000E676)
val NovaSuccessSoft    = Color(0x1400E676)
val NovaError          = Color(0xFFFF1744)    // Bright red — error
val NovaErrorGlow      = Color(0x40FF1744)
val NovaErrorSoft      = Color(0x14FF1744)
val NovaWarning        = Color(0xFFFFEA00)    // Bright yellow — warning
val NovaWarningGlow    = Color(0x40FFEA00)
val NovaWarningSoft    = Color(0x14FFEA00)
val NovaInfo           = Color(0xFF40C4FF)    // Bright blue — info
val NovaInfoGlow       = Color(0x4040C4FF)
val NovaInfoSoft       = Color(0x1440C4FF)

// ─────────────────────────────────────────────────
// CATEGORY GRADIENT PAIRS (Unified Neon System)
// Each category gets exactly ONE neon pair
// ─────────────────────────────────────────────────

val NovaMediaGradient       = listOf(NeonMagenta, NeonMagentaEnd)
val NovaDocumentGradient    = listOf(NeonCyan, NeonCyanEnd)
val NovaUtilityGradient     = listOf(NeonLime, NeonLimeEnd)
val NovaSecurityGradient    = listOf(NeonPurple, NeonPurpleEnd)
val NovaTransferGradient    = listOf(NeonBlue, NeonBlueEnd)
val NovaProductivityGradient = listOf(NeonGold, NeonGoldEnd)
val NovaSuccessGradient     = listOf(NovaSuccess, Color(0xFF69F0AE))
val NovaErrorGradient       = listOf(NovaError, Color(0xFFFF5252))

// ─────────────────────────────────────────────────
// TOOL → CATEGORY NEON MAPPING
// ─────────────────────────────────────────────────

/**
 * Returns the neon accent color for a given tool/category title.
 * All tools within a category share the same neon accent.
 */
fun getNeonForCategory(title: String): Color = when (title) {
    // Media & Creative → Magenta
    "Video Player", "Audio Player", "Video", "Audio",
    "Images", "Video Tools", "Audio Tools",
    "Batch Image", "Color Picker", "Image Watermark" -> NeonMagenta

    // Documents & Reading → Cyan
    "Document Hub", "PDF Tools", "Documents",
    "PDF Reader & Editor", "OCR / Extract Text",
    "EXIF Metadata" -> NeonCyan

    // Utility Toolkit → Lime
    "Calculator", "Flashlight", "Compass",
    "Currency Converter", "Unit Converter",
    "Voice Recorder" -> NeonLime

    // Security & Files → Purple
    "File Manager", "Vault", "Smart Saver",
    "APK Extractor", "Developer Tools",
    "Storage Analyzer" -> NeonPurple

    // Transfer & Connection → Blue
    "Wi-Fi & FTP Transfer", "SMB File Sharing",
    "Offline Comm" -> NeonBlue

    // Productivity & Learning → Gold
    "Notes", "Survival Library", "History",
    "Settings", "Feedback" -> NeonGold

    else -> NeonBlue // Default fallback
}

/**
 * Returns the neon gradient pair for a given tool/category title.
 */
fun getGradientForCategory(title: String): List<Color> = when (title) {
    "Video Player", "Audio Player", "Video", "Audio",
    "Images", "Video Tools", "Audio Tools",
    "Batch Image", "Color Picker", "Image Watermark" -> NovaMediaGradient

    "Document Hub", "PDF Tools", "Documents",
    "PDF Reader & Editor", "OCR / Extract Text",
    "EXIF Metadata" -> NovaDocumentGradient

    "Calculator", "Flashlight", "Compass",
    "Currency Converter", "Unit Converter",
    "Voice Recorder" -> NovaUtilityGradient

    "File Manager", "Vault", "Smart Saver",
    "APK Extractor", "Developer Tools",
    "Storage Analyzer" -> NovaSecurityGradient

    "Wi-Fi & FTP Transfer", "SMB File Sharing",
    "Offline Comm" -> NovaTransferGradient

    "Notes", "Survival Library", "History",
    "Settings", "Feedback" -> NovaProductivityGradient

    else -> NovaTransferGradient
}

/**
 * Returns the soft tint background color for a given category (8% alpha neon).
 */
fun getSoftTintForCategory(title: String): Color = when (title) {
    "Video Player", "Audio Player", "Video", "Audio",
    "Images", "Video Tools", "Audio Tools",
    "Batch Image", "Color Picker", "Image Watermark" -> NeonMagentaSoft

    "Document Hub", "PDF Tools", "Documents",
    "PDF Reader & Editor", "OCR / Extract Text",
    "EXIF Metadata" -> NeonCyanSoft

    "Calculator", "Flashlight", "Compass",
    "Currency Converter", "Unit Converter",
    "Voice Recorder" -> NeonLimeSoft

    "File Manager", "Vault", "Smart Saver",
    "APK Extractor", "Developer Tools",
    "Storage Analyzer" -> NeonPurpleSoft

    "Wi-Fi & FTP Transfer", "SMB File Sharing",
    "Offline Comm" -> NeonBlueSoft

    "Notes", "Survival Library", "History",
    "Settings", "Feedback" -> NeonGoldSoft

    else -> NeonBlueSoft
}

// ─────────────────────────────────────────────────
// LEGACY COMPATIBILITY (V3.1 colors kept for gradual migration)
// These will be removed once all screens are migrated to Nova colors
// ─────────────────────────────────────────────────
val PureWhite = NovaPureWhite
val MidnightIndigo = NovaDeepInk
val PlatinumSilver = Color(0xFFE2E8F0)
val SoftCoolGray = NovaCoolGray50
val LuminousGlow = Color(0xFFCBD5E1)
val ErrorRed = NovaError
val SuccessGreen = NovaSuccess
val AneganBackground = NovaGhostWhite
val AneganSurface = NovaPureWhite
val AneganCardSurface = Color(0xFFF1F5F9)
val AneganDivider = NovaCoolGray100
val AneganTextPrimary = NovaDeepInk
val AneganTextSecondary = NovaSlateGray
val AneganTextTertiary = NovaMutedGray

// Legacy gradients — mapped to new neon system
val VideoGradient = NovaMediaGradient
val AudioGradient = NovaMediaGradient
val PdfGradient = NovaDocumentGradient
val ImageGradient = NovaMediaGradient
val ToolGradient = NovaUtilityGradient
val CalcGradient = NovaUtilityGradient
val CompassGradient = NovaUtilityGradient
val FlashGradient = NovaUtilityGradient
val VaultGradient = NovaSecurityGradient
val NotesGradient = NovaProductivityGradient
val SettingsGradient = NovaProductivityGradient
val HistoryGradient = NovaProductivityGradient
val FtpGradient = NovaTransferGradient
val OcrGradient = NovaDocumentGradient
val ExifGradient = NovaDocumentGradient
val DevGradient = NovaSecurityGradient
val BatchGradient = NovaMediaGradient
val FileManagerGradient = NovaSecurityGradient
val DocHubGradient = NovaDocumentGradient
val WatermarkGradient = NovaMediaGradient
val ConverterGradient = NovaUtilityGradient
val TransferGradientColors = NovaTransferGradient
val OfflineGradient = NovaTransferGradient
val SurvivalGradient = NovaProductivityGradient
val SaverGradient = NovaSecurityGradient
val ExtractorGradient = NovaSecurityGradient
val ContinueReadingGradient = NovaDocumentGradient
val GuideGradient = NovaProductivityGradient
val UpdateGradient = NovaSuccessGradient
