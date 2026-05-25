plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.anegan.core.conversion"
    compileSdk = 34

    defaultConfig {
        minSdk = 29
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    // Note: To keep the project buildable without downloading massive native binaries immediately,
    // FFmpegKit and PDF libraries are commented out but will be added here for full implementation.
    // implementation("com.arthenica:ffmpeg-kit-full:5.1")
    // implementation("org.apache.pdfbox:pdfbox-android:2.0.27.0")
}
