// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "dev_gitlive_firebase_auth_3_0_0_beta1",
  platforms: [
    .iOS("16.0")
  ],
  products: [
    .library(
      name: "dev_gitlive_firebase_auth_3_0_0_beta1",
      type: .none,
      targets: ["dev_gitlive_firebase_auth_3_0_0_beta1"]
    )
  ],
  dependencies: [
    .package(
      url: "https://github.com/firebase/firebase-ios-sdk.git",
      from: "13.1.1"
    )
  ],
  targets: [
    .target(
      name: "dev_gitlive_firebase_auth_3_0_0_beta1",
      dependencies: [
        .product(
          name: "FirebaseAuth",
          package: "firebase-ios-sdk"
        )
      ]
    )
  ]
)
