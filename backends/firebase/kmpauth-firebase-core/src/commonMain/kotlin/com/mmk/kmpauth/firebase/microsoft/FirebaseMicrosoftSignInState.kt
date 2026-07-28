package com.mmk.kmpauth.firebase.microsoft

import androidx.compose.runtime.Composable
import com.mmk.kmpauth.core.SignInState
import com.mmk.kmpauth.core.auth.KMPAuthUser
import com.mmk.kmpauth.firebase.oauth.rememberFirebaseOAuthSignInState

/**
 * Microsoft Sign-In with Firebase as a Compose state holder.
 *
 * Enable the Microsoft provider in the Firebase console (Authentication →
 * Sign-in method) and register the app in the Azure portal first — Firebase
 * drives the OAuth web flow, no Microsoft SDK is needed.
 *
 * Parameters are read at launch time: recomposing with new values (e.g.
 * toggling [linkAccount] between sign-in and sign-up modes) updates the
 * existing state, and [SignInState.launch] uses whatever is current when
 * the user taps.
 *
 * ```
 * val microsoftSignIn = rememberFirebaseMicrosoftSignInState(onResult = onFirebaseResult)
 *
 * Button(onClick = { microsoftSignIn.launch() }) { Text("Microsoft Sign-In") }
 * ```
 *
 * To restrict sign-in to a single Azure AD tenant, pass
 * `customParameters = mapOf("tenant" to "your-tenant-id")`.
 *
 * @param requestScopes Scopes to request from Microsoft. By default the
 * user's email is requested.
 * @param customParameters Custom parameters for the Microsoft OAuth flow,
 * e.g. `"tenant"`, `"prompt"`, `"login_hint"`.
 * @param linkAccount [Boolean] flag to link account with current user. Default value is false.
 * @param onResult receives the signed-in [KMPAuthUser] or the failure. The
 * native Firebase user stays reachable through [KMPAuthUser.raw].
 */
@Composable
public fun rememberFirebaseMicrosoftSignInState(
    requestScopes: List<String> = listOf("mail.read"),
    customParameters: Map<String, String> = emptyMap(),
    linkAccount: Boolean = false,
    onResult: (Result<KMPAuthUser?>) -> Unit,
): SignInState = rememberFirebaseOAuthSignInState(
    provider = "microsoft.com",
    requestScopes = requestScopes,
    customParameters = customParameters,
    linkAccount = linkAccount,
    onResult = onResult,
)
