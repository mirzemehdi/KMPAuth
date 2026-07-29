# Email authentication

State lives in `kmpauth-core` and is served by whichever backend is
registered — [Firebase](firebase.md) or [Supabase](supabase.md). Enable the
"Email/Password" sign-in method in your backend's console first.

## Password sign-in / sign-up

```kotlin
var email by remember { mutableStateOf("") }
var password by remember { mutableStateOf("") }

// Field values are read at launch time - create the state once and reuse it as the user types.
val emailSignIn = rememberEmailAuthState(
    email = email,
    password = password,
    mode = EmailAuthMode.SignIn, // or EmailAuthMode.SignUp to create the account
    onResult = { result: Result<KMPAuthUser> -> },
)
Button(onClick = { emailSignIn.launch() }, enabled = !emailSignIn.isInProgress) {
    Text("Sign in with email")
}
```

## Password reset

```kotlin
KMPAuth.sendPasswordResetEmail(email)
```

## Passwordless magic link

Enable "Email link" in the Firebase console (or magic links in Supabase):

```kotlin
// Step 1 - send the link, then persist `email` locally:
KMPAuth.sendSignInLinkToEmail(
    email = email,
    actionCodeSettings = EmailActionCodeSettings(
        url = "https://example.com/finish-sign-in",
        canHandleCodeInApp = true,
        iOSBundleId = "com.example.app",
        androidPackageName = "com.example.app",
    ),
)

// Step 2 - in your deep/universal link handler:
if (KMPAuth.isSignInWithEmailLink(link)) {
    val result = KMPAuth.signInWithEmailLink(persistedEmail, link)
}
```

## Reauthentication

Before security-sensitive operations (deleting the account, changing the
password), Firebase requires a recent sign-in:

```kotlin
KMPAuth.reauthenticate(AuthCredential.EmailPassword(email, currentPassword))
    .onSuccess { /* now delete the account / update the password */ }
```

See [Core concepts](core-concepts.md#reauthentication) for reauthenticating
Google/Apple/Facebook users.
