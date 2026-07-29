package com.mmk.kmpauth.firebase

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.app
import dev.gitlive.firebase.initialize

internal actual fun initializeFirebasePlatform(options: FirebaseBackendOptions) {
    // Idempotent: keep an already-initialized default app.
    val alreadyInitialized = runCatching { Firebase.app }.isSuccess
    if (alreadyInitialized) return
    Firebase.initialize(
        options = FirebaseOptions(
            applicationId = options.applicationId,
            apiKey = options.apiKey,
            projectId = options.projectId,
            authDomain = options.authDomain,
        )
    )
}
