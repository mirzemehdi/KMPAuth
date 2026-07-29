package com.mmk.kmpauth.core.auth

// No classpath scanning on this platform; backend modules self-register
// eagerly at load instead.
internal actual fun loadPlatformBackends(): List<AuthProviderBackend> = emptyList()
