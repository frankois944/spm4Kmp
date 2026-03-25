
// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "exportedNativeFirebaseStuff",
    platforms: [.iOS("15.0"),.macOS("10.13"),.tvOS("12.0"),.watchOS("4.0")],
    products: [
        .library(
            name: "exportedNativeFirebaseStuff",
            type: .static,
            targets: ["exportedNativeFirebaseStuff"])
    ],
    dependencies: [
        .package(url: "https://github.com/firebase/firebase-ios-sdk", exact: "12.11.0")
    ],
    targets: [
        .target(
            name: "exportedNativeFirebaseStuff",
            dependencies: [
                .product(name: "FirebaseMessaging", package: "firebase-ios-sdk"),.product(name: "FirebasePerformance", package: "firebase-ios-sdk")
            ],
            path: "Sources"
            
            
        )
        
    ]
)
        