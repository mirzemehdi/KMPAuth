plugins {
    id("kmpauth.kmp.library")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    cocoapods {
        ios.deploymentTarget = "12.0"
        framework {
            baseName = "KMPAuthFirebaseCore"
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {
        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(libs.koin.compose)
            api(libs.firebase.gitlive.auth)
            api(project(":kmpauth-core"))
            implementation(project(":kmpauth-google"))
        }
    }
}
