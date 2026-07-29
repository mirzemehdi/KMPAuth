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
            baseName = "KMPAuthFirebaseCore"
            isStatic = true
        }
    }

    swiftPMDependencies {
        iosMinimumDeploymentTarget = "16.0"
        // GitLive's bundled FirebaseAuth cinterop (imported as
        // cocoapods.FirebaseAuth.*) resolves against the real Firebase
        // frameworks at link time; the version must match what GitLive
        // was built against.
        swiftPackage(
            url = "https://github.com/firebase/firebase-ios-sdk.git",
            version = libs.versions.firebaseIosSdk.get(),
            products = listOf("FirebaseAuth", "FirebaseCore")
        )
    }

    sourceSets {
        // GitLive firebase-auth has no wasm target. The public API lives in
        // commonMain using KMPAuth's own types (KMPAuthUser), so wasm
        // consumers can call it from their commonMain; everything touching
        // GitLive types sits in nonWasmMain (android/ios/jvm/js), and the
        // wasmJs actuals report failed Results.
        val nonWasmMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                api(libs.firebase.gitlive.auth)
            }
        }
        androidMain.get().dependsOn(nonWasmMain)
        iosMain.get().dependsOn(nonWasmMain)
        jvmMain.get().dependsOn(nonWasmMain)
        jsMain.get().dependsOn(nonWasmMain)

        jvmTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }

        jvmMain.dependencies {
            // JSON for the Firebase Auth REST engine (runtime API only —
            // JsonObject builders/parsing, no @Serializable, no compiler
            // plugin). The engine itself uses the JDK's built-in HTTP
            // client, deliberately not Ktor (#78).
            implementation(libs.kotlinx.serialization.json)
        }

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
            api(project(":kmpauth-core"))
            // Native Sign in with Apple; the iOS Firebase flow reuses this
            // module's ASAuthorization credential acquisition.
            api(project(":providers:kmpauth-apple"))
        }
    }
}
