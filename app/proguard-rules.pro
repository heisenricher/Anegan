# Proguard rules for Anegan

# Keep PDFBox Android classes (needed for rendering fonts via reflection)
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

# JCIFS SMB (smb-share module) - uses heavy reflection for SMB network protocols
-keep class jcifs.** { *; }
-dontwarn jcifs.**

# exp4j calculator engine - uses reflection to parse mathematical function names
-keep class net.objecthunter.exp4j.** { *; }
-dontwarn net.objecthunter.exp4j.**

# Coroutines reflection helpers
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

