package com.mmk.kmpauth.google

/**
 * Google Auth Credentials holder class.
 * @param serverId - This should be Web Client Id that you created in Google OAuth page
 * @param redirectUri - Desktop (JVM) only. The OAuth loopback redirect URI,
 * e.g. `http://localhost:8080/callback` or `http://127.0.0.1:9000/oauth2`.
 * It must be an `http` loopback URL (`localhost`/`127.0.0.1`) with an explicit
 * port and match an **Authorized redirect URI** registered for your OAuth
 * client in the Google Cloud console — Google's Web clients reject
 * unregistered URIs with `redirect_uri_mismatch`. The desktop flow binds a
 * local callback server to the URI's port, so the port must be free and known
 * ahead of time. Defaults to `http://localhost:8080/callback`. Ignored on
 * Android, iOS, JS and wasmJs (those platforms don't run a loopback server).
 */
public data class GoogleAuthCredentials(
    val serverId: String,
    val redirectUri: String = "http://localhost:8080/callback",
)
