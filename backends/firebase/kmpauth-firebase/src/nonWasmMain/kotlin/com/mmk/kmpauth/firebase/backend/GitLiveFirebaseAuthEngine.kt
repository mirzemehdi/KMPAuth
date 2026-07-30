package com.mmk.kmpauth.firebase.backend

import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.auth.AuthCredential
import com.mmk.kmpauth.core.auth.AuthProviderBackend
import com.mmk.kmpauth.core.auth.AuthProviderIds
import com.mmk.kmpauth.core.auth.EmailActionCodeSettings
import com.mmk.kmpauth.core.auth.KMPAuthUser
import com.mmk.kmpauth.core.auth.KMPAuthUserCollisionException
import com.mmk.kmpauth.core.auth.PhoneVerificationUi
import com.mmk.kmpauth.core.logger.currentLogger
import com.mmk.kmpauth.core.runCatchingCancellable
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.ActionCodeSettings
import dev.gitlive.firebase.auth.AndroidPackageName
import dev.gitlive.firebase.auth.EmailAuthProvider
import dev.gitlive.firebase.auth.FacebookAuthProvider
import dev.gitlive.firebase.auth.FirebaseAuthUserCollisionException
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.OAuthProvider
import dev.gitlive.firebase.auth.PhoneAuthProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.CancellationException

/**
 * [AuthProviderBackend] engine over the native Firebase SDK (GitLive
 * bindings). Used on Android, iOS and JS; Desktop (JVM) uses the REST
 * engine instead, because GitLive's firebase-java-sdk does not implement
 * auth (#204).
 */
@OptIn(KMPAuthInternalApi::class)
internal class GitLiveFirebaseAuthEngine : AuthProviderBackend {

    override suspend fun signIn(
        credential: AuthCredential,
        linkWithCurrentUser: Boolean,
    ): Result<KMPAuthUser> = signInInternal(credential, linkWithCurrentUser)
        .mappingCollision()

    private suspend fun signInInternal(
        credential: AuthCredential,
        linkWithCurrentUser: Boolean,
    ): Result<KMPAuthUser> {
        if (credential is AuthCredential.OAuthWebFlow) {
            return runCatchingCancellable {
                val user = gitLiveOAuthWebFlowSignIn(
                    oAuthProvider = OAuthProvider(
                        provider = credential.providerId,
                        scopes = credential.scopes,
                        customParameters = credential.customParameters,
                    ),
                    linkAccount = linkWithCurrentUser,
                ) ?: throw IllegalStateException("Firebase Null user")
                FirebaseKMPAuthUser(user)
            }
        }
        val firebaseCredential = credential.toFirebaseCredentialOrNull()
            ?: return Result.failure(
                UnsupportedOperationException(
                    "FirebaseAuthBackend cannot exchange this credential directly " +
                        "(provider '${credential.providerId}')."
                )
            )
        return try {
            val auth = Firebase.auth
            val currentUser = auth.currentUser
            val result = if (linkWithCurrentUser && currentUser != null) {
                currentUser.linkWithCredential(firebaseCredential)
            } else {
                auth.signInWithCredential(firebaseCredential)
            }
            val user = result.user
            if (user == null) {
                currentLogger.log("Firebase user is null")
                Result.failure(IllegalStateException("Firebase Null user"))
            } else {
                // Apple hands the full name to the app only on the first
                // authorization; persist it when the account has none.
                val displayName = (credential as? AuthCredential.IdToken)?.displayName
                if (displayName != null && user.displayName.isNullOrEmpty()) {
                    runCatching { user.updateProfile(displayName = displayName) }
                }
                Result.success(FirebaseKMPAuthUser(user))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun reauthenticate(credential: AuthCredential): Result<Unit> =
        runCatchingCancellable {
            val firebaseCredential = credential.toFirebaseCredentialOrNull()
                ?: throw UnsupportedOperationException(
                    "FirebaseAuthBackend cannot reauthenticate with this credential " +
                        "(provider '${credential.providerId}')."
                )
            val currentUser = Firebase.auth.currentUser
                ?: throw IllegalStateException("No signed-in user to reauthenticate")
            currentUser.reauthenticate(firebaseCredential)
        }

    override suspend fun deleteAccount(): Result<Unit> = runCatchingCancellable {
        val currentUser = Firebase.auth.currentUser
            ?: throw IllegalStateException("No signed-in user to delete")
        currentUser.delete()
    }

    override suspend fun signUp(
        email: String,
        password: String,
    ): Result<KMPAuthUser> = runCatchingCancellable {
        Firebase.auth.createUserWithEmailAndPassword(email, password)
            .user?.let(::FirebaseKMPAuthUser)
            ?: throw IllegalStateException("Firebase Null user")
    }.mappingCollision()

    override suspend fun signInAnonymously(): Result<KMPAuthUser> = runCatchingCancellable {
        Firebase.auth.signInAnonymously().user?.let(::FirebaseKMPAuthUser)
            ?: throw IllegalStateException("Firebase Null user")
    }

    override suspend fun signInWithPhone(
        phoneNumber: String,
        verificationUi: PhoneVerificationUi,
        linkWithCurrentUser: Boolean,
    ): Result<KMPAuthUser> = runCatchingCancellable {
        val verificationProvider = gitLivePhoneVerificationProvider(verificationUi)
            ?: throw UnsupportedOperationException(
                "Firebase phone sign-in is available on Android and iOS only: " +
                    "the web flow needs a reCAPTCHA verifier KMPAuth does not " +
                    "provide yet."
            )
        val credential =
            PhoneAuthProvider().verifyPhoneNumber(phoneNumber, verificationProvider)
        val auth = Firebase.auth
        val currentUser = auth.currentUser
        val result = if (linkWithCurrentUser && currentUser != null) {
            currentUser.linkWithCredential(credential)
        } else {
            auth.signInWithCredential(credential)
        }
        result.user?.let(::FirebaseKMPAuthUser)
            ?: throw IllegalStateException("Firebase Null user")
    }.mappingCollision()

    override suspend fun sendPasswordResetEmail(
        email: String,
        actionCodeSettings: EmailActionCodeSettings?,
    ): Result<Unit> = runCatchingCancellable {
        Firebase.auth.sendPasswordResetEmail(email, actionCodeSettings?.toFirebase())
    }

    override suspend fun sendSignInLinkToEmail(
        email: String,
        actionCodeSettings: EmailActionCodeSettings,
    ): Result<Unit> = runCatchingCancellable {
        Firebase.auth.sendSignInLinkToEmail(email, actionCodeSettings.toFirebase())
    }

    override fun isSignInWithEmailLink(link: String): Boolean =
        Firebase.auth.isSignInWithEmailLink(link)

    override suspend fun signInWithEmailLink(
        email: String,
        link: String,
        linkAccount: Boolean,
    ): Result<KMPAuthUser> = runCatchingCancellable {
        val auth = Firebase.auth
        val currentUser = auth.currentUser
        val result = if (linkAccount && currentUser != null) {
            currentUser.linkWithCredential(EmailAuthProvider.credentialWithLink(email, link))
        } else {
            auth.signInWithEmailLink(email, link)
        }
        result.user?.let(::FirebaseKMPAuthUser)
            ?: throw IllegalStateException("Firebase Null user")
    }.mappingCollision()

    override suspend fun signOut() {
        Firebase.auth.signOut()
    }

    override fun currentUser(): KMPAuthUser? =
        Firebase.auth.currentUser?.let { FirebaseKMPAuthUser(it) }

    /**
     * Surfaces account collisions as the backend-agnostic
     * [KMPAuthUserCollisionException] with a guaranteed non-empty message —
     * the iOS SDK sometimes reports collisions with an empty one, which made
     * them undetectable from common code.
     */
    private fun <T> Result<T>.mappingCollision(): Result<T> = fold(
        onSuccess = { this },
        onFailure = { error ->
            if (error is FirebaseAuthUserCollisionException) {
                Result.failure(
                    KMPAuthUserCollisionException(
                        message = error.message?.takeIf { it.isNotBlank() }
                            ?: "This credential is already associated with a different user account.",
                        cause = error,
                    )
                )
            } else this
        },
    )

    /** Maps the SDK-agnostic settings onto GitLive's ActionCodeSettings. */
    private fun EmailActionCodeSettings.toFirebase(): ActionCodeSettings = ActionCodeSettings(
        url = url,
        canHandleCodeInApp = canHandleCodeInApp,
        iOSBundleId = iOSBundleId,
        androidPackageName = androidPackageName?.let {
            AndroidPackageName(
                packageName = it,
                installIfNotAvailable = androidInstallIfNotAvailable,
                minimumVersion = androidMinimumVersion,
            )
        },
        linkDomain = linkDomain,
    )

    private fun AuthCredential.toFirebaseCredentialOrNull(): dev.gitlive.firebase.auth.AuthCredential? =
        when (this) {
            is AuthCredential.IdToken -> when (providerId) {
                AuthProviderIds.GOOGLE -> GoogleAuthProvider.credential(idToken, accessToken)
                // Facebook Limited Login issues an OIDC JWT verified through
                // its nonce; classic login uses the access token.
                AuthProviderIds.FACEBOOK ->
                    if (rawNonce != null) OAuthProvider.credential(
                        providerId = AuthProviderIds.FACEBOOK,
                        idToken = idToken,
                        rawNonce = rawNonce,
                    ) else FacebookAuthProvider.credential(accessToken ?: idToken)
                // Apple issues a verifiable identity token; Firebase checks
                // the unhashed nonce against the hash embedded in the token.
                AuthProviderIds.APPLE -> OAuthProvider.credential(
                    providerId = AuthProviderIds.APPLE,
                    idToken = idToken,
                    rawNonce = rawNonce,
                )

                else -> null
            }

            is AuthCredential.EmailPassword -> EmailAuthProvider.credential(email, password)

            is AuthCredential.OAuthWebFlow -> null
        }
}
