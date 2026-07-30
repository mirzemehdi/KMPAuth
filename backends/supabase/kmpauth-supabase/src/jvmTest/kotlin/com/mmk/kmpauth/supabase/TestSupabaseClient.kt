package com.mmk.kmpauth.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.createSupabaseClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

/**
 * Supabase client over a Ktor [MockEngine] — no network. Requests are
 * captured in [RecordingMockEngine.requests] for assertions.
 */
internal class RecordingMockEngine(private val handler: MockRequestHandler) {

    val requests: MutableList<HttpRequestData> = mutableListOf()

    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = "https://unit-test.supabase.co",
        supabaseKey = "test-anon-key",
    ) {
        httpEngine = MockEngine { request ->
            requests.add(request)
            handler(this, request)
        }
        install(Auth) {
            // Hermetic tests: no persisted session storage, no background
            // refresh job.
            autoLoadFromStorage = false
            autoSaveToStorage = false
            alwaysAutoRefresh = false
            sessionManager = MemorySessionManager()
        }
    }
}

internal const val TEST_USER_JSON: String = """{
    "id": "user-123",
    "aud": "authenticated",
    "email": "user@example.com",
    "app_metadata": {"provider": "email", "providers": ["email"]},
    "user_metadata": {"full_name": "Test User", "avatar_url": "https://example.com/a.png"}
}"""

internal const val TEST_SESSION_JSON: String = """{
    "access_token": "access-token",
    "refresh_token": "refresh-token",
    "expires_in": 3600,
    "token_type": "bearer",
    "user": $TEST_USER_JSON
}"""

internal fun MockRequestHandleScope.jsonResponse(content: String) = respond(
    content = content,
    status = HttpStatusCode.OK,
    headers = headersOf(HttpHeaders.ContentType, "application/json"),
)

internal fun MockRequestHandleScope.errorResponse(content: String, status: HttpStatusCode) = respond(
    content = content,
    status = status,
    headers = headersOf(HttpHeaders.ContentType, "application/json"),
)
