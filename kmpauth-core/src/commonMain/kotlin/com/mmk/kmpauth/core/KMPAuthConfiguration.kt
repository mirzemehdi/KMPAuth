package com.mmk.kmpauth.core

import com.mmk.kmpauth.core.auth.AuthProviderBackend
import com.mmk.kmpauth.core.auth.KMPAuthBackend
import com.mmk.kmpauth.core.logger.KMPAuthLogger
import com.mmk.kmpauth.core.logger.currentLogger

/**
 * Configuration scope of [KMPAuth.initialize]. Provider modules contribute
 * their own setup as extension functions on this scope — e.g.
 * `kmpauth-google` adds `google(...)`:
 *
 * ```
 * KMPAuth.initialize {
 *     logger { println("KMPAuthLog: $it") }
 *     google(GoogleAuthCredentials(serverId = WebClientId))
 * }
 * ```
 */
public class KMPAuthConfiguration internal constructor() {

    /** Replaces KMPAuth's logger; by default logs go to the platform console. */
    @OptIn(KMPAuthInternalApi::class)
    public fun logger(logger: KMPAuthLogger) {
        currentLogger = logger
    }

    /**
     * Registers a custom auth backend under its
     * [AuthProviderBackend.backendId]. Not needed with `kmpauth-firebase`
     * (self-registers) or `kmpauth-supabase` (`supabase(url, apiKey)`).
     * The first registered backend becomes the default; [replace] true
     * additionally makes this one the default.
     */
    public fun backendProvider(backend: AuthProviderBackend, replace: Boolean = true) {
        KMPAuthBackend.register(backend, replace)
    }

    /**
     * Picks which registered backend serves the non-keyed `KMPAuth.*`
     * operations and the auth states by default. Only needed with several
     * backends registered — e.g. Firebase and Supabase together, where
     * Firebase (registered first at load) would otherwise stay the default:
     *
     * ```
     * KMPAuth.initialize {
     *     firebase(apiKey = ..., projectId = ..., applicationId = ...)
     *     supabase(url = ..., apiKey = ...)
     *     defaultBackendProvider("supabase")
     * }
     * ```
     */
    public fun defaultBackendProvider(id: String) {
        KMPAuthBackend.setDefault(id)
    }
}
