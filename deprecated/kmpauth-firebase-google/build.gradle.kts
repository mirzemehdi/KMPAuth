@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)

plugins {
    id("kmpauth.kmp.library")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    // Custom dependsOn edges (nonWasmMain) suppress the default hierarchy
    // template; re-apply it so iosMain stays wired to the ios targets.
    applyDefaultHierarchyTemplate()

    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "KMPAuthFirebaseGoogle"
            isStatic = true
        }
    }

    swiftPMDependencies {
        iosMinimumDeploymentTarget = "16.0"
        // Link-time closure of this module's binaries: GitLive's Firebase
        // cinterop (via kmpauth-firebase) and kmpauth-google's
        // GoogleSignIn cinterop.
        swiftPackage(
            url = "https://github.com/firebase/firebase-ios-sdk.git",
            version = libs.versions.firebaseIosSdk.get(),
            products = listOf("FirebaseAuth", "FirebaseCore")
        )
        swiftPackage(
            url = "https://github.com/google/GoogleSignIn-iOS.git",
            version = libs.versions.googleSignInIos.get(),
            products = listOf("GoogleSignIn")
        )
    }

    sourceSets {
        // Same shape as kmpauth-firebase: the public API lives in
        // commonMain (KMPAuthUser-based) so wasm consumers can call it; only
        // the deprecated 2.x container, which exposes GitLive's FirebaseUser,
        // stays in nonWasmMain.
        val nonWasmMain by creating {
            dependsOn(commonMain.get())
        }
        androidMain.get().dependsOn(nonWasmMain)
        iosMain.get().dependsOn(nonWasmMain)
        jvmMain.get().dependsOn(nonWasmMain)
        jsMain.get().dependsOn(nonWasmMain)

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            api(project(":kmpauth-core"))
            api(project(":backends:firebase:kmpauth-firebase"))
            api(project(":providers:kmpauth-google"))
        }

        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
