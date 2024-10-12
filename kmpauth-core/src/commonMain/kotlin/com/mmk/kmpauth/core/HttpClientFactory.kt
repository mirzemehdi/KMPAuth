package com.mmk.kmpauth.core

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal object HttpClientFactory {
    internal fun default() = HttpClient {
        defaultRequest {
            header(HttpHeaders.ContentType, "application/json")
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    println("NetworkRequest: $message")
                }
            }
            level = LogLevel.ALL
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
}