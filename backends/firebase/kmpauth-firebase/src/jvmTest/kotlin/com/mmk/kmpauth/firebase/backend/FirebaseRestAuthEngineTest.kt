package com.mmk.kmpauth.firebase.backend

import com.mmk.kmpauth.core.auth.AuthCredential
import com.mmk.kmpauth.core.auth.AuthProviderIds
import com.mmk.kmpauth.core.auth.EmailActionCodeSettings
import com.mmk.kmpauth.core.auth.KMPAuthRecentLoginRequiredException
import com.mmk.kmpauth.core.auth.KMPAuthUserCollisionException
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Locks the Desktop REST engine's request/response contract against the
 * Firebase Auth (Identity Toolkit) API with a scripted transport — no
 * network involved.
 */
class FirebaseRestAuthEngineTest {

    private class ScriptedTransport : FirebaseRestTransport {
        val calls = mutableListOf<Pair<String, String>>()
        val responses = ArrayDeque<String>()

        override suspend fun post(url: String, jsonBody: String): String {
            calls += url to jsonBody
            return responses.removeFirstOrNull() ?: """{"error":{"message":"NO_SCRIPTED_RESPONSE"}}"""
        }
    }

    private class FakeStorage : FirebaseSessionStorage {
        val values = mutableMapOf<String, String>()
        override fun load(key: String): String? = values[key]
        override fun save(key: String, value: String?) {
            if (value == null) values.remove(key) else values[key] = value
        }
    }

    private fun engine(
        transport: ScriptedTransport,
        storage: FirebaseSessionStorage? = null,
    ) = FirebaseRestAuthEngine(
        transport = transport,
        apiKeyProvider = { "test-key" },
        sessionStorage = storage,
    )

    private val userResponse =
        """{"localId":"uid-1","email":"a@b.c","idToken":"tok","refreshToken":"r"}"""
    private val lookupResponse =
        """{"users":[{"displayName":"Ada","photoUrl":"http://p","email":"a@b.c"}]}"""

    @Test
    fun emailSignInParsesUserAndSetsSession() = runTest {
        val transport = ScriptedTransport()
        transport.responses += userResponse
        transport.responses += lookupResponse
        val engine = engine(transport)

        val result = engine.signIn(AuthCredential.EmailPassword("a@b.c", "pw"))

        val user = result.getOrThrow()
        assertEquals("uid-1", user.uid)
        assertEquals("Ada", user.displayName)
        assertEquals("a@b.c", user.email)
        assertEquals("uid-1", engine.currentUser()?.uid)
        assertContains(transport.calls[0].first, "accounts:signInWithPassword?key=test-key")
        assertContains(transport.calls[0].second, "\"email\":\"a@b.c\"")
    }

    @Test
    fun errorResponseBecomesFailureWithCode() = runTest {
        val transport = ScriptedTransport()
        transport.responses += """{"error":{"message":"INVALID_PASSWORD"}}"""
        val engine = engine(transport)

        val result = engine.signIn(AuthCredential.EmailPassword("a@b.c", "bad"))

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()!!.message!!, "INVALID_PASSWORD")
        assertNull(engine.currentUser())
    }

    @Test
    fun anonymousSignInResumesExistingAnonymousSession() = runTest {
        val transport = ScriptedTransport()
        transport.responses += """{"localId":"anon-1","idToken":"tok"}"""
        val engine = engine(transport)

        val first = engine.signInAnonymously().getOrThrow()
        val second = engine.signInAnonymously().getOrThrow()

        assertEquals("anon-1", first.uid)
        assertEquals("anon-1", second.uid)
        assertEquals(1, transport.calls.size, "second call must reuse the session")
        assertContains(transport.calls[0].first, "accounts:signUp")
    }

    @Test
    fun googleIdTokenGoesThroughSignInWithIdp() = runTest {
        val transport = ScriptedTransport()
        transport.responses += """{"localId":"uid-g","idToken":"tok","displayName":"G"}"""
        val engine = engine(transport)

        engine.signIn(
            AuthCredential.IdToken(AuthProviderIds.GOOGLE, idToken = "g-token")
        ).getOrThrow()

        val (url, body) = transport.calls.single()
        assertContains(url, "accounts:signInWithIdp")
        assertContains(body, "id_token=g-token")
        assertContains(body, "providerId=google.com")
    }

    @Test
    fun linkingSendsCurrentSessionIdToken() = runTest {
        val transport = ScriptedTransport()
        transport.responses += """{"localId":"anon-1","idToken":"anon-tok"}"""
        transport.responses += """{"localId":"anon-1","idToken":"tok2","displayName":"G"}"""
        val engine = engine(transport)

        engine.signInAnonymously().getOrThrow()
        engine.signIn(
            AuthCredential.IdToken(AuthProviderIds.GOOGLE, idToken = "g-token"),
            linkWithCurrentUser = true,
        ).getOrThrow()

        assertContains(transport.calls[1].second, "\"idToken\":\"anon-tok\"")
    }

    @Test
    fun facebookLimitedUsesOidcNonceAndClassicUsesAccessToken() = runTest {
        val transport = ScriptedTransport()
        transport.responses += """{"localId":"u","idToken":"t","displayName":"F"}"""
        transport.responses += """{"localId":"u","idToken":"t","displayName":"F"}"""
        val engine = engine(transport)

        engine.signIn(
            AuthCredential.IdToken(AuthProviderIds.FACEBOOK, idToken = "jwt", rawNonce = "n1")
        ).getOrThrow()
        engine.signIn(
            AuthCredential.IdToken(AuthProviderIds.FACEBOOK, idToken = "at", accessToken = "at")
        ).getOrThrow()

        assertContains(transport.calls[0].second, "id_token=jwt")
        assertContains(transport.calls[0].second, "nonce=n1")
        assertContains(transport.calls[1].second, "access_token=at")
        assertFalse(transport.calls[1].second.contains("nonce="))
    }

    @Test
    fun passwordResetSendsOobCodeRequest() = runTest {
        val transport = ScriptedTransport()
        transport.responses += """{"email":"a@b.c"}"""
        val engine = engine(transport)

        engine.sendPasswordResetEmail(
            "a@b.c",
            EmailActionCodeSettings(url = "https://x/finish", canHandleCodeInApp = true),
        ).getOrThrow()

        val (url, body) = transport.calls.single()
        assertContains(url, "accounts:sendOobCode")
        assertContains(body, "\"requestType\":\"PASSWORD_RESET\"")
        assertContains(body, "\"continueUrl\":\"https://x/finish\"")
    }

    @Test
    fun emailLinkSignInExtractsOobCode() = runTest {
        val transport = ScriptedTransport()
        transport.responses += userResponse
        transport.responses += lookupResponse
        val engine = engine(transport)
        val link = "https://app.example.com/finish?mode=signIn&oobCode=CODE123&apiKey=k"

        assertTrue(engine.isSignInWithEmailLink(link))
        engine.signInWithEmailLink("a@b.c", link).getOrThrow()

        assertContains(transport.calls[0].first, "accounts:signInWithEmailLink")
        assertContains(transport.calls[0].second, "\"oobCode\":\"CODE123\"")
    }

    @Test
    fun reauthenticateRejectsDifferentUser() = runTest {
        val transport = ScriptedTransport()
        transport.responses += """{"localId":"uid-1","idToken":"t1","displayName":"A"}"""
        transport.responses += """{"localId":"OTHER","idToken":"t2","displayName":"B"}"""
        val engine = engine(transport)

        engine.signIn(AuthCredential.EmailPassword("a@b.c", "pw")).getOrThrow()
        val result = engine.reauthenticate(AuthCredential.EmailPassword("other@b.c", "pw"))

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()!!.message!!, "different user")
    }

    @Test
    fun currentUserIdTokenReturnsCachedToken() = runTest {
        val transport = ScriptedTransport()
        transport.responses += userResponse
        transport.responses += lookupResponse
        val engine = engine(transport)

        engine.signIn(AuthCredential.EmailPassword("a@b.c", "pw")).getOrThrow()

        assertEquals("tok", engine.currentUserIdToken().getOrThrow())
        assertEquals(2, transport.calls.size, "cached token must not hit the network")
    }

    @Test
    fun currentUserIdTokenForceRefreshExchangesRefreshToken() = runTest {
        val transport = ScriptedTransport()
        transport.responses += userResponse
        transport.responses += lookupResponse
        transport.responses += """{"id_token":"fresh-tok","refresh_token":"r2"}"""
        val engine = engine(transport)

        engine.signIn(AuthCredential.EmailPassword("a@b.c", "pw")).getOrThrow()
        val token = engine.currentUserIdToken(forceRefresh = true).getOrThrow()

        assertEquals("fresh-tok", token)
        assertContains(transport.calls[2].first, "securetoken.googleapis.com/v1/token")
        assertContains(transport.calls[2].second, "\"refresh_token\":\"r\"")
        // The refreshed token becomes the session token.
        assertEquals("fresh-tok", engine.currentUserIdToken().getOrThrow())
    }

    @Test
    fun currentUserIdTokenWithoutSessionFails() = runTest {
        val result = engine(ScriptedTransport()).currentUserIdToken()

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()!!.message!!, "No signed-in user")
    }

    @Test
    fun currentUserFlowEmitsOnSignInAndSignOut() = runTest {
        val transport = ScriptedTransport()
        transport.responses += userResponse
        transport.responses += lookupResponse
        val engine = engine(transport)
        val states = mutableListOf<String?>()
        val job = backgroundScope.launch(kotlinx.coroutines.Dispatchers.Unconfined) {
            engine.currentUserFlow.collect { states.add(it?.uid) }
        }

        engine.signIn(AuthCredential.EmailPassword("a@b.c", "pw")).getOrThrow()
        engine.signOut()
        job.cancel()

        assertEquals(listOf(null, "uid-1", null), states)
    }

    @Test
    fun anonymousUserIsFlaggedAnonymous() = runTest {
        val transport = ScriptedTransport()
        transport.responses += """{"localId":"anon-1","idToken":"tok"}"""
        val engine = engine(transport)

        val user = engine.signInAnonymously().getOrThrow()

        assertTrue(user.isAnonymous)
    }

    @Test
    fun sessionPersistsAcrossEngineInstancesAndRefreshesOnFirstUse() = runTest {
        val storage = FakeStorage()
        val transport = ScriptedTransport()
        transport.responses += userResponse
        transport.responses += lookupResponse
        engine(transport, storage).signIn(AuthCredential.EmailPassword("a@b.c", "pw")).getOrThrow()

        // "Process restart": a new engine over the same storage.
        val restartTransport = ScriptedTransport()
        restartTransport.responses += """{"id_token":"fresh-tok","refresh_token":"r2"}"""
        val restarted = engine(restartTransport, storage)

        val restored = restarted.currentUser()
        assertEquals("uid-1", restored?.uid)
        // The stored ID token has expired; first use refreshes it.
        assertEquals("fresh-tok", restarted.currentUserIdToken().getOrThrow())
        assertContains(restartTransport.calls.single().first, "securetoken.googleapis.com")
    }

    @Test
    fun jvmDefaultStorageRoundTripsThroughTheRealFile() = runTest {
        val storage = defaultFirebaseSessionStorage()!!
        val key = "firebase-session-storage-roundtrip-test"
        try {
            val transport = ScriptedTransport()
            transport.responses += userResponse
            transport.responses += lookupResponse
            FirebaseRestAuthEngine(
                transport = transport,
                apiKeyProvider = { "storage-roundtrip-test" },
                sessionStorage = storage,
            ).signIn(AuthCredential.EmailPassword("a@b.c", "pw")).getOrThrow()

            val restored = FirebaseRestAuthEngine(
                transport = ScriptedTransport(),
                apiKeyProvider = { "storage-roundtrip-test" },
                sessionStorage = storage,
            ).currentUser()

            assertEquals("uid-1", restored?.uid)
        } finally {
            storage.save(key, null)
        }
    }

    @Test
    fun signOutClearsPersistedSession() = runTest {
        val storage = FakeStorage()
        val transport = ScriptedTransport()
        transport.responses += userResponse
        transport.responses += lookupResponse
        val engine = engine(transport, storage)

        engine.signIn(AuthCredential.EmailPassword("a@b.c", "pw")).getOrThrow()
        assertTrue(storage.values.isNotEmpty())
        engine.signOut()

        assertTrue(storage.values.isEmpty())
        assertNull(engine(ScriptedTransport(), storage).currentUser())
    }

    @Test
    fun signOutClearsSession() = runTest {
        val transport = ScriptedTransport()
        transport.responses += """{"localId":"uid-1","idToken":"t1","displayName":"A"}"""
        val engine = engine(transport)

        engine.signIn(AuthCredential.EmailPassword("a@b.c", "pw")).getOrThrow()
        engine.signOut()

        assertNull(engine.currentUser())
    }

    @Test
    fun deleteAccountCallsDeleteWithSessionTokenAndSignsOut() = runTest {
        val transport = ScriptedTransport()
        transport.responses += """{"localId":"uid-1","idToken":"t1","displayName":"A"}"""
        transport.responses += """{}"""
        val engine = engine(transport)

        engine.signIn(AuthCredential.EmailPassword("a@b.c", "pw")).getOrThrow()
        engine.deleteAccount().getOrThrow()

        assertContains(transport.calls[1].first, "accounts:delete")
        assertContains(transport.calls[1].second, "\"idToken\":\"t1\"")
        assertNull(engine.currentUser())
    }

    @Test
    fun emailExistsSurfacesTypedCollision() = runTest {
        val transport = ScriptedTransport()
        transport.responses += """{"error":{"message":"EMAIL_EXISTS"}}"""
        val engine = engine(transport)

        val result = engine.signUp("a@b.c", "pw")

        assertIs<KMPAuthUserCollisionException>(result.exceptionOrNull())
    }

    @Test
    fun linkedFederatedIdSurfacesTypedCollision() = runTest {
        val transport = ScriptedTransport()
        transport.responses += userResponse
        transport.responses += lookupResponse
        transport.responses += """{"error":{"message":"FEDERATED_USER_ID_ALREADY_LINKED : This credential is already associated with a different user account."}}"""
        val engine = engine(transport)

        engine.signIn(AuthCredential.EmailPassword("a@b.c", "pw")).getOrThrow()
        val result = engine.signIn(
            AuthCredential.IdToken(AuthProviderIds.GOOGLE, idToken = "g"),
            linkWithCurrentUser = true,
        )

        assertIs<KMPAuthUserCollisionException>(result.exceptionOrNull())
    }

    @Test
    fun staleSessionDeleteSurfacesTypedRecentLoginRequired() = runTest {
        val transport = ScriptedTransport()
        transport.responses += userResponse
        transport.responses += lookupResponse
        transport.responses += """{"error":{"message":"CREDENTIAL_TOO_OLD_LOGIN_AGAIN"}}"""
        val engine = engine(transport)

        engine.signIn(AuthCredential.EmailPassword("a@b.c", "pw")).getOrThrow()
        val result = engine.deleteAccount()

        assertIs<KMPAuthRecentLoginRequiredException>(result.exceptionOrNull())
    }

    @Test
    fun deleteAccountWithoutSessionFails() = runTest {
        val engine = engine(ScriptedTransport())

        val result = engine.deleteAccount()

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()!!.message!!, "No signed-in user")
    }
}

class DesktopWebFlowTest {

    @Test
    fun webFlowCredentialAdoptsSessionFromLookup() = kotlinx.coroutines.test.runTest {
        val calls = mutableListOf<Pair<String, String>>()
        val transport = FirebaseRestTransport { url, body ->
            calls += url to body
            """{"users":[{"localId":"uid-w","email":"w@b.c","displayName":"Web"}]}"""
        }
        val engine = FirebaseRestAuthEngine(
            transport = transport,
            apiKeyProvider = { "test-key" },
            webFlowRunner = { request ->
                assertEquals("apple.com", request.providerId)
                WebFlowResult(idToken = "web-id-token", refreshToken = "web-refresh")
            },
            sessionStorage = null,
        )

        val user = engine.signIn(
            com.mmk.kmpauth.core.auth.AuthCredential.OAuthWebFlow(providerId = "apple.com")
        ).getOrThrow()

        assertEquals("uid-w", user.uid)
        assertEquals("Web", user.displayName)
        assertEquals("apple.com", user.providerId)
        assertContains(calls.single().first, "accounts:lookup")
        assertContains(calls.single().second, "web-id-token")
        assertEquals("uid-w", engine.currentUser()?.uid)
    }

    @Test
    fun signInPageEmbedsConfigAsJsonNotMarkup() {
        val html = DesktopWebAuthFlow.buildSignInPageHtml(
            DesktopWebAuthFlow.WebFlowPageConfig(
                apiKey = "k",
                authDomain = "p.firebaseapp.com",
                projectId = "p",
                applicationId = "1:1:web:x",
            ),
            WebFlowRequest(
                providerId = "github.com",
                scopes = listOf("user:email"),
                // A hostile value must stay inert inside the JSON block.
                customParameters = mapOf("prompt" to "</script><script>alert(1)</script>"),
            ),
        )

        assertContains(html, "\"providerId\":\"github.com\"")
        assertContains(html, "\"user:email\"")
        assertContains(html, "application/json")
        assertFalse(html.contains("</script><script>alert(1)</script>"))
    }
}
