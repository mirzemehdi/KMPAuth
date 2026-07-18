# KMPAuth - Kotlin Multiplatform Authentication Library
[![Build](https://github.com/mirzemehdi/KMPAuth/actions/workflows/build_and_publish.yml/badge.svg)](https://github.com/mirzemehdi/KMPAuth/actions/workflows/build_and_publish.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.0-blue.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.mirzemehdi/kmpauth-google?color=blue)](https://search.maven.org/search?q=g:io.github.mirzemehdi+kmpauth)

![badge-android](http://img.shields.io/badge/platform-android-6EDB8D.svg?style=flat)
![badge-ios](http://img.shields.io/badge/platform-ios-AAAAFF.svg?style=flat)
![badge-web](http://img.shields.io/badge/platform-web-FFCC66.svg?style=flat)
![badge-desktop](http://img.shields.io/badge/platform-desktop-FF8E8E.svg?style=flat)


Simple and easy to use Kotlin Multiplatform Authentication library targeting iOS, Android, Desktop and Web. All modules resolve on every target; the Firebase modules expose their API on android/iOS/jvm/js (not wasm — the underlying Firebase SDK has no wasm support). Supporting **Google**, **Apple**, **Github**, **Facebook** authentication integrations using Firebase.   
Because I am using KMPAuth in [FindTravelNow](https://github.com/mirzemehdi/FindTravelNow-KMM/) production KMP project, I'll support development of this library :).   
Related blog post: [Integrating Google Sign-In into Kotlin Multiplatform](https://proandroiddev.com/integrating-google-sign-in-into-kotlin-multiplatform-8381c189a891)  
You can check out [Documentation](https://mirzemehdi.github.io/KMPAuth) for full library api information.

## Sample App and Code
<p style="text-align: center;">
  <img src="https://github.com/mirzemehdi/KMPAuth/assets/32781662/f5a3cd28-6ef2-46bf-9b07-a045ce217b34)" width="200" alt="SampleApp"/>  
</p>

```kotlin
@Composable
fun AuthUiHelperButtonsAndFirebaseAuth(
    modifier: Modifier = Modifier,
    onFirebaseResult: (Result<FirebaseUser?>) -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {

        //Google Sign-In Button and authentication with Firebase
        val googleSignIn = rememberFirebaseGoogleSignInState(onResult = onFirebaseResult)
        GoogleSignInButton(modifier = Modifier.fillMaxWidth()) { googleSignIn.launch() }

        //Apple Sign-In Button and authentication with Firebase
        val appleSignIn = rememberFirebaseAppleSignInState(onResult = onFirebaseResult)
        AppleSignInButton(modifier = Modifier.fillMaxWidth()) { appleSignIn.launch() }

        //Facebook Sign-In Button and authentication with Firebase
        val facebookSignIn = rememberFirebaseFacebookSignInState(onResult = onFirebaseResult)
        FacebookSignInButton(onClick = { facebookSignIn.launch() })

        //Github Sign-In with Custom Button and authentication with Firebase
        val githubSignIn = rememberFirebaseGithubSignInState(onResult = onFirebaseResult)
        Button(onClick = { githubSignIn.launch() }) { Text("Github Sign-In (Custom Design)") }

    }
}
```

Every `rememberXxxSignInState` returns a `SignInState` with `launch()` and an
observable `isInProgress` — wire it to any clickable and drive loading UI from
it. Parameters (e.g. `linkAccount`) are read at launch time, so toggling them
via recomposition just works. The 2.x `*UiContainer { this.onClick() }`
composables still work but are deprecated (removal planned for 4.0).

  


You can check out more [sample codes](https://github.com/mirzemehdi/KMPAuth/blob/main/sampleApp/shared/src/commonMain/kotlin/com/mmk/kmpauth/sample/App.kt) here.

## Features
- ✅ Google One Tap Sign-In (without Firebase)
- ✅ [Google Sign-In with Firebase](#google-sign-in)
- ✅ [Apple Sign-In with Firebase](#apple-sign-in), and native Apple Sign-In without Firebase on Apple platforms
- ✅ [Github Sign-In with Firebase](#github-sign-in)
- ✅ [Facebook Sign-In (android and ios) with Firebase](#facebook-sign-in)
- ✅ Apple, Google, Facebook "Sign in with " UiHelper buttons (according to each brand's guideline)
- 📱 Multiplatform (android, iOS, jvm and web (js,wasm))

## Installation
KMPAuth is available on Maven Central. In your root project `build.gradle.kts` file (or `settings.gradle` file) add `mavenCentral()` to repositories.

```kotlin
repositories { 
  mavenCentral()
}
```

Then in your shared module add desired dependencies in `commonMain`. Latest version: [![Maven Central](https://img.shields.io/maven-central/v/io.github.mirzemehdi/kmpauth-google?color=blue)](https://search.maven.org/search?q=g:io.github.mirzemehdi+kmpauth).
```kotlin
sourceSets {
  commonMain.dependencies {
    implementation("io.github.mirzemehdi:kmpauth-google:<version>") //Google Sign-In (no backend required)
    implementation("io.github.mirzemehdi:kmpauth-facebook:<version>") //Facebook Login (no backend required)
    implementation("io.github.mirzemehdi:kmpauth-apple:<version>") //Native Sign in with Apple, Apple platforms only (no backend required)

    // Firebase — pick granular artifacts (3.0+):
    implementation("io.github.mirzemehdi:kmpauth-firebase-core:<version>") //Firebase backend + Apple/Github/OAuth sign-in
    implementation("io.github.mirzemehdi:kmpauth-firebase-google:<version>") //Google Sign-In with Firebase
    implementation("io.github.mirzemehdi:kmpauth-firebase-facebook:<version>") //Facebook Sign-In with Firebase
    // ...or the 2.x-compatible bundle (aggregates -core and -google):
    implementation("io.github.mirzemehdi:kmpauth-firebase:<version>")

    implementation("io.github.mirzemehdi:kmpauth-uihelper:<version>") //UiHelper SignIn buttons (AppleSignIn, GoogleSignInButton)

  }
}
```
**_You will also need to add the native SDKs to your iOS app via Swift Package Manager_** (CocoaPods is no longer supported as of 3.0 — see [MIGRATION.md](MIGRATION.md)):
- Google Sign-In: `https://github.com/google/GoogleSignIn-iOS` (product `GoogleSignIn`)
- Firebase: `https://github.com/firebase/firebase-ios-sdk` (products `FirebaseAuth`, `FirebaseCore`)
- Facebook: `https://github.com/facebook/facebook-ios-sdk` (products `FacebookCore`, `FacebookLogin`)

#### Requirements (3.0+)

| | Minimum |
|---|---|
| iOS deployment target | 16.0 |
| Xcode | 16.4 |
| JVM runtime (desktop apps) | 17 |
| Android compileSdk | 37 |

Upgrading from 2.x? Follow the step-by-step [MIGRATION.md](MIGRATION.md). All notable changes live in [CHANGELOG.md](CHANGELOG.md).


**Note**: If in iOS you get `MissingResourceException`, I wrote solution in this [issue's comment section](https://github.com/mirzemehdi/KMPAuth/issues/2).

-----

### Google Sign-In
For Google Sign-In you can either use only one-tap sign in functionality, or also implementing firebase google authentication integration to that.
You need to set up OAuth 2.0 in Google Cloud Platform Console. 
For steps you can follow this [link](https://support.google.com/cloud/answer/6158849). **_Pro Easy Tip:_** If you use Firebase and enable Google Sign-In authentication in Firebase 
it will automatically generate OAuth client IDs for each platform, 
and one will be **_Web Client ID_** which will be needed for identifying signed-in users in backend server.

#### Platform Setup
Create GoogleAuthProvider instance by providing _**Web Client Id**_ as a serverID on Application start.
```kotlin
GoogleAuthProvider.create(credentials = GoogleAuthCredentials(serverId = WebClientId))

```
<details>
  <summary>Android</summary>

##### Android Setup
There is not any platform specific setup in Android side.

> **"Google Play services out of date" at sign-in?** Google Sign-In runs through
> Credential Manager, which needs a recent **Google Play services APK on the
> device** — this is independent of your app's `minSdk`/`targetSdk`. Update
> Google Play services on the device, and on an emulator use a system image
> that includes the **Google Play Store** (images labelled "Google APIs" only
> ship an older, non-updatable Play services).

</details>

<details>
  <summary>iOS</summary>

##### iOS Setup
Add clientID, and serverId to your `Info.plist` file as below:

```
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

And finally, you need the code below to implement application delegate function calls on the Swift side.

```swift
import SwiftUI
import shared
import GoogleSignIn

class AppDelegate: NSObject, UIApplicationDelegate {

    func application(
      _ app: UIApplication,
      open url: URL, options: [UIApplication.OpenURLOptionsKey : Any] = [:]
    ) -> Bool {
      var handled: Bool

      handled = GIDSignIn.sharedInstance.handle(url)
      if handled {
        return true
      }

      // Handle other custom URL types.

      // If not handled by this app, return false.
      return false
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
  <summary>Desktop (JVM)</summary>

##### Desktop Setup
On desktop, Google Sign-In runs the OAuth flow in the system browser and
receives the result on a localhost loopback server. Google requires the
redirect URI to be **pre-registered** with a **fixed port**, so:

1. In the Google Cloud console, add an **Authorized redirect URI** for your
   OAuth client — e.g. `http://localhost:8080/callback`.
2. Pass that exact URI as `redirectUri` when creating the credentials (it
   defaults to `http://localhost:8080/callback`):

```kotlin
GoogleAuthProvider.create(
    credentials = GoogleAuthCredentials(
        serverId = WebClientId,
        redirectUri = "http://localhost:8080/callback",
    )
)
```

Any `http` loopback host (`localhost` / `127.0.0.1`), port and path are
allowed as long as the same URI is registered in the console. The port must be
free when the user signs in; if it is already taken, sign-in fails with a
logged error — free it or register a different URI. `redirectUri` is ignored
on Android, iOS, JS and wasmJs.

</details>

#### Usage
After configuring above steps this is how you can use:

```kotlin
//Google Sign-In with Custom Button (only one tap sign-in functionality)
val googleSignIn = rememberGoogleSignInState(onResult = { googleUser ->
  val idToken = googleUser?.idToken // Send this idToken to your backend to verify
})
Button(onClick = { googleSignIn.launch() }) { Text("Google Sign-In(Custom Design)") }
```

Google Sign-In Button and authentication with Firebase. You need to implement `kmpauth-uihelper` dependency
```kotlin
val googleSignIn = rememberFirebaseGoogleSignInState(onResult = onFirebaseResult)
GoogleSignInButton(modifier = Modifier.fillMaxWidth()) { googleSignIn.launch() }
```

Google Sign-In IconOnly Button and authentication with Firebase. You need to implement `kmpauth-uihelper` dependency
```kotlin
val googleSignIn = rememberFirebaseGoogleSignInState(onResult = onFirebaseResult)
GoogleSignInButtonIconOnly(onClick = { googleSignIn.launch() })
```

### Apple Sign-In
After enabling and configuring Apple Sign-In in Firebase, make sure you added "Sign In with Apple" capability in XCode. Then, you can use it as below in your @Composable function:
```kotlin
//Apple Sign-In with Custom Button and authentication with Firebase
val appleSignIn = rememberFirebaseAppleSignInState(onResult = onFirebaseResult)
Button(onClick = { appleSignIn.launch() }) { Text("Apple Sign-In (Custom Design)") }
```

Apple Sign-In with AppleSignInButton. You need to implement `kmpauth-uihelper` dependency
```kotlin
val appleSignIn = rememberFirebaseAppleSignInState(onResult = onFirebaseResult)
AppleSignInButton(modifier = Modifier.fillMaxWidth()) { appleSignIn.launch() }
```

Apple Sign-In IconOnly Button. You need to implement `kmpauth-uihelper` dependency
```kotlin
val appleSignIn = rememberFirebaseAppleSignInState(onResult = onFirebaseResult)
AppleSignInButtonIconOnly(onClick = { appleSignIn.launch() })
```

#### Apple Sign-In without Firebase (Apple platforms only)
Use `kmpauth-apple` when you verify Apple's identity token on your own backend.
Apple's native flow returns a signed JWT that any server can validate against
Apple's public keys — no client secret on the client.

```kotlin
val appleSignIn = rememberAppleSignInState(onResult = { result ->
    val appleUser = result.getOrNull()
    val idToken = appleUser?.idToken   // send to your backend
    val rawNonce = appleUser?.nonce    // if your backend verifies the nonce claim
})
AppleSignInButton(modifier = Modifier.fillMaxWidth()) { appleSignIn.launch() }
```

> **Apple platforms only.** The native flow exists only on iOS. On Android, JVM,
> JS and wasmJs `rememberAppleSignInState` is a no-op that logs — Apple's web
> OAuth flow returns an authorization code that must be exchanged with a
> **client secret server-side**, which is not safe from a client. Use
> `rememberFirebaseAppleSignInState` (`kmpauth-firebase-core`) if you need Apple
> Sign-In on non-Apple targets; Firebase performs that exchange for you.
>
> `email` and `fullName` are returned by Apple **only on the user's first
> authorization** — persist them server-side; later sign-ins return null.

Requires the "Sign In with Apple" capability in Xcode.

### Github Sign-In
After enabling and configuring Github Sign-In in Firebase, you can use it as below in your @Composable function:
```kotlin
//Github Sign-In with Custom Button and authentication with Firebase
val githubSignIn = rememberFirebaseGithubSignInState(onResult = onFirebaseResult)
Button(onClick = { githubSignIn.launch() }) { Text("Github Sign-In (Custom Design)") }
```
### Facebook Sign-In


#### Usage Example
```kotlin
val facebookSignIn = rememberFirebaseFacebookSignInState(
    linkAccount = false,
    onResult = { result -> /* handle FirebaseUser result or error */ },
)

//Facebook button with icon
FacebookSignInButtonIconOnly(onClick = { facebookSignIn.launch() })

//Text Button
FacebookSignInButton(
    modifier = Modifier.fillMaxWidth().height(44.dp),
    fontSize = 19.sp
) { facebookSignIn.launch() }

//Custom Button
YourCustomButton(onClick = { facebookSignIn.launch() })
```

##### Login tracking (token type)
Facebook sign-in accepts a `loginTracking` parameter, consistent on Android and iOS:

- `FacebookLoginTracking.Limited` (**default**) — privacy-friendly Limited Login;
  returns an OIDC JWT + nonce (no iOS App Tracking Transparency prompt). Firebase
  exchanges it through the OIDC OAuth provider.
- `FacebookLoginTracking.Enabled` — classic login; returns a Graph-API access
  token in `FacebookUser.accessToken`. Counts as tracking on iOS (handle ATT).

If your own backend needs a Graph-API access token, request it explicitly:
```kotlin
val facebookSignIn = rememberFacebookSignInState(
    loginTracking = FacebookLoginTracking.Enabled,
    onResult = { result -> val accessToken = result.getOrNull()?.accessToken },
)
```

#### Android Setup
Add these to your `res/values/strings.xml`:
```xml
<string name="facebook_app_id">YOUR_FACEBOOK_APP_ID</string>
<string name="fb_login_protocol_scheme">fbYOUR_FACEBOOK_APP_ID</string>
<string name="facebook_client_token">YOUR_FACEBOOK_CLIENT_TOKEN</string>
```
Add these metadata tags and Facebook Activity to your `AndroidManifest.xml` inside the `<application>` tag:
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
When using `FacebookLoginTracking.Limited` (the **default**), forward your Main Activity's
activity result to `KMPAuth.handleFacebookActivityResult`:

```kotlin
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    KMPAuth.handleFacebookActivityResult(requestCode, resultCode, data)
    super.onActivityResult(requestCode, resultCode, data)
}
```

> **Why only for `Limited`?** Limited Login requires a nonce, which the Facebook SDK
> accepts only through `LoginConfiguration` — an API that has no
> `ActivityResultRegistryOwner` overload, so its result still arrives via
> `onActivityResult`. With `FacebookLoginTracking.Enabled`, KMPAuth uses the SDK's
> AndroidX Activity Result API and **no override is needed**. Calling
> `handleFacebookActivityResult` when it isn't needed is harmless.

#### IOS Setup
Add Facebook Login SDK Swift Package, and add below to your Info.plist:

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

Initialize Facebook SDK on Ios Swift side
```swift

func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        FirebaseApp.configure()
        // Initialize Facebook SDK. 
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
    
    var handled: Bool
    handled = FBSDKCoreKit.ApplicationDelegate.shared.application(
        app,
        open: url,
        options: options
    )

    if handled {
        return true
    }
    
    return false
}

```



##### Notes
- You must configure your Facebook App in Facebook Developers Console properly and enable Firebase Facebook provider.
- Facebook Sign in is supported in Android and iOS only.
- Facebook Login for iOS - https://developers.facebook.com/docs/facebook-login/android
- Facebook Login for Android - https://developers.facebook.com/docs/facebook-login/ios
- Firebase Authentication with Facebook - https://firebase.google.com/docs/auth/android/facebook-login
- Firebase Authentication with Facebook iOS - https://firebase.google.com/docs/auth/ios/facebook-login





