package com.mmk.kmpauth.sample

import com.mmk.kmpauth.core.KMPAuth
import com.mmk.kmpauth.google.GoogleAuthCredentials
import com.mmk.kmpauth.google.google


object AppInitializer {
    fun onApplicationStart() {
        onApplicationStartPlatformSpecific()
        // One-stop initialization. The Firebase backend registers itself
        // automatically; a custom backend would be set here via
        // backendProvider(...).
        KMPAuth.initialize {
            logger { println("KMPAuthLog: $it") }
            google(GoogleAuthCredentials(serverId = "180951249266-9cn8vatdnto1q3t3phfivvf0b5e453bf.apps.googleusercontent.com"))
        }
    }
}