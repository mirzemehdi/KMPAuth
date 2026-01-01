rootProject.name = "KMPAuthLib"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
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
include(":kmpauth-google")
include(":kmpauth-facebook")
include(":kmpauth-firebase")
include(":kmpauth-firebase-facebook")
include(":kmpauth-uihelper")
include(":sampleApp:composeApp")