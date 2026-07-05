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
    .package(path: "subpackages/_kmpauth-facebook"),
    .package(path: "subpackages/_kmpauth-firebase"),
    .package(path: "subpackages/_kmpauth-firebase-facebook"),
    .package(path: "subpackages/_kmpauth-google"),
    .package(path: "subpackages/_kmpauth_facebook"),
    .package(path: "subpackages/_kmpauth_firebase"),
    .package(path: "subpackages/_kmpauth_firebase_facebook"),
    .package(path: "subpackages/_kmpauth_google"),
    .package(path: "subpackages/_sampleApp_composeApp")
  ],
  targets: [
    .target(
      name: "KotlinMultiplatformLinkedPackage",
      dependencies: [
        .product(name: "_kmpauth-facebook", package: "_kmpauth-facebook"),
        .product(name: "_kmpauth-firebase", package: "_kmpauth-firebase"),
        .product(name: "_kmpauth-firebase-facebook", package: "_kmpauth-firebase-facebook"),
        .product(name: "_kmpauth-google", package: "_kmpauth-google"),
        .product(name: "_kmpauth_facebook", package: "_kmpauth_facebook"),
        .product(name: "_kmpauth_firebase", package: "_kmpauth_firebase"),
        .product(name: "_kmpauth_firebase_facebook", package: "_kmpauth_firebase_facebook"),
        .product(name: "_kmpauth_google", package: "_kmpauth_google"),
        .product(name: "_sampleApp_composeApp", package: "_sampleApp_composeApp")
      ]
    )
  ]
)
