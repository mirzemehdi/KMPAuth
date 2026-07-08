# Migrating KMPAuth 2.x → 3.0

Step-by-step guide for upgrading from KMPAuth 2.x to 3.0.

## TL;DR checklist

- [ ] Bump `io.github.mirzemehdi:kmpauth-*` to `3.0.0`.
- [ ] iOS: replace CocoaPods pods with Swift Package Manager packages;
      raise your app's iOS deployment target to 16.0 and use Xcode 16.4+.
- [ ] Desktop/JVM: run on Java 17+ (artifacts now target JVM 17 bytecode).
- [ ] If you referenced the `@KMPAuthInternalApi` DI types directly
      (`KMPKoinComponent`, `LibDependencyInitializer`): remove those usages —
      they are deleted in 3.0. Public entry points (`GoogleAuthProvider.create`,
      the `*UiContainer` composables) are unchanged and need no code changes.
- [ ] Address `@Deprecated` warnings — each carries a `ReplaceWith` migration hint.

## What did NOT change

- All public composable containers keep their 2.x signatures and behavior
  (`GoogleButtonUiContainer`, `GoogleButtonUiContainerFirebase`,
  `AppleButtonUiContainer`, `GithubButtonUiContainer`, `OAuthContainer`,
  `FacebookButtonUiContainer`, `FacebookButtonUiContainerFirebase`) and the
  `kmpauth-uihelper` buttons. The containers are deprecated in favor of the
  `SignInState` API (section 4) but stay fully functional until 4.0.
- `GoogleAuthProvider.create(credentials)` initialization flow.
- Public models: `GoogleUser`, `GoogleAuthCredentials`, `FacebookUser`,
  request-scope types.
- Maven coordinates (`io.github.mirzemehdi:kmpauth-*`).

## 1. iOS: CocoaPods → Swift Package Manager

KMPAuth 3.0 no longer ships `kmpauth_*.podspec` files and no longer uses the
`kotlin("native.cocoapods")` plugin. The library declares its Apple
frameworks through the Kotlin `swiftPMDependencies {}` DSL (Kotlin 2.4+).

**In your iOS app (Xcode):**
1. Remove the KMPAuth-related pods from your `Podfile` (`kmpauth_google`,
   `kmpauth_firebase`, `kmpauth_facebook`, `kmpauth_firebase_facebook`) and
   run `pod install`; if KMPAuth was your only pod, deintegrate CocoaPods
   entirely (`pod deintegrate`).
2. Add the native SDKs your setup uses via **File → Add Package
   Dependencies…**:
   - Google Sign-In: `https://github.com/google/GoogleSignIn-iOS` (product `GoogleSignIn`), 9.1.0+
   - Firebase: `https://github.com/firebase/firebase-ios-sdk` (products `FirebaseAuth`, `FirebaseCore`), 11.8.0+
   - Facebook: `https://github.com/facebook/facebook-ios-sdk` (products `FacebookCore`, `FacebookLogin`), 18.0.0+
3. Set your app's iOS deployment target to **16.0+** and use **Xcode 16.4+**.
4. Keep integrating your shared framework the same way as before
   (`embedAndSignAppleFrameworkForXcode` build phase or XCFramework).

**In your shared KMP module:** if you consume KMPAuth from a KMP project
that itself builds the iOS framework, no Kotlin code changes are needed —
the `cocoapods.FirebaseAuth.*` bindings inside `kmpauth-firebase-core` come from
GitLive's bundled cinterop and keep working.

## 2. Koin removal

KMPAuth no longer uses (or ships) Koin — internal wiring is plain
constructor injection.

**Most users need no changes.** `GoogleAuthProvider.create(credentials)` and
all `*UiContainer` composables behave exactly as in 2.x (first `create()`
wins, `get()` before `create()` throws the same `IllegalArgumentException`).

You are only affected if you referenced the `@KMPAuthInternalApi`-annotated
DI types directly (both deleted in 3.0):

- `com.mmk.kmpauth.core.di.KMPKoinComponent` — was an internal bridge to
  KMPAuth's private Koin container; there is no replacement. If you were
  resolving KMPAuth types through it, use the public entry points instead.
- `com.mmk.kmpauth.core.di.LibDependencyInitializer` — initialization now
  happens inside `GoogleAuthProvider.create(...)`; simply delete the call.

If your app uses Koin itself, nothing changes — KMPAuth never shared your
application's Koin container (it ran a private `koinApplication`), so no
modules need removing from your setup.

## 3. Toolchain requirements

| Requirement | 2.x | 3.0 |
|---|---|---|
| iOS deployment target | 11.0–12.0 | 16.0 |
| Xcode | 15+ | 16.4+ |
| JVM bytecode | 1.8 | 17 |
| Android debug variant artifact | published | no longer published (single-variant; debug builds resolve the release variant automatically) |
| `iosX64` (Intel simulator) target | published | removed — use the arm64 simulator (Rosetta on Intel Macs) |

**JVM 17:** the `kmpauth-*` android and jvm artifacts now contain JVM 17
bytecode. Android builds are unaffected (AGP 9 requires JDK 17 anyway);
desktop/JVM apps must run on Java 17+.

**Single-variant Android artifacts:** if your build explicitly pinned the
`-debug` artifact (rare), drop the suffix — Gradle resolves the single
published variant for all build types automatically.

**Firebase BoM on Android:** `kmpauth-firebase` now exposes the Firebase
Android BoM constraint (`api(platform(firebase-bom))`), so Firebase artifact
versions are pinned automatically. If you were pinning Firebase versions
yourself to work around resolution errors, you can remove those pins.

## 4. SignInState instead of UiContainer composables (recommended)

The `*UiContainer { this.onClick() }` pattern is deprecated. The 3.0 way:

```kotlin
// 2.x (still works, deprecated):
GoogleButtonUiContainerFirebase(linkAccount = false, onResult = onFirebaseResult) {
    GoogleSignInButton { this.onClick() }
}

// 3.0:
val googleSignIn = rememberFirebaseGoogleSignInState(
    linkAccount = false,
    onResult = onFirebaseResult,
)
GoogleSignInButton(onClick = { googleSignIn.launch() })
```

Notes:
- `SignInState.isInProgress` is observable — drive spinners/disabled state.
- Parameters are read at launch time (backed by `rememberUpdatedState`), so
  recomposing with a different `linkAccount` (e.g. a sign-in/sign-up toggle)
  affects the next `launch()` without recreating the state.
- `launch()` while a flow is running is ignored (no double-launch).
- One state per provider per screen; no wrapping container, the button is
  entirely yours.

## 5. Granular Firebase artifacts (optional)

`kmpauth-firebase` still works exactly as in 2.x — same coordinates, now
published as an aggregator of the granular artifacts (its module lives
under `deprecated/` in the repository as a compatibility shim). Optionally slim your dependency tree:

| You use | Depend on |
|---|---|
| Everything (as in 2.x) | `kmpauth-firebase` (unchanged) |
| Apple/GitHub/OAuth + backend only | `kmpauth-firebase-core` |
| Google + Firebase | `kmpauth-firebase-google` (+ `kmpauth-google`) |
| Facebook + Firebase | `kmpauth-firebase-facebook` |

## 6. Pluggable auth backends

3.0 introduces `com.mmk.kmpauth.core.auth.AuthProviderBackend` with a
backend-agnostic credential/user model (`AuthCredential`, `KMPAuthUser`).

- **Existing Firebase users: no action.** `kmpauth-firebase` registers
  `FirebaseAuthBackend` automatically the first time a container is used;
  every composable keeps returning `Result<FirebaseUser?>` exactly as in 2.x.
- **Custom/Supabase backends:** implement `AuthProviderBackend` and call
  `KMPAuthBackend.register(yourBackend)` at application start (before any
  KMPAuth UI renders). The first registration wins, so an explicit
  registration always beats the lazy Firebase default; pass
  `replace = true` to swap an already-registered backend.
- Web-flow providers (Apple on Android, GitHub, generic OAuth) are executed
  by their dedicated container composables — a backend `signIn` call cannot
  drive a browser flow.

## 7. Desktop (JVM) Google Sign-In redirect URI

The desktop Google flow now uses a **fixed, configurable** loopback redirect
URI instead of a random port (2.x picked a random port that could not be
registered with Google, so sign-in often failed with `redirect_uri_mismatch`).

- **Default:** `http://localhost:8080/callback`. Register that exact URI as an
  Authorized redirect URI for your OAuth client in the Google Cloud console.
- **Custom URI:** pass the one you registered when creating credentials —
  `GoogleAuthCredentials(serverId = WebClientId, redirectUri = "http://127.0.0.1:9000/oauth2")`.
  Any `http` loopback host (`localhost`/`127.0.0.1`), port and path are allowed;
  the callback server binds the URI's port and serves its path.
- If the port is already in use, sign-in fails with a logged error (no silent
  random-port fallback); free the port or register another URI.
- Desktop-only: `redirectUri` is ignored on Android, iOS, JS and wasmJs.

## 8. Facebook login tracking (token type is now consistent)

Facebook sign-in gained a `loginTracking: FacebookLoginTracking` parameter on
`rememberFacebookSignInState`, `rememberFirebaseFacebookSignInState` and the
Facebook containers. It controls which token the flow returns and is consistent
across Android and iOS:

- **`FacebookLoginTracking.Limited` (new default)** — privacy-friendly Limited
  Login. Returns an OIDC **authentication token (JWT) + nonce** in
  `FacebookUser` (`accessToken` holds the JWT). No App Tracking Transparency
  prompt on iOS. With Firebase, exchanged through the OIDC `OAuthProvider`.
- **`FacebookLoginTracking.Enabled`** — classic login. Returns a real
  Graph-API **access token** in `FacebookUser.accessToken` (no nonce). Counts
  as tracking on iOS (handle ATT). With Firebase, exchanged through
  `FacebookAuthProvider`.

**⚠️ Behavior change on Android.** In 2.x, iOS already used Limited Login (JWT)
while **Android** used classic login (access token). The default is now
`Limited` on **both** platforms, so **Android now returns an OIDC JWT by
default instead of a Graph-API access token.** If your backend calls the
Graph API with `FacebookUser.accessToken`, pass
`loginTracking = FacebookLoginTracking.Enabled`:

```kotlin
val facebookSignIn = rememberFacebookSignInState(
    loginTracking = FacebookLoginTracking.Enabled,
    onResult = { result -> val accessToken = result.getOrNull()?.accessToken },
)
```

Firebase-backed Facebook sign-in needs no change: the credential type is
selected automatically from `loginTracking`, so the default `Limited` keeps
working on both platforms.
