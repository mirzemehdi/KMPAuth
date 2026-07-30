package com.mmk.kmpauth.firebase

import com.mmk.kmpauth.core.KMPAuthConfiguration

/**
 * Firebase project configuration for the platforms where the Firebase SDK
 * cannot auto-initialize from a bundled config file — Desktop (JVM) and
 * Web (JS). Values come from the Firebase console (Project settings → your
 * web app), the same ones a Firebase web app uses.
 *
 * @param apiKey The project's **web API key**.
 * @param projectId The Firebase project id, e.g. `my-project-1234`.
 * @param applicationId The Firebase app id (`1:...:web:...`).
 * @param authDomain The auth handler domain; defaults to
 * `"$projectId.firebaseapp.com"`.
 */
public data class FirebaseBackendOptions(
    val apiKey: String,
    val projectId: String,
    val applicationId: String,
    val authDomain: String = "$projectId.firebaseapp.com",
)

/**
 * Configures the Firebase backend inside `KMPAuth.initialize { }` for
 * Desktop (JVM) and Web (JS), where Firebase cannot auto-initialize from
 * `google-services.json` / `GoogleService-Info.plist`:
 *
 * ```
 * KMPAuth.initialize {
 *     google(GoogleAuthCredentials(serverId = WebClientId))
 *     firebase(FirebaseBackendOptions(apiKey = ..., projectId = ..., applicationId = ...))
 * }
 * ```
 *
 * No-op on Android and iOS, where the native SDK initializes itself from
 * the bundled config file — safe to keep in shared initialization code.
 */
public fun KMPAuthConfiguration.firebase(options: FirebaseBackendOptions) {
    initializeFirebasePlatform(options)
}

internal expect fun initializeFirebasePlatform(options: FirebaseBackendOptions)

/**
 * [firebase] shorthand taking the values directly — same semantics as the
 * [FirebaseBackendOptions] overload:
 *
 * ```
 * KMPAuth.initialize {
 *     firebase(apiKey = ..., projectId = ..., applicationId = ...)
 * }
 * ```
 */
public fun KMPAuthConfiguration.firebase(
    apiKey: String,
    projectId: String,
    applicationId: String,
    authDomain: String = "$projectId.firebaseapp.com",
) {
    firebase(
        FirebaseBackendOptions(
            apiKey = apiKey,
            projectId = projectId,
            applicationId = applicationId,
            authDomain = authDomain,
        )
    )
}
