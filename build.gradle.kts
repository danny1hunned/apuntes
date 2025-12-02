// ✅ Root-level build.gradle.kts — clean and stable
plugins {
    // Use either aliases OR explicit ids — not both!
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false

    // Firebase & Google services
    id("com.google.gms.google-services") version "4.4.4" apply false
    id("com.google.firebase.firebase-perf") version "2.0.2" apply false

    // ✅ Add Kotlin serialization plugin
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21" apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.android.lint) apply false
}
