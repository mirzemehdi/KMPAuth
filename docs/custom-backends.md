# Custom backends & several backends at once

## The backend registry

`KMPAuthBackend` holds every registered backend keyed by its
`AuthProviderBackend.backendId` (`"firebase"`, `"supabase"`, custom ids).
The **first registered backend is the default** — it serves the non-keyed
`KMPAuth.*` operations and the auth states. Registering more backends adds
them alongside without changing the default.

```kotlin
KMPAuth.initialize {
    firebase(apiKey = ..., projectId = ..., applicationId = ...) // self-registers, default
    supabase(url = ..., apiKey = ...)                            // registered under "supabase"
    // defaultBackendProvider("supabase")  // optional: make Supabase the default
}

KMPAuth.getBackendProvider()             // the default backend
KMPAuth.requireBackendProvider("supabase") // a specific one, by id
KMPAuth.setDefaultBackendProvider("supabase") // switch the default later
```

## Several backends side by side

Auth states read their backend from the `LocalKMPAuthBackend` composition
local. Scope a subtree to another registered backend with one wrapper — no
instances or parameters anywhere (the sample app shows Firebase and
Supabase sections side by side):

```kotlin
// Firebase (the default) - nothing to write:
val firebaseEmail = rememberEmailAuthState(email, password, onResult = ...)

// Everything in this subtree runs against Supabase:
ProvideKMPAuthBackend(SUPABASE_BACKEND_ID) {
    val supabaseEmail = rememberEmailAuthState(email, password, onResult = ...)
    val supabaseGoogle = rememberGoogleAuthState(onResult = ...)
}

// Non-composable operations on a specific backend:
KMPAuth.requireBackendProvider(SUPABASE_BACKEND_ID).sendPasswordResetEmail(email)
```

`FIREBASE_BACKEND_ID` / `SUPABASE_BACKEND_ID` constants are published by the
backend modules; `ProvideKMPAuthBackend` also accepts a backend instance.

## Custom backends

Implement `AuthProviderBackend`, give it a stable `backendId`, and register
it once at application start:

```kotlin
object MyOwnBackend : AuthProviderBackend {
    override val backendId = "my-backend"
    // ...
}

KMPAuth.initialize {
    backendProvider(MyOwnBackend)
}
```

Interface additions ship with default implementations (unsupported
failure), so custom backends stay source-compatible across KMPAuth updates.
