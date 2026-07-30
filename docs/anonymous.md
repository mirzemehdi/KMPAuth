# Anonymous (guest) sign-in

State lives in `kmpauth-core` and is served by whichever backend is
registered — [Firebase](firebase.md) or [Supabase](supabase.md). Enable the
"Anonymous" sign-in method in your backend's console first.

Lets users try the app before creating an account:

```kotlin
val anonymousSignIn = rememberAnonymousAuthState(onResult = { result: Result<KMPAuthUser> -> })
Button(onClick = { anonymousSignIn.launch() }) { Text("Continue as guest") }
```

## Upgrading a guest to a permanent account

Sign in with any auth state using `linkAccount = true` (e.g.
`rememberEmailAuthState(..., linkAccount = true)`) — the credential is
linked to the anonymous user, keeping its uid and data.

### When the identity already has an account

Linking fails with `KMPAuthUserCollisionException` when the Google/Apple/
email identity already belongs to an existing account — the classic case is
a returning user who reinstalled the app and got a fresh anonymous session.
Handle it by signing in without linking, which enters the existing account
(the anonymous session's data must be migrated by the app):

```kotlin
KMPAuth.signIn(credential, linkWithCurrentUser = true).onFailure { error ->
    if (error is KMPAuthUserCollisionException) {
        KMPAuth.signIn(credential) // returning user - use the existing account
    }
}
```

Every backend maps its own collision errors to this type on every platform,
so one `is`-check is enough — no backend-specific exception classes or
message matching.
