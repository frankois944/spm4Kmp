# The Swift Package Manager for Kotlin multiplatform Plugin

[![GitHub Release](https://img.shields.io/github/v/release/frankois944/spm4Kmp)](https://github.com/frankois944/spm4Kmp/releases/)
[![build&tests](https://github.com/frankois944/spm4Kmp/actions/workflows/pre-merge.yaml/badge.svg)](https://github.com/frankois944/spm4Kmp/actions/workflows/pre-merge.yaml)
[![codecov](https://codecov.io/gh/frankois944/spm4Kmp/graph/badge.svg?token=OXEHFLQG1I)](https://codecov.io/gh/frankois944/spm4Kmp)
[![GitHub License](https://img.shields.io/github/license/frankois944/spm4kmp)](https://github.com/frankois944/spm4Kmp/blob/main/LICENSE)

The Swift Package Manager for Kotlin multiplatform Plugin aka `spmForKmp` gradle plugin is a Gradle plugin designed to simplify integrating Swift Package Manager (SPM) dependencies into Kotlin Multiplatform (KMP) projects. It allows you to (almost) effortlessly configure and use Swift packages in your Kotlin projects targeting Apple platforms, such as iOS.

---

## Feedback

This project greatly needs feedback and information about the edge case for progressing; the discussion tab is welcomed.

## Features

- **Support for SPM Dependencies**: Seamlessly add remote SPM dependencies to your KMP modules.
- **Import Swift compatible code to Kotlin**: Enable **SPM dependencies** and your **own Swift code** to be exposed directly in your Kotlin code (if compatible).
- **Automatic CInterop Configuration**: Simplify the process of creating native CInterop definitions for your Swift packages with dependencies.

---

## Getting Started

A fully working sample is [available](https://github.com/frankois944/spm4Kmp/tree/main/example) as a playground.

### 1. Apply the Plugin

Add the plugin to your `build.gradle.kts` or the appropriate Gradle module’s `plugins` block:

[![GitHub Release](https://img.shields.io/github/v/release/frankois944/spm4Kmp)](https://github.com/frankois944/spm4Kmp/releases/)
```kotlin
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("io.github.frankois944.spmForKmp").version("0.0.5") // Apply the spmForKmp plugin
}
```

In your gradle.properties, you must have this declaration :

```
kotlin.mpp.enableCInteropCommonization=true
```

---

### 2. Configure Kotlin Multiplatform Targets

```kotlin
kotlin {
    listOf(
        iosArm64(),           // iOS ARM64 (e.g., for physical devices)
        iosSimulatorArm64()   // iOS Simulator ARM64 (e.g., for M1/M2 machines)
        // and more Apple targets...
    ).forEach {
        it.compilations {
            val main by getting {
                cinterops.create("nativeExample") // Create a CInterop for `nativeExample`
            }
        }
    }
}

swiftPackageConfig {
    create("nativeExample") { // same name as the one in `cinterops.create("...")`
        // add your own swift code and/or your external dependencies, it's optional
        dependency(
            // available only in your own Swift code
            SwiftDependency.Package.Remote.Version(
                url = "https://github.com/krzyzanowskim/CryptoSwift.git",   // Repository URL
                names = listOf("CryptoSwift"),                              // Library names
                version = "1.8.4",                                          // Package version
            )
        )
        dependency(
            // available in the Swift and Kolin code
            SwiftDependency.Package.Remote.Version(
                url = "https://github.com/firebase/firebase-ios-sdk.git", // Repository URL
                names = listOf("FirebaseAnalytics", "FirebaseCore"),     // Libraries from the package
                packageName = "firebase-ios-sdk",                        // (Optional) Package name, can be required in some cases
                version = "11.6.0",                                      // Package version
                exportToKotlin = true                                    // Export to Kotlin for usage in shared Kotlin code
            )
        )
        // and more : https://github.com/frankois944/spm4Kmp/blob/main/plugin-build/plugin/src/main/java/io/github/frankois944/spmForKmp/definition/SwiftDependency.kt
    }
}
```

### 2.1. Supported Swift Package Types

The plugin supports the following configurations :
- **Remote Package**: A Swift package hosted on a remote repository using version, commit, or branch.
- **Local Package**: A Swift package located in a local directory.
- **Local Binary xcFramework**: A xcFramework in a local directory.
- **Remote Binary xcFramework**: A xcFramework hosted on a remote repository.

For more information, refer to the [SwiftDependency](https://github.com/frankois944/spm4Kmp/blob/main/plugin-build/plugin/src/main/java/io/github/frankois944/spmForKmp/definition/SwiftDependency.kt) file.

---

### 3. Add your own Swift code

You can now add your own Swift code in the `src/swift` folder.

> [!IMPORTANT]
> Your swift code need to be mark as [@objc/@objcMembers](https://akdebuging.com/posts/what-is-objc-and-objcmember/) and the visibility set as `public`
> or it won't be exported and available from your Kotlin code
> ```swift
> @objcMembers public class MyOwnSwiftCode: NSObject {
>    public func exportedMethod() -> String {
>        return "value"
>    }
> }
> ```

### 3.1. With external dependencies

You can also use the dependency you have added in the `swiftPackageConfig` block in your Swift code.

For example, `CryptoSwift` is not a library that can be used directly in Kotlin code, but you can create a bridge in your Swift code.

```kotlin
swiftPackageConfig {
    create("dummy") {
        dependency(
            SwiftDependency.Package.Remote.Version(
                url = "https://github.com/krzyzanowskim/CryptoSwift.git",
                names = listOf("CryptoSwift"),
                version = "1.8.4",
            )
        )
    }
}
```

```swift
import Foundation
import CryptoSwift
// inside the folder src/swift
// the class will be automatically accessible from your Kotlin code
@objcMembers public class MySwiftDummyClassWithDependencies: NSObject {
    public func toMD5(value: String) -> String {
        return value.md5()
    }
}
```

### 3.2. Export your dependency directly to your Kotlin Code

You can also use the dependency you have added in the `swiftPackageConfig` in your Kotlin and Swift applications.

> [!WARNING]
> This feature is highly experimental

```kotlin
swiftPackageConfig {
    create("dummy") {
        dependency(
            SwiftDependency.Binary.Local(
                path = "path/to/DummyFramework.xcframework.zip",
                packageName = "DummyFramework",
                exportToKotlin = true, // by default false
            ),
        )
    }
}
```

> [!IMPORTANT]
> When exporting dependency, some configuration need to be added to your xcode project
> 
> A local swift package is being generated during the build of the application and this message diplayed
>
> `
> Spm4Kmp: A local package has been generated in /path/to/the/local/package
>
> Please add it to your project as a local package dependency.
> `
>
> Add the folder to your project as a Local package, that's all.


---

## License

This plugin is distributed under the MIT License. For more details, refer to the plugin repository/documentation.

---

For additional help or detailed documentation, refer to the official repository or contact the plugin maintainers. Happy coding! 🎉
