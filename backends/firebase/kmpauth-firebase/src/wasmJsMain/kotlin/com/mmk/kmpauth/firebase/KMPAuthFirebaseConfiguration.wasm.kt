package com.mmk.kmpauth.firebase

// The Firebase SDK has no wasm target; nothing to initialize.
internal actual fun initializeFirebasePlatform(options: FirebaseBackendOptions): Unit = Unit
