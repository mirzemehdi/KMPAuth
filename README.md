# KMPAuth — Kotlin Multiplatform Authentication Library

[![Build](https://github.com/mirzemehdi/KMPAuth/actions/workflows/build_and_publish.yml/badge.svg)](https://github.com/mirzemehdi/KMPAuth/actions/workflows/build_and_publish.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.0-blue.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.mirzemehdi/kmpauth-google?color=blue)](https://search.maven.org/search?q=g:io.github.mirzemehdi+kmpauth)

![badge-android](http://img.shields.io/badge/platform-android-6EDB8D.svg?style=flat)
![badge-ios](http://img.shields.io/badge/platform-ios-AAAAFF.svg?style=flat)
![badge-desktop](http://img.shields.io/badge/platform-desktop-FF8E8E.svg?style=flat)
![badge-web](http://img.shields.io/badge/platform-web-FFCC66.svg?style=flat)

Simple and easy-to-use authentication for Compose Multiplatform apps on
**Android, iOS, Desktop (JVM) and Web (JS + wasm)**. Sign in with **Google,
Apple, Facebook, GitHub, Microsoft, email/password, magic links, phone
number or anonymously** — backed by **Firebase** or **Supabase** (or your
own backend), with every API callable from `commonMain` on every target.

```kotlin
@Composable
fun SignInButtons(onResult: (Result<KMPAuthUser>) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val googleSignIn = rememberGoogleAuthState(onResult = onResult)
        GoogleSignInButton { googleSignIn.launch() }

        val appleSignIn = rememberAppleAuthState(onResult = onResult)
        AppleSignInButton { appleSignIn.launch() }

        val facebookSignIn = rememberFacebookAuthState(onResult = onResult)
        FacebookSignInButton { facebookSignIn.launch() }

        val githubSignIn = rememberGithubAuthState(onResult = onResult)
        Button(onClick = { githubSignIn.launch() }) { Text("Sign in with GitHub") }
    }
}
```

KMPAuth powers [FindTravelNow](https://github.com/mirzemehdi/FindTravelNow-KMM/),
a production KMP app. Full API reference:
[mirzemehdi.github.io/KMPAuth](https://mirzemehdi.github.io/KMPAuth).
Related blog post:
[Integrating Google Sign-In into Kotlin Multiplatform](https://proandroiddev.com/integrating-google-sign-in-into-kotlin-multiplatform-8381c189a891).

<p style="text-align: center;">
  <img src="https://github.com/mirzemehdi/KMPAuth/assets/32781662/f5a3cd28-6ef2-46bf-9b07-a045ce217b34)" width="200" alt="SampleApp"/>
</p>

## Table of contents

- [What's supported where](#whats-supported-where)
- [Installation](#installation)
- [Quick start](#quick-start)
- [Core concepts](#core-concepts)
- [Sign-in providers](#sign-in-providers)
  - [Google](#google-sign-in)
  - [Apple](#apple-sign-in)
  - [Facebook](#facebook-sign-in)
  - [GitHub](#github-sign-in)
  - [Microsoft](#microsoft-sign-in)
  - [Other OAuth providers](#other-oauth-providers)
  - [Email (password, reset, magic link)](#email-authentication)
  - [Phone number](#phone-sign-in)
  - [Anonymous (guest)](#anonymous-sign-in)
- [Account operations](#account-operations)
- [Auth backends](#auth-backends)
  - [Firebase (default)](#firebase-backend)
  - [Supabase](#supabase-backend)
  - [Several backends at once](#using-several-backends-at-once)
  - [Custom backends](#custom-backends)
- [UI helper buttons](#ui-helper-buttons)
- [Migrating from 2.x](#migrating-from-2x)

## What's supported where

| Feature | Android | iOS | Desktop (JVM) | Web (JS) | Web (wasm) |
|---|:---:|:---:|:---:|:---:|:---:|
| Google sign-in | ✅ | ✅ | ✅ | ✅ | ✅ |
| Apple sign-in (via Firebase) | ✅ | ✅ native | ✅ | — | — |
| Apple sign-in (native, no backend) | — | ✅ | — | — | — |
| Facebook sign-in | ✅ | ✅ | — | — | — |
| GitHub / Microsoft / any OAuth (via Firebase) | ✅ | ✅ | ✅ | — | — |
| Email password / reset / magic link | ✅ | ✅ | ✅ | ✅ | ✅ Supabase |
| Phone number sign-in | ✅ | ✅ | — | — | — |
| Anonymous sign-in | ✅ | ✅ | ✅ | ✅ | ✅ Supabase |
| Firebase backend | ✅ | ✅ | ✅ REST | ✅ | — |
| Supabase backend | ✅ | ✅ | ✅ | ✅ | ✅ |

Everything compiles and is callable from `commonMain` on **all** targets —
a feature unavailable on the current platform reports a failed
`Result` with the reason instead of not compiling or silently doing nothing.
On wasm the Firebase SDK does not exist yet, so Firebase-backed flows fail
with a clear message; the Supabase backend fully works on wasm.

## Installation

KMPAuth is on Maven Central:

```kotlin
repositories {
    mavenCentral()
}
```

Pick the artifacts you need in your shared module's `commonMain`
(latest version: [![Maven Central](https://img.shields.io/maven-central/v/io.github.mirzemehdi/kmpauth-google?color=blue)](https://search.maven.org/search?q=g:io.github.mirzemehdi+kmpauth)):

```kotlin
sourceSets {
    commonMain.dependencies {
        // Identity providers (credential only - no backend required):
        implementation("io.github.mirzemehdi:kmpauth-google:<version>")
        implementation("io.github.mirzemehdi:kmpauth-facebook:<version>")
        implementation("io.github.mirzemehdi:kmpauth-apple:<version>")

        // Session backend - pick one (or both, see "Several backends at once"):
        implementation("io.github.mirzemehdi:kmpauth-firebase-core:<version>")
        implementation("io.github.mirzemehdi:kmpauth-supabase:<version>")

        // Pre-styled "Sign in with ..." buttons:
        implementation("io.github.mirzemehdi:kmpauth-uihelper:<version>")
    }
}
```

| Artifact | What it gives you |
|---|---|
| `kmpauth-google` | Google sign-in (`rememberGoogleSignInState`, `rememberGoogleAuthState`) |
| `kmpauth-facebook` | Facebook login (`rememberFacebookSignInState`, `rememberFacebookAuthState`) |
| `kmpauth-apple` | Native Sign in with Apple, Apple platforms only, no backend needed |
| `kmpauth-firebase-core` | Firebase auth backend + the Apple/GitHub/Microsoft/OAuth/phone auth states |
| `kmpauth-supabase` | Supabase auth backend |
| `kmpauth-uihelper` | Google/Apple/Facebook buttons per each brand's guidelines |
| `kmpauth-firebase` | 2.x-compatible bundle: `kmpauth-firebase-core` + the deprecated 2.x containers |

**iOS native SDKs** are added to your Xcode project via **Swift Package
Manager** (CocoaPods is no longer supported as of 3.0 — see
[MIGRATION.md](MIGRATION.md)):

| You use | Add package | Products |
|---|---|---|
| `kmpauth-google` | `https://github.com/google/GoogleSignIn-iOS` | `GoogleSignIn` |
| `kmpauth-firebase-*` | `https://github.com/firebase/firebase-ios-sdk` | `FirebaseAuth`, `FirebaseCore` |
| `kmpauth-facebook` | `https://github.com/facebook/facebook-ios-sdk` | `FacebookCore`, `FacebookLogin` |

#### Requirements (3.0+)

| | Minimum |
|---|---|
| iOS deployment target | 16.0 |
| Xcode | 16.4 |
| JVM runtime (desktop apps) | 17 |
| Android compileSdk | 37 |

> [!NOTE]
> If you get `MissingResourceException` on iOS, see the solution in
> [this issue's comments](https://github.com/mirzemehdi/KMPAuth/issues/2).

## Quick start

**1. Initialize once at application start.** Provider and backend modules
plug their setup into the same block:

```kotlin
KMPAuth.initialize {
    logger { println("KMPAuthLog: $it") }          // optional
    google(serverId = WebClientId)                  // kmpauth-google
    // Firebase on Desktop/Web also needs the web config (no-op on Android/iOS):
    firebase(apiKey = "...", projectId = "...", applicationId = "...")
    // ...or Supabase instead of Firebase:
    // supabase(url = projectUrl, apiKey = publishableKey)
}
```

With `kmpauth-firebase-core` in your dependencies **the Firebase backend
registers itself automatically** (ServiceLoader on JVM/Android, load-time
registration on iOS/JS/wasm) — on Android/iOS, where the Firebase SDK reads
its bundled config files, no `firebase(...)` call is needed at all.

**2. Drop a sign-in state next to any button:**

```kotlin
val googleSignIn = rememberGoogleAuthState(onResult = { result ->
    result.onSuccess { user -> println("Signed in: ${user.uid}") }
        .onFailure { error -> println("Failed: $error") }
})
GoogleSignInButton { googleSignIn.launch() }
```

**3. Session and account operations live on the `KMPAuth` object:**

```kotlin
val user: KMPAuthUser? = KMPAuth.currentUser()
KMPAuth.signOut()
```

Check out the [sample app](sampleApp/shared/src/commonMain/kotlin/com/mmk/kmpauth/sample/App.kt)
for a working screen covering every feature.

## Core concepts

**Two layers of composable states**, both returning a `SignInState` with
`launch()` and an observable `isInProgress`:

| Layer | Naming | Returns | Backend needed |
|---|---|---|---|
| Credential only | `rememberXxxSignInState` | `Result<GoogleUser>` / `Result<FacebookUser>` / `Result<AppleUser>` | no — you handle the token |
| Session | `rememberXxxAuthState` | `Result<KMPAuthUser>` | yes — credential exchanged through the registered backend |

Wire either to any clickable and drive loading UI from `isInProgress`.
Parameters (e.g. `linkAccount`) are read at launch time, so toggling them
via recomposition just works; double-taps cannot start two flows. Results
are non-null: a flow that produces no user is a **failure with a reason** —
cancellation, misconfiguration, unsupported platform — never a silent null.

**`KMPAuth` object** is the entry point for everything that isn't a
launchable flow: `initialize { }`, `currentUser()`, `signOut()`,
`signIn(credential)`, `signUp`, `signInAnonymously`,
`reauthenticate(credential)`, `sendPasswordResetEmail`, email-link sign-in
and backend registration — see [Account operations](#account-operations).

**`KMPAuthUser`** is KMPAuth's own backend-agnostic user model (`uid`,
`email`, `displayName`, `photoUrl`, `providerId`). The native SDK object
stays reachable via `KMPAuthUser.raw` (e.g.
`dev.gitlive.firebase.auth.FirebaseUser` with the Firebase backend).

The 2.x `*UiContainer { this.onClick() }` composables still work but are
deprecated (removal planned for 4.0) — see [MIGRATION.md](MIGRATION.md).

## Sign-in providers

### Google Sign-In

Set up OAuth 2.0 in the Google Cloud console
([steps](https://support.google.com/cloud/answer/6158849)). **Easy tip:**
enabling Google sign-in in Firebase auto-generates the OAuth clients for each
platform — the **Web Client ID** is the one you pass as `serverId`:

```kotlin
KMPAuth.initialize {
    google(serverId = WebClientId)
}
```

The 2.x `GoogleAuthProvider.create(GoogleAuthCredentials(serverId))` call
still works and does the same thing.

```kotlin
// Credential only - send googleUser.idToken to your own backend:
val googleSignIn = rememberGoogleSignInState(onResult = { result ->
    result.onSuccess { googleUser -> val idToken = googleUser.idToken }
        .onFailure { error -> /* cancellation, GetCredentialException, ... */ }
})
Button(onClick = { googleSignIn.launch() }) { Text("Google Sign-In") }

// Or a full session through the registered backend (Firebase/Supabase):
val googleAuth = rememberGoogleAuthState(onResult = { result: Result<KMPAuthUser> -> })
GoogleSignInButton { googleAuth.launch() }
```

Options (both states): `filterByAuthorizedAccounts` (true limits the chooser
to accounts that already signed in to your app; when none exists the flow
retries with all accounts), `isAutoSelectEnabled` (one-account auto sign-in),
`scopes`, and `requestAccessToken` (below).

<details>
<summary><b>Android setup</b></summary>

No platform-specific setup.

> **"Google Play services out of date" at sign-in?** Google Sign-In runs through
> Credential Manager, which needs a recent **Google Play services APK on the
> device** — independent of your app's `minSdk`/`targetSdk`. Update Google
> Play services on the device; on an emulator use a system image with the
> **Google Play Store** (images labelled "Google APIs" only ship an older,
> non-updatable Play services).

</details>

<details>
<summary><b>iOS setup</b></summary>

Add the client IDs to `Info.plist`:

```xml
<key>GIDServerClientID</key>
<string>YOUR_SERVER_CLIENT_ID</string>

<key>GIDClientID</key>
<string>YOUR_IOS_CLIENT_ID</string>
<key>CFBundleURLTypes</key>
<array>
  <dict>
    <key>CFBundleURLSchemes</key>
    <array>
      <string>YOUR_DOT_REVERSED_IOS_CLIENT_ID</string>
    </array>
  </dict>
</array>
```

And forward the URL callback on the Swift side:

```swift
import SwiftUI
import shared
import GoogleSignIn

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
      _ app: UIApplication,
      open url: URL, options: [UIApplication.OpenURLOptionsKey : Any] = [:]
    ) -> Bool {
      return GIDSignIn.sharedInstance.handle(url)
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    var body: some Scene {
        WindowGroup {
            ContentView().onOpenURL(perform: { url in
                GIDSignIn.sharedInstance.handle(url)
            })
        }
    }
}
```

</details>

<details>
<summary><b>Desktop (JVM) setup</b></summary>

On desktop the OAuth flow runs in the system browser and returns on a
localhost loopback server. Google requires the redirect URI to be
**pre-registered** with a **fixed port**:

1. In the Google Cloud console, add an **Authorized redirect URI** for your
   OAuth client — e.g. `http://localhost:8080/callback`.
2. Pass that exact URI as `redirectUri` (defaults to
   `http://localhost:8080/callback`):

```kotlin
KMPAuth.initialize {
    google(serverId = WebClientId, redirectUri = "http://localhost:8080/callback")
}
```

Any `http` loopback host (`localhost`/`127.0.0.1`), port and path are allowed
as long as the same URI is registered. If the port is taken when the user
signs in, the failure is logged clearly — free it or register a different
URI. `redirectUri` is ignored on other platforms.

> **Packaging with jpackage/jlink:** the loopback runs on the JDK's built-in
> `com.sun.net.httpserver` (`jdk.httpserver` module). jlink strips unused
> modules, so declare it or sign-in fails at runtime in packaged builds:
> ```kotlin
> compose.desktop {
>     application {
>         nativeDistributions {
>             modules("jdk.httpserver")
>         }
>     }
> }
> ```

</details>

<details>
<summary><b>Web (JS/wasm) setup</b></summary>

Add your site's origin (e.g. `http://localhost:8080` during development) to
the OAuth client's **Authorized JavaScript origins** in the Google Cloud
console. The ID token comes from Sign in with Google (One Tap, FedCM);
an access token — when requested — from the GIS token flow.

</details>

##### Getting an access token

`GoogleUser.idToken` is always present. `accessToken` — for calling Google
APIs on the user's behalf — depends on the platform:

| Platform | `accessToken` |
|---|---|
| iOS, Desktop | always returned |
| Android (Credential Manager), JS, wasm | returned only when you ask (below) |
| Android legacy fallback | never returned |

Android's Credential Manager hands back an ID token only; an access token
needs a **separate authorization request with its own consent prompt**, so
you opt in:

```kotlin
val googleSignIn = rememberGoogleSignInState(
    requestAccessToken = true,
    onResult = { result -> val accessToken = result.getOrNull()?.accessToken },
)
```

Requesting `scopes` beyond `email`/`profile` implies it.

### Apple Sign-In

Enable Apple in the Firebase console and add the **"Sign In with Apple"
capability** in Xcode. On iOS the flow is native; on Android/Desktop/JS
Firebase drives Apple's web flow:

```kotlin
val appleSignIn = rememberAppleAuthState(onResult = { result: Result<KMPAuthUser> -> })
AppleSignInButton { appleSignIn.launch() }
```

#### Native Apple Sign-In without a backend (Apple platforms only)

Use `kmpauth-apple` when your own server verifies Apple's identity token.
The native flow returns a signed JWT that any backend can validate against
Apple's public keys — no client secret involved:

```kotlin
val appleSignIn = rememberAppleSignInState(onResult = { result ->
    val appleUser = result.getOrNull()
    val idToken = appleUser?.idToken   // send to your backend
    val rawNonce = appleUser?.nonce    // if your backend verifies the nonce claim
})
AppleSignInButton { appleSignIn.launch() }
```

> **Apple platforms only.** On Android, JVM, JS and wasm
> `rememberAppleSignInState` reports a failed `Result` — Apple's web flow
> returns an authorization code that must be exchanged with a **client secret
> server-side**. Use `rememberAppleAuthState` there; Firebase performs the
> exchange for you.
>
> `email` and `fullName` are returned by Apple **only on the user's first
> authorization** — persist them server-side; later sign-ins return null.

### Facebook Sign-In

Android and iOS only (the Facebook SDK has no other targets).

```kotlin
// Credential only:
val facebookSignIn = rememberFacebookSignInState(onResult = { result: Result<FacebookUser> -> })

// Or a full session through the registered backend:
val facebookAuth = rememberFacebookAuthState(onResult = { result: Result<KMPAuthUser> -> })
FacebookSignInButton { facebookAuth.launch() }
```

##### Login tracking (token type)

Both states accept `loginTracking`, consistent on Android and iOS:

- `FacebookLoginTracking.Limited` (**default**) — privacy-friendly Limited
  Login; returns an OIDC JWT + nonce (no iOS App Tracking Transparency
  prompt). Firebase exchanges it through the OIDC OAuth provider.
- `FacebookLoginTracking.Enabled` — classic login; returns a Graph-API
  access token in `FacebookUser.accessToken`. Counts as tracking on iOS
  (handle ATT).

```kotlin
// If your backend needs a Graph-API access token:
val facebookSignIn = rememberFacebookSignInState(
    loginTracking = FacebookLoginTracking.Enabled,
    onResult = { result -> val accessToken = result.getOrNull()?.accessToken },
)
```

<details>
<summary><b>Android setup</b></summary>

Add to `res/values/strings.xml`:

```xml
<string name="facebook_app_id">YOUR_FACEBOOK_APP_ID</string>
<string name="fb_login_protocol_scheme">fbYOUR_FACEBOOK_APP_ID</string>
<string name="facebook_client_token">YOUR_FACEBOOK_CLIENT_TOKEN</string>
```

Add to `AndroidManifest.xml` inside `<application>`:

```xml
<meta-data
    android:name="com.facebook.sdk.ApplicationId"
    android:value="@string/facebook_app_id" />

<meta-data
    android:name="com.facebook.sdk.ClientToken"
    android:value="@string/facebook_client_token" />

<activity
    android:name="com.facebook.FacebookActivity"
    android:configChanges="keyboard|keyboardHidden|screenLayout|screenSize|orientation"
    android:label="@string/app_name" />
```

When using `FacebookLoginTracking.Limited` (the **default**), forward your
main Activity's result to `KMPAuth.handleFacebookActivityResult`:

```kotlin
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    KMPAuth.handleFacebookActivityResult(requestCode, resultCode, data)
    super.onActivityResult(requestCode, resultCode, data)
}
```

> **Why only for `Limited`?** Limited Login requires a nonce, which the
> Facebook SDK accepts only through `LoginConfiguration` — an API whose
> result still arrives via `onActivityResult`. With
> `FacebookLoginTracking.Enabled`, KMPAuth uses the SDK's AndroidX Activity
> Result API and **no override is needed**. Calling
> `handleFacebookActivityResult` when it isn't needed is harmless.

</details>

<details>
<summary><b>iOS setup</b></summary>

Add the Facebook SDK Swift package, then add to `Info.plist`:

```xml
<key>CFBundleURLTypes</key>
<array>
  <dict>
    <key>CFBundleURLSchemes</key>
    <array>
      <string>fbFACEBOOK_APP_ID</string> <!-- Your Facebook App ID with 'fb' prefix -->
    </array>
  </dict>
</array>

<key>FacebookAppID</key>
<string>FACEBOOK_APP_ID</string>

<key>FacebookClientToken</key>
<string>YOUR_FACEBOOK_CLIENT_TOKEN</string>

<key>FacebookDisplayName</key>
<string>YourAppDisplayName</string>

<key>LSApplicationQueriesSchemes</key>
<array>
  <string>fbapi</string>
  <string>fb-messenger-api</string>
  <string>fbauth2</string>
  <string>fbshareextension</string>
</array>
```

Initialize the Facebook SDK on the Swift side:

```swift
func application(_ application: UIApplication,
                 didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
    FirebaseApp.configure()
    FBSDKCoreKit.ApplicationDelegate.shared.application(
        application,
        didFinishLaunchingWithOptions: launchOptions
    )
    return true
}

func application(
    _ app: UIApplication,
    open url: URL,
    options: [UIApplication.OpenURLOptionsKey : Any] = [:]
) -> Bool {
    return FBSDKCoreKit.ApplicationDelegate.shared.application(
        app,
        open: url,
        options: options
    )
}
```

</details>

Reference docs:
[Facebook Login for Android](https://developers.facebook.com/docs/facebook-login/android) ·
[Facebook Login for iOS](https://developers.facebook.com/docs/facebook-login/ios) ·
[Firebase + Facebook](https://firebase.google.com/docs/auth/android/facebook-login)

### GitHub Sign-In

Enable the GitHub provider in the Firebase console — Firebase drives the
OAuth web flow, no GitHub SDK involved:

```kotlin
val githubSignIn = rememberGithubAuthState(onResult = { result: Result<KMPAuthUser> -> })
Button(onClick = { githubSignIn.launch() }) { Text("Sign in with GitHub") }
```

Options: `requestScopes` (default `["user:email"]`), `customParameters`,
`linkAccount`.

### Microsoft Sign-In

Enable the Microsoft provider in the Firebase console and register the app
in the Azure portal — Firebase drives the OAuth web flow, no Microsoft SDK
involved:

```kotlin
val microsoftSignIn = rememberMicrosoftAuthState(onResult = { result: Result<KMPAuthUser> -> })
Button(onClick = { microsoftSignIn.launch() }) { Text("Sign in with Microsoft") }
```

To restrict sign-in to one Azure AD tenant, pass
`customParameters = mapOf("tenant" to "your-tenant-id")`.

### Other OAuth providers

Any OAuth provider configured in the Firebase console works through the
generic state (GitHub and Microsoft above are thin wrappers over it):

```kotlin
val yahooSignIn = rememberOAuthState(
    provider = "yahoo.com",
    onResult = { result: Result<KMPAuthUser> -> },
)
```

### Email Authentication

Enable the "Email/Password" sign-in method in the console. The state lives
in `kmpauth-core` and is served by whichever backend is registered —
Firebase or Supabase:

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

**Password reset:**

```kotlin
KMPAuth.sendPasswordResetEmail(email)
```

**Passwordless magic link** (enable "Email link" in the Firebase console):

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

### Phone Sign-In

Enable the "Phone" sign-in method in the Firebase console. Two-step flow:
`launch()` sends the SMS, `submitCode` completes sign-in. Android (with
automatic SMS verification when Play services can) and iOS:

```kotlin
var phoneNumber by remember { mutableStateOf("") }
var smsCode by remember { mutableStateOf("") }
val phoneSignIn = rememberPhoneAuthState(
    phoneNumber = phoneNumber, // E.164 format, e.g. +15551234567
    onResult = { result: Result<KMPAuthUser> -> },
)

if (!phoneSignIn.isCodeSent) {
    Button(onClick = { phoneSignIn.launch() }) { Text("Send code") }
} else {
    OutlinedTextField(value = smsCode, onValueChange = { smsCode = it })
    Button(onClick = { phoneSignIn.submitCode(smsCode) }) { Text("Verify") }
}
```

`phoneSignIn.cancel()` abandons the flow. On Desktop and web, launching
reports a failed `Result` — the Firebase Java SDK does not implement phone
auth, and the web flow would need a reCAPTCHA verifier KMPAuth does not
provide yet.

### Anonymous Sign-In

Enable the "Anonymous" sign-in method in the console. Lets users try the
app before creating an account:

```kotlin
val anonymousSignIn = rememberAnonymousAuthState(onResult = { result: Result<KMPAuthUser> -> })
Button(onClick = { anonymousSignIn.launch() }) { Text("Continue as guest") }
```

To later upgrade the guest to a permanent account, sign in with any auth
state using `linkAccount = true` (e.g.
`rememberEmailAuthState(..., linkAccount = true)`) — the credential is
linked to the anonymous user, keeping its uid and data.

## Account operations

Everything that isn't a launchable flow lives on the `KMPAuth` object and is
served by the registered backend:

```kotlin
KMPAuth.currentUser()                 // KMPAuthUser? - null when signed out
KMPAuth.signOut()
KMPAuth.signIn(credential)            // exchange a credential you obtained yourself
KMPAuth.signUp(email, password)
KMPAuth.signInAnonymously()
KMPAuth.sendPasswordResetEmail(email)
KMPAuth.sendSignInLinkToEmail(email, actionCodeSettings)
KMPAuth.isSignInWithEmailLink(link)
KMPAuth.signInWithEmailLink(email, link)
KMPAuth.reauthenticate(credential)
```

**Reauthentication:** Firebase requires a recent sign-in before
security-sensitive operations (deleting the account, changing the
password). Obtain a fresh credential and retry:

```kotlin
// Email/password users:
KMPAuth.reauthenticate(AuthCredential.EmailPassword(email, currentPassword))
    .onSuccess { /* now delete the account / update the password */ }

// Google/Apple/Facebook users: rerun the provider flow for a fresh token, then
KMPAuth.reauthenticate(
    AuthCredential.IdToken(AuthProviderIds.GOOGLE, googleUser.idToken)
)
```

## Auth backends

The `rememberXxxAuthState` flows and `KMPAuth.*` operations are served by a
pluggable `AuthProviderBackend`. The provider-only states
(`rememberGoogleSignInState`, `rememberFacebookSignInState`,
`rememberAppleSignInState`) don't need a backend at all.

### Firebase backend

**Zero setup on Android and iOS**: with `kmpauth-firebase-core` in your
dependencies the backend registers itself automatically (`ServiceLoader` on
JVM/Android — the R8 keep rule ships in the consumer rules — and eager
load-time registration on iOS/JS/wasm), and the Firebase SDK reads its
bundled `google-services.json` / `GoogleService-Info.plist`.

**Desktop (JVM) and Web** additionally need the web app config once:

```kotlin
KMPAuth.initialize {
    firebase(apiKey = "...", projectId = "...", applicationId = "...")
}
```

Desktop support is complete: email/password, email link, anonymous,
reauthentication and Google/Facebook/Apple token exchange run against the
**Firebase Auth REST API** (GitLive's firebase-java-sdk has no auth
implementation; the session is held in memory). The **browser flows**
(`rememberOAuthState` / GitHub / Microsoft / Apple) open the system browser
on a local page that runs Firebase's official JS SDK against your project's
hosted auth handler — every provider configured in the Firebase console
works, including Apple. Phone sign-in stays unavailable on Desktop
(reCAPTCHA).

On **Web (JS)** email/anonymous/Google run through the Firebase JS SDK; the
browser-flow states (OAuth/GitHub/Microsoft/Apple) and phone are not
implemented there yet and report a failed `Result`. On **wasm** all Firebase
flows report a failed `Result` (the Firebase SDK has no wasm target — use
Supabase for wasm).

### Supabase backend

Firebase is not required — `kmpauth-supabase` serves the same
backend-agnostic API from a [Supabase](https://supabase.com) project on
every target **including wasm**:

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
platform (e.g. `ktor-client-okhttp` on Android, `ktor-client-darwin` on
iOS, `ktor-client-cio` on Desktop, `ktor-client-js` on JS/wasm) — same as
any supabase-kt setup.

What runs against Supabase (enable the matching providers in the dashboard):

- **Works**: `rememberEmailAuthState` (sign-in and sign-up),
  `rememberAnonymousAuthState`, `rememberGoogleAuthState`,
  `rememberFacebookAuthState` (Limited Login/OIDC only), and the `KMPAuth`
  operations: `signIn`, `signUp`, `signInAnonymously`,
  `sendPasswordResetEmail`, magic links (`sendSignInLinkToEmail` /
  `isSignInWithEmailLink` / `signInWithEmailLink` — `token_hash`, PKCE
  `code` and implicit-flow redirects are all recognized), `reauthenticate`
  (as a fresh sign-in; Supabase has no recent-login requirement),
  `currentUser`, `signOut`. Id-token linking (`linkAccount = true`) uses
  Supabase identity linking and requires manual linking enabled on the
  project.
- **Doesn't (by design)**: classic Facebook access tokens (Supabase's
  `id_token` grant accepts only OIDC tokens — use Facebook Limited Login),
  the `kmpauth-firebase-core`-resident web-flow states
  (GitHub/Microsoft/OAuth/Apple-web/phone — use supabase-kt's
  `signInWith(Github)` etc. directly via
  `SupabaseAuthBackend.supabaseClient`), and linking an email/password
  credential (Supabase adds an email via `auth.updateUser` instead). All
  unsupported paths report a failed `Result` naming the Supabase-idiomatic
  alternative.

Of the `EmailActionCodeSettings` fields only `url` maps to Supabase (as the
redirect URL, which must be in the project's allow-list); the
iOS/Android-app fields are Firebase dynamic-link concepts.

### Using several backends at once

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

### Custom backends

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

## UI helper buttons

`kmpauth-uihelper` provides pre-styled buttons following each brand's
guidelines — mix freely with your own designs:

```kotlin
GoogleSignInButton(modifier = Modifier.fillMaxWidth()) { googleSignIn.launch() }
GoogleSignInButtonIconOnly(onClick = { googleSignIn.launch() })

AppleSignInButton(modifier = Modifier.fillMaxWidth()) { appleSignIn.launch() }
AppleSignInButtonIconOnly(onClick = { appleSignIn.launch() })

FacebookSignInButton(modifier = Modifier.fillMaxWidth()) { facebookSignIn.launch() }
FacebookSignInButtonIconOnly(onClick = { facebookSignIn.launch() })
```

## Migrating from 2.x

Follow the step-by-step [MIGRATION.md](MIGRATION.md) — most 2.x code keeps
compiling (the `*UiContainer` composables and `GoogleAuthProvider.create`
still work, deprecated). All notable changes live in
[CHANGELOG.md](CHANGELOG.md); the full API reference is at
[mirzemehdi.github.io/KMPAuth](https://mirzemehdi.github.io/KMPAuth).
