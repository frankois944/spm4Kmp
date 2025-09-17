// swift-tools-version: 5.9
import PackageDescription

let package = Package(
  name: "exportedNativeIosShared",
  platforms: [.iOS("16.0"), .macOS("10.13"), .tvOS("12.0"), .watchOS("4.0")],
  products: [
    .library(
      name: "exportedNativeIosShared",
      type: .static,
      targets: ["exportedNativeIosShared"])
  ],
  dependencies: [
    .package(url: "https://github.com/firebase/firebase-ios-sdk.git", exact: "12.3.0")
  ],
  targets: [
    .target(
      name: "exportedNativeIosShared",
      dependencies: [
        .product(name: "FirebaseAnalytics", package: "firebase-ios-sdk")
      ],
      path: "Sources"

    )

  ]
)
