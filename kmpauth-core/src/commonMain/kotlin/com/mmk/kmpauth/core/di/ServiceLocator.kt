package com.mmk.kmpauth.core.di

import com.mmk.kmpauth.core.HttpClientFactory
import io.ktor.client.HttpClient

/**
 * Holds the few services shared across the library; everything else is
 * wired by plain constructor injection at the call site.
 */
internal object ServiceLocator {
    val httpClient: HttpClient by lazy { HttpClientFactory.default() }
}
