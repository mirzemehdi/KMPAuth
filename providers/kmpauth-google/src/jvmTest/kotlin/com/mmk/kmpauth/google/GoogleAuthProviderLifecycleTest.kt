package com.mmk.kmpauth.google

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

/**
 * Characterization test locking the [GoogleAuthProvider] entry-point contract
 * on JVM (the JVM implementation has no platform prerequisites, so the full
 * create/get round-trip is safe to exercise here).
 *
 * The provider registry is process-global with no reset hook, so the whole
 * lifecycle is asserted in one ordered test method.
 *
 * Contract locked here (the manual-DI replacement must preserve it):
 * 1. get() before create() throws IllegalArgumentException with the exact
 *    documented message.
 * 2. create(credentials) returns a working provider.
 * 3. get() after create() succeeds; create() stays callable (idempotent
 *    initialization) and signOut() does not throw.
 */
class GoogleAuthProviderLifecycleTest {

    @Test
    fun lifecycle_getBeforeCreateFails_createThenGetSucceeds() = runTest {
        // 1. get() before create(): exact error message is public contract.
        val failure = assertFailsWith<IllegalArgumentException> {
            GoogleAuthProvider.get()
        }
        assertEquals(
            "Make sure you invoked GoogleAuthProvider #create method with providing credentials",
            failure.message,
        )

        // 2. create() returns a provider.
        val created = GoogleAuthProvider.create(GoogleAuthCredentials(serverId = "test-server-id"))
        assertNotNull(created)

        // 3. get() now succeeds; repeated create() is still safe; signOut()
        //    is a no-op on JVM and must not throw.
        assertNotNull(GoogleAuthProvider.get())
        assertNotNull(GoogleAuthProvider.create(GoogleAuthCredentials(serverId = "another-id")))
        created.signOut()
    }
}
