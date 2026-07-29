package com.mmk.kmpauth.google

import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.auth.AuthCredential
import com.mmk.kmpauth.core.auth.AuthProviderBackend
import com.mmk.kmpauth.core.auth.AuthProviderIds
import com.mmk.kmpauth.core.logger.currentLogger
import com.mmk.kmpauth.core.auth.KMPAuthUser

/**
 * Non-composable orchestration behind [GoogleButtonUiContainerFirebase]:
 * turns a [GoogleUser] sign-in result into a Firebase session through the
 * pluggable [AuthProviderBackend]. Kept separate from the composable so the
 * contract (exact failure messages, link-vs-sign-in decision) stays
 * unit-testable with a fake backend.
 */
internal class GoogleAuthSignInHandler(
    private val backend: AuthProviderBackend,
) {

    @OptIn(KMPAuthInternalApi::class)
    suspend fun signIn(googleUser: GoogleUser?, linkAccount: Boolean): Result<KMPAuthUser> {
        val idToken = googleUser?.idToken
        if (idToken == null) {
            currentLogger.log("Google idToken is null")
            return Result.failure(IllegalStateException("Idtoken is null"))
        }
        val credential = AuthCredential.IdToken(
            providerId = AuthProviderIds.GOOGLE,
            idToken = idToken,
            accessToken = googleUser.accessToken,
        )
        return backend.signIn(credential, linkWithCurrentUser = linkAccount).fold(
            onSuccess = { user -> Result.success(user) },
            onFailure = { e ->
                currentLogger.log("Google sign-in failed with exception: $e")
                Result.failure(e)
            },
        )
    }
}
