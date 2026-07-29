package com.mmk.kmpauth.supabase

import com.mmk.kmpauth.core.KMPAuth
import com.mmk.kmpauth.core.auth.KMPAuthBackend
import io.github.jan.supabase.auth.auth
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertSame

/**
 * Locks the registration contract of `KMPAuth.initialize { supabase(...) }`:
 * explicit registration, first-one-wins by default, replace to swap.
 * KMPAuthBackend is a process-wide singleton, so each test starts by
 * force-registering a known backend.
 */
class SupabaseBackendRegistrationTest {

    @Test
    fun supabaseClientOverloadRegistersBackend() {
        val engine = RecordingMockEngine { jsonResponse("{}") }
        KMPAuth.initialize { supabase(engine.client, replace = true) }

        val backend = assertIs<SupabaseAuthBackend>(KMPAuthBackend.getOrNull())
        assertSame(engine.client, backend.supabaseClient)
    }

    @Test
    fun firstRegistrationWinsUnlessReplaceIsSet() {
        val first = RecordingMockEngine { jsonResponse("{}") }
        val second = RecordingMockEngine { jsonResponse("{}") }
        KMPAuth.initialize { supabase(first.client, replace = true) }

        // Default replace = false: the already registered backend stays.
        KMPAuth.initialize { supabase(second.client) }
        assertSame(first.client, assertIs<SupabaseAuthBackend>(KMPAuthBackend.getOrNull()).supabaseClient)

        // replace = true swaps it.
        KMPAuth.initialize { supabase(second.client, replace = true) }
        assertSame(second.client, assertIs<SupabaseAuthBackend>(KMPAuthBackend.getOrNull()).supabaseClient)
    }

    @Test
    fun optionsOverloadCreatesClientWithAuthInstalled() {
        KMPAuth.initialize {
            supabase(
                options = SupabaseBackendOptions(
                    url = "https://unit-test.supabase.co",
                    apiKey = "test-anon-key",
                ),
                replace = true,
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
