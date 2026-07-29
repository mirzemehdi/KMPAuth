plugins {
    id("kmpauth.kmp.library")
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "KMPAuthSupabase"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":kmpauth-core"))
            // supabase-kt supports every KMPAuth target (android/ios/jvm/js/
            // wasmJs), so unlike the firebase modules no nonWasmMain split is
            // needed. It is built on Ktor but ships no engine — consumers add
            // the Ktor client engine for their platform themselves (a
            // supabase-kt requirement, not a KMPAuth one; the "no Ktor" rule
            // in CLAUDE.md is about KMPAuth's own HTTP usage).
            api(libs.supabase.auth)
        }

        jvmTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            // Test-only Ktor MockEngine to exercise the backend against
            // canned GoTrue responses without network. Never shipped.
            implementation(libs.ktor.client.mock)
        }
    }
}
