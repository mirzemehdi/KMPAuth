# CLAUDE.md

Guidance for AI agents and contributors working in this repository.

## What this is

KMPAuth — a Kotlin Multiplatform authentication library (Google, Apple, GitHub, Facebook, Microsoft, email/password, phone and anonymous sign-in, with optional Firebase integration and a pluggable backend abstraction). Published to Maven Central as `io.github.mirzemehdi:kmpauth-*`. Version lives in `gradle.properties` (`kmpAuthVersion`).

## Module map

Layout: identity **providers** (credential sources) live under `providers/`; session **backends** under `backends/` (Firebase today, Supabase planned under `backends/supabase/`). Directory grouping only — artifact ids are the project names. Legacy compatibility shims live under `deprecated/`.

| Module (gradle path) | Purpose | Depends on |
|---|---|---|
| `:kmpauth-core` | Base infrastructure: logging, `SignInState`/`LaunchingSignInState`/`UnsupportedSignInState`, `runCatchingCancellable`, the `KMPAuth` client facade, and the `com.mmk.kmpauth.core.auth` backend abstraction (`AuthProviderBackend`, `KMPAuthBackend`, `AuthCredential`, `KMPAuthUser`, `EmailActionCodeSettings`) | — |
| `:providers:kmpauth-google` | Google Sign-In (Credential Manager on Android, GoogleSignIn SDK on iOS, OAuth loopback on JVM) | core |
| `:providers:kmpauth-facebook` | Facebook Login via Facebook SDK (no Firebase) | core |
| `:providers:kmpauth-apple` | Native Sign in with Apple via AuthenticationServices (no Firebase). iOS-only flow — other targets are no-op stubs, since Apple's web flow needs a server-side client-secret exchange. Also supplies the iOS credential acquisition reused by firebase-core (`performAppleSignIn`, `@KMPAuthInternalApi`) | core |
| `:backends:firebase:kmpauth-firebase-core` | `FirebaseAuthBackend` (default backend) + Apple/GitHub/Microsoft/OAuth/email/phone/anonymous `rememberXxxSignInState` flows (GitLive firebase-auth). Its iOS Apple flow delegates to `:providers:kmpauth-apple` for the native authorization | core, apple |
| `:backends:firebase:kmpauth-firebase-google` | `rememberFirebaseGoogleSignInState` + sign-in handler | firebase-core, google |
| `:backends:firebase:kmpauth-firebase-facebook` | Facebook + Firebase combo container | firebase-core, facebook |
| `:deprecated:kmpauth-firebase` | Backward-compat **aggregator** artifact: `api(firebase-core, firebase-google)` — keeps 2.x dependency blocks working | firebase-core, firebase-google |
| `:kmpauth-uihelper` | Pre-styled Compose sign-in buttons (Google/Apple/Facebook) | core |
| `sampleApp/shared` + `androidApp`/`desktopApp`/`webApp`/`iosApp` | Demo: shared UI module + per-platform entry points (webApp builds both js and wasm variants — the wasm one is the smoke test for the wasm-callable Firebase API) | all |

Targets: android, iosArm64/iosSimulatorArm64, jvm, js(IR), wasmJs — declared by the convention plugin for every module. Firebase modules declare their public API in `commonMain` using KMPAuth's own types (`KMPAuthUser`, never GitLive types — GitLive has no wasm target), so consumers call everything from commonMain on every target including wasm. GitLive-facing implementations and the deprecated 2.x containers (which expose `FirebaseUser`) live in a `nonWasmMain` intermediate source set; wasm actuals report failed `Result`s with a clear reason. No iosX64 (dropped in 3.0; Compose Multiplatform 1.11+ does not ship it).

## Build conventions

- `build-logic/` included build hosts the `kmpauth.kmp.library` convention plugin: applies KMP + `com.android.kotlin.multiplatform.library` (AGP 9) + vanniktech publishing, target set, explicit API, JVM 17, namespace derived from module name, shared kotlin-test dep, common POM. Modules keep only: iOS framework name, `swiftPMDependencies {}`, dependencies (and firebase modules their `nonWasmMain` wiring — custom dependsOn edges require re-applying `applyDefaultHierarchyTemplate()`).
- **iOS dependencies via SwiftPM** (`swiftPMDependencies {}` DSL, Kotlin 2.4+): GoogleSignIn-iOS (google), facebook-ios-sdk products `FacebookCore`/`FacebookLogin` (facebook), firebase-ios-sdk pinned to GitLive's build version (firebase). No CocoaPods anywhere. `cocoapods.FirebaseAuth.*` imports in kmpauth-firebase-core iosMain are GitLive's **bundled cinterop** — never rewrite them.
- **No Koin.** Manual constructor injection: `internal expect fun createGoogleAuthProvider(...)` (google), androidx.startup `KMPAuthContextInitializer` captures the Android context.
- **No Ktor at all.** Shipping a Ktor client forced a Ktor version on consumers and broke their own setup (#78); the desktop OAuth loopback then also pulled `ktor-server` + Netty onto desktop classpaths. Both are gone: the loopback runs on the JDK's `com.sun.net.httpserver` (`kmpauth-google` jvmMain). Don't reintroduce an HTTP dependency without a real consumer — and note the JDK server needs the `jdk.httpserver` module declared for jlink-packaged apps. SPM cinterop import namespaces derive from the full gradle path (e.g. `swiftPMImport.io.github.mirzemehdi.providers.kmpauth.google.*`) — moving a module changes its imports.
- **Binary compatibility validator enforced** (JVM `.api` + `.klib.api` dumps under `<module>/api/`; android-specific dumps are not generated under AGP 9). Any public API change requires `./gradlew apiDump` with the diff reviewed — CI runs `apiCheck`. Never hand-edit dumps except deliberate, approved removals.
- Deprecation style: `@Deprecated(message = "...migration hint...", replaceWith = ReplaceWith(...))`, old path stays functional; removal one major version later.
- Explicit API mode on library modules; public API needs KDoc.
- Version catalog `gradle/libs.versions.toml` is the single source of truth. JDK 17 required to build.
- User-facing changes → `CHANGELOG.md`; migrations → `MIGRATION.md`.
- Commits explain the "why", not just the "what".

## Testing

- commonTest runs on jvm, android (`testAndroidHostTest`), iOS simulator, js, wasm.
- **Constraint:** any commonTest source in `kmpauth-firebase` forces an iOS test binary link that currently fails (Firebase framework closure). Firebase tests live in `src/jvmTest` instead.
- Characterization tests lock the 2.x public contracts (exact error messages, overload defaults, initialization semantics) — keep them green through refactors.

## Build & test commands

```bash
./gradlew build                 # full build all targets
./gradlew apiCheck              # binary-compat check (CI gate)
./gradlew apiDump               # regenerate api dumps after public API change
./gradlew jvmTest               # JVM unit tests
./gradlew testAndroid           # Android host (unit) tests — AGP 9 task name
./gradlew iosSimulatorArm64Test # iOS simulator tests (macOS only)
./gradlew publishToMavenLocal   # local artifact smoke test
```

Sample apps: `./gradlew :sampleApp:desktopApp:run` (desktop), `:sampleApp:androidApp:assembleDebug` (Android), `:sampleApp:webApp:jsBrowserDevelopmentRun` (web js, port 8080), `:sampleApp:webApp:wasmJsBrowserDevelopmentRun` (web wasm, port 8081), iOS via `sampleApp/iosApp/iosApp.xcodeproj` (framework `shared`; the embedAndSign build phase drives Gradle).

## CI / release

`.github/workflows/build_and_publish.yml`: apiCheck → `testAndroid jvmTest` + androidApp APK (ubuntu) and `iosSimulatorArm64Test` (macOS) → on `v*` tag: Dokka docs to GitHub Pages + `publishAndReleaseToMavenCentral` (vanniktech, GPG-signed) + GitHub release. PR builds trigger only for PRs targeting `main`. CI does **not** run `./gradlew build`, so failures outside `apiCheck`/tests (e.g. a stale `kotlin-js-store/yarn.lock` after dependency bumps — fix with `kotlinUpgradeYarnLock`) surface only locally.

Release flow: bump `kmpAuthVersion` in `gradle.properties`, merge to `main`, tag `vX.Y.Z`.

## Sign-in API (3.0)

Primary API: `rememberXxxSignInState(...)` composables returning
`com.mmk.kmpauth.core.SignInState` (`launch()`, observable `isInProgress`,
double-launch guard via internal `LaunchingSignInState`). Phone sign-in
returns `PhoneSignInState` (extends `SignInState` with `isCodeSent`,
`submitCode(code)`, `cancel()`). All parameters are read at launch time
through `rememberUpdatedState`. The 2.x `*UiContainer` composables are
deprecated thin wrappers over these states — keep them delegating, never
reimplement logic in them.

Everything that isn't a launchable flow goes through the **`KMPAuth`
facade** (`kmpauth-core`): `currentUser()`, `signOut()`, `signIn(credential)`,
`reauthenticate(credential)`, `sendPasswordResetEmail`, email-link sign-in,
and `registerBackendProvider`/`getBackendProvider`/`requireBackendProvider`.
It delegates to `KMPAuthBackend`; keep new session/account operations on this
facade rather than inventing provider-specific top-level objects.

**Failures are values, not nulls.** Every provider's `onResult` takes a
`Result<T>` (`Result<GoogleUser>`, `Result<FacebookUser>`, `Result<AppleUser>`,
`Result<KMPAuthUser?>` for Firebase-backed flows); never swallow an exception
into a `null` — the reason is what makes a field report actionable
(#102, #103). Platforms where a flow cannot work report a failed `Result`
(`UnsupportedSignInState`), never a silent no-op. Only the deprecated 2.x
containers keep their old callbacks (`GoogleUser?`, `Result<FirebaseUser?>`
via `KMPAuthUser.raw` unwrap) for source compat.

Provider resolution must not happen during composition: guard on
`LocalInspectionMode` and return `NoOpSignInState`, or IDE previews crash
because `GoogleAuthProvider.create()` never ran (#162).

## Future consideration (when Supabase lands)

A backend-agnostic `rememberSignInState(...)` returning `KMPAuthUser` — one
generic entry point over the `AuthProviderBackend` abstraction, likely a
sealed "contract" payload per provider (mirroring
`rememberLauncherForActivityResult(contract)`). Deliberately NOT done at the
Firebase layer: providers have disjoint parameter shapes, and a unified
function would need visibility of all provider modules, undoing the granular
`kmpauth-firebase-*` split. Revisit when `backends/supabase/` is added.

## Auth backend architecture (3.0)

`KMPAuthBackend` registry holds one `AuthProviderBackend` — and implements the interface itself, delegating to the registered backend (no-backend calls return failed `Result`s with a how-to-register message). The client-facing surface is the `KMPAuth` facade on top of it. `kmpauth-firebase` self-registers `FirebaseAuthBackend` (an `expect object`; wasm actual fails everything) lazily on first sign-in-state use; an app-supplied backend (e.g. future Supabase) registered at startup always wins (first registration wins; `replace = true` to swap).

Credentials: `AuthCredential.IdToken` (Google/Facebook/Apple — Apple needs `rawNonce`) and `AuthCredential.EmailPassword` are exchanged directly by the backend (`signIn`, `reauthenticate`); `AuthCredential.OAuthWebFlow` cannot be — web-flow providers (Apple-on-Android, GitHub, Microsoft, generic OAuth) are driven by their platform sign-in states. Backend interface additions must ship default implementations (unsupported failure) so custom backends stay source-compatible.
