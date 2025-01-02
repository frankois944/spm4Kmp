import io.github.frankois944.spmForKmp.definition.SwiftDependency

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("io.github.frankois944.spmForKmp")
}

kotlin {
    listOf(
        iosArm64(),
        // iosSimulatorArm64(),
    ).forEach {
        it.compilations {
            val main by getting {
                cinterops.create("nativeExample")
            }
        }
    }
}

swiftPackageConfig {
    create("nativeExample") {
        sharedCachePath = "/tmp/build/cache"
        sharedSecurityPath = "/tmp/build/security"
        sharedConfigPath = "/tmp/build/config"
        dependency(
            SwiftDependency.Package.Remote.Version(
                url = "https://github.com/krzyzanowskim/CryptoSwift.git",
                names = listOf("CryptoSwift"),
                version = "1.8.4",
            ),
            SwiftDependency.Package.Remote.Version(
                url = "https://github.com/firebase/firebase-ios-sdk.git",
                names = listOf("FirebaseAnalytics", "FirebaseCore"),
                packageName = "firebase-ios-sdk",
                version = "11.6.0",
                exportToKotlin = true,
            ),
        )
    }
}
