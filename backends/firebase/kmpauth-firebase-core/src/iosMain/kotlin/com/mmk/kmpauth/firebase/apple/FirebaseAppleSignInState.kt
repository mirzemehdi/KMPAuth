package com.mmk.kmpauth.firebase.apple

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import cocoapods.FirebaseAuth.FIRAuth
import cocoapods.FirebaseAuth.FIRAuthDataResult
import cocoapods.FirebaseAuth.FIROAuthProvider
import com.mmk.kmpauth.apple.AppleSignInRequestScope
import com.mmk.kmpauth.apple.performAppleSignIn
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.LaunchingSignInState
import com.mmk.kmpauth.core.SignInState
import dev.gitlive.firebase.Firebase
import com.mmk.kmpauth.core.auth.KMPAuthUser
import com.mmk.kmpauth.firebase.backend.FirebaseKMPAuthUser
import dev.gitlive.firebase.auth.auth
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSError
import kotlin.coroutines.resume

@OptIn(KMPAuthInternalApi::class)
@Composable
public actual fun rememberFirebaseAppleSignInState(
    requestScopes: List<AppleSignInRequestScope>,
    linkAccount: Boolean,
    onResult: (Result<KMPAuthUser?>) -> Unit,
): SignInState {
    val scope = rememberCoroutineScope()
    val currentRequestScopes by rememberUpdatedState(requestScopes)
    val currentLinkAccount by rememberUpdatedState(linkAccount)
    val currentOnResult by rememberUpdatedState(onResult)

    return remember {
        LaunchingSignInState(scope) {
            currentOnResult(signInWithApple(currentRequestScopes, currentLinkAccount))
        }
    }
}

/**
 * Runs the native Apple authorization through `kmpauth-apple`, then exchanges
 * the resulting identity token for a Firebase session.
 */
@OptIn(KMPAuthInternalApi::class)
private suspend fun signInWithApple(
    requestScopes: List<AppleSignInRequestScope>,
    linkAccount: Boolean,
): Result<KMPAuthUser?> {
    val credentialResult = performAppleSignIn(requestScopes)
    val appleCredential = credentialResult.getOrElse { return Result.failure(it) }
    return signInToFirebase(
        idToken = appleCredential.idToken,
        rawNonce = appleCredential.rawNonce,
        fullName = appleCredential.fullNameComponents,
        linkAccount = linkAccount,
    )
}

@OptIn(ExperimentalForeignApi::class, KMPAuthInternalApi::class)
private suspend fun signInToFirebase(
    idToken: String,
    rawNonce: String,
    fullName: platform.Foundation.NSPersonNameComponents?,
    linkAccount: Boolean,
): Result<KMPAuthUser?> = suspendCancellableCoroutine { continuation ->
    // Pass Apple's name components along so Firebase can populate the display
    // name on the user's first sign-in.
    val credential = FIROAuthProvider.appleCredentialWithIDToken(idToken, rawNonce, fullName)
    val currentUser = FIRAuth.auth().currentUser()

    val handleResult: (FIRAuthDataResult?, NSError?) -> Unit = { firAuthDataResult, nsError ->
        if (continuation.isActive) {
            if (nsError != null || firAuthDataResult == null) {
                continuation.resume(
                    Result.failure(
                        IllegalStateException(
                            nsError?.localizedFailureReason ?: nsError?.localizedDescription
                        )
                    )
                )
            } else {
                continuation.resume(Result.success(Firebase.auth.currentUser?.let(::FirebaseKMPAuthUser)))
            }
        }
    }

    if (linkAccount && currentUser != null) {
        currentUser.linkWithCredential(credential, handleResult)
    } else {
        FIRAuth.auth().signInWithCredential(credential, handleResult)
    }
}
