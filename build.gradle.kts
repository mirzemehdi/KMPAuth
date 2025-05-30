import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinNativeCocoaPods) apply false
    alias(libs.plugins.dokka) apply false
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
}




allprojects {
    group = "io.github.mirzemehdi"
    version = project.properties["kmpAuthVersion"] as String

    val excludedModules = listOf(":sampleApp:composeApp", ":sampleApp")
    if (project.path in excludedModules) return@allprojects

    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

}


