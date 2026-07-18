import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("org.jetbrains.kotlin.jvm")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":sampleApp:shared"))
    implementation(project(":providers:kmpauth-google"))
    implementation(project(":kmpauth-uihelper"))
    implementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "com.mmk.kmpauth.sample.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.mmk.kmpauthdesktop"
            packageVersion = "1.0.0"
            // Google Sign-In runs its OAuth loopback on the JDK's built-in
            // com.sun.net.httpserver, which lives in this module. jlink strips
            // it otherwise and sign-in fails at runtime in packaged builds.
            modules("jdk.httpserver")
        }
    }
}
