package com.mmk.kmpauth.sample

import com.mmk.kmpauth.core.KMPAuth
import com.mmk.kmpauth.google.GoogleAuthCredentials
import com.mmk.kmpauth.google.GoogleAuthProvider


object AppInitializer {
    fun onApplicationStart() {
        onApplicationStartPlatformSpecific()
        KMPAuth.setLogger {
            println("KMPAuthLog: $it")
        }
        GoogleAuthProvider.create(credentials = GoogleAuthCredentials(serverId = "180951249266-9cn8vatdnto1q3t3phfivvf0b5e453bf.apps.googleusercontent.com"))
    }
}