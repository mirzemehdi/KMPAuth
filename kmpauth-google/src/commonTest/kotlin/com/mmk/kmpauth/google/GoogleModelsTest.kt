package com.mmk.kmpauth.google

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Characterization tests locking the public model shapes shipped in 2.x.
 * These models cross the library boundary (returned to user code), so their
 * defaults and value semantics are part of the public contract.
 */
class GoogleModelsTest {

    @Test
    fun googleUserDefaults() {
        val user = GoogleUser(idToken = "token-123")

        assertEquals("token-123", user.idToken)
        assertNull(user.accessToken)
        assertNull(user.email)
        assertEquals("", user.displayName)
        assertNull(user.profilePicUrl)
        assertNull(user.serverAuthCode)
    }

    @Test
    fun googleUserValueSemantics() {
        val a = GoogleUser(idToken = "t", email = "a@b.c", displayName = "Name")
        val b = GoogleUser(idToken = "t", email = "a@b.c", displayName = "Name")

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertEquals(a.copy(email = "x@y.z"), b.copy(email = "x@y.z"))
    }

    @Test
    fun googleAuthCredentialsValueSemantics() {
        val a = GoogleAuthCredentials(serverId = "web-client-id")
        val b = GoogleAuthCredentials(serverId = "web-client-id")

        assertEquals("web-client-id", a.serverId)
        assertEquals(a, b)
    }
}
