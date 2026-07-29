# Changelog

All notable changes to KMPAuth are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed
- **Backend-agnostic auth states — the `Firebase` prefix is gone** and the
  names now mark the two layers: `rememberXxxSignInState` returns the
  provider's credential (unchanged: `rememberGoogleSignInState`,
  `rememberFacebookSignInState`, `rememberAppleSignInState`), while
  `rememberXxxAuthState` exchanges it for a session through the registered
  `AuthProviderBackend` (Firebase today, Supabase-ready):
  `rememberGoogleAuthState` (in `kmpauth-google`), `rememberFacebookAuthState`
  (in `kmpauth-facebook`), `rememberAppleAuthState`, `rememberGithubAuthState`,
  `rememberMicrosoftAuthState`, `rememberOAuthState(provider)`,
  `rememberPhoneAuthState` (in `kmpauth-firebase-core`),
  `rememberEmailAuthState` and `rememberAnonymousAuthState` (in
  `kmpauth-core`). Google/Facebook exchange states moved into their provider
  modules — SDK isolation is unchanged (no Facebook SDK unless you depend on
  `kmpauth-facebook`), and `kmpauth-firebase-google`/`-facebook` now carry
  only the deprecated 2.x containers.
- **The Firebase backend registers itself automatically** when
  `kmpauth-firebase-core` is in the dependencies: `ServiceLoader` discovery
  on JVM/Android (R8 keep rule ships in the consumer rules) and eager
  load-time registration on iOS/JS/wasm - no setup call needed.
  `KMPAuth.registerBackendProvider(backend)` exists for custom backends or
  overriding the default; an explicit registration always wins.
- **Every Firebase API is callable from `commonMain`, including on wasm**
  (#179-adjacent). No GitLive types in signatures: every `onResult` receives
  `Result<KMPAuthUser>` — non-null; a backend producing no user is a failure
  with a reason, never a null success. The native
  `dev.gitlive.firebase.auth.FirebaseUser` stays reachable through
  `KMPAuthUser.raw`. On wasm (no Firebase SDK target) flows report a failed
  `Result` instead of not compiling.
- **`KMPAuth` is the single client-facing entry point** for everything that
  isn't a launchable sign-in state: `currentUser()`, `signOut()`,
  `signIn(credential)`, `signUp(email, password)`, `signInAnonymously()`,
  `reauthenticate(credential)`, `sendPasswordResetEmail`, email-link sign-in
  (`sendSignInLinkToEmail` / `isSignInWithEmailLink` / `signInWithEmailLink`),
  plus `registerBackendProvider(backend)` / `getBackendProvider()` /
  `requireBackendProvider()`. With no backend registered, `Result`-returning
  operations fail with a how-to-register message instead of throwing. Link
  configuration uses `EmailActionCodeSettings` (KMPAuth's own type in
  `kmpauth-core`) instead of GitLive's `ActionCodeSettings`.
- On Desktop and JS, the unimplemented Firebase flows (OAuth/GitHub/Apple web
  flow, Facebook) now report a failed `Result` explaining why instead of
  silently doing nothing when launched.
- The deprecated 2.x `*UiContainer` composables are unchanged: they keep their
  `Result<FirebaseUser?>` callbacks (unwrapping through `KMPAuthUser.raw`) and
  remain non-wasm.

### Added
- **`KMPAuth.initialize { }` one-stop setup**. Provider modules contribute
  their configuration as extensions on the scope - `kmpauth-google` adds
  `google(GoogleAuthCredentials(serverId))` (equivalent to
  `GoogleAuthProvider.create`, which still works); `logger { }` and
  `backendProvider(backend)` (custom backends only) are built in.
- **Email authentication** (#97, #110).
  `rememberEmailAuthState(email, password, mode, linkAccount, onResult)`
  signs in or — with `EmailAuthMode.SignUp` — creates the account; field values
  are read at launch time, so the state is created once and reused as the user
  types. The flows that don't fit a launchable state are `KMPAuth`
  operations: `sendPasswordResetEmail`, and passwordless email-link (magic
  link) sign-in via `sendSignInLinkToEmail` / `isSignInWithEmailLink` /
  `signInWithEmailLink`.
- **Phone number sign-in** (#111).
  `rememberPhoneAuthState(phoneNumber, ...)` returns a
  `PhoneAuthState`: `launch()` sends the SMS, `isCodeSent`/`onCodeSent`
  signal when to show the code input, `submitCode(code)` completes sign-in and
  `cancel()` abandons the flow. Android supports automatic SMS verification
  (Play services); iOS falls back to Firebase's reCAPTCHA when needed. On
  Desktop and JS/web launching reports a failed `Result` — the Firebase Java
  SDK does not implement phone auth, and the web flow would need a reCAPTCHA
  verifier KMPAuth does not provide yet.
- **Microsoft sign-in** (#173, #95).
  `rememberMicrosoftAuthState(requestScopes, customParameters,
  linkAccount, onResult)` — Firebase drives the OAuth web flow, no Microsoft
  SDK involved. Restrict to one Azure AD tenant via
  `customParameters = mapOf("tenant" to "...")`.
- **Anonymous (guest) sign-in**.
  `rememberAnonymousAuthState(onResult)` creates or resumes a
  temporary account; upgrade it later by signing in with any auth state
  using `linkAccount = true`, which keeps the anonymous uid and its data.
- **Reauthentication** (#167). Firebase requires a recent sign-in before
  security-sensitive operations (account deletion, password change). The
  provider-agnostic `KMPAuth.reauthenticate(credential)` accepts
  any credential — `AuthCredential.EmailPassword`, or a fresh
  `AuthCredential.IdToken` from rerunning the Google/Apple/Facebook flow.
  `AuthCredential` gains the `EmailPassword` variant, and the Firebase
  backend can now also exchange Apple `IdToken` (idToken + rawNonce) and
  Facebook Limited-Login OIDC credentials directly.

  All of these are served by the registered backend and report failures as
  `Result` values. On Desktop (JVM) the underlying Firebase SDK does not
  implement auth yet (#204), so they return failed `Result`s there.

## [3.0.0-alpha04] — 2026-07-19

### Added
- **`requestAccessToken` for Google sign-in** (#90, #129). Android's Credential
  Manager returns an ID token only; an access token requires a separate
  authorization request with its own consent prompt. That was previously
  triggered implicitly by passing scopes other than `email`/`profile`, which was
  undocumented and — because the check compared the scope *list* — meant
  reordering the same scopes changed the result. There is now an explicit flag:

  ```kotlin
  val googleSignIn = rememberGoogleSignInState(
      requestAccessToken = true,
      onResult = { result -> result.getOrNull()?.accessToken },
  )
  ```

  Requesting scopes beyond `email`/`profile` still implies it, and that check now
  compares sets, so ordering no longer matters. The flag has no effect on iOS,
  desktop, JS and wasm, which always return an access token.
- `GoogleUser` is now documented, including the per-platform availability of
  `accessToken` and `serverAuthCode` — the legacy Android fallback never returns
  an access token, which is why it appeared to be missing (#129).

### Fixed
- **Compose resources are packaged again on Android.** `kmpauth-uihelper`'s icons
  and font never reached consuming apps, so every sign-in button crashed at
  runtime with
  `MissingResourceException: Missing resource with path: composeResources/…`.
  AGP 9's KMP library plugin disables Android resources by default, and Compose
  Multiplatform resources ship as Android assets, so the modules generated their
  `composeResources` but published none of them. The convention plugin now sets
  `androidResources { enable = true }`.
- **Apple sign-in button logo is the size Apple specifies** (#169). Apple's
  official logo asset is exported on a 56×56 artboard whose content box is the
  inner 44×44, but the button rendered the whole artboard — so the export margin
  ate into the button and the glyph came out at 34% of the button height instead
  of the ~43% Apple's button spec requires. The artwork is unchanged; only the
  export margin is removed, so the content box now maps to the button.

## [3.0.0-alpha03] — 2026-07-19

### Changed
- **Google sign-in now reports why it failed** (#102, #103, #67). Every failure
  path used to return `null` and log the reason, so apps could not tell a
  cancelled sign-in from a misconfigured client, a missing credential or a
  token parsing error — and neither could their users' bug reports.
  `GoogleAuthUiProvider.signIn(...)` and `rememberGoogleSignInState`'s
  `onResult` now use `Result<GoogleUser>`, carrying the underlying exception
  (`GetCredentialException`, `NoCredentialException`, `ApiException`,
  `GoogleIdTokenParsingException`, …). This also makes Google consistent with
  `rememberFacebookSignInState` and `rememberAppleSignInState`, which already
  used `Result<T>`.

  ```kotlin
  val googleSignIn = rememberGoogleSignInState(onResult = { result ->
      result.onSuccess { user -> /* user.idToken */ }
            .onFailure { error -> /* show or report the reason */ }
  })
  ```

  The deprecated `GoogleButtonUiContainer` keeps its 2.x
  `(GoogleUser?) -> Unit` callback and is unaffected;
  `rememberFirebaseGoogleSignInState` is unchanged but now propagates the real
  Google failure instead of a generic "id token is null".

### Fixed
- **Google Sign-In now works in minified release builds** (#144). `kmpauth-google`
  ships consumer R8/ProGuard rules, so apps no longer need to add keep rules of
  their own. Credential Manager resolves its Play services provider
  reflectively; R8 stripped it in release builds, which is why sign-in worked in
  debug and silently did nothing once minified. Any module can now ship rules by
  placing a `consumer-rules.pro` next to its build file — the convention plugin
  publishes it with the artifact.
- **Google sign-in composables no longer crash IDE previews** (#162).
  `rememberGoogleSignInState` and `rememberFirebaseGoogleSignInState` resolved
  the provider during composition via `GoogleAuthProvider.get()`, which throws
  `IllegalArgumentException` when `create()` has not been called — and a
  `@Preview` never runs application startup. Both now return an inert
  `SignInState` when `LocalInspectionMode` is true, so previews render and
  `launch()` is a no-op. The deprecated `GoogleButtonUiContainer` /
  `GoogleButtonUiContainerFirebase` are thin wrappers over these states, so
  they are fixed too.

## [3.0.0-alpha02] — 2026-07-19

### Fixed
- **KMPAuth no longer depends on Ktor at all.** Building on the client removal
  below, the desktop Google OAuth loopback moved from `ktor-server-netty` to the
  JDK's built-in `com.sun.net.httpserver`, so **Ktor and Netty are gone from
  desktop/JVM consumers too** — the same version-clash risk that hit Android in
  #78 applied to desktop apps using their own Ktor. Behavior is unchanged
  (same routes, same fixed-port binding and error handling).
  **Desktop packaging note:** `com.sun.net.httpserver` lives in the
  `jdk.httpserver` JDK module; apps packaged with jpackage/jlink must declare
  `modules("jdk.httpserver")` or sign-in fails at runtime.
- **KMPAuth no longer drags a Ktor client onto consumers** (#78). `kmpauth-core`
  depended on the full Ktor client stack — `ktor-client-core`,
  `ktor-client-content-negotiation`, `ktor-client-logging`,
  `ktor-serialization-kotlinx-json` and the OkHttp/Darwin/JS engines — to build
  an `HttpClient` that nothing in the library ever used. Apps on a different
  Ktor version were pushed onto KMPAuth's, producing crashes such as
  `NoClassDefFoundError: io/ktor/client/plugins/contentnegotiation/ContentNegotiation`.
  The dead `HttpClientFactory`/`ServiceLocator` and every Ktor client
  dependency are removed, so **KMPAuth now contributes no Ktor to Android, iOS,
  JS or wasm consumers at all**; Ktor remains only as `ktor-server-*` inside
  `kmpauth-google`'s JVM source set, which powers the desktop OAuth loopback.

### Changed
- Dependency updates: Gradle 9.4.1 → **9.6.1**, Android Gradle Plugin 9.2.0 →
  **9.2.1** (9.3.0 was tried but needs a newer Android Studio than the project
  targets), `play-services-auth` 21.4.0 → **21.6.0**,
  `google-services` 4.4.4 → **4.5.0**, `java-jwt` 4.5.2 → **4.6.0**. Kotlin,
  Compose Multiplatform, Ktor, GitLive Firebase, Firebase BoM, the Facebook SDK,
  `androidx.credentials` and `googleid` were already on their latest stable
  releases.

### Added
- **`kmpauth-apple`: native Sign in with Apple without Firebase** (#60). New
  `rememberAppleSignInState(...)` returns an `AppleUser` carrying Apple's
  identity token (JWT), the raw nonce, and — on the user's first authorization
  only — `email`/`fullName`. The token is verifiable by any backend against
  Apple's public keys, so no client secret is needed on the client.
  **Apple platforms only:** on Android, JVM, JS and wasmJs the state is a
  logged no-op, because Apple's web OAuth flow returns an authorization code
  that must be exchanged with a client secret server-side; use
  `rememberFirebaseAppleSignInState` there. `kmpauth-firebase-core` now
  delegates its iOS Apple authorization to this module instead of carrying its
  own copy of the `ASAuthorization` flow.
  **Breaking:** `AppleSignInRequestScope` moved from
  `com.mmk.kmpauth.firebase.apple` to `com.mmk.kmpauth.apple` so both flows
  share one type (matching how `kmpauth-firebase-facebook` reuses
  `FacebookSignInRequestScope` from `kmpauth-facebook`). Update the import;
  nothing else changes. See MIGRATION section 9.

### Changed
- **Facebook on Android: classic login no longer requires `onActivityResult`.**
  With `FacebookLoginTracking.Enabled`, KMPAuth now launches Facebook Login
  through the SDK's `logInWithReadPermissions(ActivityResultRegistryOwner,
  CallbackManager, permissions)` overload (the pattern used by Facebook's own
  Compose sample), so apps no longer need to override `onActivityResult` or
  call `KMPAuth.handleFacebookActivityResult` for that mode.
  `FacebookLoginTracking.Limited` (the default) still needs it: Limited Login
  requires a nonce, which the Facebook SDK only accepts via
  `LoginConfiguration`, and that API has no `ActivityResultRegistryOwner`
  overload — the SDK exposes one only as a `private` function. Calling
  `handleFacebookActivityResult` when it is not needed remains harmless (#199).

## [3.0.0-alpha01] — 2026-07-08

See [MIGRATION.md](MIGRATION.md) for the step-by-step 2.x → 3.0 upgrade guide.

### Added
- **`SignInState` API.** New `rememberXxxSignInState(...)` composables
  (`rememberGoogleSignInState`, `rememberFacebookSignInState`,
  `rememberFirebaseGoogleSignInState`, `rememberFirebaseAppleSignInState`,
  `rememberFirebaseGithubSignInState`, `rememberFirebaseOAuthSignInState`,
  `rememberFirebaseFacebookSignInState`) return a `SignInState` handle with
  `launch()` and an observable `isInProgress`. Wire `launch()` to any
  clickable; parameters such as `linkAccount` are read at launch time, so
  toggling them via recomposition works; double-taps cannot start two flows.
- **Granular Firebase artifacts.** `kmpauth-firebase` is split into
  `kmpauth-firebase-core` (FirebaseAuthBackend + Apple/GitHub/OAuth
  containers) and `kmpauth-firebase-google` (Google + Firebase container).
  The `kmpauth-firebase` artifact remains published as an aggregator of
  both (its module lives under `deprecated/` in the repo), so existing
  dependency declarations keep working; depend on the granular artifacts
  to avoid pulling the Google Sign-In stack into apps that don't use it.
- **wasmJs on every module.** `kmpauth-firebase` and
  `kmpauth-firebase-facebook` now compile for wasmJs too, so a wasm-targeting
  app can keep them in `commonMain` dependencies. Their Firebase API surface
  exists on android/ios/jvm/js only (GitLive has no wasm target); on wasm the
  modules resolve as empty artifacts.
- **Pluggable auth backends.** New `com.mmk.kmpauth.core.auth` API in
  `kmpauth-core`: `AuthProviderBackend` (sign-in/sign-out/current-user over
  a backend-agnostic `AuthCredential` + `KMPAuthUser` model) and the
  `KMPAuthBackend` registry. Firebase remains the default implementation
  (registered automatically by `kmpauth-firebase`); a Supabase backend can
  plug in via `KMPAuthBackend.register(...)` without any changes to UI code.
- Characterization test suite locking the public 2.x behavior (DI lifecycle,
  `GoogleAuthProvider` entry points, sign-in overload defaults, public model shapes).
- **`FacebookLoginTracking` option.** `rememberFacebookSignInState`,
  `rememberFirebaseFacebookSignInState` and the Facebook containers accept a
  `loginTracking` parameter (default `Limited`). `Limited` uses Facebook's
  privacy-friendly Limited Login (OIDC JWT + nonce; no App Tracking
  Transparency prompt on iOS); `Enabled` uses classic login and returns a
  Graph-API access token. The mode now determines the token type consistently
  on **both** Android and iOS, and the Firebase exchange picks the matching
  credential (`OAuthProvider` for Limited, `FacebookAuthProvider` for Enabled)
  (#170).

### Changed
- **Facebook iOS/Android token consistency.** `FacebookUser.accessToken` now
  holds the same token type on both platforms for a given `loginTracking`
  mode. Because the new default is `FacebookLoginTracking.Limited`, **Android
  now returns an OIDC JWT + nonce by default instead of a Graph-API access
  token** (iOS was already Limited). Apps that send `FacebookUser.accessToken`
  to a backend expecting a Graph-API access token must pass
  `loginTracking = FacebookLoginTracking.Enabled` (#170).
- Sample restructured into `sampleApp/shared` plus per-platform entry
  modules (`androidApp`, `desktopApp`, `webApp`, `iosApp`) per the AGP 9
  layout; the iOS framework is now named `shared`. A web sample (js) is new.
- Build toolchain: Android Gradle Plugin 9.2 (`com.android.kotlin.multiplatform.library`),
  Gradle 9.4.1. **JVM target raised from 1.8 to 17** for the android and jvm
  artifacts — consumers need a Java 17+ runtime for desktop/JVM apps.
- Android artifacts are now published as a **single variant**; the separate
  `debug` variant is no longer published. Debug builds resolve the release
  variant automatically.
- Android host (unit) test task is now `testAndroid` (was
  `testDebugUnitTest`/`testReleaseUnitTest`).
- Kotlin 2.2.21 → **2.4.0**; Compose Multiplatform plugin 1.9.3 → 1.11.1; Ktor 3.5.1,
  kotlinx-serialization 1.11.0, kotlinx-coroutines 1.11.0,
  Facebook Android SDK 18.3.0, androidx.credentials 1.6.0, googleid 1.2.0,
  Dokka 2.2.0, vanniktech maven-publish 0.37.0, Firebase Android BoM 34.15.0.
- Android `compileSdk` raised to 37 (required by androidx.core 1.19);
  `targetSdk`/`minSdk` unchanged (36/24).
- **iOS dependencies now integrate via Swift Package Manager instead of
  CocoaPods.** The `kmpauth_*.podspec` files are no longer published; add
  GoogleSignIn-iOS / firebase-ios-sdk / facebook-ios-sdk as SPM packages in
  your Xcode project instead (see MIGRATION.md). iOS minimum deployment
  target raised from 12.0 to **16.0**; building the library requires
  Xcode 16.4+.

### Deprecated
- All `*UiContainer` composables (`GoogleButtonUiContainer`,
  `GoogleButtonUiContainerFirebase`, `AppleButtonUiContainer`,
  `GithubButtonUiContainer`, `OAuthContainer`, `FacebookButtonUiContainer`,
  `FacebookButtonUiContainerFirebase`) in favor of the `SignInState` API.
  They keep working as thin wrappers over the new implementation and are
  slated for removal in 4.0.
- The parameter-less legacy overloads of `GoogleButtonUiContainerFirebase`,
  `AppleButtonUiContainer`, `GithubButtonUiContainer` and `OAuthContainer`
  (deprecated since 2.x in favor of the `linkAccount`/
  `filterByAuthorizedAccounts` overloads) remain deprecated with warnings
  and are slated for removal in 4.0.

### Fixed
- **Desktop (JVM) Google Sign-In redirect URI.** The loopback server used a
  random port that never matched the `redirect_uri` sent to Google, so the
  callback failed (`redirect_uri_mismatch`, blank callback page, or
  `id_token=null` on a second attempt). The redirect is now fixed and
  configurable via `GoogleAuthCredentials(serverId, redirectUri = "http://localhost:8080/callback")` —
  pass the exact **Authorized redirect URI** registered for your OAuth client
  in the Google Cloud console (any `http` loopback host/port/path;
  defaults to `http://localhost:8080/callback`). The callback server binds the
  URI's port and serves its path; when the port is already in use the failure
  is logged clearly instead of silently falling back to a random port. The
  browser now opens only after the server is listening (#172, #177, #181).

### Removed
- **Koin.** KMPAuth no longer uses or ships Koin. The `@KMPAuthInternalApi`
  DI types `KMPKoinComponent` and `LibDependencyInitializer` are deleted and
  the `io.insert-koin:koin-core` dependency is gone from every module.
  Public entry points (`GoogleAuthProvider.create`, all `*UiContainer`
  composables) are unchanged — internal wiring is now plain constructor
  injection. Only code that referenced those two internal-API types directly
  is affected (see MIGRATION.md).
- **`iosX64` target.** Dropped from all modules (Compose Multiplatform 1.11+
  no longer ships iosX64 artifacts; Intel-Mac simulators can use Rosetta
  with the arm64 simulator target). It was briefly re-added in 2.5.0-alpha01.
- Stale `api/android/*.api` compatibility dumps (no longer generated or
  validated under AGP 9's KMP library plugin; the klib and JVM dumps remain
  the source of truth).

## [2.5.0-alpha01] — 2026-06

### Added
- Dedicated Facebook auth modules: `kmpauth-facebook` (SDK-only) and
  `kmpauth-firebase-facebook` (Facebook + Firebase) (#163).
- `iosX64` target restored (#164).

### Fixed
- Google JVM sign-in uses a dynamically found port for the OAuth redirect URI (#165).

## [2.4.0] — 2025

### Added
- `setAutoSelectEnabled(isAutoSelectEnabled)` on Google sign-in (#126).
- Non-legacy Google sign-in with custom scopes on Android (#132).

### Fixed
- Icon and text alignment in `FacebookSignInButton` (#142).
- Compatibility with Firebase Android BoM 34.0.0 (#141).

_Older releases: see [GitHub releases](https://github.com/mirzemehdi/KMPAuth/releases)._
