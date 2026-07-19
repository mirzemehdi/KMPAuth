package com.mmk.kmpauth.google

import swiftPMImport.io.github.mirzemehdi.providers.kmpauth.google.GIDSignIn
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.logger.currentLogger
import kotlinx.cinterop.ExperimentalForeignApi
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
                        continutation.resume(
                            Result.failure(
                                IllegalStateException(
                                    nsError?.localizedDescription
                                        ?: "Google Sign-In did not return an id token"
                                )
                            )
                        )
                    }
                }

        }
    }


}