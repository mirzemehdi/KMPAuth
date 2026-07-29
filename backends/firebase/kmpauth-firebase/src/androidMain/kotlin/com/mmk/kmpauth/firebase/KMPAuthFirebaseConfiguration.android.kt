package com.mmk.kmpauth.firebase

// The native Firebase SDK initializes itself from the bundled config file
// (google-services.json / GoogleService-Info.plist).
internal actual fun initializeFirebasePlatform(options: FirebaseBackendOptions): Unit = Unit
