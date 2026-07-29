package com.mmk.kmpauth.firebase.oauth

import androidx.compose.runtime.Composable
import com.mmk.kmpauth.core.SignInState
import com.mmk.kmpauth.core.auth.KMPAuthUser

/**
 * OAuth sign-in with Firebase for the given provider, as a Compose state
 * holder. Firebase drives the provider's OAuth web flow — no provider SDK
 * is needed. Used directly for any provider enabled in the Firebase console
 * (e.g. `"twitter.com"`, `"yahoo.com"`, or an OIDC provider `"oidc.xxx"`);
 * GitHub and Microsoft have dedicated wrappers.
 *
 * Parameters are read at launch time: recomposing with new values (e.g.
 * toggling [linkAccount] between sign-in and sign-up modes) updates the
 * existing state, and [SignInState.launch] uses whatever is current when
 * the user taps.
 *
 * ```
 * val oAuthSignIn = rememberOAuthState(
 *     provider = "github.com",
 *     onResult = onFirebaseResult,
 * )
 *
 * Button(onClick = { oAuthSignIn.launch() }) { Text("Github Sign-In (Custom Design)") }
 * ```
 *
 * Platform support: Android and iOS run the browser flow. On Desktop, JS
 * and wasm, launching reports a failed [Result] — the flow is not
 * implemented there.
 *
 * @param provider Provider id as configured in Firebase, e.g. `"github.com"`.
 * @param requestScopes OAuth scopes to request from the provider.
 * @param customParameters Extra provider-specific OAuth parameters.
 * @param linkAccount [Boolean] flag to link account with current user. Default value is false.
 * @param onResult receives the signed-in [KMPAuthUser] or the failure. The
 * native Firebase user stays reachable through [KMPAuthUser.raw].
 */
@Composable
public expect fun rememberOAuthState(
    provider: String,
    requestScopes: List<String> = emptyList(),
    customParameters: Map<String, String> = emptyMap(),
    linkAccount: Boolean = false,
    onResult: (Result<KMPAuthUser>) -> Unit,
): SignInState
