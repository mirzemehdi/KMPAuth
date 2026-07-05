plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidKmpLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.dokka)
    alias(libs.plugins.googleServices) apply false
    alias(libs.plugins.kotlinx.serialization).apply(false)
    alias(libs.plugins.mavenPublish) apply false
    alias(libs.plugins.kotlinx.binary.validator)
}


apiValidation {
    @OptIn(kotlinx.validation.ExperimentalBCVApi::class)
    klib {
        enabled = true
    }
    ignoredProjects += "sampleApp"
    ignoredProjects += "composeApp"
    ignoredProjects += "androidApp"
}




allprojects {
    group = "io.github.mirzemehdi"
    version = project.properties["kmpAuthVersion"] as String

    val excludedModules = listOf(":sampleApp:composeApp", ":sampleApp:androidApp", ":sampleApp")
    if (project.path in excludedModules) return@allprojects

    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

}

// Dokka 2.x aggregated documentation site (replaces the removed V1
// dokkaHtmlMultiModule task). CI publishes the output of
// :dokkaGeneratePublicationHtml to GitHub Pages.
dependencies {
    dokka(project(":kmpauth-core"))
    dokka(project(":kmpauth-google"))
    dokka(project(":kmpauth-facebook"))
    dokka(project(":kmpauth-firebase"))
    dokka(project(":kmpauth-firebase-facebook"))
    dokka(project(":kmpauth-uihelper"))
}
