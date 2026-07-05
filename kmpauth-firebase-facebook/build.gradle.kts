@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)

plugins {
    id("kmpauth.kmp.library")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    // Custom dependsOn edges (nonWasmMain) suppress the default hierarchy
    // template; re-apply it so iosMain stays wired to the ios targets.
    applyDefaultHierarchyTemplate()

    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "KMPAuthFirebaseFacebook"
            isStatic = true
        }
    }

    swiftPMDependencies {
        iosMinimumDeploymentTarget = "16.0"
        // Link-time closure of this module's binaries: GitLive Firebase
        // cinterop and kmpauth-facebook's FBSDK cinterop (GoogleSignIn is
        // not referenced by anything this module links beyond what
        // kmpauth-firebase already declares).
        swiftPackage(
            url = "https://github.com/firebase/firebase-ios-sdk.git",
            version = libs.versions.firebaseIosSdk.get(),
            products = listOf("FirebaseAuth", "FirebaseCore")
        )
        swiftPackage(
            url = "https://github.com/facebook/facebook-ios-sdk.git",
            version = libs.versions.facebookAuthIos.get(),
            products = listOf("FacebookCore", "FacebookLogin")
        )
    }

    sourceSets {
        // kmpauth-firebase exposes its Firebase API only on non-wasm
        // targets (GitLive has no wasm); this module mirrors that shape.
        val nonWasmMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                api(project(":kmpauth-firebase"))
            }
        }
        androidMain.get().dependsOn(nonWasmMain)
        iosMain.get().dependsOn(nonWasmMain)
        jvmMain.get().dependsOn(nonWasmMain)
        jsMain.get().dependsOn(nonWasmMain)

        androidMain.dependencies {
            // GitLive's Android artifacts declare Firebase dependencies without
            // versions; the BoM must be on the classpath to pin them. Exposed
            // as api so consumers of this module inherit the constraints.
            api(project.dependencies.platform(libs.firebase.android.bom))
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            api(project(":kmpauth-core"))
            api(project(":kmpauth-facebook"))
        }
    }
}
