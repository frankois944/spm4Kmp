import fr.frankois944.spm.kmp.plugin.definition.SwiftPackageDependencyDefinition
import java.net.URI

plugins {
    java
    id("fr.frankois944.spm.kmp.plugin")
}

swiftPackageConfig {
    generatedPackageDirectory = File("src/Package.swift")
    minIos = "12.0"
    minMacos = "10.13"

    dependencies.add(
        SwiftPackageDependencyDefinition.RemoteDefinition.Version(
            url = URI("https://github.com/firebase/firebase-ios-sdk"),
            names = listOf("FirebaseAuth", "FirebaseCore", "FirebaseAnalytics"),
            packageName = "firebase-ios-sdk",
            version = "11.6.0",
        ),
    )
    dependencies.add(
        SwiftPackageDependencyDefinition.RemoteDefinition.Version(
            url = URI("https://github.com/bugsnag/bugsnag-cocoa.git"),
            names = listOf("Bugsnag"),
            packageName = "bugsnag-cocoa",
            version = "6.30.2",
        ),
    )
    dependencies.add(
        SwiftPackageDependencyDefinition.RemoteDefinition.Version(
            url = URI("https://github.com/krzyzanowskim/CryptoSwift.git"),
            names = listOf("CryptoSwift"),
            version = "1.8.4",
        ),
    )
}
