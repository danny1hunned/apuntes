import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.apple-privacy-manifests") version "1.0.0"
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.android.lint)
}

kotlin {
    privacyManifest {
        embed(privacyManifest = layout.projectDirectory.file("PrivacyInfo.xcprivacy").asFile)
    }

    android {
        namespace = "com.apuntes.shared"
        compileSdk = 36
        minSdk = 24

        withHostTest {}
        withDeviceTest {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    val xcfName = "sharedKit"
    val xcf = XCFramework(xcfName)

    // ✅ Explicitly define iOS targets (forces Gradle to generate iOS tasks)
    val iosArm64Target = iosArm64()
    val iosSimulatorArm64Target = iosSimulatorArm64()
    val iosX64Target = iosX64()

    listOf(iosArm64Target, iosSimulatorArm64Target, iosX64Target).forEach {
        it.binaries.framework {
            baseName = xcfName
            isStatic = false
            xcf.add(this)
        }
    }

    sourceSets {
        getByName("commonTest") {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        val commonMain = getByName("commonMain") {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
            }
        }

        getByName("androidMain") {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
            }
        }

        getByName("androidHostTest") {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.junit)
            }
        }

        getByName("androidDeviceTest") {
            dependencies {
                implementation(libs.androidx.runner)
                implementation(libs.androidx.junit)
            }
        }

        // ✅ iOS source sets configuration
        val iosMain = create("iosMain") {
            dependsOn(commonMain)
        }
        getByName("iosArm64Main") {
            dependsOn(iosMain)
        }
        getByName("iosSimulatorArm64Main") {
            dependsOn(iosMain)
        }
        getByName("iosX64Main") {
            dependsOn(iosMain)
        }
    }
}

// ✅ Force creation of iOS release framework tasks
tasks.register("buildIosFrameworks") {
    group = "build"
    description = "Builds iOS release frameworks for XCFramework"
    dependsOn(
        ":shared:linkReleaseFrameworkIosArm64",
        ":shared:linkReleaseFrameworkIosSimulatorArm64",
        ":shared:linkReleaseFrameworkIosX64"
    )
}

// Preserve the existing output location using Kotlin's XCFramework packaging task.
tasks.register<Sync>("createXCFrameworkFinal") {
    group = "build"
    description = "Build and package the sharedKit XCFramework"

    dependsOn("assembleSharedKitReleaseXCFramework")
    from(layout.buildDirectory.dir("XCFrameworks/release/sharedKit.xcframework"))
    into(layout.buildDirectory.dir("XCFrameworks/releaseSharedKit/sharedKit.xcframework"))
    onlyIf("XCFramework packaging requires macOS and Xcode") { HostManager.hostIsMac }
}
