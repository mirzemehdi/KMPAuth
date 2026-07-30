package com.mmk.kmpauth.firebase

import com.mmk.kmpauth.firebase.backend.wasmFirebaseOptions

// The Firebase SDK has no wasm target; the web config feeds the REST
// engine (Identity Toolkit over fetch) instead.
internal actual fun initializeFirebasePlatform(options: FirebaseBackendOptions) {
    wasmFirebaseOptions = options
}
