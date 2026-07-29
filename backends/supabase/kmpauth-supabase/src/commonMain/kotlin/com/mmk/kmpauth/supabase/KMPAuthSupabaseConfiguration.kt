package com.mmk.kmpauth.supabase

import com.mmk.kmpauth.core.KMPAuthConfiguration
import com.mmk.kmpauth.core.auth.KMPAuthBackend
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.SupabaseClientBuilder
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient

/**
 * Connection settings for the Supabase auth backend — both values come from
 * the Supabase dashboard (Project Settings → API).
 *
 * @param url The project URL, e.g. `https://xyzcompany.supabase.co`.
 * @param apiKey The public API key (the `anon` key — never the
 * service-role key, which must not ship in a client app).
 */
public data class SupabaseBackendOptions(
    val url: String,
    val apiKey: String,
)

/**
 * Configures Supabase as the auth backend inside `KMPAuth.initialize { }`:
 *
 * ```
 * KMPAuth.initialize {
 *     supabase(SupabaseBackendOptions(url = projectUrl, apiKey = anonKey))
 * }
 * ```
 *
 * Creates a [SupabaseClient] with the `Auth` plugin installed and registers
 * a [SupabaseAuthBackend] over it — after that the backend-generic states
 * (`rememberEmailAuthState`, `rememberAnonymousAuthState`,
 * `rememberGoogleAuthState`, `rememberFacebookAuthState`) and the `KMPAuth`
 * operations run against Supabase. supabase-kt needs a Ktor client engine
 * on the runtime classpath — add the engine artifact for each platform
 * (e.g. `io.ktor:ktor-client-okhttp` on Android, `io.ktor:ktor-client-darwin`
 * on iOS), as in any supabase-kt setup.
 *
 * Unlike the Firebase backend there is **no automatic registration**:
 * Firebase reads its configuration from platform config files
 * (`google-services.json` / `GoogleService-Info.plist`), so its backend can
 * self-register from the classpath; a Supabase client cannot exist without
 * the project URL and key, which only this call provides.
 *
 * Calling this makes Supabase the active backend: an explicit registration
 * always supersedes Firebase's auto-registered default, regardless of when
 * that self-registration happened.
 *
 * @param options Supabase project URL and public API key.
 * @param builder Extra [SupabaseClientBuilder] configuration — install
 * additional plugins (Postgrest, Storage, ...) or re-install `Auth` with
 * custom settings (flow type, deep-link scheme/host, session storage).
 */
public fun KMPAuthConfiguration.supabase(
    options: SupabaseBackendOptions,
    builder: SupabaseClientBuilder.() -> Unit = {},
) {
    val client = createSupabaseClient(
        supabaseUrl = options.url,
        supabaseKey = options.apiKey,
    ) {
        install(Auth)
        builder()
    }
    supabase(client)
}

/**
 * [supabase] variant for apps that already have a configured
 * [SupabaseClient] (e.g. one shared with Postgrest/Storage usage). The
 * client must have the `Auth` plugin installed. Same registration
 * semantics as the options-based overload.
 */
public fun KMPAuthConfiguration.supabase(
    client: SupabaseClient,
) {
    // The DSL call is an explicit choice: it supersedes any auto-registered
    // default and any earlier configuration.
    KMPAuthBackend.register(SupabaseAuthBackend(client), replace = true)
}

/**
 * Builds a standalone [SupabaseAuthBackend] without registering it as the
 * process-wide backend — for using several backends side by side by passing
 * it to a state's `backend` parameter:
 *
 * ```
 * val supabaseBackend = SupabaseAuthBackend(SupabaseBackendOptions(url, key))
 * val emailAuth = rememberEmailAuthState(email, password, backend = supabaseBackend, onResult = ...)
 * ```
 *
 * @param options Supabase project URL and public API key.
 * @param builder Extra [SupabaseClientBuilder] configuration.
 */
public fun SupabaseAuthBackend(
    options: SupabaseBackendOptions,
    builder: SupabaseClientBuilder.() -> Unit = {},
): SupabaseAuthBackend = SupabaseAuthBackend(
    createSupabaseClient(
        supabaseUrl = options.url,
        supabaseKey = options.apiKey,
    ) {
        install(Auth)
        builder()
    }
)

/**
 * [supabase] shorthand taking the values directly — same semantics as the
 * [SupabaseBackendOptions] overload:
 *
 * ```
 * KMPAuth.initialize {
 *     supabase(url = projectUrl, apiKey = publishableKey)
 * }
 * ```
 */
public fun KMPAuthConfiguration.supabase(
    url: String,
    apiKey: String,
    builder: SupabaseClientBuilder.() -> Unit = {},
) {
    supabase(SupabaseBackendOptions(url = url, apiKey = apiKey), builder)
}

/**
 * [SupabaseAuthBackend] factory shorthand taking the values directly.
 */
public fun SupabaseAuthBackend(
    url: String,
    apiKey: String,
    builder: SupabaseClientBuilder.() -> Unit = {},
): SupabaseAuthBackend = SupabaseAuthBackend(SupabaseBackendOptions(url, apiKey), builder)
