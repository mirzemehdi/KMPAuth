package com.mmk.kmpauth.google

import androidx.compose.runtime.Composable
import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.di.KMPKoinComponent
import com.mmk.kmpauth.core.di.LibDependencyInitializer
import com.mmk.kmpauth.google.di.googleAuthModule
import org.koin.core.component.get

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
            return GoogleAuthProviderImpl.create(credentials)
        }

        internal fun get(): GoogleAuthProvider {
            return GoogleAuthProviderImpl.get()
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

    @OptIn(KMPAuthInternalApi::class)
    private object GoogleAuthProviderImpl : KMPKoinComponent() {
        fun create(credentials: GoogleAuthCredentials): GoogleAuthProvider {
            LibDependencyInitializer.initialize(googleAuthModule(credentials = credentials))
            return (this as KMPKoinComponent).get()
        }

        fun get(): GoogleAuthProvider {
            try {
                return (this as KMPKoinComponent).get()
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Make sure you invoked GoogleAuthProvider #create method with providing credentials")
            }

        }

    }
}
