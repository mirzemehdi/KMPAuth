# Custom backends & several backends at once

## Custom backends

Implement `AuthProviderBackend` and register it once at application start —
an explicit registration always supersedes the auto-registered Firebase
default:

```kotlin
KMPAuth.initialize {
    backendProvider(MyOwnBackend) // or KMPAuth.registerBackendProvider(MyOwnBackend)
}
```

Interface additions ship with default implementations (unsupported
failure), so custom backends stay source-compatible across KMPAuth updates.

## Several backends at once

The registered backend is only the default. Auth states read their backend
from the `LocalKMPAuthBackend` composition local, so scoping a subtree to
another backend is one wrapper — no per-call parameters (the sample app
shows Firebase and Supabase sections side by side):

```kotlin
// Firebase (registered default) - nothing to write:
val firebaseEmail = rememberEmailAuthState(email, password, onResult = ...)

// Scope a whole section to a standalone Supabase backend:
val supabase = remember { SupabaseAuthBackend(url = projectUrl, apiKey = publishableKey) }
CompositionLocalProvider(LocalKMPAuthBackend provides supabase) {
    // every auth state in here is served by Supabase
    val supabaseEmail = rememberEmailAuthState(email, password, onResult = ...)
    val supabaseGoogle = rememberGoogleAuthState(onResult = ...)
}

// Non-composable operations run on the instance directly:
supabase.sendPasswordResetEmail(email)
```
