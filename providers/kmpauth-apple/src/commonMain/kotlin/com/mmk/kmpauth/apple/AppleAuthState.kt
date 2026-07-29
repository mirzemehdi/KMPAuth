package com.mmk.kmpauth.apple

import androidx.compose.runtime.Composable
import com.mmk.kmpauth.core.SignInState
import com.mmk.kmpauth.core.auth.KMPAuthUser
import com.mmk.kmpauth.core.auth.rememberOAuthState

/**
 * Apple sign-in as a Compose state holder, served by the registered auth
 * backend (Firebase or Supabase today; any
 * [com.mmk.kmpauth.core.auth.AuthProviderBackend]):
 *
 * - **iOS**: runs the native AuthenticationServices flow and exchanges the
 *   identity token through the backend's `id_token` grant — including the
 *   user's full name, which Apple returns only on the first authorization.
 * - **Other platforms**: the backend's browser OAuth web flow for
 *   `apple.com` (Firebase: Android + Desktop; Supabase: see its guide for
 *   the per-platform setup).
 *
 * ```
 * val appleSignIn = rememberAppleAuthState(onResult = onAuthResult)
 * AppleSignInButton { appleSignIn.launch() }
 * ```
 *
 * Enable the Apple provider in the Firebase console / Supabase dashboard,
 * and add the "Sign In with Apple" capability in Xcode.
 *
 * @param requestScopes Apple scopes to request on iOS (full name / email).
 * @param linkAccount true links the credential to the currently signed-in
 * user instead of creating a new session (backend support varies).
 * @param onResult receives the signed-in [KMPAuthUser] or the failure. The
 * backend's native user stays reachable through [KMPAuthUser.raw].
 */
@Composable
public expect fun rememberAppleAuthState(
    requestScopes: List<AppleSignInRequestScope> = listOf(
        AppleSignInRequestScope.FullName,
        AppleSignInRequestScope.Email,
    ),
    linkAccount: Boolean = false,
    onResult: (Result<KMPAuthUser>) -> Unit,
): SignInState

/** Shared non-iOS implementation: Apple as a backend browser OAuth flow. */
@Composable
internal fun rememberAppleWebFlowAuthState(
    requestScopes: List<AppleSignInRequestScope>,
    linkAccount: Boolean,
    onResult: (Result<KMPAuthUser>) -> Unit,
): SignInState = rememberOAuthState(
    provider = "apple.com",
    requestScopes = requestScopes.map {
        when (it) {
            AppleSignInRequestScope.Email -> "email"
            AppleSignInRequestScope.FullName -> "name"
        }
    },
    linkAccount = linkAccount,
    onResult = onResult,
)
