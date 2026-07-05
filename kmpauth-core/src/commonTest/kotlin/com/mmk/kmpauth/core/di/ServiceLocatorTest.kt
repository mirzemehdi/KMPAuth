package com.mmk.kmpauth.core.di

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertSame

/**
 * Contract test for the manual DI replacement of the former Koin container:
 * the library-internal HttpClient is lazily created and shared.
 */
class ServiceLocatorTest {

    @Test
    fun httpClientIsLazySingleton() {
        val first = ServiceLocator.httpClient
        val second = ServiceLocator.httpClient

        assertNotNull(first)
        assertSame(first, second)
    }
}
