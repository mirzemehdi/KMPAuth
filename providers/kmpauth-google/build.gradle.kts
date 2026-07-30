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
            api(project(":kmpauth-core"))
        }
        jvmMain.dependencies {
            // The desktop OAuth loopback runs on the JDK's built-in
            // com.sun.net.httpserver, so no server framework is pulled into
            // consumers' desktop classpath.
            implementation(libs.java.jwt)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
