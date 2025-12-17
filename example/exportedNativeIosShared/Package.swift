
        // swift-tools-version: 5.9
        import PackageDescription

        let package = Package(
            name: "exportedNativeIosShared",
            platforms: [.iOS("16.0"),.macOS("10.13"),.tvOS("12.0"),.watchOS("4.0")],
            products: [
                .library(
                    name: "exportedNativeIosShared",
                    type: .static,
                    targets: ["exportedNativeIosShared","HevSocks5Tunnel"])
            ],
            dependencies: [
                .package(url: "https://github.com/firebase/firebase-ios-sdk.git", exact: "12.3.0")
            ],
            targets: [
                .target(
                    name: "exportedNativeIosShared",
                    dependencies: [
                        .product(name: "FirebaseAnalytics", package: "firebase-ios-sdk"),"HevSocks5Tunnel"
                    ],
                    path: "Sources"
                    
                )
                ,.binaryTarget(name: "HevSocks5Tunnel", url:"https://github.com/wanliyunyan/HevSocks5Tunnel/releases/download/2.10.0/HevSocks5Tunnel.xcframework.zip", checksum:"f66fc314edbdb7611c5e8522bc50ee62e7930f37f80631b8d08b2a40c81a631a")
            ]
        )
        