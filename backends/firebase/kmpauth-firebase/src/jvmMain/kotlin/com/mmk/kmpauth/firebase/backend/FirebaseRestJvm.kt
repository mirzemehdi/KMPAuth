package com.mmk.kmpauth.firebase.backend

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.app
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * The GitLive Firebase options configured for this process, or an
 * instructive failure. Populated by `KMPAuth.initialize { firebase(...) }`
 * (see `KMPAuthFirebaseConfiguration`).
 */
internal fun firebaseOptionsOrFail(): dev.gitlive.firebase.FirebaseOptions =
    runCatching { Firebase.app.options }.getOrNull()
        ?: throw IllegalStateException(
            "Firebase is not configured on Desktop. Add firebase(FirebaseBackendOptions(" +
                "apiKey = ..., projectId = ..., applicationId = ...)) inside " +
                "KMPAuth.initialize { } at application start."
        )

/** Default transport on the JDK's built-in HTTP client. */
internal class JdkFirebaseRestTransport : FirebaseRestTransport {

    private val client: HttpClient by lazy { HttpClient.newHttpClient() }

    override suspend fun post(url: String, jsonBody: String): String =
        withContext(Dispatchers.IO) {
            val request = HttpRequest.newBuilder(URI(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build()
            client.send(request, HttpResponse.BodyHandlers.ofString()).body()
        }
}
