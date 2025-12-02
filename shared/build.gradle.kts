import org.gradle.internal.os.OperatingSystem
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

    // ✅ Define iOS targets and configure XCFramework output
    val iosTargets = listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    )

    val xcf = XCFramework(xcfName)

    iosTargets.forEach {
        it.binaries.framework {
            baseName = xcfName
            isStatic = false
            xcf.add(this)
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

tasks.register<Exec>("createXCFramework") {
    group = "build"
    description = "Builds the shared iOS XCFramework (KMP)"

    val gradlewPath = layout.projectDirectory.file("../gradlew").asFile.absolutePath

    commandLine(gradlewPath, ":shared:assembleXCFramework")

}



