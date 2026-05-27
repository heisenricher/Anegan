# Proguard rules for Anegan

# Keep PDFBox Android classes
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**

# Keep FFmpegKit classes and JNI
-keep class com.arthenica.ffmpegkit.** { *; }
-keep class com.moizhassan.ffmpeg.** { *; }
-dontwarn com.arthenica.ffmpegkit.**
-dontwarn com.moizhassan.ffmpeg.**

# General Compose and Android rules
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Google Play Services & ML Kit (OCR / Segmenter)
-keep class com.google.android.gms.internal.** { *; }
-dontwarn com.google.android.gms.internal.**
-keep class com.google.android.gms.tasks.** { *; }
-dontwarn com.google.android.gms.tasks.**
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# ZXing Barcode Scanning
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# WorkManager
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**
