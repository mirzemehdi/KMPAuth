package com.mmk.kmpauth.google

import androidx.compose.runtime.Composable
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.logger.currentLogger

/**
 * Google Auth Provider class
 */

public interface GoogleAuthProvider {

    public companion object {
        /**
         * Creates new [GoogleAuthProvider] class instance
         * @param credentials [GoogleAuthCredentials] instance.
         * @return returns [GoogleAuthProvider]
         */
        public fun create(credentials: GoogleAuthCredentials): GoogleAuthProvider {
            return GoogleAuthProviderHolder.create(credentials)
        }

        internal fun get(): GoogleAuthProvider {
            return GoogleAuthProviderHolder.get()
        }
    }

    /**
     * Returns [GoogleAuthUiProvider] that can be used in [Composable] function.
     * @return [GoogleAuthUiProvider]
     */
    @Composable
    public fun getUiProvider(): GoogleAuthUiProvider

    /**
     * Signs out user and clears credentials.
     * This function can be called also from data layer. It is not necessary
     * to call #signOut function only from UI layer
     */
    public suspend fun signOut()
}

/**
 * Process-wide holder replacing the former Koin container. Mirrors the 2.x
 * initialization contract: the first create() wins (subsequent calls are
 * no-ops returning the existing provider), and get() before create() fails
 * with the documented error message.
 */
private object GoogleAuthProviderHolder {

    private var instance: GoogleAuthProvider? = null

    @OptIn(KMPAuthInternalApi::class)
    fun create(credentials: GoogleAuthCredentials): GoogleAuthProvider {
        return instance ?: createGoogleAuthProvider(credentials).also {
            instance = it
            currentLogger.log("KMPAuth Library is initialized")
        }
    }

    fun get(): GoogleAuthProvider {
        return instance
            ?: throw IllegalArgumentException("Make sure you invoked GoogleAuthProvider #create method with providing credentials")
    }
}

/**
 * Creates the platform-specific [GoogleAuthProvider] implementation.
 */
internal expect fun createGoogleAuthProvider(credentials: GoogleAuthCredentials): GoogleAuthProvider
