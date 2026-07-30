// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_sampleApp_shared",
  platforms: [
    .iOS("16.0")
  ],
  products: [
    .library(
      name: "_sampleApp_shared",
      type: .none,
      targets: ["_sampleApp_shared"]
    )
  ],
  dependencies: [
  ],
  targets: [
    .target(
      name: "_sampleApp_shared",
      dependencies: [
      ]
    )
  ]
)
