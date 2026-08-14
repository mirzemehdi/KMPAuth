// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_deprecated_kmpauth_firebase_google",
  platforms: [
    .iOS("16.0")
  ],
  products: [
    .library(
      name: "_deprecated_kmpauth_firebase_google",
      type: .none,
      targets: ["_deprecated_kmpauth_firebase_google"]
    )
  ],
  dependencies: [
    .package(
      url: "https://github.com/firebase/firebase-ios-sdk.git",
      "12.17.0"..."12.999.999"
    ),
    .package(
      url: "https://github.com/google/GoogleSignIn-iOS.git",
      from: "9.1.0"
    )
  ],
  targets: [
    .target(
      name: "_deprecated_kmpauth_firebase_google",
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
        )
      ]
    )
  ]
)
