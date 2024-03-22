# KMPAuth - Kotlin Multiplatform Authentication Library
[![Build](https://github.com/mirzemehdi/KMPAuth/actions/workflows/build_and_publish.yml/badge.svg)](https://github.com/mirzemehdi/KMPAuth/actions/workflows/build_and_publish.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.21-blue.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.mirzemehdi/kmpauth-google?color=blue)](https://search.maven.org/search?q=g:io.github.mirzemehdi+kmpauth)

![badge-android](http://img.shields.io/badge/platform-android-6EDB8D.svg?style=flat)
![badge-ios](http://img.shields.io/badge/platform-ios-CDCDCD.svg?style=flat)

Simple and easy to use Kotlin Multiplatform Authentication library targeting iOS and Android. Supporting **Google**, **Apple**, **Github** authentication integrations using Firebase.   
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
    Column(modifier = modifier,verticalArrangement = Arrangement.spacedBy(10.dp)) {

        //Google Sign-In Button and authentication with Firebase
        GoogleButtonUiContainerFirebase(onResult = onFirebaseResult) {
            GoogleSignInButton(modifier = Modifier.fillMaxWidth()) { this.onClick() }
        }

        //Apple Sign-In Button and authentication with Firebase
        AppleButtonUiContainer(onResult = onFirebaseResult) {
            AppleSignInButton(modifier = Modifier.fillMaxWidth()) { this.onClick() }
        }

        //Github Sign-In with Custom Button and authentication with Firebase
        GithubButtonUiContainer(onResult = onFirebaseResult) {
            Button(onClick = { this.onClick() }) { Text("Github Sign-In (Custom Design)") }
        }

    }
}

```

  


You can check out more [sample codes](https://github.com/mirzemehdi/KMPAuth/blob/main/sampleApp/composeApp/src/commonMain/kotlin/com/mmk/kmpauth/sample/App.kt) here.

## Features
- ✅ Google One Tap Sign-In (without Firebase)
- ✅ [Google Sign-In with Firebase](#google-sign-in)
- ✅ [Apple Sign-In with Firebase](#apple-sign-in)
- ✅ [Github Sign-In with Firebase](#github-sign-in)
- ✅ Apple and Google "Sign in with " UiHelper buttons (according to each brand's guideline)
- 🚧 Facebook Sign-In (can be implemented in future)
- 📱 Multiplatform (android and iOS)

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
    implementation("io.github.mirzemehdi:kmpauth-google:<version>") //Google One Tap Sign-In 
    implementation("io.github.mirzemehdi:kmpauth-firebase:<version>") //Integrated Authentications with Firebase
    implementation("io.github.mirzemehdi:kmpauth-uihelper:<version>") //UiHelper SignIn buttons (AppleSignIn, GoogleSignInButton)

  }
}
```
**_You will also need to include Google Sign-In and/or FirebaseAuth library to your ios app using Swift Package Manager or Cocoapods._**   

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

#### Usage
After configuring above steps this is how you can use:

```kotlin
//Google Sign-In with Custom Button (only one tap sign-in functionality)
GoogleButtonUiContainer(onGoogleSignInResult = { googleUser ->
  val idToken = googleUser?.idToken // Send this idToken to your backend to verify
}) {
  Button(onClick = { this.onClick() }) { Text("Google Sign-In(Custom Design)") }
}

```

Google Sign-In Button and authentication with Firebase. You need to implement `kmpauth-uihelper` dependency
```kotlin
GoogleButtonUiContainerFirebase(onResult = onFirebaseResult) {
  GoogleSignInButton(modifier = Modifier.fillMaxWidth()) { this.onClick() }
}
```

Google Sign-In IconOnly Button and authentication with Firebase. You need to implement `kmpauth-uihelper` dependency
```kotlin
GoogleButtonUiContainerFirebase(onResult = onFirebaseResult) {
  GoogleSignInButtonIconOnly(onClick = { this.onClick() })
}

```

### Apple Sign-In
After enabling and configuring Apple Sign-In in Firebase, make sure you added "Sign In with Apple" capability in XCode. Then, you can use it as below in your @Composable function:
```kotlin
//Apple Sign-In with Custom Button and authentication with Firebase
AppleButtonUiContainer(onResult = onFirebaseResult) {
  //Any View, you just need to delegate child view's click to this UI Container's click method
  Button(onClick = { this.onClick() }) { Text("Apple Sign-In (Custom Design)") }
}

```

Apple Sign-In with AppleSignInButton. You need to implement `kmpauth-uihelper` dependency
```kotlin
AppleButtonUiContainer(onResult = onFirebaseResult) {
  AppleSignInButton(modifier = Modifier.fillMaxWidth()) { this.onClick() }
}
```

Apple Sign-In IconOnly Button. You need to implement `kmpauth-uihelper` dependency
```kotlin
AppleButtonUiContainer(onResult = onFirebaseResult) {
  AppleSignInButtonIconOnly(onClick = { this.onClick() })
}

```

### Github Sign-In
After enabling and configuring Github Sign-In in Firebase, you can use it as below in your @Composable function:
```kotlin
//Github Sign-In with Custom Button and authentication with Firebase
GithubButtonUiContainer(onResult = onFirebaseResult) {
  //Any View, you just need to delegate child view's click to this UI Container's click method
  Button(onClick = { this.onClick() }) { Text("Github Sign-In (Custom Design)") }
}

```



