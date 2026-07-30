package com.mmk.kmpauth.supabase

import com.mmk.kmpauth.core.KMPAuth
import com.mmk.kmpauth.core.auth.AuthCredential
import com.mmk.kmpauth.core.auth.AuthProviderBackend
import com.mmk.kmpauth.core.auth.KMPAuthBackend
import com.mmk.kmpauth.core.auth.KMPAuthUser
import io.github.jan.supabase.auth.auth
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertSame

/**
 * Locks the registration contract of `KMPAuth.initialize { supabase(...) }`:
 * the backend registers under id "supabase" and becomes the default only
 * when it is the first backend; alongside another backend it stays
 * fetchable by id and `defaultBackendProvider("supabase")` promotes it.
 * Re-configuring the same id swaps the instance in place. KMPAuthBackend is
 * a process-wide singleton, so tests re-configure freely.
 */
class SupabaseBackendRegistrationTest {

    @Test
    fun supabaseClientOverloadRegistersBackend() {
        val engine = RecordingMockEngine { jsonResponse("{}") }
        KMPAuth.initialize { supabase(engine.client) }

        val backend = assertIs<SupabaseAuthBackend>(KMPAuthBackend.getOrNull())
        assertSame(engine.client, backend.supabaseClient)
    }

    @Test
    fun lastExplicitConfigurationWins() {
        val first = RecordingMockEngine { jsonResponse("{}") }
        val second = RecordingMockEngine { jsonResponse("{}") }

        KMPAuth.initialize { supabase(first.client) }
        assertSame(first.client, assertIs<SupabaseAuthBackend>(KMPAuthBackend.getOrNull()).supabaseClient)

        KMPAuth.initialize { supabase(second.client) }
        assertSame(second.client, assertIs<SupabaseAuthBackend>(KMPAuthBackend.getOrNull()).supabaseClient)
    }

    @Test
    fun alongsideAnotherBackendSupabaseIsSecondaryUntilPromoted() {
        // Simulates Firebase's self-registration having happened first.
        val firstBackend = object : AuthProviderBackend {
            override val backendId: String get() = "firebase"
            override suspend fun signIn(
                credential: AuthCredential,
                linkWithCurrentUser: Boolean,
            ): Result<KMPAuthUser> = Result.failure(UnsupportedOperationException("fake"))

            override suspend fun signOut() = Unit
            override fun currentUser(): KMPAuthUser? = null
        }
        KMPAuth.registerBackendProvider(firstBackend, replace = true)

        val engine = RecordingMockEngine { jsonResponse("{}") }
        KMPAuth.initialize { supabase(engine.client) }

        // First backend keeps the default; Supabase is reachable by id.
        assertSame(firstBackend, KMPAuthBackend.getOrNull())
        assertIs<SupabaseAuthBackend>(KMPAuth.requireBackendProvider("supabase"))

        // Explicit promotion switches the default.
        KMPAuth.initialize { defaultBackendProvider("supabase") }
        assertIs<SupabaseAuthBackend>(KMPAuthBackend.getOrNull())
    }

    @Test
    fun optionsOverloadCreatesClientWithAuthInstalled() {
        KMPAuth.initialize {
            supabase(
                options = SupabaseBackendOptions(
                    url = "https://unit-test.supabase.co",
                    apiKey = "test-anon-key",
                ),
            ) {
                // The builder hook must reach the SupabaseClientBuilder -
                // here it injects the offline test engine.
                httpEngine = io.ktor.client.engine.mock.MockEngine { jsonResponse("{}") }
            }
        }

        val backend = assertIs<SupabaseAuthBackend>(KMPAuthBackend.getOrNull())
        // auth accessor throws if the Auth plugin was not installed.
        assertNotNull(backend.supabaseClient.auth)
    }
}
