import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.android.lint)
}

kotlin {
    androidLibrary {
        namespace = "com.apuntes.shared"
        compileSdk = 36
        minSdk = 24
    }

    val xcfName = "sharedKit"
    val xcf = XCFramework(xcfName)

    // ✅ Explicitly define iOS targets (Kotlin 2.x style)
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        if (konanTarget.family.isAppleFamily) {
            binaries.framework {
                baseName = xcfName
                isStatic = false
                xcf.add(this)
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
            }
        }

        val androidMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
            }
        }
    }
}

// ✅ Define build entry task for XCFramework
tasks.register("createXCFramework") {
    group = "build"
    description = "Builds the iOS XCFramework from KMP targets"
    dependsOn("assembleSharedKitXCFramework")
}
