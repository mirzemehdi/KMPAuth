# Supabase backend

Module: `kmpauth-supabase`. Firebase is not required — the same
backend-agnostic API served from a [Supabase](https://supabase.com) project
on every target **including wasm**.

## Setup

```kotlin
KMPAuth.initialize {
    supabase(url = projectUrl, apiKey = publishableKey)
    // apps that already use supabase-kt can pass their client instead:
    // supabase(existingSupabaseClient)
}
```

There is no config-file auto-registration — a Supabase client cannot exist
without the project URL and key, so registration is this one explicit call
(which always supersedes Firebase's auto-registered default, so having both
dependencies is fine).

`kmpauth-supabase` is built on the community
[supabase-kt](https://github.com/supabase-community/supabase-kt) SDK, which
needs a [Ktor client engine](https://ktor.io/docs/client-engines.html) per
platform — same as any supabase-kt setup:

```kotlin
androidMain.dependencies { implementation("io.ktor:ktor-client-okhttp:<ktor-version>") }
iosMain.dependencies { implementation("io.ktor:ktor-client-darwin:<ktor-version>") }
jvmMain.dependencies { implementation("io.ktor:ktor-client-cio:<ktor-version>") }
jsMain.dependencies { implementation("io.ktor:ktor-client-js:<ktor-version>") }
wasmJsMain.dependencies { implementation("io.ktor:ktor-client-js:<ktor-version>") }
```

## What runs against Supabase

Enable the matching providers in the Supabase dashboard.

**Works:**

- [`rememberEmailAuthState`](email.md) (sign-in and sign-up),
  [`rememberAnonymousAuthState`](anonymous.md),
  [`rememberGoogleAuthState`](google.md),
  [`rememberFacebookAuthState`](facebook.md) (Limited Login/OIDC only)
- `KMPAuth` operations: `signIn`, `signUp`, `signInAnonymously`,
  `sendPasswordResetEmail`, `currentUser`, `signOut`
- Magic links: `sendSignInLinkToEmail` / `isSignInWithEmailLink` /
  `signInWithEmailLink` — `token_hash`, PKCE `code` and implicit-flow
  redirects are all recognized
- `reauthenticate` (as a fresh sign-in; Supabase has no recent-login
  requirement)
- Id-token linking (`linkAccount = true`) via Supabase identity linking —
  requires manual linking enabled on the project

**Doesn't (by design), failing with the Supabase-idiomatic alternative:**

- Classic Facebook access tokens — Supabase's `id_token` grant accepts only
  OIDC tokens; use Facebook Limited Login
- The `kmpauth-firebase`-resident web-flow states
  (GitHub/Microsoft/OAuth/Apple-web/phone) — use supabase-kt's
  `signInWith(Github)` etc. directly via
  `SupabaseAuthBackend.supabaseClient`
- Linking an email/password credential — Supabase adds an email via
  `auth.updateUser` instead

Of the `EmailActionCodeSettings` fields only `url` maps to Supabase (as the
redirect URL, which must be in the project's allow-list); the
iOS/Android-app fields are Firebase dynamic-link concepts.
