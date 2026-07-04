# CLAUDE.md

Guidance for AI agents and contributors working in this repository.

## What this is

KMPAuth — a Kotlin Multiplatform authentication library (Google, Apple, GitHub, Facebook sign-in, with optional Firebase integration). Published to Maven Central as `io.github.mirzemehdi:kmpauth-*`. Version lives in `gradle.properties` (`kmpAuthVersion`).

## Module map

| Module | Purpose | Depends on |
|---|---|---|
| `kmpauth-core` | Base infrastructure: logging (`KMPAuth.setLogger`), `UiContainerScope`, HTTP client factory, DI plumbing | — |
| `kmpauth-google` | Google Sign-In (Credential Manager on Android, GoogleSignIn SDK on iOS, OAuth loopback on JVM) | core |
| `kmpauth-facebook` | Facebook Login via Facebook SDK (no Firebase) | core |
| `kmpauth-firebase` | Firebase auth flows: `GoogleButtonUiContainerFirebase`, `AppleButtonUiContainer`, `GithubButtonUiContainer`, `OAuthContainer` (GitLive firebase-auth) | core, google |
| `kmpauth-firebase-facebook` | Facebook + Firebase combo container | firebase, facebook |
| `kmpauth-uihelper` | Pre-styled Compose sign-in buttons (Google/Apple/Facebook) | core, firebase |
| `sampleApp/composeApp` + `sampleApp/iosApp` | Demo app, all targets | all |

Targets: android, iosX64/iosArm64/iosSimulatorArm64, jvm, js(IR), wasmJs — except firebase modules (no js/wasm; GitLive limitation).

## Architecture notes

- Public UI entry points are `@Composable ...UiContainer` functions wrapping a caller-supplied button; caller triggers flow via `UiContainerScope.onClick()`.
- `expect`/`actual` per platform; composable expect/actuals for containers with platform SDK interop.
- iOS platform SDKs (GoogleSignIn, FBSDKLoginKit, FirebaseAuth) consumed via cinterop — import namespace is migration-sensitive (CocoaPods → SPM changes `cocoapods.X.*` imports to `swiftPMImport.*`).
- Android context acquired via androidx.startup `KMPAuthContextInitializer` (kmpauth-core androidMain) — no manual init required by consumers.
- `GoogleAuthProvider.create(credentials)` must be called once at app start before `GoogleAuthProvider.get()`; sample wiring in `sampleApp/composeApp/.../AppInitializer.kt`.

## Conventions

- **Binary compatibility validator is enforced** (JVM/android `.api` + `.klib.api` dumps per module under `<module>/api/`). Any public API change requires `./gradlew apiDump` and the diff reviewed — CI runs `apiCheck`. Never hand-edit dumps except deliberate, approved removals.
- Deprecation style: `@Deprecated(message = "...clear migration hint...", replaceWith = ReplaceWith(...))`, keep old path functional. See old overloads in `AppleButtonUiContainer`, `OAuthContainer`.
- Explicit API mode on library modules; public API needs KDoc.
- Version catalog: `gradle/libs.versions.toml`. Single source of truth for all versions.
- User-facing changes → `CHANGELOG.md`; migration steps → `MIGRATION.md`.
- Commits explain the "why", not just the "what".

## Build & test commands

```bash
./gradlew build                 # full build all targets
./gradlew apiCheck              # binary-compat check (CI gate)
./gradlew apiDump               # regenerate api dumps after public API change
./gradlew jvmTest               # JVM unit tests
./gradlew testDebugUnitTest testReleaseUnitTest   # Android unit tests
./gradlew iosSimulatorArm64Test # iOS simulator tests (macOS only)
./gradlew publishToMavenLocal   # local artifact smoke test
```

Sample app: `./gradlew :sampleApp:composeApp:run` (desktop), Android via IDE, iOS via `sampleApp/iosApp` Xcode project.

## CI / release

`.github/workflows/build_and_publish.yml`: apiCheck → Android + iOS tests → on `v*` tag: Dokka docs to GitHub Pages + `publishAndReleaseToMavenCentral` (vanniktech plugin, GPG-signed) + GitHub release.

Release flow: bump `kmpAuthVersion` in `gradle.properties`, merge to `main`, tag `vX.Y.Z`.

## 3.0.0 effort (in progress)

Integration branch: `rel_3.0.0`. All feature PRs target it, not `main`. Plan: characterization tests → build-logic extraction → AGP 9 / Kotlin 2.4 / CocoaPods→SPM → Koin removal (approved ABI break for DI types) → pluggable `AuthProviderBackend` abstraction (Firebase default, Supabase later) → CHANGELOG/MIGRATION/README.
