package com.mmk.kmpauth.core.auth

import com.mmk.kmpauth.core.KMPAuthInternalApi

/**
 * Pluggable authentication backend. KMPAuth ships a Firebase implementation
 * (registered automatically by `kmpauth-firebase`); other backends — e.g.
 * Supabase — implement this interface and register via
 * [KMPAuthBackend.register].
 */
public interface AuthProviderBackend {

    /**
     * Stable id this backend registers under in [KMPAuthBackend] —
     * `"firebase"`, `"supabase"`, or your own for custom backends. Used to
     * fetch a specific backend in multi-backend apps:
     * `KMPAuth.requireBackendProvider("supabase")`.
     */
    public val backendId: String get() = "custom"

    /**
     * Exchanges [credential] for a signed-in session.
     *
     * @param credential Credential obtained from an identity provider.
     * @param linkWithCurrentUser When true, links the credential to the
     * currently signed-in user instead of creating a new session (matches
     * the 2.x `linkAccount` semantics of the UiContainer composables).
     * @return the signed-in (or linked) user, or a failed [Result].
     */
    public suspend fun signIn(
        credential: AuthCredential,
        linkWithCurrentUser: Boolean = false,
    ): Result<KMPAuthUser>

    /**
     * Reauthenticates the currently signed-in user with a fresh
     * [credential], without creating a new session.
     *
     * Backends like Firebase require a recent sign-in before
     * security-sensitive operations (deleting the account, changing the
     * password or email). Obtain a fresh credential — for email/password
     * an [AuthCredential.EmailPassword]; for token providers rerun the
     * provider flow (e.g. Google sign-in) and pass the resulting
     * [AuthCredential.IdToken] — then call this and retry the operation.
     *
     * The default implementation reports the operation as unsupported.
     */
    public suspend fun reauthenticate(credential: AuthCredential): Result<Unit> =
        Result.failure(
            UnsupportedOperationException(
                "This AuthProviderBackend does not support reauthentication."
            )
        )

    /**
     * Creates a new account with an email/password credential and signs it
     * in.
     *
     * The default implementation reports the operation as unsupported.
     */
    public suspend fun signUp(
        email: String,
        password: String,
    ): Result<KMPAuthUser> = Result.failure(
        UnsupportedOperationException(
            "This AuthProviderBackend does not support email/password sign-up."
        )
    )

    /**
     * Signs in anonymously, creating (or resuming) a temporary account.
     *
     * The default implementation reports the operation as unsupported.
     */
    public suspend fun signInAnonymously(): Result<KMPAuthUser> = Result.failure(
        UnsupportedOperationException(
            "This AuthProviderBackend does not support anonymous sign-in."
        )
    )

    /**
     * Signs in with a phone number: sends an SMS verification code, then
     * obtains the code the user received through
     * [PhoneVerificationUi.awaitVerificationCode] and completes sign-in
     * with it. Driven by `rememberPhoneAuthState`, which supplies the
     * [verificationUi] wired to its Compose state.
     *
     * The default implementation reports the operation as unsupported.
     *
     * @param phoneNumber Phone number in E.164 format (e.g. `+15551234567`).
     * @param verificationUi Hooks back into the caller's UI: code retrieval
     * and — where a backend needs it — the platform UI handle.
     * @param linkWithCurrentUser true links the phone credential to the
     * currently signed-in user instead of creating a new session.
     */
    public suspend fun signInWithPhone(
        phoneNumber: String,
        verificationUi: PhoneVerificationUi,
        linkWithCurrentUser: Boolean = false,
    ): Result<KMPAuthUser> = Result.failure(
        UnsupportedOperationException(
            "This AuthProviderBackend does not support phone number sign-in."
        )
    )

    /**
     * Sends a password-reset email for the account registered under
     * [email].
     *
     * The default implementation reports the operation as unsupported.
     *
     * @param email Address of the account to reset.
     * @param actionCodeSettings Optional link-handling configuration; when
     * null the backend uses its console-configured defaults.
     */
    public suspend fun sendPasswordResetEmail(
        email: String,
        actionCodeSettings: EmailActionCodeSettings? = null,
    ): Result<Unit> = Result.failure(
        UnsupportedOperationException(
            "This AuthProviderBackend does not support password-reset emails."
        )
    )

    /**
     * Sends a passwordless sign-in link (magic link) to [email]. Step 1 of
     * the email-link flow: persist the email locally, and when the user
     * opens the link, complete with [signInWithEmailLink].
     *
     * The default implementation reports the operation as unsupported.
     *
     * @param email Address to send the sign-in link to.
     * @param actionCodeSettings Where the link lands and which apps may
     * handle it; `canHandleCodeInApp` must be true for email-link sign-in.
     */
    public suspend fun sendSignInLinkToEmail(
        email: String,
        actionCodeSettings: EmailActionCodeSettings,
    ): Result<Unit> = Result.failure(
        UnsupportedOperationException(
            "This AuthProviderBackend does not support email-link sign-in."
        )
    )

    /**
     * Returns true when [link] (a deep link the app received) is an email
     * sign-in link that [signInWithEmailLink] can complete. The default
     * implementation returns false.
     */
    public fun isSignInWithEmailLink(link: String): Boolean = false

    /**
     * Completes passwordless sign-in with the [link] the user opened.
     * Step 2 of the email-link flow — see [sendSignInLinkToEmail].
     *
     * The default implementation reports the operation as unsupported.
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
    ): Result<KMPAuthUser> = Result.failure(
        UnsupportedOperationException(
            "This AuthProviderBackend does not support email-link sign-in."
        )
    )

    /** Signs out the current user. */
    public suspend fun signOut()

    /** Currently signed-in user, or null when signed out. */
    public fun currentUser(): KMPAuthUser?
}

/**
 * Process-wide registry of [AuthProviderBackend]s, keyed by
 * [AuthProviderBackend.backendId] — and itself an [AuthProviderBackend]
 * delegating to the **default** backend, so backend operations are called
 * directly:
 *
 * ```
 * KMPAuthBackend.currentUser()
 * KMPAuthBackend.signOut()
 * KMPAuthBackend.sendPasswordResetEmail(email)
 * ```
 *
 * Registration:
 * - `kmpauth-firebase` registers itself automatically (ServiceLoader on
 *   JVM/Android, eager load-time registration on iOS/JS/wasm).
 * - Other backends register in `KMPAuth.initialize { }` —
 *   `supabase(url, apiKey)` — or via `KMPAuth.registerBackendProvider`.
 *
 * The **default** backend serves every non-keyed call (and the auth
 * states' `LocalKMPAuthBackend` fallback). The first registered backend
 * becomes the default; registering more backends does NOT change it — with
 * Firebase and Supabase both present, Firebase (registered first at load)
 * stays the default and Supabase is fetched with [getOrNull]/[require] by
 * id. Pick a different default with [setDefault] (or
 * `defaultBackendProvider("supabase")` inside `KMPAuth.initialize { }`).
 *
 * When no backend is registered yet, `Result`-returning operations report
 * an [IllegalStateException] failure explaining how to register one;
 * [currentUser] returns null and [signOut] is a no-op.
 */
public object KMPAuthBackend : AuthProviderBackend {

    private val backends = LinkedHashMap<String, AuthProviderBackend>()
    private var defaultId: String? = null
    private var discoveryAttempted: Boolean = false

    /**
     * Registers [backend] under its [AuthProviderBackend.backendId]. The
     * first registered backend becomes the default; later registrations
     * are added alongside it without changing the default (re-registering
     * the same id swaps the instance in place).
     *
     * @param replace true additionally makes [backend] the default even if
     * a different backend currently is.
     */
    public fun register(backend: AuthProviderBackend, replace: Boolean = false) {
        discoverIfNeeded()
        backends[backend.backendId] = backend
        if (defaultId == null || replace) defaultId = backend.backendId
    }

    /**
     * Registers [backend] as an auto-provided entry — used by backend
     * modules' self-registration (`kmpauth-firebase`'s ServiceLoader /
     * load-time hooks and lazy state-side registration). Never replaces an
     * instance the app registered explicitly for the same id, and becomes
     * the default only when none exists yet.
     */
    @KMPAuthInternalApi
    public fun registerDefault(backend: AuthProviderBackend) {
        if (backends[backend.backendId] == null) backends[backend.backendId] = backend
        if (defaultId == null) defaultId = backend.backendId
    }

    /**
     * Makes the backend registered under [id] the default one — the target
     * of every non-keyed operation and of the auth states unless scoped via
     * `LocalKMPAuthBackend`.
     *
     * @throws IllegalStateException when no backend with [id] is registered.
     */
    public fun setDefault(id: String) {
        discoverIfNeeded()
        check(backends.containsKey(id)) {
            "No AuthProviderBackend registered under id '$id'. Registered: " +
                (backends.keys.takeIf { it.isNotEmpty() }?.joinToString() ?: "none")
        }
        defaultId = id
    }

    @OptIn(KMPAuthInternalApi::class)
    private fun discoverIfNeeded() {
        if (discoveryAttempted) return
        discoveryAttempted = true
        // On JVM/Android backend modules publish a ServiceLoader service;
        // on iOS/JS/wasm they self-register eagerly at load instead.
        loadPlatformBackends().forEach { registerDefault(it) }
    }

    private fun activeBackend(): AuthProviderBackend? {
        discoverIfNeeded()
        return defaultId?.let { backends[it] }
    }

    /** The default backend, or null if none is registered or discoverable. */
    public fun getOrNull(): AuthProviderBackend? = activeBackend()

    /** The backend registered under [id], or null. */
    public fun getOrNull(id: String): AuthProviderBackend? {
        discoverIfNeeded()
        return backends[id]
    }

    /**
     * The default backend, or an [IllegalStateException] explaining how to
     * register one. Rarely needed — [KMPAuthBackend] itself delegates every
     * backend operation.
     */
    public fun require(): AuthProviderBackend =
        activeBackend() ?: throw IllegalStateException(NO_BACKEND_MESSAGE)

    /**
     * The backend registered under [id], or an [IllegalStateException]
     * naming the ids that are registered.
     */
    public fun require(id: String): AuthProviderBackend =
        getOrNull(id) ?: throw IllegalStateException(
            "No AuthProviderBackend registered under id '$id'. Registered: " +
                (backends.keys.takeIf { it.isNotEmpty() }?.joinToString() ?: "none") +
                ". Register backends in KMPAuth.initialize { } (e.g. supabase(url, apiKey))."
        )

    override suspend fun signIn(
        credential: AuthCredential,
        linkWithCurrentUser: Boolean,
    ): Result<KMPAuthUser> =
        activeBackend()?.signIn(credential, linkWithCurrentUser) ?: noBackendFailure()

    override suspend fun reauthenticate(credential: AuthCredential): Result<Unit> =
        activeBackend()?.reauthenticate(credential) ?: noBackendFailure()

    override suspend fun signUp(email: String, password: String): Result<KMPAuthUser> =
        activeBackend()?.signUp(email, password) ?: noBackendFailure()

    override suspend fun signInAnonymously(): Result<KMPAuthUser> =
        activeBackend()?.signInAnonymously() ?: noBackendFailure()

    override suspend fun signInWithPhone(
        phoneNumber: String,
        verificationUi: PhoneVerificationUi,
        linkWithCurrentUser: Boolean,
    ): Result<KMPAuthUser> =
        activeBackend()?.signInWithPhone(phoneNumber, verificationUi, linkWithCurrentUser)
            ?: noBackendFailure()

    override suspend fun sendPasswordResetEmail(
        email: String,
        actionCodeSettings: EmailActionCodeSettings?,
    ): Result<Unit> =
        activeBackend()?.sendPasswordResetEmail(email, actionCodeSettings) ?: noBackendFailure()

    override suspend fun sendSignInLinkToEmail(
        email: String,
        actionCodeSettings: EmailActionCodeSettings,
    ): Result<Unit> =
        activeBackend()?.sendSignInLinkToEmail(email, actionCodeSettings) ?: noBackendFailure()

    override fun isSignInWithEmailLink(link: String): Boolean =
        activeBackend()?.isSignInWithEmailLink(link) ?: false

    override suspend fun signInWithEmailLink(
        email: String,
        link: String,
        linkAccount: Boolean,
    ): Result<KMPAuthUser> =
        activeBackend()?.signInWithEmailLink(email, link, linkAccount) ?: noBackendFailure()

    override suspend fun signOut() {
        activeBackend()?.signOut()
    }

    override fun currentUser(): KMPAuthUser? = activeBackend()?.currentUser()

    private const val NO_BACKEND_MESSAGE: String =
        "No AuthProviderBackend is registered. Add the kmpauth-firebase " +
            "dependency (its backend registers automatically), or call " +
            "KMPAuthBackend.register(...) with your own implementation."

    private fun <T> noBackendFailure(): Result<T> =
        Result.failure(IllegalStateException(NO_BACKEND_MESSAGE))
}

/**
 * Platform hook for discovering a default [AuthProviderBackend] when none
 * was registered explicitly. JVM and Android use `ServiceLoader` (backend
 * modules publish a `META-INF/services` entry); other platforms return
 * nothing — there, backend modules self-register eagerly at load instead.
 */
internal expect fun loadPlatformBackends(): List<AuthProviderBackend>
