package com.mmk.kmpauth.google

import com.mmk.kmpauth.core.auth.AuthCredential
import com.mmk.kmpauth.core.auth.AuthProviderBackend
import com.mmk.kmpauth.core.auth.KMPAuthUser
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Characterization tests for the orchestration behind
 * GoogleButtonUiContainerFirebase, locking the 2.x contract now that it
 * routes through the pluggable backend:
 * - null idToken fails fast with the exact 2.x message
 * - credential carries idToken/accessToken with the google.com provider id
 * - linkAccount flag is forwarded as linkWithCurrentUser
 * - backend failures pass through unchanged
 */
class GoogleAuthSignInHandlerTest {

    private class RecordingBackend(
        private val result: Result<KMPAuthUser>,
    ) : AuthProviderBackend {
        var credential: AuthCredential? = null
        var linkWithCurrentUser: Boolean? = null

        override suspend fun signIn(
            credential: AuthCredential,
            linkWithCurrentUser: Boolean,
        ): Result<KMPAuthUser> {
            this.credential = credential
            this.linkWithCurrentUser = linkWithCurrentUser
            return result
        }

        override suspend fun signOut() = Unit
        override fun currentUser(): KMPAuthUser? = null
    }

    private class FakeUser(override val raw: Any?) : KMPAuthUser {
        override val uid: String = "uid"
        override val email: String? = null
        override val displayName: String? = null
        override val photoUrl: String? = null
        override val providerId: String? = "google.com"
    }

    @Test
    fun nullIdTokenFailsFastWithExactMessage() = runTest {
        val backend = RecordingBackend(Result.failure(IllegalStateException("unused")))
        val handler = GoogleAuthSignInHandler(backend)

        val result = handler.signIn(googleUser = null, linkAccount = false)

        assertTrue(result.isFailure)
        assertEquals("Idtoken is null", result.exceptionOrNull()?.message)
        assertNull(backend.credential, "Backend must not be called without an idToken")
    }

    @Test
    fun forwardsTokensProviderIdAndLinkFlag() = runTest {
        val backend = RecordingBackend(Result.success(FakeUser(raw = null)))
        val handler = GoogleAuthSignInHandler(backend)

        handler.signIn(
            googleUser = GoogleUser(idToken = "id-token", accessToken = "access-token"),
            linkAccount = true,
        )

        val credential = backend.credential as AuthCredential.IdToken
        assertEquals("google.com", credential.providerId)
        assertEquals("id-token", credential.idToken)
        assertEquals("access-token", credential.accessToken)
        assertEquals(true, backend.linkWithCurrentUser)
    }

    @Test
    fun backendFailurePassesThroughUnchanged() = runTest {
        val boom = RuntimeException("boom")
        val backend = RecordingBackend(Result.failure(boom))
        val handler = GoogleAuthSignInHandler(backend)

        val result = handler.signIn(GoogleUser(idToken = "t"), linkAccount = false)

        assertTrue(result.isFailure)
        assertEquals(boom, result.exceptionOrNull())
    }

    @Test
    fun backendUserPassesThroughUnchanged() = runTest {
        // The handler returns the backend's KMPAuthUser as-is; unwrapping to
        // the native FirebaseUser is the deprecated container's job.
        val user = FakeUser(raw = "not-a-firebase-user")
        val backend = RecordingBackend(Result.success(user))
        val handler = GoogleAuthSignInHandler(backend)

        val result = handler.signIn(GoogleUser(idToken = "t"), linkAccount = false)

        assertTrue(result.isSuccess)
        assertEquals(user, result.getOrNull())
    }
}
