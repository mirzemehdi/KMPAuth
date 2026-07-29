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
 * Registration follows the first-one-wins rule of [KMPAuthBackend.register]:
 * called at application start, `supabase(...)` beats the lazily discovered
 * Firebase default on JVM/Android. On iOS/JS/wasm however,
 * `kmpauth-firebase-core` (when it is also in the dependencies) registers
 * eagerly at binary load — pass [replace] = true there (harmless elsewhere)
 * to make Supabase the active backend.
 *
 * @param options Supabase project URL and public API key.
 * @param replace Override an already registered backend (see above).
 * @param builder Extra [SupabaseClientBuilder] configuration — install
 * additional plugins (Postgrest, Storage, ...) or re-install `Auth` with
 * custom settings (flow type, deep-link scheme/host, session storage).
 */
public fun KMPAuthConfiguration.supabase(
    options: SupabaseBackendOptions,
    replace: Boolean = false,
    builder: SupabaseClientBuilder.() -> Unit = {},
) {
    val client = createSupabaseClient(
        supabaseUrl = options.url,
        supabaseKey = options.apiKey,
    ) {
        install(Auth)
        builder()
    }
    supabase(client, replace)
}

/**
 * [supabase] variant for apps that already have a configured
 * [SupabaseClient] (e.g. one shared with Postgrest/Storage usage). The
 * client must have the `Auth` plugin installed. Same registration
 * semantics as the options-based overload.
 */
public fun KMPAuthConfiguration.supabase(
    client: SupabaseClient,
    replace: Boolean = false,
) {
    KMPAuthBackend.register(SupabaseAuthBackend(client), replace)
}
