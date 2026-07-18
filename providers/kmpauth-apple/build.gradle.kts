@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)

plugins {
    id("kmpauth.kmp.library")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
}

/*
 * Sign in with Apple as a standalone provider (no backend required).
 *
 * Apple's native flow is only available on Apple platforms, where
 * AuthenticationServices hands back an identity token (a JWT) that any backend
 * can verify against Apple's public keys - no client secret is needed on the
 * client. Other platforms would require Apple's web OAuth flow, whose
 * authorization code must be exchanged server-side using a client secret, so
 * they are intentionally no-op stubs here. Use the Firebase Apple containers in
 * kmpauth-firebase-core if you need Apple Sign-In on non-Apple targets.
 *
 * No SwiftPM dependency: AuthenticationServices ships with the iOS SDK.
 */
kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "KMPAuthApple"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            api(project(":kmpauth-core"))
        }
    }
}
