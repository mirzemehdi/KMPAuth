import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
}

kotlin {

    android {
        namespace = "com.mmk.kmpauth.sample.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm("desktop")

    js(IR) {
        browser()
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.forEach {
            binary ->
                binary.linkerOpts.addAll(listOf("-framework", "FirebaseCore"))
        }
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }
    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
            implementation(libs.compose.ui)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.facebookAuthAndroid)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            @OptIn(ExperimentalComposeLibrary::class)
            implementation(compose.components.resources)
            implementation(project(":providers:kmpauth-google"))
            implementation(project(":deprecated:kmpauth-firebase"))
            implementation(project(":backends:firebase:kmpauth-firebase-facebook"))
            implementation(project(":kmpauth-uihelper"))
        }
        desktopMain.dependencies {
            implementation(compose.desktop.common)
        }
    }
}
