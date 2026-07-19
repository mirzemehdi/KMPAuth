import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SourcesJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * Convention plugin shared by every published kmpauth-* library module.
 *
 * Centralizes: the full target set (android/jvm/js/wasmJs/ios), explicit API
 * mode, Android library configuration via the AGP 9 KMP library plugin
 * (namespace derived from the module name), the shared kotlin-test
 * dependency, and Maven Central publishing with the common POM.
 *
 * Modules keep only what genuinely differs: the iOS framework name, the
 * swiftPMDependencies block (external Apple frameworks), their dependencies,
 * and any custom source-set wiring (e.g. the firebase modules' nonWasmMain).
 */

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("com.vanniktech.maven.publish")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

// kmpauth-firebase-facebook -> com.mmk.kmpauth.firebase.facebook
val moduleNamespace = "com.mmk.kmpauth." +
    project.name.removePrefix("kmpauth-").replace("-", ".")

kotlin {
    explicitApi()

    android {
        namespace = moduleNamespace
        compileSdk = libs.findVersion("android-compileSdk").get().requiredVersion.toInt()
        minSdk = libs.findVersion("android-minSdk").get().requiredVersion.toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }

        withHostTest { }

        // A module ships consumer R8 rules by dropping a consumer-rules.pro
        // next to its build file; they are then published with the artifact so
        // apps do not have to copy them into their own configuration.
        val consumerRules = project.file("consumer-rules.pro")
        if (consumerRules.exists()) {
            optimization {
                consumerKeepRules.file(consumerRules)
                consumerKeepRules.publish = true
            }
        }

        packaging {
            resources {
                excludes.add("/META-INF/AL2.0")
                excludes.add("/META-INF/LGPL2.1")
            }
        }
    }

    js {
        nodejs()
        browser()
        binaries.library()
    }
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets.commonTest.dependencies {
        implementation(libs.findLibrary("kotlin-test").get().get())
    }
}

mavenPublishing {
    configure(
        KotlinMultiplatform(
            // Dokka 2.x (V2 mode) task name; the V1 dokkaHtml task no longer exists.
            javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml"),
            sourcesJar = SourcesJar.Sources()
        )
    )
    coordinates(
        "io.github.mirzemehdi",
        project.name,
        project.properties["kmpAuthVersion"] as String
    )
    pom {
        name = "KMPAuth"
        description = " Kotlin Multiplatform Authentication Library targeting ios and android"
        url = "https://github.com/mirzemehdi/KMPAuth/"
        licenses {
            license {
                name.set("Apache-2.0")
                url.set("https://opensource.org/licenses/Apache-2.0")
            }
        }
        developers {
            developer {
                name.set("Mirzamehdi Karimov")
                email.set("mirzemehdi@gmail.com")
            }
        }
        scm {
            connection.set("https://github.com/mirzemehdi/KMPAuth.git")
            url.set("https://github.com/mirzemehdi/KMPAuth")
        }
        issueManagement {
            system.set("Github")
            url.set("https://github.com/mirzemehdi/KMPAuth/issues")
        }
    }

    publishToMavenCentral()
    signAllPublications()
}
