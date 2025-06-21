// swift-tools-version: 5.9
import PackageDescription

let package = Package(
  name: "exportedNativeIosShared",
  platforms: [.iOS("12.0"), .macOS("10.13"), .tvOS("12.0"), .watchOS("4.0")],
  products: [
    .library(
      name: "exportedNativeIosShared",
      type: .static,
      targets: ["exportedNativeIosShared"])
  ],
  dependencies: [],
  targets: [
    .target(
      name: "exportedNativeIosShared",
      dependencies: [],
      path: "Sources"

    )

  ]
)
