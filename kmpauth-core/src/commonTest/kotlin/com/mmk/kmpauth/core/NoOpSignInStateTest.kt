package com.mmk.kmpauth.core

import kotlin.test.Test
import kotlin.test.assertFalse

@OptIn(KMPAuthInternalApi::class)
class NoOpSignInStateTest {

    @Test
    fun isNeverInProgress() {
        assertFalse(NoOpSignInState.isInProgress)
    }

    @Test
    fun launchDoesNothingAndNeverThrows() {
        // Previews render real sign-in buttons; clicking one must not blow up
        // just because application startup never ran.
        NoOpSignInState.launch()
        NoOpSignInState.launch()

        assertFalse(NoOpSignInState.isInProgress)
    }
}
