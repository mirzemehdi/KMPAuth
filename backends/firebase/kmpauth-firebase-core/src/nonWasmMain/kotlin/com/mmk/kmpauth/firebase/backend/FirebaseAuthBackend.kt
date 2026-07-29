package com.mmk.kmpauth.firebase.backend

import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.auth.AuthCredential
import com.mmk.kmpauth.core.auth.AuthProviderBackend
import com.mmk.kmpauth.core.auth.EmailActionCodeSettings
import com.mmk.kmpauth.core.auth.KMPAuthUser
import dev.gitlive.firebase.auth.FirebaseUser

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

public actual object FirebaseAuthBackend : AuthProviderBackend {

    // Native Firebase SDK (GitLive) on Android/iOS/JS; the Firebase Auth
    // REST API on Desktop (JVM), where firebase-java-sdk lacks auth (#204).
    private val engine: AuthProviderBackend by lazy { createFirebaseAuthEngine() }

    override suspend fun signIn(
        credential: AuthCredential,
        linkWithCurrentUser: Boolean,
    ): Result<KMPAuthUser> = engine.signIn(credential, linkWithCurrentUser)

    override suspend fun reauthenticate(credential: AuthCredential): Result<Unit> =
        engine.reauthenticate(credential)

    override suspend fun signUp(email: String, password: String): Result<KMPAuthUser> =
        engine.signUp(email, password)

    override suspend fun signInAnonymously(): Result<KMPAuthUser> =
        engine.signInAnonymously()

    override suspend fun sendPasswordResetEmail(
        email: String,
        actionCodeSettings: EmailActionCodeSettings?,
    ): Result<Unit> = engine.sendPasswordResetEmail(email, actionCodeSettings)

    override suspend fun sendSignInLinkToEmail(
        email: String,
        actionCodeSettings: EmailActionCodeSettings,
    ): Result<Unit> = engine.sendSignInLinkToEmail(email, actionCodeSettings)

    override fun isSignInWithEmailLink(link: String): Boolean =
        engine.isSignInWithEmailLink(link)

    override suspend fun signInWithEmailLink(
        email: String,
        link: String,
        linkAccount: Boolean,
    ): Result<KMPAuthUser> = engine.signInWithEmailLink(email, link, linkAccount)

    override suspend fun signOut(): Unit = engine.signOut()

    override fun currentUser(): KMPAuthUser? = engine.currentUser()

    /**
     * Adapts a legacy `Result<FirebaseUser?>` callback (the deprecated 2.x
     * container composables) to the `Result<KMPAuthUser>` the sign-in
     * states now produce, unwrapping the native user through
     * [KMPAuthUser.raw].
     */
    internal fun toFirebaseUserCallback(
        onResult: (Result<FirebaseUser?>) -> Unit,
    ): (Result<KMPAuthUser>) -> Unit = { result ->
        onResult(result.map { it.raw as? FirebaseUser })
    }
}
