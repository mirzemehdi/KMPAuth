package com.mmk.kmpauth.core

import com.mmk.kmpauth.core.auth.AuthCredential
import com.mmk.kmpauth.core.auth.AuthProviderBackend
import com.mmk.kmpauth.core.auth.EmailActionCodeSettings
import com.mmk.kmpauth.core.auth.KMPAuthBackend
import com.mmk.kmpauth.core.auth.KMPAuthUser
import com.mmk.kmpauth.core.auth.PhoneVerificationUi
import com.mmk.kmpauth.core.logger.KMPAuthLogger
import com.mmk.kmpauth.core.logger.currentLogger

/**
 * KMPAuth's main entry point for everything that isn't a launchable
 * sign-in flow. Sign-in flows are the `rememberXxxSignInState` /
 * `rememberXxxAuthState` composables; session and account operations
 * live here:
 *
 * ```
 * KMPAuth.currentUser()
 * KMPAuth.signOut()
 * KMPAuth.sendPasswordResetEmail(email)
 * KMPAuth.reauthenticate(AuthCredential.EmailPassword(email, password))
 * ```
 *
 * Operations are served by the registered [AuthProviderBackend] —
 * `kmpauth-firebase` registers its backend automatically the first time a
 * Firebase sign-in state is used; a custom backend (e.g. Supabase) is
 * registered once at application start via [registerBackendProvider].
 * When no backend is registered yet, `Result`-returning operations report
 * an `IllegalStateException` failure explaining how to register one;
 * [currentUser] returns null and [signOut] is a no-op.
 */
@OptIn(KMPAuthInternalApi::class)
public object KMPAuth {

    /**
     * One-stop initialization, called once at application start. Provider
     * modules contribute their setup as extensions on
     * [KMPAuthConfiguration] — e.g. `kmpauth-google` adds `google(...)`:
     *
     * ```
     * KMPAuth.initialize {
     *     logger { println("KMPAuthLog: $it") }
     *     google(GoogleAuthCredentials(serverId = WebClientId))
     *     // backendProvider(MyOwnBackend) — only for custom backends;
     *     // the Firebase backend registers itself automatically.
     * }
     * ```
     */
    public fun initialize(block: KMPAuthConfiguration.() -> Unit) {
        KMPAuthConfiguration().block()
    }

    /** Replaces KMPAuth's logger; by default logs go to the platform console. */
    public fun setLogger(logger: KMPAuthLogger) {
        currentLogger = logger
    }

    /**
     * Registers [backend] as the process-wide auth backend provider. Call
     * once at application start when using a custom backend; not needed
     * with `kmpauth-firebase`, which registers itself.
     *
     * @param replace When false (default), registration is ignored if a
     * backend is already registered — a lazily self-registering default
     * never overrides an explicitly chosen backend.
     */
    public fun registerBackendProvider(
        backend: AuthProviderBackend,
        replace: Boolean = false,
    ) {
        KMPAuthBackend.register(backend, replace)
    }

    /** The registered backend provider, or null if none is registered yet. */
    public fun getBackendProvider(): AuthProviderBackend? = KMPAuthBackend.getOrNull()

    /**
     * The registered backend provider, or an [IllegalStateException]
     * explaining how to register one. Rarely needed — the operations on
     * [KMPAuth] already delegate to it.
     */
    public fun requireBackendProvider(): AuthProviderBackend = KMPAuthBackend.require()

    /** Currently signed-in user, or null when signed out. */
    public fun currentUser(): KMPAuthUser? = KMPAuthBackend.currentUser()

    /** Signs out the current user. */
    public suspend fun signOut(): Unit = KMPAuthBackend.signOut()

    /**
     * Exchanges [credential] for a signed-in session — for callers that
     * obtained a credential outside the `rememberXxxSignInState` flows.
     *
     * @param credential Credential obtained from an identity provider.
     * @param linkWithCurrentUser When true, links the credential to the
     * currently signed-in user instead of creating a new session.
     */
    public suspend fun signIn(
        credential: AuthCredential,
        linkWithCurrentUser: Boolean = false,
    ): Result<KMPAuthUser> = KMPAuthBackend.signIn(credential, linkWithCurrentUser)

    /**
     * Reauthenticates the currently signed-in user with a fresh
     * [credential], without creating a new session.
     *
     * Backends like Firebase require a recent sign-in before
     * security-sensitive operations (deleting the account, changing the
     * password or email). Obtain a fresh credential — for email/password
     * an [AuthCredential.EmailPassword]; for token providers rerun the
     * provider flow (e.g. Google sign-in) and pass the resulting
     * [AuthCredential.IdToken] — then call this and retry the operation:
     *
     * ```
     * KMPAuth.reauthenticate(AuthCredential.EmailPassword(email, currentPassword))
     *     .onSuccess { /* delete the account / update the password */ }
     * ```
     */
    public suspend fun reauthenticate(credential: AuthCredential): Result<Unit> =
        KMPAuthBackend.reauthenticate(credential)

    /**
     * Creates a new account with an email/password credential and signs it
     * in. For the composable flow use
     * `rememberEmailAuthState(mode = EmailAuthMode.SignUp)`.
     */
    public suspend fun signUp(
        email: String,
        password: String,
    ): Result<KMPAuthUser> = KMPAuthBackend.signUp(email, password)

    /**
     * Signs in anonymously, creating (or resuming) a temporary account. For
     * the composable flow use `rememberAnonymousAuthState`.
     */
    public suspend fun signInAnonymously(): Result<KMPAuthUser> =
        KMPAuthBackend.signInAnonymously()

    /**
     * Signs in with a phone number: sends an SMS verification code and
     * completes sign-in with the code obtained through [verificationUi].
     * For the composable flow use `rememberPhoneAuthState`, which supplies
     * the [com.mmk.kmpauth.core.auth.PhoneVerificationUi] wired to its
     * Compose state.
     */
    public suspend fun signInWithPhone(
        phoneNumber: String,
        verificationUi: PhoneVerificationUi,
        linkWithCurrentUser: Boolean = false,
    ): Result<KMPAuthUser> =
        KMPAuthBackend.signInWithPhone(phoneNumber, verificationUi, linkWithCurrentUser)

    /**
     * Sends a password-reset email for the account registered under
     * [email].
     *
     * @param email Address of the account to reset.
     * @param actionCodeSettings Optional link-handling configuration; when
     * null the backend uses its console-configured defaults.
     */
    public suspend fun sendPasswordResetEmail(
        email: String,
        actionCodeSettings: EmailActionCodeSettings? = null,
    ): Result<Unit> = KMPAuthBackend.sendPasswordResetEmail(email, actionCodeSettings)

    /**
     * Sends a passwordless sign-in link (magic link) to [email]. Step 1 of
     * the email-link flow: persist the email locally, and when the user
     * opens the link in your app, complete with [signInWithEmailLink].
     *
     * @param email Address to send the sign-in link to.
     * @param actionCodeSettings Where the link lands and which apps may
     * handle it; `canHandleCodeInApp` must be true for email-link sign-in.
     */
    public suspend fun sendSignInLinkToEmail(
        email: String,
        actionCodeSettings: EmailActionCodeSettings,
    ): Result<Unit> = KMPAuthBackend.sendSignInLinkToEmail(email, actionCodeSettings)

    /**
     * Returns true when [link] (a deep link the app received) is an email
     * sign-in link that [signInWithEmailLink] can complete.
     */
    public fun isSignInWithEmailLink(link: String): Boolean =
        KMPAuthBackend.isSignInWithEmailLink(link)

    /**
     * Completes passwordless sign-in with the [link] the user opened.
     * Step 2 of the email-link flow — see [sendSignInLinkToEmail].
     *
     * @param email The address the link was sent to (persisted in step 1).
     * @param link The full deep link the app received.
     * @param linkAccount true links the email credential to the currently
     * signed-in user instead of creating a new session — e.g. to upgrade
     * an anonymous user to a permanent account.
     */
    public suspend fun signInWithEmailLink(
        email: String,
        link: String,
        linkAccount: Boolean = false,
    ): Result<KMPAuthUser> = KMPAuthBackend.signInWithEmailLink(email, link, linkAccount)
}
