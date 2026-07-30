package com.mmk.kmpauth.supabase

import com.mmk.kmpauth.core.auth.AuthCredential
import com.mmk.kmpauth.core.auth.AuthProviderIds
import com.mmk.kmpauth.core.auth.EmailActionCodeSettings
import com.mmk.kmpauth.core.auth.KMPAuthUserCollisionException
import io.ktor.client.engine.mock.toByteArray
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Exercises the backend's operation mapping against canned GoTrue responses
 * (Ktor MockEngine — no network): which endpoint and grant each KMPAuth
 * operation hits, how the Supabase user maps to KMPAuthUser, and which
 * credentials are rejected as unsupported before any request is made.
 */
class SupabaseAuthBackendTest {

    @Test
    fun emailPasswordSignInUsesPasswordGrantAndMapsUser() = runTest {
        val engine = RecordingMockEngine { jsonResponse(TEST_SESSION_JSON) }
        val backend = SupabaseAuthBackend(engine.client)

        val result = backend.signIn(AuthCredential.EmailPassword("user@example.com", "secret"))

        val user = result.getOrThrow()
        assertEquals("user-123", user.uid)
        assertEquals("user@example.com", user.email)
        assertEquals("Test User", user.displayName)
        assertEquals("https://example.com/a.png", user.photoUrl)
        assertEquals("email", user.providerId)

        val request = engine.requests.single()
        assertTrue(request.url.encodedPath.endsWith("/auth/v1/token"))
        assertEquals("password", request.url.parameters["grant_type"])
        val body = request.body.toByteArray().decodeToString()
        assertTrue("user@example.com" in body && "secret" in body)
    }

    @Test
    fun googleIdTokenSignInUsesIdTokenGrant() = runTest {
        val engine = RecordingMockEngine { jsonResponse(TEST_SESSION_JSON) }
        val backend = SupabaseAuthBackend(engine.client)

        val result = backend.signIn(
            AuthCredential.IdToken(
                providerId = AuthProviderIds.GOOGLE,
                idToken = "google-id-token",
                rawNonce = "raw-nonce",
            )
        )

        assertTrue(result.isSuccess)
        val request = engine.requests.single()
        assertEquals("id_token", request.url.parameters["grant_type"])
        val body = request.body.toByteArray().decodeToString()
        assertTrue("google-id-token" in body)
        assertTrue("\"provider\":\"google\"" in body)
        assertTrue("raw-nonce" in body)
    }

    @Test
    fun classicFacebookAccessTokenIsRejectedWithoutRequest() = runTest {
        val engine = RecordingMockEngine { jsonResponse(TEST_SESSION_JSON) }
        val backend = SupabaseAuthBackend(engine.client)

        // No rawNonce = classic Facebook login (access token), which
        // Supabase's id_token grant cannot exchange.
        val result = backend.signIn(
            AuthCredential.IdToken(
                providerId = AuthProviderIds.FACEBOOK,
                idToken = "access-token",
                accessToken = "access-token",
            )
        )

        assertIs<UnsupportedOperationException>(result.exceptionOrNull())
        assertTrue(engine.requests.isEmpty(), "unsupported credentials must fail before any request")
    }

    @Test
    fun facebookLimitedLoginTokenIsExchanged() = runTest {
        val engine = RecordingMockEngine { jsonResponse(TEST_SESSION_JSON) }
        val backend = SupabaseAuthBackend(engine.client)

        val result = backend.signIn(
            AuthCredential.IdToken(
                providerId = AuthProviderIds.FACEBOOK,
                idToken = "oidc-jwt",
                rawNonce = "nonce",
            )
        )

        assertTrue(result.isSuccess)
        val body = engine.requests.single().body.toByteArray().decodeToString()
        assertTrue("\"provider\":\"facebook\"" in body)
    }

    @Test
    fun oauthWebFlowRejectsUnknownProviderBeforeAnyRequest() = runTest {
        val engine = RecordingMockEngine { jsonResponse(TEST_SESSION_JSON) }
        val backend = SupabaseAuthBackend(engine.client)

        val result = backend.signIn(AuthCredential.OAuthWebFlow(providerId = "myspace.com"))

        assertIs<IllegalArgumentException>(result.exceptionOrNull())
        assertTrue(engine.requests.isEmpty())
    }

    @Test
    fun oauthWebFlowRejectsLinkingBeforeAnyRequest() = runTest {
        val engine = RecordingMockEngine { jsonResponse(TEST_SESSION_JSON) }
        val backend = SupabaseAuthBackend(engine.client)

        val result = backend.signIn(
            AuthCredential.OAuthWebFlow(providerId = AuthProviderIds.GITHUB),
            linkWithCurrentUser = true,
        )

        val error = result.exceptionOrNull()
        assertIs<UnsupportedOperationException>(error)
        assertTrue("linkIdentity" in error.message.orEmpty())
        assertTrue(engine.requests.isEmpty())
    }

    @Test
    fun oauthProviderMappingAcceptsFirebaseAndGoTrueIds() {
        assertEquals("github", supabaseOAuthProviderOrNull("github.com")?.name)
        assertEquals("github", supabaseOAuthProviderOrNull("github")?.name)
        assertEquals("azure", supabaseOAuthProviderOrNull("microsoft.com")?.name)
        assertEquals("azure", supabaseOAuthProviderOrNull("azure")?.name)
        assertEquals("gitlab", supabaseOAuthProviderOrNull("GitLab")?.name)
        assertNull(supabaseOAuthProviderOrNull("myspace.com"))
    }

    @Test
    fun linkingEmailPasswordCredentialIsUnsupported() = runTest {
        val engine = RecordingMockEngine { jsonResponse(TEST_SESSION_JSON) }
        val backend = SupabaseAuthBackend(engine.client)

        val result = backend.signIn(
            AuthCredential.EmailPassword("user@example.com", "secret"),
            linkWithCurrentUser = true,
        )

        assertIs<UnsupportedOperationException>(result.exceptionOrNull())
        assertTrue(engine.requests.isEmpty())
    }

    @Test
    fun signUpWithPendingEmailConfirmationReturnsUserWithoutSession() = runTest {
        // Email confirmation enabled: GoTrue answers with the created user
        // only - no access_token, no session.
        val engine = RecordingMockEngine { jsonResponse(TEST_USER_JSON) }
        val backend = SupabaseAuthBackend(engine.client)

        val result = backend.signUp("user@example.com", "secret")

        assertEquals("user-123", result.getOrThrow().uid)
        assertNull(backend.currentUser(), "no session may exist until the email is confirmed")
        assertTrue(engine.requests.single().url.encodedPath.endsWith("/auth/v1/signup"))
    }

    @Test
    fun anonymousSignInHitsSignupEndpointAndCreatesSession() = runTest {
        val engine = RecordingMockEngine { jsonResponse(TEST_SESSION_JSON) }
        val backend = SupabaseAuthBackend(engine.client)

        val result = backend.signInAnonymously()

        assertEquals("user-123", result.getOrThrow().uid)
        assertTrue(engine.requests.single().url.encodedPath.endsWith("/auth/v1/signup"))
    }

    @Test
    fun signInWithPhoneSendsOtpThenVerifiesCode() = runTest {
        val engine = RecordingMockEngine { request ->
            if (request.url.encodedPath.endsWith("/auth/v1/otp")) jsonResponse("{}")
            else jsonResponse(TEST_SESSION_JSON)
        }
        val backend = SupabaseAuthBackend(engine.client)

        val result = backend.signInWithPhone(
            phoneNumber = "+15551234567",
            verificationUi = object : com.mmk.kmpauth.core.auth.PhoneVerificationUi {
                override suspend fun awaitVerificationCode(): String = "123456"
            },
        )

        assertEquals("user-123", result.getOrThrow().uid)
        assertEquals(2, engine.requests.size)
        val otpRequest = engine.requests[0]
        assertTrue(otpRequest.url.encodedPath.endsWith("/auth/v1/otp"))
        assertTrue("+15551234567" in otpRequest.body.toByteArray().decodeToString())
        val verifyRequest = engine.requests[1]
        assertTrue(verifyRequest.url.encodedPath.endsWith("/auth/v1/verify"))
        val verifyBody = verifyRequest.body.toByteArray().decodeToString()
        assertTrue("123456" in verifyBody)
        assertTrue("sms" in verifyBody)
    }

    @Test
    fun signInWithPhoneRejectsLinkingBeforeAnyRequest() = runTest {
        val engine = RecordingMockEngine { jsonResponse(TEST_SESSION_JSON) }
        val backend = SupabaseAuthBackend(engine.client)

        val result = backend.signInWithPhone(
            phoneNumber = "+15551234567",
            verificationUi = object : com.mmk.kmpauth.core.auth.PhoneVerificationUi {
                override suspend fun awaitVerificationCode(): String = "123456"
            },
            linkWithCurrentUser = true,
        )

        val error = result.exceptionOrNull()
        assertIs<UnsupportedOperationException>(error)
        assertTrue("updateUser" in error.message.orEmpty())
        assertTrue(engine.requests.isEmpty())
    }

    @Test
    fun sendPasswordResetEmailHitsRecoverWithRedirectUrl() = runTest {
        val engine = RecordingMockEngine { jsonResponse("{}") }
        val backend = SupabaseAuthBackend(engine.client)

        val result = backend.sendPasswordResetEmail(
            email = "user@example.com",
            actionCodeSettings = EmailActionCodeSettings(url = "https://example.com/reset"),
        )

        assertTrue(result.isSuccess)
        val request = engine.requests.single()
        assertTrue(request.url.encodedPath.endsWith("/auth/v1/recover"))
        assertEquals("https://example.com/reset", request.url.parameters["redirect_to"])
    }

    @Test
    fun sendSignInLinkToEmailHitsOtpEndpoint() = runTest {
        val engine = RecordingMockEngine { jsonResponse("{}") }
        val backend = SupabaseAuthBackend(engine.client)

        val result = backend.sendSignInLinkToEmail(
            email = "user@example.com",
            actionCodeSettings = EmailActionCodeSettings(
                url = "https://example.com/finish",
                canHandleCodeInApp = true,
            ),
        )

        assertTrue(result.isSuccess)
        val request = engine.requests.single()
        assertTrue(request.url.encodedPath.endsWith("/auth/v1/otp"))
        assertEquals("https://example.com/finish", request.url.parameters["redirect_to"])
        assertTrue("user@example.com" in request.body.toByteArray().decodeToString())
    }

    @Test
    fun signInWithEmailLinkVerifiesTokenHash() = runTest {
        val engine = RecordingMockEngine { jsonResponse(TEST_SESSION_JSON) }
        val backend = SupabaseAuthBackend(engine.client)

        val result = backend.signInWithEmailLink(
            email = "user@example.com",
            link = "https://example.com/finish?token_hash=hash-abc&type=magiclink",
        )

        assertEquals("user-123", result.getOrThrow().uid)
        val request = engine.requests.single()
        assertTrue(request.url.encodedPath.endsWith("/auth/v1/verify"))
        val body = request.body.toByteArray().decodeToString()
        assertTrue("hash-abc" in body)
    }

    @Test
    fun signInWithEmailLinkRejectsLinkAccount() = runTest {
        val engine = RecordingMockEngine { jsonResponse(TEST_SESSION_JSON) }
        val backend = SupabaseAuthBackend(engine.client)

        val result = backend.signInWithEmailLink(
            email = "user@example.com",
            link = "https://example.com/finish?token_hash=hash-abc&type=magiclink",
            linkAccount = true,
        )

        assertIs<UnsupportedOperationException>(result.exceptionOrNull())
        assertTrue(engine.requests.isEmpty())
    }

    @Test
    fun reauthenticateWithoutSignedInUserFails() = runTest {
        val engine = RecordingMockEngine { jsonResponse(TEST_SESSION_JSON) }
        val backend = SupabaseAuthBackend(engine.client)

        val result = backend.reauthenticate(
            AuthCredential.EmailPassword("user@example.com", "secret")
        )

        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertTrue(engine.requests.isEmpty())
    }

    @Test
    fun reauthenticateWithMatchingCredentialSucceeds() = runTest {
        val engine = RecordingMockEngine { jsonResponse(TEST_SESSION_JSON) }
        val backend = SupabaseAuthBackend(engine.client)
        backend.signIn(AuthCredential.EmailPassword("user@example.com", "secret")).getOrThrow()

        val result = backend.reauthenticate(
            AuthCredential.EmailPassword("user@example.com", "secret")
        )

        assertTrue(result.isSuccess)
    }

    @Test
    fun alreadyRegisteredSignUpSurfacesTypedCollision() = runTest {
        val engine = RecordingMockEngine {
            errorResponse(
                """{"code":422,"error_code":"user_already_exists","msg":"User already registered"}""",
                io.ktor.http.HttpStatusCode.UnprocessableEntity,
            )
        }
        val backend = SupabaseAuthBackend(engine.client)

        val result = backend.signUp("user@example.com", "secret")

        assertIs<KMPAuthUserCollisionException>(result.exceptionOrNull())
    }

    @Test
    fun deleteAccountIsUnsupportedAndNamesTheEdgeFunctionAlternative() = runTest {
        val engine = RecordingMockEngine { jsonResponse(TEST_SESSION_JSON) }
        val backend = SupabaseAuthBackend(engine.client)

        val result = backend.deleteAccount()

        val error = assertIs<UnsupportedOperationException>(result.exceptionOrNull())
        assertContains(error.message!!, "Edge")
        assertTrue(engine.requests.isEmpty())
    }

    @Test
    fun signOutClearsCurrentUser() = runTest {
        val engine = RecordingMockEngine { request ->
            if (request.url.encodedPath.endsWith("/auth/v1/logout")) jsonResponse("{}")
            else jsonResponse(TEST_SESSION_JSON)
        }
        val backend = SupabaseAuthBackend(engine.client)
        backend.signIn(AuthCredential.EmailPassword("user@example.com", "secret")).getOrThrow()

        backend.signOut()

        assertNull(backend.currentUser())
    }
}
