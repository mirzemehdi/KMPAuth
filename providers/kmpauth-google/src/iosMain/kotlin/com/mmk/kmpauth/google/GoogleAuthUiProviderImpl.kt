package com.mmk.kmpauth.google

import swiftPMImport.io.github.mirzemehdi.providers.kmpauth.google.GIDSignIn
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.auth.KMPAuthNSErrorException
import com.mmk.kmpauth.core.auth.KMPAuthNetworkException
import com.mmk.kmpauth.core.auth.KMPAuthUserCancelledException
import com.mmk.kmpauth.core.logger.currentLogger
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSError
import platform.Foundation.NSURLErrorDomain
import platform.UIKit.UIApplication
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(KMPAuthInternalApi::class)
internal class GoogleAuthUiProviderImpl : GoogleAuthUiProvider {
    @OptIn(ExperimentalForeignApi::class)
    override suspend fun signIn(
        filterByAuthorizedAccounts: Boolean,
        isAutoSelectEnabled: Boolean,
        scopes: List<String>,
        requestAccessToken: Boolean,
    ): Result<GoogleUser> = suspendCoroutine { continutation ->

        val rootViewController =
            UIApplication.sharedApplication.keyWindow?.rootViewController

        if (rootViewController == null) {
            currentLogger.log("Root View Controller is null")
            continutation.resume(
                Result.failure(IllegalStateException("Root View Controller is null"))
            )
        } else {
            GIDSignIn.sharedInstance
                .signInWithPresentingViewController(rootViewController,null, scopes) { gidSignInResult, nsError ->
                    nsError?.let { currentLogger.log("Error While signing: $nsError") }

                    val user = gidSignInResult?.user
                    val idToken = user?.idToken?.tokenString
                    val accessToken = user?.accessToken?.tokenString
                    val profile = gidSignInResult?.user?.profile
                    if (idToken != null && accessToken != null) {
                        val googleUser = GoogleUser(
                            idToken = idToken,
                            accessToken = accessToken,
                            email = profile?.email,
                            serverAuthCode = gidSignInResult.serverAuthCode,
                            displayName = profile?.name ?: "",
                            profilePicUrl = profile?.imageURLWithDimension(320u)?.absoluteString
                        )
                        continutation.resume(Result.success(googleUser))
                    } else {
                        continutation.resume(Result.failure(nsError.asSignInError()))
                    }
                }

        }
    }

    private companion object {
        // GIDSignInErrorCode.canceled in GIDSignIn's error domain.
        const val GID_SIGN_IN_ERROR_DOMAIN = "com.google.GIDSignIn"
        const val GID_ERROR_CODE_CANCELED = -5L
    }

    /**
     * Classifies the GIDSignIn failure. The full [NSError] stays reachable
     * through [KMPAuthNSErrorException] — as the failure itself when
     * unclassified, as the `cause` of the typed exceptions otherwise.
     */
    private fun NSError?.asSignInError(): Throwable {
        if (this == null) {
            return IllegalStateException("Google Sign-In did not return an id token")
        }
        val wrapped = KMPAuthNSErrorException(this)
        return when {
            domain == GID_SIGN_IN_ERROR_DOMAIN && code == GID_ERROR_CODE_CANCELED ->
                KMPAuthUserCancelledException(
                    message = "The user cancelled the sign-in flow. ($domain $code)",
                    cause = wrapped,
                )

            domain == NSURLErrorDomain ->
                KMPAuthNetworkException(
                    message = "Google sign-in failed because of a network problem: ${wrapped.message}",
                    cause = wrapped,
                )

            else -> wrapped
        }
    }
}