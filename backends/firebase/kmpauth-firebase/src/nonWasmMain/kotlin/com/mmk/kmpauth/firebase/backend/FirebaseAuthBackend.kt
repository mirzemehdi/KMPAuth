package com.mmk.kmpauth.firebase.backend

import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.auth.AuthCredential
import com.mmk.kmpauth.core.auth.AuthProviderBackend
import com.mmk.kmpauth.core.auth.EmailActionCodeSettings
import com.mmk.kmpauth.core.auth.KMPAuthUser
import com.mmk.kmpauth.core.auth.PhoneVerificationUi
import dev.gitlive.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

/**
 * [KMPAuthUser] view over a Firebase user. The native
 * [dev.gitlive.firebase.auth.FirebaseUser] stays reachable through [raw].
 *
 * [email], [displayName] and [photoUrl] fall back to the first linked
 * provider that has the value when the account-level field is empty — a
 * guest upgraded by linking a Google account gets the Google name and
 * photo without hand-rolling the aggregation. (Some SDKs report missing
 * provider values as the literal string "null"; those are filtered.)
 */
@KMPAuthInternalApi
public class FirebaseKMPAuthUser(private val user: FirebaseUser) : KMPAuthUser {
    override val uid: String get() = user.uid
    override val email: String?
        get() = user.email.orFromProviders { it.email }
    override val displayName: String?
        get() = user.displayName.orFromProviders { it.displayName }
    override val photoUrl: String?
        get() = user.photoURL.orFromProviders { it.photoURL }
    override val providerId: String? get() = user.providerId
    override val isAnonymous: Boolean get() = user.isAnonymous
    override val providerIds: List<String> get() = user.providerData.map { it.providerId }
    override val raw: Any get() = user

    private inline fun String?.orFromProviders(
        crossinline value: (dev.gitlive.firebase.auth.UserInfo) -> String?,
    ): String? = takeUnless { it.isNullOrEmpty() || it == "null" }
        ?: user.providerData.firstNotNullOfOrNull {
            value(it)?.takeUnless { v -> v.isEmpty() || v == "null" }
        }
}

public actual object FirebaseAuthBackend : AuthProviderBackend {

    override val backendId: String get() = FIREBASE_BACKEND_ID

    // Native Firebase SDK (GitLive) on Android/iOS/JS; the Firebase Auth
    // REST API on Desktop (JVM), where firebase-java-sdk lacks auth (#204).
    private val engine: AuthProviderBackend by lazy { createFirebaseAuthEngine() }

    actual override suspend fun signIn(
        credential: AuthCredential,
        linkWithCurrentUser: Boolean,
    ): Result<KMPAuthUser> = engine.signIn(credential, linkWithCurrentUser)

    override suspend fun reauthenticate(credential: AuthCredential): Result<Unit> =
        engine.reauthenticate(credential)

    override suspend fun signUp(email: String, password: String): Result<KMPAuthUser> =
        engine.signUp(email, password)

    override suspend fun signInAnonymously(): Result<KMPAuthUser> =
        engine.signInAnonymously()

    override suspend fun signInWithPhone(
        phoneNumber: String,
        verificationUi: PhoneVerificationUi,
        linkWithCurrentUser: Boolean,
    ): Result<KMPAuthUser> =
        engine.signInWithPhone(phoneNumber, verificationUi, linkWithCurrentUser)

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

    override suspend fun deleteAccount(): Result<Unit> = engine.deleteAccount()

    override suspend fun currentUserIdToken(forceRefresh: Boolean): Result<String> =
        engine.currentUserIdToken(forceRefresh)

    actual override suspend fun signOut(): Unit = engine.signOut()

    actual override fun currentUser(): KMPAuthUser? = engine.currentUser()

    override val currentUserFlow: Flow<KMPAuthUser?> get() = engine.currentUserFlow

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
