package com.mmk.kmpauth.sample

import android.app.Application
import android.os.StrictMode
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppInitializer.onApplicationStart()
        Firebase.auth
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                // .detectAll() for all detectable problems
                .penaltyLog()
                .penaltyDeath()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                // .penaltyDeath()
                .build()
        )
    }
}