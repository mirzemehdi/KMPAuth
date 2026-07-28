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

    /** Signs out the current user. */
    public suspend fun signOut()

    /** Currently signed-in user, or null when signed out. */
    public fun currentUser(): KMPAuthUser?
}

/**
 * Process-wide registry for the active [AuthProviderBackend].
 *
 * `kmpauth-firebase` registers its backend automatically the first time a
 * Firebase container is used; a Supabase (or custom) backend should call
 * [register] once at application start. The first registration wins unless
 * [replace] is set — this keeps a lazily self-registering default from
 * overriding an explicitly chosen backend.
 */
public object KMPAuthBackend {

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
     * The active backend, or an [IllegalStateException] failure explaining
     * how to register one.
     */
    public fun require(): AuthProviderBackend =
        backend ?: throw IllegalStateException(
            "No AuthProviderBackend is registered. Add the kmpauth-firebase " +
                "dependency (its backend registers automatically), or call " +
                "KMPAuthBackend.register(...) with your own implementation."
        )
}
