package com.mmk.kmpauth.core.di

import com.mmk.kmpauth.core.KMPAuthInternalApi
import io.ktor.client.HttpClient
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Characterization test locking the 2.x DI lifecycle contract of
 * [LibDependencyInitializer] and [KMPKoinComponent].
 *
 * [LibDependencyInitializer] is a process-wide singleton with no reset hook,
 * so the whole lifecycle is asserted in one ordered test method — individual
 * test methods would depend on execution order.
 *
 * Contract locked here (the manual-DI replacement must preserve it):
 * 1. Resolving anything before initialize() fails with
 *    IllegalArgumentException("Make sure you invoked #initialize method").
 * 2. initialize() creates the container and makes HttpClient resolvable.
 * 3. initialize() is idempotent — subsequent calls are no-ops keeping the
 *    same container instance.
 */
@OptIn(KMPAuthInternalApi::class)
class DiLifecycleTest {

    private class TestComponent : KMPKoinComponent()

    @Test
    fun lifecycle_uninitializedFails_initializeResolves_reinitializeIsNoOp() {
        // 1. Before initialize(): resolution must fail with the exact message.
        if (LibDependencyInitializer.koinApp == null) {
            val failure = assertFailsWith<IllegalArgumentException> {
                TestComponent().getKoin()
            }
            assertTrue(
                failure.message.orEmpty().contains("Make sure you invoked #initialize method"),
                "Unexpected error message: ${failure.message}"
            )
        }

        // 2. initialize() creates the container; HttpClient is registered.
        LibDependencyInitializer.initialize()
        val koinApp = LibDependencyInitializer.koinApp
        assertNotNull(koinApp, "koinApp must be set after initialize()")
        val httpClient = koinApp.koin.get<HttpClient>()
        assertNotNull(httpClient)

        // 3. Re-initialize is a no-op: same container instance survives.
        LibDependencyInitializer.initialize()
        assertSame(koinApp, LibDependencyInitializer.koinApp)

        // KMPKoinComponent now resolves through the initialized container.
        assertSame(koinApp.koin, TestComponent().getKoin())
    }
}
