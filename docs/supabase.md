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
without the project URL and key, so registration is this one explicit call.
In a Supabase-only app it becomes the default backend automatically; with
Firebase also present, Firebase (registered first) stays the default and
Supabase is reachable by id — add `defaultBackendProvider("supabase")` to
prefer Supabase, or scope subtrees with `ProvideKMPAuthBackend` (see
[Custom & multiple backends](custom-backends.md)).

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
- [`rememberPhoneAuthState`](phone.md) — SMS OTP on **every target**
  (enable the Phone provider and an SMS sender in the dashboard); the one
  backend that serves phone sign-in beyond Android/iOS
- `reauthenticate` (as a fresh sign-in; Supabase has no recent-login
  requirement)
- Id-token linking (`linkAccount = true`) via Supabase identity linking —
  requires manual linking enabled on the project
- **Browser OAuth (GitHub, Azure/Microsoft, GitLab, Discord, ...)** via
  `KMPAuth.signIn(AuthCredential.OAuthWebFlow("github.com"))` — accepts
  Firebase-style ids and GoTrue provider names; see the platform notes
  below

**Browser OAuth per platform:**

- **Desktop (JVM)**: works out of the box — supabase-kt opens the system
  browser and catches the redirect on its own localhost callback server
  (allow-list `http://localhost:<port>` in the Supabase dashboard;
  configure the port via supabase-kt's `httpCallbackConfig`).
- **Android / iOS**: needs supabase-kt's standard deep-link setup — set
  `scheme`/`host` on the Supabase client, register the scheme in
  `AndroidManifest.xml` / `Info.plist`, forward the link
  (`handleDeeplinks`), and allow-list the redirect URL in the dashboard.
- **Web (JS/wasm)**: the page redirects to the provider — the app unloads,
  so no `Result` callback fires; the session is restored by supabase-kt
  when the page reloads after the round-trip.

**Doesn't (by design), failing with the Supabase-idiomatic alternative:**

- Classic Facebook access tokens — Supabase's `id_token` grant accepts only
  OIDC tokens; use Facebook Limited Login
- Linking a browser-OAuth identity — use
  `supabaseClient.auth.linkIdentity(Github)` directly
- Linking an email/password credential — Supabase adds an email via
  `auth.updateUser` instead

Of the `EmailActionCodeSettings` fields only `url` maps to Supabase (as the
redirect URL, which must be in the project's allow-list); the
iOS/Android-app fields are Firebase dynamic-link concepts.
