rootProject.name = "KMPAuthLib"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

include(":kmpauth-core")
include(":kmpauth-uihelper")

// Identity providers (credential sources, no backend dependencies)
include(":providers:kmpauth-google")
include(":providers:kmpauth-facebook")
include(":providers:kmpauth-apple")

// Session backends
include(":backends:firebase:kmpauth-firebase")
include(":backends:supabase:kmpauth-supabase")

// Backward-compatibility shims for the deprecated 2.x container composables
// (folder signals legacy status; artifact ids and coordinates are unchanged).
// Removal planned for 4.0.
include(":deprecated:kmpauth-firebase-google")
include(":deprecated:kmpauth-firebase-facebook")

include(":sampleApp:shared")
include(":sampleApp:androidApp")
include(":sampleApp:desktopApp")
include(":sampleApp:webApp")
