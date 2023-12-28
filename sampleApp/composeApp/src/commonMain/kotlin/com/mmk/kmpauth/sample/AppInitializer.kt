package com.mmk.kmpauth.sample

import com.mmk.kmpauth.google.GoogleAuthCredentials
import com.mmk.kmpauth.google.GoogleAuthProvider


object AppInitializer {
    fun onApplicationStart(){
        onApplicationStartPlatformSpecific()
        GoogleAuthProvider.create(credentials = GoogleAuthCredentials(serverId = "testID"))
    }
}