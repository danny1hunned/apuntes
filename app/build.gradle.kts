plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.apuntes"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.apuntes"
        minSdk = 26
        targetSdk = 36
        versionCode = 12
        versionName = "2.0"
        vectorDrawables.useSupportLibrary = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // --- Android core ---
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.recyclerview:recyclerview:1.4.0")

    // --- Google Ads (only one clean version)
    implementation("com.google.android.gms:play-services-ads:24.7.0")

    // --- Kotlinx Serialization (JSON) ---
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    // --- Gson (optional) ---
    implementation("com.google.code.gson:gson:2.13.2")

    // --- Confetti animation ---
    implementation("nl.dionsegijn:konfetti-xml:2.0.5")
}
