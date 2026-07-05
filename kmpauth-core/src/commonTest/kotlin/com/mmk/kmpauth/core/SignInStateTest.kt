package com.mmk.kmpauth.core

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Contract tests for the SignInState launch guard: one flow at a time,
 * isInProgress driven around the block, reset even on failure.
 */
@OptIn(KMPAuthInternalApi::class, ExperimentalCoroutinesApi::class)
class SignInStateTest {

    @Test
    fun launchRunsBlockAndDrivesProgress() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val gate = CompletableDeferred<Unit>()
        var runs = 0
        val state = LaunchingSignInState(scope) {
            runs++
            gate.await()
        }

        assertFalse(state.isInProgress)
        state.launch() // unconfined: runs eagerly until the gate suspends
        assertTrue(state.isInProgress)

        gate.complete(Unit) // unconfined: resumes inline
        assertFalse(state.isInProgress)
        assertEquals(1, runs)
    }

    @Test
    fun launchWhileInProgressIsIgnored() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val gate = CompletableDeferred<Unit>()
        var runs = 0
        val state = LaunchingSignInState(scope) {
            runs++
            gate.await()
        }

        state.launch()
        state.launch() // double-tap while first flow is still running
        state.launch()
        gate.complete(Unit)

        assertEquals(1, runs)
        assertFalse(state.isInProgress)
    }

    @Test
    fun progressResetsWhenBlockThrows() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        var runs = 0
        val state = LaunchingSignInState(scope) {
            runs++
            throw IllegalStateException("boom")
        }

        state.launch() // exception is absorbed and logged, not rethrown
        assertFalse(state.isInProgress)

        // A failed flow must not lock the state: the next launch runs again.
        state.launch()
        assertEquals(2, runs)
        assertFalse(state.isInProgress)
    }
}
