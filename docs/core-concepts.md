# Core concepts

## Two layers of composable states

Both return a `SignInState` with `launch()` and an observable `isInProgress`:

| Layer | Naming | Returns | Backend needed |
|---|---|---|---|
| Credential only | `rememberXxxSignInState` | `Result<GoogleUser>` / `Result<FacebookUser>` / `Result<AppleUser>` | no — you handle the token |
| Session | `rememberXxxAuthState` | `Result<KMPAuthUser>` | yes — credential exchanged through the registered backend |

Wire either to any clickable and drive loading UI from `isInProgress`:

```kotlin
val googleSignIn = rememberGoogleAuthState(onResult = { result -> /* ... */ })
Button(onClick = { googleSignIn.launch() }, enabled = !googleSignIn.isInProgress) {
    Text("Sign in with Google")
}
```

- Parameters (e.g. `linkAccount`) are read at **launch time**, so toggling
  them via recomposition just works.
- Double-taps cannot start two flows.
- Results are **non-null**: a flow that produces no user is a failure with a
  reason — cancellation, misconfiguration, unsupported platform — never a
  silent null.
- A flow that cannot work on the current platform still compiles and reports
  a failed `Result` explaining why when launched.

The 2.x `*UiContainer { this.onClick() }` composables still work but are
deprecated (removal planned for 4.0) — see [MIGRATION.md](../MIGRATION.md).

## `KMPAuthUser`

KMPAuth's own backend-agnostic user model: `uid`, `email`, `displayName`,
`photoUrl`, `providerId`. The native SDK object stays reachable via
`KMPAuthUser.raw` (e.g. `dev.gitlive.firebase.auth.FirebaseUser` with the
Firebase backend).

## The `KMPAuth` object

Entry point for everything that isn't a launchable flow, served by the
registered backend:

```kotlin
KMPAuth.initialize { /* one-stop setup - see Getting started */ }

KMPAuth.currentUser()                 // KMPAuthUser? - null when signed out
KMPAuth.currentUserFlow               // Flow<KMPAuthUser?> - reactive auth state
KMPAuth.currentUserIdToken()          // Result<String> - JWT for your own server
KMPAuth.signOut()
KMPAuth.signIn(credential)            // exchange a credential you obtained yourself
KMPAuth.signUp(email, password)
KMPAuth.signInAnonymously()
KMPAuth.signInWithPhone(phoneNumber, verificationUi)
KMPAuth.sendPasswordResetEmail(email)
KMPAuth.sendSignInLinkToEmail(email, actionCodeSettings)
KMPAuth.isSignInWithEmailLink(link)
KMPAuth.signInWithEmailLink(email, link)
KMPAuth.reauthenticate(credential)
KMPAuth.deleteAccount()               // irreversible; may need reauthentication first
```

### Reactive auth state and API calls to your own server

`KMPAuth.currentUserFlow` emits on every auth-state change — sign-in,
sign-out, and (unlike Firebase's raw listener) also after linking upgrades
the current user, so a guest-to-Google upgrade re-emits without manual
triggers:

```kotlin
val user: StateFlow<KMPAuthUser?> = KMPAuth.currentUserFlow
    .stateIn(scope, SharingStarted.Eagerly, KMPAuth.currentUser())
```

`KMPAuth.currentUserIdToken(forceRefresh = true)` returns the signed-in
user's JWT for an `Authorization: Bearer` header — your server verifies it
against Firebase's public keys (or the Supabase project's JWT secret).

`KMPAuthUser` also exposes `isAnonymous` (guest sessions) and falls back
across the linked providers for `email`/`displayName`/`photoUrl`, so a
guest upgraded with a Google account shows the Google name and photo.

### Reauthentication

Firebase requires a recent sign-in before security-sensitive operations
(deleting the account, changing the password). Obtain a fresh credential and
retry:

```kotlin
// Email/password users:
KMPAuth.reauthenticate(AuthCredential.EmailPassword(email, currentPassword))
    .onSuccess { /* now delete the account / update the password */ }

// Google/Apple/Facebook users: rerun the provider flow for a fresh token, then
KMPAuth.reauthenticate(
    AuthCredential.IdToken(AuthProviderIds.GOOGLE, googleUser.idToken)
)
```

Route to the right flow with `user.providerIds` — the account's linked
providers in `AuthProviderIds` convention on every backend:

```kotlin
val user = KMPAuth.currentUser() ?: return
when {
    AuthProviderIds.EMAIL in user.providerIds -> askForPassword()
    AuthProviderIds.APPLE in user.providerIds -> appleReauth.launch()
    AuthProviderIds.GOOGLE in user.providerIds -> googleReauth.launch()
}
```

### Deleting the account

`KMPAuth.deleteAccount()` permanently deletes the signed-in user and ends the
session. A stale session fails with the typed
`KMPAuthRecentLoginRequiredException` — reauthenticate (above) and retry:

```kotlin
KMPAuth.deleteAccount().onFailure { error ->
    if (error is KMPAuthRecentLoginRequiredException) {
        // ask for a fresh credential, then:
        KMPAuth.reauthenticate(freshCredential)
            .onSuccess { KMPAuth.deleteAccount() }
    }
}
```

The Supabase backend reports deletion as unsupported — GoTrue only deletes
users through the admin API, so expose a Supabase Edge Function calling
`auth.admin.deleteUser` and invoke that from the app.

## Auth backends

The `rememberXxxAuthState` flows and `KMPAuth.*` operations are served by a
pluggable `AuthProviderBackend`. KMPAuth ships two implementations —
[Firebase](firebase.md) (self-registering default) and
[Supabase](supabase.md) — and you can plug in
[your own or several at once](custom-backends.md).

The provider-only states (`rememberGoogleSignInState`,
`rememberFacebookSignInState`, `rememberAppleSignInState`) don't need a
backend at all.

## Account linking

Every auth state and `KMPAuth.signIn` accept `linkAccount = true` /
`linkWithCurrentUser = true`: the new credential is linked to the currently
signed-in user instead of creating a new session — the standard way to
upgrade an [anonymous user](anonymous.md) to a permanent account while
keeping its uid and data.
