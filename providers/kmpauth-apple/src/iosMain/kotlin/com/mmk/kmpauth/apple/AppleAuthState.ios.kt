package com.mmk.kmpauth.apple

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.LaunchingSignInState
import com.mmk.kmpauth.core.SignInState
import com.mmk.kmpauth.core.auth.AuthCredential
import com.mmk.kmpauth.core.auth.AuthProviderIds
import com.mmk.kmpauth.core.auth.KMPAuthUser
import com.mmk.kmpauth.core.auth.LocalKMPAuthBackend
import platform.Foundation.NSPersonNameComponents

@OptIn(KMPAuthInternalApi::class)
@Composable
public actual fun rememberAppleAuthState(
    requestScopes: List<AppleSignInRequestScope>,
    linkAccount: Boolean,
    onResult: (Result<KMPAuthUser>) -> Unit,
): SignInState {
    val scope = rememberCoroutineScope()
    val currentRequestScopes by rememberUpdatedState(requestScopes)
    val currentLinkAccount by rememberUpdatedState(linkAccount)
    val currentBackend by rememberUpdatedState(LocalKMPAuthBackend.current)
    val currentOnResult by rememberUpdatedState(onResult)

    return remember {
        LaunchingSignInState(scope) {
            // Native flow first; then the backend's id_token grant. Apple
            // returns the full name only on the first authorization, so it
            // rides along for the backend to persist.
            val result = performAppleSignIn(currentRequestScopes).fold(
                onSuccess = { credential ->
                    currentBackend.signIn(
                        credential = AuthCredential.IdToken(
                            providerId = AuthProviderIds.APPLE,
                            idToken = credential.idToken,
                            rawNonce = credential.rawNonce,
                            displayName = credential.fullNameComponents?.formattedOrNull(),
                        ),
                        linkWithCurrentUser = currentLinkAccount,
                    )
                },
                onFailure = { Result.failure(it) },
            )
            currentOnResult(result)
        }
    }
}

private fun NSPersonNameComponents.formattedOrNull(): String? =
    listOfNotNull(givenName, familyName).joinToString(" ").ifBlank { null }
