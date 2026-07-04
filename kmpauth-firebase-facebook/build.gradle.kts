plugins {
    id("kmpauth.kmp.library")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    cocoapods {
        ios.deploymentTarget = "12.0"
        framework {
            baseName = "KMPAuthFirebaseFacebook"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(libs.koin.compose)
            api(project(":kmpauth-core"))
            api(project(":kmpauth-firebase"))
            api(project(":kmpauth-facebook"))
        }
    }
}
