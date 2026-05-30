<p align="center">
  <h1 align="center">Anegan V3.2 "Nova" 🌌</h1>
  <p align="center"><strong>A Premium, Privacy-First, 100% Offline-First Multi-Tool Utility Hub for Android</strong></p>
</p>

<p align="center">
  <a href="https://github.com/heisenricher/Anegan/releases/latest"><strong>📥 Download Latest Version</strong></a>
  &nbsp;&bull;&nbsp;
  <a href="https://github.com/heisenricher/Anegan/releases"><strong>🗂️ View All Versions</strong></a>
</p>

---

## 📖 Introduction

**Anegan** (meaning *versatile* or *many-sided* in Tamil) is an elegant, premium, and completely offline multi-tool conversion and management application for Android. Designed to prioritize extreme privacy, ease of use, and state-of-the-art interactive aesthetics, Anegan processes all utility tasks **100% locally on the user's device**. There are no remote API requests, no tracking analytics, no cloud uploads, and no ads.

The **V3.2 "Nova Update"** introduces a total design overhaul, wrapping the utility features in a futuristic cyberpunk × glassmorphism visual system. Moving particles, dynamic neon-lit borders, custom sliders, segmented index selectors, and spring haptic feedbacks are unified to create a premium, state-of-the-art user experience.

---

## ✨ Features Directory

### 📄 1. PDF & Document Utilities (`NeonCyan` Themed)
*   **Merge PDFs**: Combine multiple separate PDF documents into a single file locally.
*   **Split PDF**: Extract specific page ranges (1-indexed) into separate documents.
*   **Compress PDF**: Reduce file sizes by downsampling embedded images with customizable target resolutions (72–300 DPI).
*   **Password Protect**: Apply secure PDF password encryption (user/owner passwords) with access controls.
*   **PDF to Images Export**: Convert vector PDF pages into individual high-definition PNG/JPG files.
*   **Images to PDF Import**: Convert and merge batches of images into a single vector PDF.
*   **PDF Page Organizer**: Visually reorder, insert, or delete individual pages of a PDF document.
*   **Text & Code Viewer**: Custom editor console with yellow keyword highlighting and monospaced JetBrains Mono layout.
*   **Generic File Info**: Inspect metadata and generate cryptographic hashes (MD5, SHA-1, SHA-256) inside clean, comparative data tables.

### 🧠 2. Second Brain Notes & Organization (`NeonGold` Themed)
*   **Biometric History & Security**: Terminal-locked history sheets that verify entry using device biometrics and trigger physical haptic warnings on failure.
*   **Interactive Mind-Map Brain Graph**: Render structural notes as dynamic, 2D physics-simulated node graphs with glowing vector links and floating gold particles.
*   **Markdown Editor & Snapshot Snap**: Advanced note editor with side-by-side markdown previews, version snap history, outlines drawer, and clickable wiki-link indexing.

### 🖼️ 3. Native Image Tools (`NeonMagenta` Themed)
*   **Format Shifts**: Direct conversions between JPG, PNG, WEBP, HEIC, and AVIF formats.
*   **Image Compression**: Slider-based adjustments (0–100% quality target) and exact file size targeting.
*   **Dimension Adjustments**: Multi-axis rotation, rectangular cropping, and exact dimension resizing (custom scale factors).
*   **Image Watermark**: Apply custom rotated text watermarks with control over transparency, sizing, and colors.
*   **Batch Conversion**: Stage and convert multiple image files concurrently in the background.

### 🎥 4. Video & Audio Tools (`NeonMagenta` Themed)
*   **Video Trim**: Lossless cutting/trimming of video clips (seeking via MM:SS or seconds) without re-encoding (`-c copy`).
*   **Video Compression**: Compress videos using custom CRF (18–35) quality sliders and resolution presets (Original, 1080p, 720p, 480p).
*   **Video Speed Control**: Playback speed modifier (0.25x to 4.0x).
*   **Video to GIF**: Convert video segments into looping animated GIFs with custom FPS, duration, and width scales.
*   **Audio Cutter**: Crop audio files to make ringtones or short clips.
*   **Video to MP3 (Audio Extraction)**: Strip audio tracks from video files and export them to standalone MP3 formats.

### 🛠️ 5. Offline Connectivity & Core Utilities (`NeonBlue` / `NeonLime` Themed)
*   **Wifi Transfer (HTTP/FTP Server)**: Host a local transfer hub with dynamic signal radar wave animation to transfer files between device and computer.
*   **Local SMB File Share**: Securely connect to local Samba network endpoints to access files locally.
*   **Smart Offline Saver**: Organize sensitive documents behind custom tags (Receipts, Starred, IDs, Scans) protected by local AES-256 vault encryption.
*   **Flashlight Controller**: Precision slider strobe speed controller, hardware level selector, and a red Emergency SOS flasher.
*   **Compass & Inclinometer**: Cyber dial with solar equation indices, bubble inclinometer checks, and snap level feedback.
*   **Color Picker**: Interactive HSV spectra selector, color code display (Hex, RGB, HSL), screen-sampling eye-dropper to extract colors from local images, and a saved swatches palette drawer.
*   **Unit Converter**: Multi-category converter for Length, Mass, Temperature, Data Size, and Area.

---

## 🛠️ System & Tech Stack

Anegan is built on a clean multi-module architecture:

-   **Jetpack Compose**: Butter-smooth UI matching dynamic material themes.
-   **WorkManager & Foreground Services**: Moves heavy operations into system-persistent background queues with real-time percentage progress notifications.
-   **FFmpeg (Native)**: JNI integration to execute high-performance media transcoding pipelines locally.
-   **PDFBox (Android Port)**: Direct rendering, metadata extraction, and vector modification of document layouts.
-   **Room Database**: Local SQL records registry protected behind biometric security overlays.
-   **ML Kit Vision SDK**: On-device neural network execution for barcode scanner and OCR.

---

## 📦 Version Roadmap

*   **v1.0 (Core Engine)**: Initial image conversion pipelines, native database registry, and biometric authentication setup.
*   **v2.0 (Master Update)**:
    *   Transition to background WorkManager workers with Foreground Services and progress notifications.
    *   Extended document manipulation (DPI downsamplers, organization grids, passcode controls).
    *   Premium video/audio transcoders (lossless copy trims, quality CRF scaling, GIF formats).
*   **v3.2 (Nova Overhaul - Current)**:
    *   Complete cyberpunk × glassmorphism visual overhaul.
    *   Custom high-fidelity UI components (`NovaTextField` glowing halos, `NovaSlider` aura rings, `NovaSwitch` spring toggles, `NovaSegmentedControl` sliders).
    *   Mind-Map Brain Graph view with dynamic 2D physics nodes.
    *   Wifi FTP/HTTP transfer server and SMB file share integration.
    *   Advanced Text/Code Viewer and Generic File Info hash validators.
    *   Premium custom cybernetic launcher icon.
    *   Fixed asset upload stalling at 100% via API direct publishing pipeline.

---

## 🏗️ Building from Source

To build and compile the APK locally, make sure you have **Java 17 (JBR)** configured.

1. Clone the repository:
   ```bash
   git clone https://github.com/heisenricher/Anegan.git
   ```
2. Navigate to directory:
   ```bash
   cd Anegan
   ```
3. Compile debug build:
   ```bash
   ./gradlew assembleDebug
   ```
4. Compile optimized release build:
   ```bash
   ./gradlew assembleRelease
   ```
   *(Note: The release configuration will fall back to debug signatures if `keystore.properties` is not present).*

---

## 📜 License & Copyright

Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.

This source code is licensed under the custom Anegan Attribution License. Any person or entity using, modifying, or building upon this code must prominently attribute the original creator Mahilan (heisenricher). Personal and educational use only.
