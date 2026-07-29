package com.mmk.kmpauth.sample

import com.mmk.kmpauth.core.KMPAuth
import com.mmk.kmpauth.firebase.firebase
import com.mmk.kmpauth.google.google

object AppInitializer {

    fun onApplicationStart() {
        onApplicationStartPlatformSpecific()
        // One-stop initialization. The Firebase backend registers itself
        // automatically; firebase(...) supplies the web config that Desktop
        // and Web need (no-op on Android/iOS, which use the bundled files).
        //
        // A single-backend Supabase app would add supabase(url, apiKey)
        // here instead - an explicit registration supersedes Firebase's
        // auto-registered default. This sample runs BOTH backends side by
        // side, so Supabase is a standalone instance scoped via
        // LocalKMPAuthBackend in App.kt, not the registered default.
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
