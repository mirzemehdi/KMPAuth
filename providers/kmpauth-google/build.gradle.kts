@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)

plugins {
    id("kmpauth.kmp.library")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "KMPAuthGoogle"
            isStatic = true
        }
    }

    swiftPMDependencies {
        iosMinimumDeploymentTarget = "16.0"
        swiftPackage(
            url = "https://github.com/google/GoogleSignIn-iOS.git",
            version = libs.versions.googleSignInIos.get(),
            products = listOf("GoogleSignIn")
        )
    }

    sourceSets {

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.playServicesAuth)
            implementation(libs.android.legacy.playServicesAuth)
            implementation(libs.googleIdIdentity)

        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(libs.ktor.core)
            api(project(":kmpauth-core"))
        }
        jvmMain.dependencies {
            implementation(libs.ktor.server.core)
            implementation(libs.ktor.server.netty)
            implementation(libs.ktor.server.html.builder)
            implementation(libs.java.jwt)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
