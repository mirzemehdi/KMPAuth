package com.mmk.kmpauth.core.auth

/**
 * Pluggable authentication backend. KMPAuth ships a Firebase implementation
 * (registered automatically by `kmpauth-firebase`); other backends — e.g.
 * Supabase — implement this interface and register via
 * [KMPAuthBackend.register].
 */
public interface AuthProviderBackend {

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
 * Process-wide registry for the active [AuthProviderBackend] — and itself
 * an [AuthProviderBackend] delegating to the registered one, so backend
 * operations are called directly:
 *
 * ```
 * KMPAuthBackend.currentUser()
 * KMPAuthBackend.signOut()
 * KMPAuthBackend.sendPasswordResetEmail(email)
 * KMPAuthBackend.reauthenticate(AuthCredential.EmailPassword(email, password))
 * ```
 *
 * `kmpauth-firebase` registers its backend automatically the first time a
 * Firebase sign-in state is used; a Supabase (or custom) backend should
 * call [register] once at application start. The first registration wins
 * unless [replace] is set — this keeps a lazily self-registering default
 * from overriding an explicitly chosen backend.
 *
 * When no backend is registered yet, `Result`-returning operations report
 * an [IllegalStateException] failure explaining how to register one;
 * [currentUser] returns null and [signOut] is a no-op.
 */
public object KMPAuthBackend : AuthProviderBackend {

    private var backend: AuthProviderBackend? = null

    /**
     * Registers [backend] as the process-wide auth backend.
     *
     * @param replace When false (default), registration is ignored if a
     * backend is already registered.
     */
    public fun register(backend: AuthProviderBackend, replace: Boolean = false) {
        if (this.backend == null || replace) {
            this.backend = backend
        }
    }

    /** The active backend, or null if none has been registered yet. */
    public fun getOrNull(): AuthProviderBackend? = backend

    /**
     * The active backend, or an [IllegalStateException] explaining how to
     * register one. Rarely needed — [KMPAuthBackend] itself delegates every
     * backend operation.
     */
    public fun require(): AuthProviderBackend =
        backend ?: throw IllegalStateException(NO_BACKEND_MESSAGE)

    override suspend fun signIn(
        credential: AuthCredential,
        linkWithCurrentUser: Boolean,
    ): Result<KMPAuthUser> =
        backend?.signIn(credential, linkWithCurrentUser) ?: noBackendFailure()

    override suspend fun reauthenticate(credential: AuthCredential): Result<Unit> =
        backend?.reauthenticate(credential) ?: noBackendFailure()

    override suspend fun signUp(email: String, password: String): Result<KMPAuthUser> =
        backend?.signUp(email, password) ?: noBackendFailure()

    override suspend fun signInAnonymously(): Result<KMPAuthUser> =
        backend?.signInAnonymously() ?: noBackendFailure()

    override suspend fun sendPasswordResetEmail(
        email: String,
        actionCodeSettings: EmailActionCodeSettings?,
    ): Result<Unit> =
        backend?.sendPasswordResetEmail(email, actionCodeSettings) ?: noBackendFailure()

    override suspend fun sendSignInLinkToEmail(
        email: String,
        actionCodeSettings: EmailActionCodeSettings,
    ): Result<Unit> =
        backend?.sendSignInLinkToEmail(email, actionCodeSettings) ?: noBackendFailure()

    override fun isSignInWithEmailLink(link: String): Boolean =
        backend?.isSignInWithEmailLink(link) ?: false

    override suspend fun signInWithEmailLink(
        email: String,
        link: String,
        linkAccount: Boolean,
    ): Result<KMPAuthUser> =
        backend?.signInWithEmailLink(email, link, linkAccount) ?: noBackendFailure()

    override suspend fun signOut() {
        backend?.signOut()
    }

    override fun currentUser(): KMPAuthUser? = backend?.currentUser()

    private const val NO_BACKEND_MESSAGE: String =
        "No AuthProviderBackend is registered. Add the kmpauth-firebase " +
            "dependency (its backend registers automatically), or call " +
            "KMPAuthBackend.register(...) with your own implementation."

    private fun <T> noBackendFailure(): Result<T> =
        Result.failure(IllegalStateException(NO_BACKEND_MESSAGE))
}
