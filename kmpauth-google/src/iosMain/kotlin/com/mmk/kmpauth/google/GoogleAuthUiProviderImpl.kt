package com.mmk.kmpauth.google

import cocoapods.GoogleSignIn.GIDSignIn
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIApplication
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class GoogleAuthUiProviderImpl : GoogleAuthUiProvider {
    @OptIn(ExperimentalForeignApi::class)
    override suspend fun signIn(): GoogleUser? = suspendCoroutine { continutation ->

        val rootViewController =
            UIApplication.sharedApplication.keyWindow?.rootViewController

        if (rootViewController == null) continutation.resume(null)
        else {
            GIDSignIn.sharedInstance
                .signInWithPresentingViewController(rootViewController) { gidSignInResult, nsError ->
                    nsError?.let { println("Error While signing: $nsError") }

                    val user = gidSignInResult?.user
                    val idToken = user?.idToken?.tokenString
                    val accessToken = user?.accessToken?.tokenString
                    val profile = gidSignInResult?.user?.profile
                    if (idToken != null && accessToken != null) {
                        val googleUser = GoogleUser(
                            idToken = idToken,
                            accessToken = accessToken,
                            displayName = profile?.name ?: "",
                            profilePicUrl = profile?.imageURLWithDimension(320u)?.absoluteString
                        )
                        continutation.resume(googleUser)
                    } else continutation.resume(null)
                }

        }
    }


}