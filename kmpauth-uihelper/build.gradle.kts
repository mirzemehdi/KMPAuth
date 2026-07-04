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
        ios.deploymentTarget = "11.0"
        framework {
            baseName = "KMPAuthUiHelper"
            isStatic = true
        }
        noPodspec()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.components.resources)
            implementation(project(":kmpauth-core"))
        }
    }
}

android {
    // Compose resources also live under commonMain/resources for this module.
    sourceSets["main"].res.srcDirs("src/androidMain/res", "src/commonMain/resources")
}
