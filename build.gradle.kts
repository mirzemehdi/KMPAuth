import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
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
    alias(libs.plugins.kotlinx.binary.validator)
    alias(libs.plugins.nexusPublish)
}




allprojects {
    group = "io.github.mirzemehdi"
    version = project.properties["kmpAuthVersion"] as String
    
    val gpgKeySecret = gradleLocalProperties(rootDir, providers).getProperty("gpgKeySecret")
    val gpgKeyPassword = gradleLocalProperties(rootDir, providers).getProperty("gpgKeyPassword")

    val excludedModules = listOf(":sampleApp:composeApp", ":sampleApp")
    if (project.path in excludedModules) return@allprojects

    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    extensions.configure<PublishingExtension> {
        val javadocJar = tasks.register<Jar>("javadocJar") {
            dependsOn(tasks.getByName<DokkaTask>("dokkaHtml"))
            archiveClassifier.set("javadoc")
            from("${layout.buildDirectory}/dokka")
        }

        publications {
            withType<MavenPublication> {
                artifact(javadocJar)
                pom {
                    groupId = "io.github.mirzemehdi"
                    name.set("KMPAuth")
                    description.set(" Kotlin Multiplatform Authentication Library targeting ios and android")
                    licenses {
                        license {
                            name.set("Apache-2.0")
                            url.set("https://opensource.org/licenses/Apache-2.0")
                        }
                    }
                    url.set("mirzemehdi.github.io/KMPAuth/")
                    issueManagement {
                        system.set("Github")
                        url.set("https://github.com/mirzemehdi/KMPAuth/issues")
                    }
                    scm {
                        connection.set("https://github.com/mirzemehdi/KMPAuth.git")
                        url.set("https://github.com/mirzemehdi/KMPAuth")
                    }
                    developers {
                        developer {
                            name.set("Mirzamehdi Karimov")
                            email.set("mirzemehdi@gmail.com")
                        }
                    }
                }
            }
        }
    }

    val publishing = extensions.getByType<PublishingExtension>()
    extensions.configure<SigningExtension> {
        useInMemoryPgpKeys(gpgKeySecret, gpgKeyPassword)
        sign(publishing.publications)
    }

    // TODO: remove after https://youtrack.jetbrains.com/issue/KT-46466 is fixed
    project.tasks.withType(AbstractPublishToMaven::class.java).configureEach {
        dependsOn(project.tasks.withType(Sign::class.java))
    }
}
nexusPublishing {
    repositories {
        sonatype {  //only for users registered in Sonatype after 24 Feb 2021
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            val sonatypeUsername = gradleLocalProperties(rootDir, providers).getProperty("sonatypeUsername")
            val sonatypePassword = gradleLocalProperties(rootDir, providers).getProperty("sonatypePassword")
            username = sonatypeUsername
            password = sonatypePassword
        }
    }
}


