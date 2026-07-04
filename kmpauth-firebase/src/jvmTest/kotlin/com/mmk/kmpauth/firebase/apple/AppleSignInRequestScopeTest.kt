package com.mmk.kmpauth.firebase.apple

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Characterization test locking the public [AppleSignInRequestScope] shape
 * shipped in 2.x: exactly two scope objects, both usable as list elements
 * (the default request-scope list of AppleButtonUiContainer is
 * [FullName, Email]).
 */
class AppleSignInRequestScopeTest {

    @Test
    fun requestScopeObjectsAreSingletonsAndDistinct() {
        val scopes: List<AppleSignInRequestScope> = listOf(
            AppleSignInRequestScope.FullName,
            AppleSignInRequestScope.Email,
        )

        assertEquals(2, scopes.distinct().size)
        assertTrue(AppleSignInRequestScope.FullName is AppleSignInRequestScope)
        assertTrue(AppleSignInRequestScope.Email is AppleSignInRequestScope)
    }
}
