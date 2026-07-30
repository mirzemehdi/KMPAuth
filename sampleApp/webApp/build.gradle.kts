import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    js(IR) {
        outputModuleName.set("webApp")
        browser {
            commonWebpackConfig {
                outputFileName = "webApp.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    port = 8082 // TEMP-E2E: Docker holds 8080 on this machine
                    static = (static ?: mutableListOf()).apply {
                        // Serve sources to debug inside browser
                        add(project.projectDir.path)
                    }
                }
            }
        }
        binaries.executable()
    }

    // Wasm variant of the same sample. Firebase sign-in states are callable
    // from commonMain here too; on wasm they report failed Results (the
    // Firebase SDK has no wasm target), while Google sign-in works natively.
    // Run with :sampleApp:webApp:wasmJsBrowserDevelopmentRun.
    wasmJs {
        outputModuleName.set("webAppWasm")
        browser {
            commonWebpackConfig {
                outputFileName = "webAppWasm.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    // The js variant's dev server uses the default 8080.
                    port = 8082
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        jsMain.dependencies {
            implementation(project(":sampleApp:shared"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
        }
        wasmJsMain.dependencies {
            implementation(project(":sampleApp:shared"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            // kotlinx.browser (document) for the wasm entry point.
            implementation(libs.kotlinx.browser)
        }
    }
}
