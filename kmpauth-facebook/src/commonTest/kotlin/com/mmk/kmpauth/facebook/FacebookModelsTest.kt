package com.mmk.kmpauth.facebook

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Characterization tests locking the public Facebook model shapes shipped
 * in 2.x. These cross the library boundary, so defaults and value semantics
 * are part of the public contract.
 */
class FacebookModelsTest {

    @Test
    fun facebookUserDefaults() {
        val user = FacebookUser()

        assertNull(user.accessToken)
        assertNull(user.nonce)
    }

    @Test
    fun facebookUserValueSemantics() {
        val a = FacebookUser(accessToken = "token", nonce = "nonce")
        val b = FacebookUser(accessToken = "token", nonce = "nonce")

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertEquals(a.copy(nonce = "other"), b.copy(nonce = "other"))
    }

    @Test
    fun requestScopeObjectsAreSingletonsAndDistinct() {
        val scopes: List<FacebookSignInRequestScope> = listOf(
            FacebookSignInRequestScope.PublicProfile,
            FacebookSignInRequestScope.Email,
        )

        assertEquals(2, scopes.distinct().size)
        assertTrue(FacebookSignInRequestScope.PublicProfile is FacebookSignInRequestScope)
        assertTrue(FacebookSignInRequestScope.Email is FacebookSignInRequestScope)
    }
}
