package com.mmk.kmpauth.core.di

import com.mmk.kmpauth.core.HttpClientFactory
import io.ktor.client.HttpClient

/**
 * Minimal manual service locator replacing the previous Koin container.
 * Holds the few shared services the library needs; everything else is
 * wired by plain constructor injection at the call site.
 */
internal object ServiceLocator {
    val httpClient: HttpClient by lazy { HttpClientFactory.default() }
}
