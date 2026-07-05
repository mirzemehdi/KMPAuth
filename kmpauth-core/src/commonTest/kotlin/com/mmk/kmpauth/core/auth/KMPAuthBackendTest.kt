package com.mmk.kmpauth.core.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Contract tests for the pluggable auth-backend registry introduced in 3.0.
 *
 * [KMPAuthBackend] is a process-wide singleton with no reset hook, so the
 * whole lifecycle is asserted in one ordered test method.
 */
class KMPAuthBackendTest {

    private class FakeBackend(private val name: String) : AuthProviderBackend {
        override suspend fun signIn(
            credential: AuthCredential,
            linkWithCurrentUser: Boolean,
        ): Result<KMPAuthUser> = Result.failure(UnsupportedOperationException(name))

        override suspend fun signOut() = Unit
        override fun currentUser(): KMPAuthUser? = null
    }

    @Test
    fun lifecycle_unregisteredFails_firstRegistrationWins_replaceOverrides() {
        // 1. Nothing registered: getOrNull() is null, require() throws with guidance.
        if (KMPAuthBackend.getOrNull() == null) {
            val failure = assertFailsWith<IllegalStateException> { KMPAuthBackend.require() }
            assertTrue(
                failure.message.orEmpty().contains("KMPAuthBackend.register"),
                "Error should explain how to register: ${failure.message}"
            )
        }

        // 2. First registration wins; a second default registration is a no-op.
        val first = FakeBackend("first")
        val second = FakeBackend("second")
        KMPAuthBackend.register(first)
        KMPAuthBackend.register(second)
        assertSame(first, KMPAuthBackend.getOrNull())
        assertSame(first, KMPAuthBackend.require())

        // 3. replace=true overrides — explicit choice beats lazy default.
        KMPAuthBackend.register(second, replace = true)
        assertSame(second, KMPAuthBackend.getOrNull())
    }

    @Test
    fun credentialModelsCarryProviderIdsAndValues() {
        val idToken = AuthCredential.IdToken(
            providerId = AuthProviderIds.GOOGLE,
            idToken = "token",
            accessToken = "access",
            rawNonce = "nonce",
        )
        assertEquals("google.com", idToken.providerId)
        assertEquals("token", idToken.idToken)
        assertEquals("access", idToken.accessToken)
        assertEquals("nonce", idToken.rawNonce)

        val webFlow = AuthCredential.OAuthWebFlow(
            providerId = AuthProviderIds.GITHUB,
            scopes = listOf("user:email"),
            customParameters = mapOf("allow_signup" to "false"),
        )
        assertEquals("github.com", webFlow.providerId)
        assertEquals(listOf("user:email"), webFlow.scopes)
        assertEquals(mapOf("allow_signup" to "false"), webFlow.customParameters)

        assertNotNull(AuthProviderIds.APPLE)
        assertEquals("facebook.com", AuthProviderIds.FACEBOOK)
    }
}
