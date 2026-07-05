@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)

plugins {
    id("kmpauth.kmp.library")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "KMPAuthFacebook"
            isStatic = true
        }
    }

    swiftPMDependencies {
        iosMinimumDeploymentTarget = "16.0"
        swiftPackage(
            url = "https://github.com/facebook/facebook-ios-sdk.git",
            version = libs.versions.facebookAuthIos.get(),
            products = listOf("FacebookCore", "FacebookLogin")
        )
    }

    sourceSets {

        androidMain.dependencies {
            implementation(libs.facebookAuthAndroid)
            implementation(libs.androidx.activity.ktx)
        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            api(project(":kmpauth-core"))
        }
    }
}
