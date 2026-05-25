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
