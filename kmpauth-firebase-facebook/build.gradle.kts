import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinNativeCocoaPods)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.mavenPublish)
}

kotlin {
    explicitApi()
    androidTarget {
        publishLibraryVariants("release", "debug")
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }
    js(IR) {
        nodejs()
        browser()
        binaries.library()
    }
    jvm()

    iosArm64()
    iosSimulatorArm64()


    cocoapods {
        ios.deploymentTarget = "12.0"
        framework {
            baseName = "KMPAuthFirebaseFacebook"
            isStatic = true
        }
    }



    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(libs.koin.compose)
            api(project(":kmpauth-core"))
            api(project(":kmpauth-firebase"))
            api(project(":kmpauth-facebook"))
        }
    }
}

android {
    namespace = "com.mmk.kmpauth.firebase.facebook"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

mavenPublishing {
    configure(
        KotlinMultiplatform(
            javadocJar = JavadocJar.Dokka("dokkaHtml"),
            sourcesJar = true
        )
    )
    coordinates(
        "io.github.mirzemehdi",
        "kmpauth-firebase-facebook",
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

