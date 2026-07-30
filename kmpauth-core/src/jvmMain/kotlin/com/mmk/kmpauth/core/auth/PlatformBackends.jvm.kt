package com.mmk.kmpauth.core.auth

import java.util.ServiceLoader

internal actual fun loadPlatformBackends(): List<AuthProviderBackend> =
    ServiceLoader.load(AuthProviderBackend::class.java).toList()
