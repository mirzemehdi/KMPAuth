@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)

plugins {
    id("kmpauth.kmp.library")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "KMPAuthFirebaseCore"
            isStatic = true
        }
    }

    swiftPMDependencies {
        iosMinimumDeploymentTarget = "16.0"
        // GitLive's bundled FirebaseAuth cinterop (imported as
        // cocoapods.FirebaseAuth.*) resolves against the real Firebase
        // frameworks at link time; the version must match what GitLive
        // was built against.
        swiftPackage(
            url = "https://github.com/firebase/firebase-ios-sdk.git",
            version = libs.versions.firebaseIosSdk.get(),
            products = listOf("FirebaseAuth", "FirebaseCore")
        )
        // kmpauth-google's GoogleSignIn cinterop is linked into this
        // module's binaries (test executable, framework).
        swiftPackage(
            url = "https://github.com/google/GoogleSignIn-iOS.git",
            version = libs.versions.googleSignInIos.get(),
            products = listOf("GoogleSignIn")
        )
    }

    sourceSets {
        androidMain.dependencies {
            // GitLive's Android artifacts declare Firebase dependencies without
            // versions; the BoM must be on the classpath to pin them. Exposed
            // as api so consumers of this module inherit the constraints.
            api(project.dependencies.platform(libs.firebase.android.bom))
        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            api(libs.firebase.gitlive.auth)
            api(project(":kmpauth-core"))
            implementation(project(":kmpauth-google"))
        }
    }
}
