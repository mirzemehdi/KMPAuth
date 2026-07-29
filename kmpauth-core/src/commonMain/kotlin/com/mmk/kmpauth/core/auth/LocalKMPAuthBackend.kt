package com.mmk.kmpauth.core.auth

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The [AuthProviderBackend] serving the auth states
 * ([rememberEmailAuthState], [rememberAnonymousAuthState],
 * `rememberGoogleAuthState`, `rememberFacebookAuthState`) in the current
 * composition.
 *
 * Defaults to [KMPAuthBackend] — the process-wide registered backend — so
 * single-backend apps never touch this. To run several backends side by
 * side, scope a subtree to a specific backend instance:
 *
 * ```
 * val supabase = remember { SupabaseAuthBackend(url = ..., apiKey = ...) }
 * CompositionLocalProvider(LocalKMPAuthBackend provides supabase) {
 *     // every auth state in here is served by Supabase, while states
 *     // outside keep using the registered default (e.g. Firebase)
 * }
 * ```
 */
public val LocalKMPAuthBackend: ProvidableCompositionLocal<AuthProviderBackend> =
    staticCompositionLocalOf { KMPAuthBackend }
