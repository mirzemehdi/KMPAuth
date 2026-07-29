@file:OptIn(KMPAuthInternalApi::class)

package com.mmk.kmpauth.firebase

import com.google.firebase.FirebasePlatform
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.logger.currentLogger
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.app
import dev.gitlive.firebase.initialize
import java.util.concurrent.ConcurrentHashMap

internal actual fun initializeFirebasePlatform(options: FirebaseBackendOptions) {
    // Idempotent: keep an already-initialized default app.
    val alreadyInitialized = runCatching { Firebase.app }.isSuccess
    if (alreadyInitialized) return
    // The firebase-java-sdk requires a FirebasePlatform (key/value storage +
    // logging hooks) before any Firebase call; without it initialization
    // NPEs. Provide an in-memory default unless the app installed its own.
    if (!firebasePlatformInstalled()) {
        FirebasePlatform.initializeFirebasePlatform(object : FirebasePlatform() {
            private val storage = ConcurrentHashMap<String, String>()
            override fun store(key: String, value: String) {
                storage[key] = value
            }

            override fun retrieve(key: String): String? = storage[key]

            override fun clear(key: String) {
                storage.remove(key)
            }

            override fun log(msg: String) {
                currentLogger.log(msg)
            }
        })
    }
    Firebase.initialize(
        // firebase-java-sdk emulates the Android SDK; its stub Application
        // is the expected "context" on the JVM.
        context = android.app.Application(),
        options = FirebaseOptions(
            applicationId = options.applicationId,
            apiKey = options.apiKey,
            projectId = options.projectId,
            authDomain = options.authDomain,
        )
    )
}

private fun firebasePlatformInstalled(): Boolean =
    // `firebasePlatform` is an internal lateinit; probing any Firebase API
    // before installation throws, so a cheap reflective check is the only
    // non-throwing way to detect an app-installed platform.
    runCatching {
        val field = FirebasePlatform::class.java.getDeclaredField("firebasePlatform")
        field.isAccessible = true
        field.get(FirebasePlatform.Companion) != null
    }.getOrElse { false }
