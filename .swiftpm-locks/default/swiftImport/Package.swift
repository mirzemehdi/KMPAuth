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
    .package(path: "subpackages/_backends_firebase_kmpauth-firebase-core"),
    .package(path: "subpackages/_backends_firebase_kmpauth-firebase-facebook"),
    .package(path: "subpackages/_backends_firebase_kmpauth-firebase-google"),
    .package(path: "subpackages/_backends_firebase_kmpauth_firebase_core"),
    .package(path: "subpackages/_backends_firebase_kmpauth_firebase_facebook"),
    .package(path: "subpackages/_backends_firebase_kmpauth_firebase_google"),
    .package(path: "subpackages/_deprecated_kmpauth-firebase"),
    .package(path: "subpackages/_providers_kmpauth-facebook"),
    .package(path: "subpackages/_providers_kmpauth-google"),
    .package(path: "subpackages/_providers_kmpauth_facebook"),
    .package(path: "subpackages/_providers_kmpauth_google"),
    .package(path: "subpackages/_sampleApp_shared")
  ],
  targets: [
    .target(
      name: "KotlinMultiplatformLinkedPackage",
      dependencies: [
        .product(name: "_backends_firebase_kmpauth-firebase-core", package: "_backends_firebase_kmpauth-firebase-core"),
        .product(name: "_backends_firebase_kmpauth-firebase-facebook", package: "_backends_firebase_kmpauth-firebase-facebook"),
        .product(name: "_backends_firebase_kmpauth-firebase-google", package: "_backends_firebase_kmpauth-firebase-google"),
        .product(name: "_backends_firebase_kmpauth_firebase_core", package: "_backends_firebase_kmpauth_firebase_core"),
        .product(name: "_backends_firebase_kmpauth_firebase_facebook", package: "_backends_firebase_kmpauth_firebase_facebook"),
        .product(name: "_backends_firebase_kmpauth_firebase_google", package: "_backends_firebase_kmpauth_firebase_google"),
        .product(name: "_deprecated_kmpauth-firebase", package: "_deprecated_kmpauth-firebase"),
        .product(name: "_providers_kmpauth-facebook", package: "_providers_kmpauth-facebook"),
        .product(name: "_providers_kmpauth-google", package: "_providers_kmpauth-google"),
        .product(name: "_providers_kmpauth_facebook", package: "_providers_kmpauth_facebook"),
        .product(name: "_providers_kmpauth_google", package: "_providers_kmpauth_google"),
        .product(name: "_sampleApp_shared", package: "_sampleApp_shared")
      ]
    )
  ]
)
