// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_providers_kmpauth-google",
  platforms: [
    .iOS("16.0")
  ],
  products: [
    .library(
      name: "_providers_kmpauth-google",
      type: .none,
      targets: ["_providers_kmpauth-google"]
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
      name: "_providers_kmpauth-google",
      dependencies: [
        .product(
          name: "GoogleSignIn",
          package: "GoogleSignIn-iOS"
        )
      ]
    )
  ]
)
