package com.mmk.kmpauth.core.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The [AuthProviderBackend] serving the auth states
 * (`rememberEmailAuthState`, `rememberAnonymousAuthState`,
 * `rememberPhoneAuthState`, `rememberOAuthState`, `rememberGoogleAuthState`,
 * ...) in the current composition.
 *
 * Defaults to [KMPAuthBackend] — the process-wide default backend — so
 * single-backend apps never touch this. To run several backends side by
 * side, scope a subtree with [ProvideKMPAuthBackend]:
 *
 * ```
 * ProvideKMPAuthBackend("supabase") {
 *     // every auth state in here is served by Supabase, while states
 *     // outside keep using the default backend (e.g. Firebase)
 * }
 * ```
 */
public val LocalKMPAuthBackend: ProvidableCompositionLocal<AuthProviderBackend> =
    staticCompositionLocalOf { KMPAuthBackend }

/**
 * Scopes [content] to the backend registered under [backendId]
 * (`"firebase"`, `"supabase"`, or a custom backend's
 * [AuthProviderBackend.backendId]) — every auth state inside runs against
 * it instead of the default backend. The multi-backend companion of
 * `KMPAuth.initialize { }`:
 *
 * ```
 * KMPAuth.initialize {
 *     firebase(apiKey = ..., projectId = ..., applicationId = ...)
 *     supabase(url = ..., apiKey = ...)
 * }
 *
 * // Firebase (the default) elsewhere; Supabase for this subtree:
 * ProvideKMPAuthBackend("supabase") {
 *     val emailAuth = rememberEmailAuthState(email, password, onResult = ...)
 * }
 * ```
 *
 * @throws IllegalStateException when no backend with [backendId] is
 * registered.
 */
@Composable
public fun ProvideKMPAuthBackend(backendId: String, content: @Composable () -> Unit) {
    val backend = remember(backendId) { KMPAuthBackend.require(backendId) }
    CompositionLocalProvider(LocalKMPAuthBackend provides backend, content = content)
}

/** [ProvideKMPAuthBackend] variant taking a backend instance directly. */
@Composable
public fun ProvideKMPAuthBackend(backend: AuthProviderBackend, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalKMPAuthBackend provides backend, content = content)
}
