package com.mmk.kmpauth.google

/**
 * Google Auth Credentials holder class.
 * @param serverId - This should be Web Client Id that you created in Google OAuth page
 * @param redirectUri - (JVM only) Custom redirect URI for OAuth callback.
 *                      If not provided, defaults to "http://localhost:8080/callback".
 *                      Make sure this URI is registered in Google Cloud Console.
 */
public data class GoogleAuthCredentials(
    val serverId: String,
    val redirectUri: String? = null
)
