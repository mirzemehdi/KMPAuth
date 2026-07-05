package com.mmk.kmpauth.firebase.backend

import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.auth.AuthCredential
import com.mmk.kmpauth.core.auth.AuthProviderBackend
import com.mmk.kmpauth.core.auth.AuthProviderIds
import com.mmk.kmpauth.core.auth.KMPAuthBackend
import com.mmk.kmpauth.core.auth.KMPAuthUser
import com.mmk.kmpauth.core.logger.currentLogger
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FacebookAuthProvider
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.CancellationException

/**
 * [KMPAuthUser] view over a Firebase user. The native
 * [dev.gitlive.firebase.auth.FirebaseUser] stays reachable through [raw].
 */
internal class FirebaseKMPAuthUser(private val user: FirebaseUser) : KMPAuthUser {
    override val uid: String get() = user.uid
    override val email: String? get() = user.email
    override val displayName: String? get() = user.displayName
    override val photoUrl: String? get() = user.photoURL
    override val providerId: String? get() = user.providerId
    override val raw: Any get() = user
}

/**
 * Firebase implementation of [AuthProviderBackend], KMPAuth's default
 * backend. Registered automatically the first time a Firebase container is
 * used; register a different backend at application start to override.
 *
 * Token-based credentials ([AuthCredential.IdToken] from Google or
 * Facebook) are exchanged directly. Apple and web-flow sign-in
 * ([AuthCredential.OAuthWebFlow]) require a platform-driven browser flow
 * and are served by the dedicated composables (`AppleButtonUiContainer`,
 * `GithubButtonUiContainer`, `OAuthContainer`) rather than this backend.
 */
public object FirebaseAuthBackend : AuthProviderBackend {

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

    override suspend fun signOut() {
        Firebase.auth.signOut()
    }

    override fun currentUser(): KMPAuthUser? =
        Firebase.auth.currentUser?.let { FirebaseKMPAuthUser(it) }

    private fun AuthCredential.toFirebaseCredentialOrNull(): dev.gitlive.firebase.auth.AuthCredential? =
        when (this) {
            is AuthCredential.IdToken -> when (providerId) {
                AuthProviderIds.GOOGLE -> GoogleAuthProvider.credential(idToken, accessToken)
                AuthProviderIds.FACEBOOK -> accessToken?.let { FacebookAuthProvider.credential(it) }
                else -> null
            }

            is AuthCredential.OAuthWebFlow -> null
        }
}

/**
 * Registers [FirebaseAuthBackend] as the default backend. No-op when the
 * application already registered a backend (first registration wins).
 */
internal fun ensureFirebaseBackendRegistered() {
    KMPAuthBackend.register(FirebaseAuthBackend)
}
