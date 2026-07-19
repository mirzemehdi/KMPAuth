package com.mmk.kmpauth.google

/**
 * GoogleUser class holds most necessary fields
 *
 * @param idToken Google's ID token (a JWT). Always present - send it to your
 * backend to identify the user.
 * @param accessToken OAuth access token, for calling Google APIs on the user's
 * behalf. **Availability differs by platform:** iOS, desktop, JS and wasm always
 * return one; on Android, Credential Manager returns an ID token only, so a
 * token is fetched through a separate authorization request (with its own
 * consent prompt) when `requestAccessToken = true` or when scopes beyond
 * `email`/`profile` are requested. The legacy Android fallback, used when
 * Credential Manager is unavailable, never returns one.
 * @param serverAuthCode One-time code your backend can exchange for a refresh
 * token. Only populated by the legacy Android flow and by iOS.
 */
public data class GoogleUser(
    val idToken: String,
    val accessToken: String? = null,
    val email: String? = null,
    val displayName: String = "",
    val profilePicUrl: String? = null,
    val serverAuthCode: String? = null
)