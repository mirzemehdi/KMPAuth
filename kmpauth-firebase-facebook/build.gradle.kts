@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)

plugins {
    id("kmpauth.kmp.library")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "KMPAuthFirebaseFacebook"
            isStatic = true
        }
    }

    swiftPMDependencies {
        iosMinimumDeploymentTarget = "16.0"
        // Link-time closure of this module's binaries: GitLive Firebase
        // cinterop and kmpauth-facebook's FBSDK cinterop (GoogleSignIn is
        // not referenced by anything this module links beyond what
        // kmpauth-firebase already declares).
        swiftPackage(
            url = "https://github.com/firebase/firebase-ios-sdk.git",
            version = libs.versions.firebaseIosSdk.get(),
            products = listOf("FirebaseAuth", "FirebaseCore")
        )
        swiftPackage(
            url = "https://github.com/facebook/facebook-ios-sdk.git",
            version = libs.versions.facebookAuthIos.get(),
            products = listOf("FacebookCore", "FacebookLogin")
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
            implementation(compose.material)
            api(project(":kmpauth-core"))
            api(project(":kmpauth-firebase"))
            api(project(":kmpauth-facebook"))
        }
    }
}
