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
        }
    }
}
