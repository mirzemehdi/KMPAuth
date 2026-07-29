package com.mmk.kmpauth.sample

import com.mmk.kmpauth.core.KMPAuth
import com.mmk.kmpauth.firebase.FirebaseBackendOptions
import com.mmk.kmpauth.firebase.firebase
import com.mmk.kmpauth.google.GoogleAuthCredentials
import com.mmk.kmpauth.google.google
import com.mmk.kmpauth.supabase.SupabaseBackendOptions
import com.mmk.kmpauth.supabase.supabase

/**
 * Flip to true to serve every `rememberXxxAuthState` flow and `KMPAuth.*`
 * operation from Supabase instead of Firebase — nothing else in the app
 * changes. (Web-flow providers — GitHub/Microsoft/Apple-on-Android — stay
 * Firebase-driven; the Supabase backend reports them as unsupported.)
 */
private const val USE_SUPABASE_BACKEND = false

object AppInitializer {
    fun onApplicationStart() {
        onApplicationStartPlatformSpecific()
        // One-stop initialization. The Firebase backend registers itself
        // automatically; firebase(...) supplies the web config that Desktop
        // and Web need (no-op on Android/iOS, which use the bundled files).
        KMPAuth.initialize {
            logger { println("KMPAuthLog: $it") }
            google(GoogleAuthCredentials(serverId = "180951249266-9cn8vatdnto1q3t3phfivvf0b5e453bf.apps.googleusercontent.com"))
            firebase(
                FirebaseBackendOptions(
                    apiKey = "AIzaSyAU4EB8PdtZ0faNDJaLvn6r6aXgcOKGpxQ",
                    projectId = "kmpauthapp",
                    applicationId = "1:180951249266:android:1a83eb8eea4835070e2deb",
                )
            )
            if (USE_SUPABASE_BACKEND) {
                // replace = true because the Firebase backend registers
                // eagerly at load on iOS/JS/wasm.
                supabase(
                    SupabaseBackendOptions(
                        url = "https://mlhefwyzasscsqjqtvhk.supabase.co",
                        apiKey = "sb_publishable_dCWSZWxJqYcdsi6Hm0gXcQ_aeM91-z2",
                    ),
                    replace = true,
                )
            }
        }
    }
}