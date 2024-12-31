import fr.frankois944.spm.kmp.plugin.definition.SwiftDependency

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("fr.frankois944.spm.kmp.plugin")
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach {
        it.compilations {
            val main by getting {
                cinterops.create("nativeExample")
            }
        }
    }
}

swiftPackageConfig {
    cinteropsName = "nativeExample"
    packages.add(
        SwiftDependency.Package.Remote.Version(
            url = "https://github.com/krzyzanowskim/CryptoSwift.git",
            names = listOf("CryptoSwift"),
            version = "1.8.4",
        ),
    )
    packages.add(
        SwiftDependency.Package.Remote.Version(
            url = "https://github.com/firebase/firebase-ios-sdk.git",
            names = listOf("FirebaseAnalytics", "FirebaseCore"),
            packageName = "firebase-ios-sdk",
            version = "11.6.0",
            export = true,
        ),
    )
}
