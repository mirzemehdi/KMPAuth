plugins {
    `kotlin-dsl`
}

dependencies {
    // Puts the third-party plugins on the classpath of the precompiled script
    // plugins so they can be applied by id.
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.android.gradle.plugin)
    implementation(libs.vanniktech.maven.publish.plugin)
}
