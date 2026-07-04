package com.mmk.kmpauth.core

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame

/**
 * Characterization tests for the internal HTTP client factory: every call
 * produces a new, ready-to-use client (factory semantics, no caching).
 */
class HttpClientFactoryTest {

    @Test
    fun defaultReturnsUsableClient() {
        val client = HttpClientFactory.default()
        assertNotNull(client)
        client.close()
    }

    @Test
    fun defaultReturnsNewInstancePerCall() {
        val first = HttpClientFactory.default()
        val second = HttpClientFactory.default()
        assertNotSame(first, second)
        first.close()
        second.close()
    }
}
