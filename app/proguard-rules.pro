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
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.**

# WorkManager
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

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

# Media3 (ExoPlayer)
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# JCIFS SMB (smb-share module)  
-keep class jcifs.** { *; }
-dontwarn jcifs.**
