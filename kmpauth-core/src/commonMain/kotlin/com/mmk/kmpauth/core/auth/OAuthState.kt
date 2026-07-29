package com.mmk.kmpauth.core.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.LaunchingSignInState
import com.mmk.kmpauth.core.SignInState

/**
 * Browser OAuth sign-in for any [provider] as a Compose state holder,
 * served by the registered auth backend:
 *
 * - **Firebase**: any provider enabled in the Firebase console. Native
 *   web-flow UI on Android/iOS; on Desktop the system browser runs
 *   Firebase's hosted auth handler. Not implemented on JS/web yet; on
 *   wasm web flows are unavailable — both report a failed [Result].
 * - **Supabase**: any GoTrue provider. Desktop works out of the box;
 *   Android/iOS need supabase-kt's deep-link setup; on web the page
 *   redirects and the session is restored after reload.
 *
 * [provider] accepts Firebase-style ids (`github.com`, `microsoft.com`)
 * and — with Supabase — GoTrue names (`github`, `azure`, `gitlab`, ...).
 *
 * ```
 * val yahooSignIn = rememberOAuthState(provider = "yahoo.com", onResult = onAuthResult)
 * Button(onClick = { yahooSignIn.launch() }) { Text("Sign in with Yahoo") }
 * ```
 *
 * @param provider OAuth provider id.
 * @param requestScopes OAuth scopes to request.
 * @param customParameters Provider-specific parameters (e.g.
 * `mapOf("tenant" to "...")` for Microsoft).
 * @param linkAccount true links the credential to the currently signed-in
 * user instead of creating a new session (backend support varies).
 * @param onResult receives the signed-in [KMPAuthUser] or the failure. The
 * backend's native user stays reachable through [KMPAuthUser.raw].
 */
@OptIn(KMPAuthInternalApi::class)
@Composable
public fun rememberOAuthState(
    provider: String,
    requestScopes: List<String> = emptyList(),
    customParameters: Map<String, String> = emptyMap(),
    linkAccount: Boolean = false,
    onResult: (Result<KMPAuthUser>) -> Unit,
): SignInState {
    val scope = rememberCoroutineScope()
    val currentProvider by rememberUpdatedState(provider)
    val currentScopes by rememberUpdatedState(requestScopes)
    val currentCustomParameters by rememberUpdatedState(customParameters)
    val currentLinkAccount by rememberUpdatedState(linkAccount)
    val currentBackend by rememberUpdatedState(LocalKMPAuthBackend.current)
    val currentOnResult by rememberUpdatedState(onResult)

    return remember {
        LaunchingSignInState(scope) {
            currentOnResult(
                currentBackend.signIn(
                    credential = AuthCredential.OAuthWebFlow(
                        providerId = currentProvider,
                        scopes = currentScopes,
                        customParameters = currentCustomParameters,
                    ),
                    linkWithCurrentUser = currentLinkAccount,
                )
            )
        }
    }
}

/**
 * GitHub sign-in via the registered backend's OAuth web flow — a thin
 * wrapper over [rememberOAuthState] with `provider = "github.com"`. Enable
 * the GitHub provider in the Firebase console / Supabase dashboard first.
 */
@Composable
public fun rememberGithubAuthState(
    requestScopes: List<String> = listOf("user:email"),
    customParameters: Map<String, String> = emptyMap(),
    linkAccount: Boolean = false,
    onResult: (Result<KMPAuthUser>) -> Unit,
): SignInState = rememberOAuthState(
    provider = "github.com",
    requestScopes = requestScopes,
    customParameters = customParameters,
    linkAccount = linkAccount,
    onResult = onResult,
)

/**
 * Microsoft sign-in via the registered backend's OAuth web flow — a thin
 * wrapper over [rememberOAuthState] with `provider = "microsoft.com"`.
 * Enable the Microsoft/Azure provider in the Firebase console / Supabase
 * dashboard first. Restrict to one Azure AD tenant via
 * `customParameters = mapOf("tenant" to "...")`.
 */
@Composable
public fun rememberMicrosoftAuthState(
    requestScopes: List<String> = listOf("mail.read"),
    customParameters: Map<String, String> = emptyMap(),
    linkAccount: Boolean = false,
    onResult: (Result<KMPAuthUser>) -> Unit,
): SignInState = rememberOAuthState(
    provider = "microsoft.com",
    requestScopes = requestScopes,
    customParameters = customParameters,
    linkAccount = linkAccount,
    onResult = onResult,
)
