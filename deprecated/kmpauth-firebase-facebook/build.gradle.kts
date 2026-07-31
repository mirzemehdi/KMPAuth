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
        // Closed range: SPM's `from` caps at <12.0.0, conflicting with
        // consumer apps already on firebase-ios-sdk 12.x.
        swiftPackage(
            url("https://github.com/firebase/firebase-ios-sdk.git"),
            range(libs.versions.firebaseIosSdk.get(), "12.999.999"),
            listOf(product("FirebaseAuth"), product("FirebaseCore"))
        )
        swiftPackage(
            url = "https://github.com/facebook/facebook-ios-sdk.git",
            version = libs.versions.facebookAuthIos.get(),
            products = listOf("FacebookCore", "FacebookLogin")
        )
    }

    sourceSets {
        // Same shape as kmpauth-firebase: the public API lives in
        // commonMain (KMPAuthUser-based) so wasm consumers can call it; the
        // GitLive-facing exchange and the deprecated 2.x container stay in
        // nonWasmMain.
        val nonWasmMain by creating {
            dependsOn(commonMain.get())
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
            api(project(":providers:kmpauth-facebook"))
            api(project(":backends:firebase:kmpauth-firebase"))
        }
    }
}
