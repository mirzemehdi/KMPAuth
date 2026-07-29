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
     * Sets the auth backend serving the `rememberXxxAuthState` flows and
     * the `KMPAuth` operations. Not needed with `kmpauth-firebase-core`,
     * which registers itself automatically — use this for a custom backend
     * (e.g. Supabase) or to override the discovered default.
     */
    public fun backendProvider(backend: AuthProviderBackend, replace: Boolean = true) {
        KMPAuthBackend.register(backend, replace)
    }
}
