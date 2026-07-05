# CLAUDE.md

Guidance for AI agents and contributors working in this repository.

## What this is

KMPAuth — a Kotlin Multiplatform authentication library (Google, Apple, GitHub, Facebook sign-in, with optional Firebase integration and a pluggable backend abstraction). Published to Maven Central as `io.github.mirzemehdi:kmpauth-*`. Version lives in `gradle.properties` (`kmpAuthVersion`).

## Module map

Layout: identity **providers** (credential sources) live under `providers/`; session **backends** under `backends/` (Firebase today, Supabase planned under `backends/supabase/`). Directory grouping only — artifact ids are the project names. Legacy compatibility shims live under `deprecated/`.

| Module (gradle path) | Purpose | Depends on |
|---|---|---|
| `:kmpauth-core` | Base infrastructure: logging, `SignInState`/`LaunchingSignInState`, HTTP client factory, `com.mmk.kmpauth.core.auth` backend abstraction (`AuthProviderBackend`, `KMPAuthBackend`, `AuthCredential`, `KMPAuthUser`) | — |
| `:providers:kmpauth-google` | Google Sign-In (Credential Manager on Android, GoogleSignIn SDK on iOS, OAuth loopback on JVM) | core |
| `:providers:kmpauth-facebook` | Facebook Login via Facebook SDK (no Firebase) | core |
| `:backends:firebase:kmpauth-firebase-core` | `FirebaseAuthBackend` (default backend) + Apple/GitHub/OAuth `rememberXxxSignInState` flows (GitLive firebase-auth) | core |
| `:backends:firebase:kmpauth-firebase-google` | `rememberFirebaseGoogleSignInState` + sign-in handler | firebase-core, google |
| `:backends:firebase:kmpauth-firebase-facebook` | Facebook + Firebase combo container | firebase-core, facebook |
| `:deprecated:kmpauth-firebase` | Backward-compat **aggregator** artifact: `api(firebase-core, firebase-google)` — keeps 2.x dependency blocks working | firebase-core, firebase-google |
| `:kmpauth-uihelper` | Pre-styled Compose sign-in buttons (Google/Apple/Facebook) | core |
| `sampleApp/shared` + `androidApp`/`desktopApp`/`webApp`/`iosApp` | Demo: shared UI module + per-platform entry points (webApp targets js — Firebase API is empty on wasm) | all |

Targets: android, iosArm64/iosSimulatorArm64, jvm, js(IR), wasmJs — declared by the convention plugin for every module. Firebase modules expose their API from a `nonWasmMain` intermediate source set (GitLive has no wasm target); their wasm variant is an intentionally empty klib so wasm consumers can still depend on them from commonMain. No iosX64 (dropped in 3.0; Compose Multiplatform 1.11+ does not ship it).

## Build conventions

- `build-logic/` included build hosts the `kmpauth.kmp.library` convention plugin: applies KMP + `com.android.kotlin.multiplatform.library` (AGP 9) + vanniktech publishing, target set, explicit API, JVM 17, namespace derived from module name, shared kotlin-test dep, common POM. Modules keep only: iOS framework name, `swiftPMDependencies {}`, dependencies (and firebase modules their `nonWasmMain` wiring — custom dependsOn edges require re-applying `applyDefaultHierarchyTemplate()`).
- **iOS dependencies via SwiftPM** (`swiftPMDependencies {}` DSL, Kotlin 2.4+): GoogleSignIn-iOS (google), facebook-ios-sdk products `FacebookCore`/`FacebookLogin` (facebook), firebase-ios-sdk pinned to GitLive's build version (firebase). No CocoaPods anywhere. `cocoapods.FirebaseAuth.*` imports in kmpauth-firebase iosMain are GitLive's **bundled cinterop** — never rewrite them.
- **No Koin.** Manual constructor injection: internal `ServiceLocator` (core), `internal expect fun createGoogleAuthProvider(...)` (google), androidx.startup `KMPAuthContextInitializer` captures the Android context. SPM cinterop import namespaces derive from the full gradle path (e.g. `swiftPMImport.io.github.mirzemehdi.providers.kmpauth.google.*`) — moving a module changes its imports.
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

Sample apps: `./gradlew :sampleApp:desktopApp:run` (desktop), `:sampleApp:androidApp:assembleDebug` (Android), `:sampleApp:webApp:jsBrowserDevelopmentRun` (web), iOS via `sampleApp/iosApp/iosApp.xcodeproj` (framework `shared`; the embedAndSign build phase drives Gradle).

## CI / release

`.github/workflows/build_and_publish.yml`: apiCheck → `testAndroid jvmTest` + androidApp APK (ubuntu) and `iosSimulatorArm64Test` (macOS) → on `v*` tag: Dokka docs to GitHub Pages + `publishAndReleaseToMavenCentral` (vanniktech, GPG-signed) + GitHub release. PR builds trigger for PRs targeting `main` and `rel_3.0.0`.

Release flow: bump `kmpAuthVersion` in `gradle.properties`, merge to `main`, tag `vX.Y.Z`.

## Sign-in API (3.0)

Primary API: `rememberXxxSignInState(...)` composables returning
`com.mmk.kmpauth.core.SignInState` (`launch()`, observable `isInProgress`,
double-launch guard via internal `LaunchingSignInState`). All parameters are
read at launch time through `rememberUpdatedState`. The 2.x `*UiContainer`
composables are deprecated thin wrappers over these states — keep them
delegating, never reimplement logic in them.

## Future consideration (when Supabase lands)

A backend-agnostic `rememberSignInState(...)` returning `KMPAuthUser` — one
generic entry point over the `AuthProviderBackend` abstraction, likely a
sealed "contract" payload per provider (mirroring
`rememberLauncherForActivityResult(contract)`). Deliberately NOT done at the
Firebase layer: providers have disjoint parameter shapes, and a unified
function would need visibility of all provider modules, undoing the granular
`kmpauth-firebase-*` split. Revisit when `backends/supabase/` is added.

## Auth backend architecture (3.0)

`KMPAuthBackend` registry holds one `AuthProviderBackend`. `kmpauth-firebase` self-registers `FirebaseAuthBackend` lazily on first container use; an app-supplied backend (e.g. future Supabase) registered at startup always wins (first registration wins; `replace = true` to swap). Token credentials (`AuthCredential.IdToken`) flow through the backend; web-flow providers (Apple-on-Android, GitHub, generic OAuth) are driven by their platform container composables.
