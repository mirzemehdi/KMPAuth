# Google Sign-In

Module: `kmpauth-google`. Works on Android, iOS, Desktop (JVM), Web (JS and
wasm).

## Setup

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

## Usage

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

Options (both states):

- `filterByAuthorizedAccounts` — true limits the chooser to accounts that
  already signed in to your app; when none exists the flow retries with all
  accounts so first-time users can still sign in.
- `isAutoSelectEnabled` — one-account auto sign-in.
- `scopes` — defaults to `email`/`profile`.
- `requestAccessToken` — see [Access token](#getting-an-access-token).

## Platform setup

### Android

No platform-specific setup.

> **"Google Play services out of date" at sign-in?** Google Sign-In runs through
> Credential Manager, which needs a recent **Google Play services APK on the
> device** — independent of your app's `minSdk`/`targetSdk`. Update Google
> Play services on the device; on an emulator use a system image with the
> **Google Play Store** (images labelled "Google APIs" only ship an older,
> non-updatable Play services).
>
> At runtime this condition reaches your `onResult` as the typed
> `KMPAuthProviderUnavailableException` (and a device without any Google
> account as `KMPAuthNoAccountAvailableException`), so the app can tell the
> user exactly what to fix — see
> [Typed failures](core-concepts.md#typed-failures).

### iOS

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

### Desktop (JVM)

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

### Web (JS/wasm)

Add your site's origin (e.g. `http://localhost:8080` during development) to
the OAuth client's **Authorized JavaScript origins** in the Google Cloud
console. The ID token comes from Sign in with Google (One Tap, FedCM); an
access token — when requested — from the GIS token flow.

## Getting an access token

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
