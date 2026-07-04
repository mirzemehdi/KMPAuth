import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("kmpauth.kmp.library")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    cocoapods {
        ios.deploymentTarget = "12.0"
        framework {
            baseName = "KMPAuthFacebook"
            isStatic = true
        }
        pod("FBSDKCoreKit") {
            extraOpts += listOf("-compiler-option", "-fmodules")
            version = libs.versions.facebookAuthIos.get()
        }
        pod("FBSDKLoginKit") {
            extraOpts += listOf("-compiler-option", "-fmodules")
            version = libs.versions.facebookAuthIos.get()
        }
    }

    sourceSets {

        androidMain.dependencies {
            implementation(libs.facebookAuthAndroid)
        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(libs.koin.compose)
            api(project(":kmpauth-core"))
        }
    }
}
