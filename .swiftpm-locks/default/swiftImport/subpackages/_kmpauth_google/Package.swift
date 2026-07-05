// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_kmpauth_google",
  platforms: [
    .iOS("16.0")
  ],
  products: [
    .library(
      name: "_kmpauth_google",
      type: .none,
      targets: ["_kmpauth_google"]
    )
  ],
  dependencies: [
    .package(
      url: "https://github.com/google/GoogleSignIn-iOS.git",
      from: "9.1.0"
    )
  ],
  targets: [
    .target(
      name: "_kmpauth_google",
      dependencies: [
        .product(
          name: "GoogleSignIn",
          package: "GoogleSignIn-iOS"
        )
      ]
    )
  ]
)
