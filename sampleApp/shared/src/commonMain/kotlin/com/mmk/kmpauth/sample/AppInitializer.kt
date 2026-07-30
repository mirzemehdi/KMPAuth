package com.mmk.kmpauth.sample

import com.mmk.kmpauth.core.KMPAuth
import com.mmk.kmpauth.firebase.firebase
import com.mmk.kmpauth.google.google
import com.mmk.kmpauth.supabase.supabase

object AppInitializer {

    fun onApplicationStart() {
        onApplicationStartPlatformSpecific()
        // One-stop initialization. The Firebase backend registers itself
        // automatically and, as the first registered backend, stays the
        // default; supabase(...) registers alongside it under id "supabase"
        // (fetched in App.kt via KMPAuth.requireBackendProvider). A
        // Supabase-only app would need nothing more; to prefer Supabase
        // with both present, add defaultBackendProvider("supabase").
        KMPAuth.initialize {
            logger { println("KMPAuthLog: $it") }
            google(serverId = "180951249266-9cn8vatdnto1q3t3phfivvf0b5e453bf.apps.googleusercontent.com")
            firebase(
                apiKey = "AIzaSyAU4EB8PdtZ0faNDJaLvn6r6aXgcOKGpxQ",
                projectId = "kmpauthapp",
                applicationId = "1:180951249266:android:1a83eb8eea4835070e2deb",
            )
            supabase(
                url = "https://mlhefwyzasscsqjqtvhk.supabase.co",
                apiKey = "sb_publishable_dCWSZWxJqYcdsi6Hm0gXcQ_aeM91-z2",
            )
        }
    }
}
