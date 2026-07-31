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
    alias(libs.plugins.mavenPublish) apply false
    alias(libs.plugins.kotlinx.binary.validator)
}


apiValidation {
    @OptIn(kotlinx.validation.ExperimentalBCVApi::class)
    klib {
        enabled = true
    }
    ignoredProjects += "sampleApp"
    ignoredProjects += "shared"
    ignoredProjects += "androidApp"
    ignoredProjects += "desktopApp"
    ignoredProjects += "webApp"
    ignoredProjects += "providers"
    ignoredProjects += "backends"
    ignoredProjects += "firebase"
    ignoredProjects += "deprecated"
}




allprojects {
    group = "io.github.mirzemehdi"
    version = project.properties["kmpAuthVersion"] as String

    // Sample modules plus the synthetic grouping projects created by nested
    // include() paths. The real modules inside them (e.g.
    // :deprecated:kmpauth-firebase-google) DO publish - the deprecated 2.x
    // container shims must stay on Maven Central for 2.x consumers.
    val excludedModules = listOf(
        ":sampleApp:shared", ":sampleApp:androidApp", ":sampleApp:desktopApp", ":sampleApp:webApp", ":sampleApp",
        ":providers", ":backends", ":backends:firebase", ":deprecated",
    )
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
    dokka(project(":providers:kmpauth-google"))
    dokka(project(":providers:kmpauth-facebook"))
    dokka(project(":backends:firebase:kmpauth-firebase"))
    dokka(project(":deprecated:kmpauth-firebase-google"))
    dokka(project(":deprecated:kmpauth-firebase-facebook"))
    dokka(project(":kmpauth-uihelper"))
}

// Force patched versions of vulnerable npm packages in the Kotlin/JS dev
// toolchain (Dependabot alerts on kotlin-js-store/*.lock). Dev-server/test
// tooling only - nothing here ships in the published artifacts. Revisit on
// Kotlin upgrades; drop entries once the toolchain's own ranges catch up.
// (brace-expansion is pinned across two majors by different tools and can't
// be forced to a single version; its DoS advisories only affect local
// tooling input.)
plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
    the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().apply {
        resolution("webpack-dev-server", "5.2.6")
        resolution("webpack", "5.104.1")
        resolution("ws", "8.21.0")
        resolution("undici", "6.27.0")
        resolution("fast-uri", "3.1.4")
        resolution("body-parser", "1.20.6")
        resolution("serialize-javascript", "7.0.7")
        resolution("uuid", "11.1.1")
        resolution("diff", "8.0.3")
    }
}
plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin> {
    the<org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootExtension>().apply {
        resolution("webpack", "5.104.1")
        resolution("ws", "8.21.0")
        resolution("serialize-javascript", "7.0.7")
    }
}
