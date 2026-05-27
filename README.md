<p align="center">
  <h1 align="center">Anegan 2.0</h1>
  <p align="center"><strong>A Premium, Privacy-First, 100% Offline-First File Conversion Toolkit for Android</strong></p>
</p>

<p align="center">
  <a href="https://github.com/heisenricher/Anegan/releases/latest"><strong>📥 Download Latest Version</strong></a>
  &nbsp;&bull;&nbsp;
  <a href="https://github.com/heisenricher/Anegan/releases"><strong>🗂️ View All Versions</strong></a>
</p>

---

## 📖 Introduction

**Anegan** is an elegant, premium, and completely offline multi-tool conversion application for Android. Designed to prioritize privacy, ease-of-use, and robust file management, Anegan processes all conversion tasks **locally on the user's device**. There are no remote API requests, no tracking analytics, no cloud uploads, and no ads. 

Whether you need to merge sensitive PDFs, compress high-definition video, run optical character recognition on document photos, or convert batch image sets, Anegan handles them inside private background workers.

---

## ✨ Features Directory

### 📄 1. PDF & Document Utilities
*   **Merge PDFs**: Combine multiple separate PDF documents into a single file.
*   **Split PDF**: Extract specific page ranges (1-indexed) into separate documents.
*   **Compress PDF**: Reduce file sizes by downsampling embedded images with customizable target resolutions (72–300 DPI).
*   **Password Protect**: Apply secure PDF password encryption (user/owner passwords) with access controls.
*   **PDF to Images Export**: Convert vector PDF pages into individual high-definition PNG/JPG files.
*   **Images to PDF Import**: Convert and merge batches of images into a single vector PDF.
*   **PDF Page Organizer**: Visually reorder, insert, or delete individual pages of a PDF document.

### 🖼️ 2. Native Image Tools
*   **Format Shifts**: Direct conversions between JPG, PNG, WEBP, HEIC, and AVIF formats.
*   **Image Compression**: Slider-based lossy/lossless adjustments (0–100% quality target) and exact file size targeting.
*   **Dimension Adjustments**: Multi-axis rotation, rectangular cropping, and exact dimension resizing (custom scale factors).
*   **Image Watermark**: Apply custom rotated text watermarks with control over transparency, sizing, and colors.
*   **Batch Conversion**: Stage and convert multiple image files concurrently in the background.

### 🎥 3. Video & Audio Tools
*   **Video Trim**: Lossless cutting/trimming of video clips (seeking via MM:SS or seconds) without re-encoding (`-c copy`).
*   **Video Compression**: Compress videos using custom CRF (18–35) quality sliders and resolution presets (Original, 1080p, 720p, 480p).
*   **Video Speed Control**: Playback speed modifier (0.25x to 4.0x).
*   **Video to GIF**: Convert video segments into looping animated GIFs with custom FPS, duration, and width scales.
*   **Audio Cutter**: Crop audio files to make ringtones or short clips.
*   **Video to MP3 (Audio Extraction)**: Strip audio tracks from video files and export them to standalone MP3 formats.

### 🧠 4. Machine Learning & AI Utilities (100% Local)
*   **AI OCR (Extract Text)**: Offline optical character recognition scanning of photos/scans to copyable text, automatically saved as local `.txt` logs.
*   **AI Background Remover**: Isolate subjects and strip backgrounds from photos offline using local segmentation engines.
*   **Ask Anegan AI Assistant**: An offline natural language query router. It parses user requests (e.g., *"extract text from photo"*) and uses keyword matching to map them to specific screen routes (like `OcrScreen`) and pre-fill parameters.

### 🛠️ 5. Developer & Utility Tools
*   **Hash Generator**: Compute cryptographic checksums (MD5, SHA-1, SHA-256) of input texts or selected files.
*   **Base64 Converter**: Text encoder/decoder helper.
*   **QR Scanner & Generator**: Generate QR code images from text inputs or scan QR codes from files using native camera engines.
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
-   **ML Kit Vision SDK**: On-device neural network execution for barcode, subject segmenter, and OCR.

---

## 📦 Version Roadmap

*   **v1.0 (Core Engine)**: Initial image conversion pipelines, native database registry, and biometric authentication setup.
*   **v2.0 (Anegan Master Update)**:
    *   Transition to background WorkManager workers with Foreground Services and progress notifications.
    *   Extended document manipulation (DPI downsamplers, organization grids, passcode controls).
    *   Premium video/audio transcoders (lossless copy trims, quality CRF scaling, GIF palettes).
    *   Local machine learning tools (Subject Segmenter, Text Detector, Query router).
    *   Extended offline developer utilities (Color Picker, Unit Converter, QR Tools, Hashes).

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
