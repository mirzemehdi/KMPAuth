import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.googleServices)
}

kotlin {

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }
    jvm("desktop")
    listOf(
        iosArm64(),
        iosX64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.forEach {
            binary ->
                binary.linkerOpts.addAll(listOf("-framework", "FirebaseCore"))
        }
        iosTarget.binaries.framework {
            baseName = "composeApp"
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
            implementation(project(":kmpauth-google"))
            implementation(project(":kmpauth-firebase"))
            implementation(project(":kmpauth-firebase-facebook"))
            implementation(project(":kmpauth-uihelper"))
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}

android {
    namespace = "com.mmk.kmpauth.sample"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        applicationId = "com.mmk.kmpauth.sample"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    dependencies {
        debugImplementation(libs.compose.ui.tooling)
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.mmk.kmpauthdesktop"
            packageVersion = "1.0.0"
        }
    }
}

