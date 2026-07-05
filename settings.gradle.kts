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

// Session backends
include(":backends:firebase:kmpauth-firebase-core")
include(":backends:firebase:kmpauth-firebase-google")
include(":backends:firebase:kmpauth-firebase-facebook")

// Backward-compatibility aggregators for 2.x artifacts (folder signals
// legacy status; artifact ids and coordinates are unchanged)
include(":deprecated:kmpauth-firebase")

include(":sampleApp:composeApp")
include(":sampleApp:androidApp")
