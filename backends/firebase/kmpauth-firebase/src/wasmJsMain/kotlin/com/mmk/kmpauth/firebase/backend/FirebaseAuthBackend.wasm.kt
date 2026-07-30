package com.mmk.kmpauth.firebase.backend

import com.mmk.kmpauth.core.auth.AuthCredential
import com.mmk.kmpauth.core.auth.AuthProviderBackend
import com.mmk.kmpauth.core.auth.EmailActionCodeSettings
import com.mmk.kmpauth.core.auth.KMPAuthUser
import com.mmk.kmpauth.core.auth.PhoneVerificationUi
import com.mmk.kmpauth.firebase.FirebaseBackendOptions

internal const val WASM_UNSUPPORTED_REASON: String =
    "Firebase browser-flow sign-in is not available on wasm: the Firebase " +
        "SDK (GitLive firebase-kotlin-sdk) has no wasm target, and the REST " +
        "engine cannot drive a browser OAuth flow. Email, anonymous and " +
        "id-token sign-in work via the REST engine."

/**
 * Web config for the wasm REST engine, stored by
 * `KMPAuth.initialize { firebase(apiKey = ..., ...) }` (the wasm actual of
 * `initializeFirebasePlatform`).
 */
internal var wasmFirebaseOptions: FirebaseBackendOptions? = null

internal fun wasmApiKeyOrFail(): String =
    wasmFirebaseOptions?.apiKey ?: throw IllegalStateException(
        "Firebase is not configured on wasm. Add firebase(apiKey = ..., " +
            "projectId = ..., applicationId = ...) inside KMPAuth.initialize { } " +
            "at application start."
    )

/**
 * Firebase backend on wasm: the GitLive SDK has no wasm target, so — as on
 * Desktop (#204) — the backend talks to the Firebase Auth REST API
 * (Identity Toolkit) over fetch. Serves email/password, anonymous,
 * email-link, password reset, reauthentication and id-token exchange
 * (Google's One Tap on web already produces the ID token to exchange).
 * Browser web flows (OAuth/GitHub/Microsoft/Apple) and phone are not
 * available; they report failed `Result`s with the reason. The session is
 * held in memory for the page lifetime.
 */
public actual object FirebaseAuthBackend : AuthProviderBackend {

    override val backendId: String get() = FIREBASE_BACKEND_ID

    private val engine: AuthProviderBackend by lazy {
        FirebaseRestAuthEngine(
            transport = FetchFirebaseRestTransport(),
            apiKeyProvider = ::wasmApiKeyOrFail,
            webFlowRunner = null,
        )
    }

    actual override suspend fun signIn(
        credential: AuthCredential,
        linkWithCurrentUser: Boolean,
    ): Result<KMPAuthUser> = engine.signIn(credential, linkWithCurrentUser)

    override suspend fun reauthenticate(credential: AuthCredential): Result<Unit> =
        engine.reauthenticate(credential)

    override suspend fun signUp(
        email: String,
        password: String,
    ): Result<KMPAuthUser> = engine.signUp(email, password)

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

    actual override suspend fun signOut(): Unit = engine.signOut()

    actual override fun currentUser(): KMPAuthUser? = engine.currentUser()
}
