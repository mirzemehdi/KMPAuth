package com.mmk.kmpauth.google

import com.mmk.kmpauth.core.KMPAuthConfiguration

/**
 * Configures Google Sign-In inside `KMPAuth.initialize { }`:
 *
 * ```
 * KMPAuth.initialize {
 *     google(GoogleAuthCredentials(serverId = WebClientId))
 * }
 * ```
 *
 * Equivalent to calling [GoogleAuthProvider.create] — the first
 * configuration wins, matching its semantics.
 */
public fun KMPAuthConfiguration.google(credentials: GoogleAuthCredentials) {
    GoogleAuthProvider.create(credentials)
}

/**
 * Shorthand for [google] with just the **Web Client Id** (and, for Desktop,
 * an optional Google-console-registered loopback [redirectUri]).
 */
public fun KMPAuthConfiguration.google(
    serverId: String,
    redirectUri: String = "http://localhost:8080/callback",
) {
    google(GoogleAuthCredentials(serverId = serverId, redirectUri = redirectUri))
}
