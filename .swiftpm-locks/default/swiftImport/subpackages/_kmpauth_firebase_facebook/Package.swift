// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_kmpauth_firebase_facebook",
  platforms: [
    .iOS("16.0")
  ],
  products: [
    .library(
      name: "_kmpauth_firebase_facebook",
      type: .none,
      targets: ["_kmpauth_firebase_facebook"]
    )
  ],
  dependencies: [
    .package(
      url: "https://github.com/firebase/firebase-ios-sdk.git",
      from: "11.8.0"
    ),
    .package(
      url: "https://github.com/google/GoogleSignIn-iOS.git",
      from: "9.1.0"
    ),
    .package(
      url: "https://github.com/facebook/facebook-ios-sdk.git",
      from: "18.0.0"
    )
  ],
  targets: [
    .target(
      name: "_kmpauth_firebase_facebook",
      dependencies: [
        .product(
          name: "FirebaseAuth",
          package: "firebase-ios-sdk"
        ),
        .product(
          name: "FirebaseCore",
          package: "firebase-ios-sdk"
        ),
        .product(
          name: "GoogleSignIn",
          package: "GoogleSignIn-iOS"
        ),
        .product(
          name: "FacebookCore",
          package: "facebook-ios-sdk"
        ),
        .product(
          name: "FacebookLogin",
          package: "facebook-ios-sdk"
        )
      ]
    )
  ]
)
