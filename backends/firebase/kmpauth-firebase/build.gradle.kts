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
            baseName = "KMPAuthFirebase"
            isStatic = true
        }
    }

    swiftPMDependencies {
        iosMinimumDeploymentTarget = "16.0"
        // GitLive's bundled FirebaseAuth cinterop resolves against the real
        // Firebase frameworks at link time. A range instead of from(): SPM's
        // `from` caps at <12.0.0, conflicting with consumer apps already on
        // firebase-ios-sdk 12.x. range() renders as a CLOSED SPM range, so
        // the upper bound 12.999.999 admits every 12.x and keeps 13.0.0 out.
        swiftPackage(
            url("https://github.com/firebase/firebase-ios-sdk.git"),
            range(libs.versions.firebaseIosSdk.get(), "12.999.999"),
            listOf(product("FirebaseAuth"), product("FirebaseCore"))
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

        // The Firebase Auth REST engine (Identity Toolkit) is shared by
        // Desktop (JVM, where firebase-java-sdk has no auth, #204) and wasm
        // (where the Firebase SDK has no target at all). Platform leaves
        // supply the HTTP transport: JDK HttpClient on JVM, fetch on wasm —
        // deliberately no Ktor (#78).
        val restEngineMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                // JSON runtime API only — JsonObject builders/parsing, no
                // @Serializable, no compiler plugin.
                implementation(libs.kotlinx.serialization.json)
            }
        }
        jvmMain.get().dependsOn(restEngineMain)
        wasmJsMain.get().dependsOn(restEngineMain)

        jvmTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
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
