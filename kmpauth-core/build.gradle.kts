plugins {
    id("kmpauth.kmp.library")
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "KMPAuthCore"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.runtime)
        }

        androidMain.dependencies {
            implementation(libs.androidx.startup.runtime)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.ktx)
        }

        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
