// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "KotlinMultiplatformLinkedPackage",
  platforms: [
    .iOS("16.0")
  ],
  products: [
    .library(
      name: "KotlinMultiplatformLinkedPackage",
      type: .none,
      targets: ["KotlinMultiplatformLinkedPackage"]
    )
  ],
  dependencies: [
    .package(path: "subpackages/_deprecated_kmpauth_firebase_google"),
    .package(path: "subpackages/_deprecated_kmpauth_firebase_facebook"),
    .package(path: "subpackages/_backends_firebase_kmpauth_firebase"),
    .package(path: "subpackages/_providers_kmpauth_google"),
    .package(path: "subpackages/_providers_kmpauth_facebook")
  ],
  targets: [
    .target(
      name: "KotlinMultiplatformLinkedPackage",
      dependencies: [
        .product(name: "_deprecated_kmpauth_firebase_google", package: "_deprecated_kmpauth_firebase_google"),
        .product(name: "_deprecated_kmpauth_firebase_facebook", package: "_deprecated_kmpauth_firebase_facebook"),
        .product(name: "_backends_firebase_kmpauth_firebase", package: "_backends_firebase_kmpauth_firebase"),
        .product(name: "_providers_kmpauth_google", package: "_providers_kmpauth_google"),
        .product(name: "_providers_kmpauth_facebook", package: "_providers_kmpauth_facebook")
      ]
    )
  ]
)
