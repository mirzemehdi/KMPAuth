package com.mmk.kmpauth.firebase.backend

import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.auth.AuthCredential
import com.mmk.kmpauth.core.auth.AuthProviderBackend
import com.mmk.kmpauth.core.auth.AuthProviderIds
import com.mmk.kmpauth.core.auth.KMPAuthBackend
import com.mmk.kmpauth.core.auth.KMPAuthUser
import com.mmk.kmpauth.core.auth.EmailActionCodeSettings
import com.mmk.kmpauth.core.logger.currentLogger
import com.mmk.kmpauth.core.runCatchingCancellable
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.ActionCodeSettings
import dev.gitlive.firebase.auth.AndroidPackageName
import dev.gitlive.firebase.auth.EmailAuthProvider
import dev.gitlive.firebase.auth.FacebookAuthProvider
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.OAuthProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.CancellationException

/**
 * [KMPAuthUser] view over a Firebase user. The native
 * [dev.gitlive.firebase.auth.FirebaseUser] stays reachable through [raw].
 */
@KMPAuthInternalApi
public class FirebaseKMPAuthUser(private val user: FirebaseUser) : KMPAuthUser {
    override val uid: String get() = user.uid
    override val email: String? get() = user.email
    override val displayName: String? get() = user.displayName
    override val photoUrl: String? get() = user.photoURL
    override val providerId: String? get() = user.providerId
    override val raw: Any get() = user
}

@OptIn(KMPAuthInternalApi::class)
public actual object FirebaseAuthBackend : AuthProviderBackend {

    @OptIn(KMPAuthInternalApi::class)
    override suspend fun signIn(
        credential: AuthCredential,
        linkWithCurrentUser: Boolean,
    ): Result<KMPAuthUser> {
        val firebaseCredential = credential.toFirebaseCredentialOrNull()
            ?: return Result.failure(
                UnsupportedOperationException(
                    "FirebaseAuthBackend cannot exchange this credential directly " +
                        "(provider '${credential.providerId}'). Use the dedicated " +
                        "container composable for web-flow providers."
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

    override suspend fun signUp(
        email: String,
        password: String,
    ): Result<KMPAuthUser> = runCatchingCancellable {
        Firebase.auth.createUserWithEmailAndPassword(email, password)
            .user?.let(::FirebaseKMPAuthUser)
            ?: throw IllegalStateException("Firebase Null user")
    }

    override suspend fun signInAnonymously(): Result<KMPAuthUser> = runCatchingCancellable {
        Firebase.auth.signInAnonymously().user?.let(::FirebaseKMPAuthUser)
            ?: throw IllegalStateException("Firebase Null user")
    }

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
    }

    override suspend fun signOut() {
        Firebase.auth.signOut()
    }

    override fun currentUser(): KMPAuthUser? =
        Firebase.auth.currentUser?.let { FirebaseKMPAuthUser(it) }

    /**
     * Adapts a legacy `Result<FirebaseUser?>` callback (the deprecated 2.x
     * container composables) to the `Result<KMPAuthUser>` the sign-in
     * states now produce, unwrapping the native user through
     * [KMPAuthUser.raw].
     */
    internal fun toFirebaseUserCallback(
        onResult: (Result<FirebaseUser?>) -> Unit,
    ): (Result<KMPAuthUser>) -> Unit = { result ->
        onResult(result.map { it?.raw as? FirebaseUser })
    }

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
