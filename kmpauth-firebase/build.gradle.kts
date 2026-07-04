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
            // GitLive's Android artifacts declare Firebase dependencies without
            // versions; the BoM must be on the classpath to pin them. Exposed
            // as api so consumers of this module inherit the constraints.
            api(project.dependencies.platform(libs.firebase.android.bom))
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
