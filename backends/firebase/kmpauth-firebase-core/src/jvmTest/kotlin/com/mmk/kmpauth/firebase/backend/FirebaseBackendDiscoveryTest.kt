package com.mmk.kmpauth.firebase.backend

import com.mmk.kmpauth.core.KMPAuthInternalApi
import com.mmk.kmpauth.core.auth.KMPAuthBackend
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/**
 * Locks the JVM auto-registration contract: having kmpauth-firebase-core on
 * the classpath is enough — ServiceLoader discovers the backend without an
 * explicit KMPAuth.registerBackendProvider call.
 */
@OptIn(KMPAuthInternalApi::class)
class FirebaseBackendDiscoveryTest {

    @Test
    fun backendIsDiscoveredFromClasspathWithoutExplicitRegistration() {
        val backend = KMPAuthBackend.getOrNull()
        assertNotNull(backend, "ServiceLoader should discover the Firebase backend")
        assertIs<FirebaseAuthBackendService>(backend)
    }
}
