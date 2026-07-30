package com.mmk.kmpauth.supabase

// Manual live harness against the sample's real Supabase project - @Ignore-d
// because it does network calls; remove @Ignore to run.

import com.mmk.kmpauth.core.auth.AuthCredential
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import kotlinx.coroutines.runBlocking
import kotlin.test.Ignore
import kotlin.test.Test

class ManualSupabaseE2ETest {

    @Test
    @Ignore
    fun liveSupabaseE2E(): Unit = runBlocking {
        val client = createSupabaseClient(
            supabaseUrl = "https://mlhefwyzasscsqjqtvhk.supabase.co",
            supabaseKey = "sb_publishable_dCWSZWxJqYcdsi6Hm0gXcQ_aeM91-z2",
        ) { install(Auth) }
        val backend = SupabaseAuthBackend(client)

        val anon = backend.signInAnonymously()
        println("SB_ANON=${anon.map { "${it.uid} anonymous-ok" }}")
        println("SB_CURRENT=${backend.currentUser()?.uid}")
        backend.signOut()

        val signUp = backend.signUp("kmpauth-e2e-test@example.com", "Str0ngPass!e2e")
        println("SB_SIGNUP=${signUp.map { "${it.uid} email=${it.email}" }}")

        val badLogin = backend.signIn(
            AuthCredential.EmailPassword("nonexistent-e2e@example.com", "wrong-pass-123")
        )
        println("SB_BADLOGIN=${badLogin.exceptionOrNull()?.message ?: badLogin}")
    }
}
