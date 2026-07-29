package com.mmk.kmpauth.sample

import com.mmk.kmpauth.core.KMPAuth
import com.mmk.kmpauth.firebase.firebase
import com.mmk.kmpauth.google.google
import com.mmk.kmpauth.supabase.SupabaseAuthBackend

object AppInitializer {

    /**
     * Standalone Supabase backend used by the sample's Supabase section via
     * the states' `backend` parameter — Firebase stays the registered
     * default, both run side by side.
     */
    val supabaseBackend by lazy {
        SupabaseAuthBackend(
            url = "https://mlhefwyzasscsqjqtvhk.supabase.co",
            apiKey = "sb_publishable_dCWSZWxJqYcdsi6Hm0gXcQ_aeM91-z2",
        )
    }

    fun onApplicationStart() {
        onApplicationStartPlatformSpecific()
        // One-stop initialization. The Firebase backend registers itself
        // automatically; firebase(...) supplies the web config that Desktop
        // and Web need (no-op on Android/iOS, which use the bundled files).
        KMPAuth.initialize {
            logger { println("KMPAuthLog: $it") }
            google(serverId = "180951249266-9cn8vatdnto1q3t3phfivvf0b5e453bf.apps.googleusercontent.com")
            firebase(
                apiKey = "AIzaSyAU4EB8PdtZ0faNDJaLvn6r6aXgcOKGpxQ",
                projectId = "kmpauthapp",
                applicationId = "1:180951249266:android:1a83eb8eea4835070e2deb",
            )
        }
    }
}
