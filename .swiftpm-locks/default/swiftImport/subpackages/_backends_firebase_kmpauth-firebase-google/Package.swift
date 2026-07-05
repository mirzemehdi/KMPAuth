// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_backends_firebase_kmpauth-firebase-google",
  platforms: [
    .iOS("16.0")
  ],
  products: [
    .library(
      name: "_backends_firebase_kmpauth-firebase-google",
      type: .none,
      targets: ["_backends_firebase_kmpauth-firebase-google"]
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
    )
  ],
  targets: [
    .target(
      name: "_backends_firebase_kmpauth-firebase-google",
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
