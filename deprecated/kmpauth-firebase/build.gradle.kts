plugins {
    id("kmpauth.kmp.library")
}

/*
 * Backward-compatibility aggregator: 2.x consumers depend on
 * io.github.mirzemehdi:kmpauth-firebase and get the full Firebase feature
 * set. New code can depend on the granular artifacts instead —
 * kmpauth-firebase-core (backend + Apple/GitHub/OAuth containers) and
 * kmpauth-firebase-google (Google container) — e.g. to avoid pulling the
 * Google Sign-In stack into a GitHub-only app.
 */
kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":backends:firebase:kmpauth-firebase-core"))
            api(project(":backends:firebase:kmpauth-firebase-google"))
        }
    }
}
