# The Swift Package Manager for Kotlin multiplatform Plugin

[![build&tests](https://github.com/frankois944/spm4Kmp/actions/workflows/pre-merge.yaml/badge.svg)](https://github.com/frankois944/spm4Kmp/actions/workflows/pre-merge.yaml)

The Swift Package Manager for Kotlin multiplatform Plugin aka `spmForKmp` gradle plugin is a Gradle plugin designed to simplify integrating Swift Package Manager (SPM) dependencies into Kotlin Multiplatform (KMP) projects. It allows you to effortlessly configure and use Swift packages in your Kotlin projects targeting Apple platforms, such as iOS.

---

## Feedback

This project greatly needs feedback and get information about the edge case for progressing; the discussion tab is welcomed.

## Features

- **Support for SPM Dependencies**: Seamlessly add remote SPM dependencies to your KMP modules.
- **KMP Compatibility**: Configure Swift packages for iOS and other Apple targets.
- **Export Dependencies to Kotlin**: Enable specific SPM dependencies to be exposed directly in your Kotlin code (if compatible).
- **Automatic CInterop Configuration**: Simplify the process of creating native CInterop definitions for your Swift packages with dependencies.

---

## Prerequisites

Before using the `spmForKmp` plugin, ensure the following:

1. Your project is set up as a Kotlin Multiplatform (KMP) project.
2. You are using the Gradle build system.
3. You have added Apple targets (e.g., `iosArm64`, `iosSimulatorArm64`, etc.) to your KMP project.

---

## Getting Started

Follow these steps to add and configure the `spmForKmp` plugin in your project:

### 1. Apply the Plugin

Add the plugin to your `build.gradle.kts` or the appropriate Gradle module’s `plugins` block:

```kotlin
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("io.github.frankois944.spmForKmp").version("0.0.1") // Apply the spmForKmp plugin
}
```

In your gradle.properties, you must have this declaration :

```
kotlin.mpp.enableCInteropCommonization=true
```

---

### 2. Configure Kotlin Multiplatform Targets

Make sure you define your Kotlin Multiplatform targets for Apple. Here is an example configuration:

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
    }
}
```

---

### 3. Configure Swift Package Dependencies Plugin

To use Swift Package Manager (SPM) dependencies in your project, define them in your `swiftPackageConfig` block.

You have many options to configure the plugin, some examples hereafter.

### 3.1 With no external dependencies

The following example creates a `nativeExample` Kotlin module with the content of the `src/swift` folder, it's the default behavior.

The content of `src/swift` is optional and will be replaced with a dummy Swift class, so you can only declare the dependencies.

```kotlin
swiftPackageConfig {
    create("nativeExample") { // same name as the one in `cinterops.create("...")`
        // optional content
    }
}
```

### 3.2 With external dependencies

The following example creates a `nativeExample` Kotlin module with the content of the `src/swift` folder and the declared dependencies.

- `CryptoSwift` is a Swift package that will be used in the Swift code.
- `firebase-ios-sdk` is a ObjC compatible package that can be used in the Swift code and in the Kotlin code.

The `exportToKotlin` parameter is used to export the compatible code of the package to Kotlin for use in shared Kotlin code.

By default, the package is not exported to Kotlin.

```kotlin
swiftPackageConfig {
    create("nativeExample") { // same name as the one in `cinterops.create("...")`
        customPackageSourcePath = "src/nativeExample" // (Optional) Custom path for your own Swift source files
        dependency(
            // available only in the Swift code
            SwiftDependency.Package.Remote.Version(
                url = "https://github.com/krzyzanowskim/CryptoSwift.git", // Repository URL
                names = listOf("CryptoSwift"),                           // Library names
                version = "1.8.4",                                       // Package version
            )
        )
        dependency(
            // available in the Swift and kolin code
            SwiftDependency.Package.Remote.Version(
                url = "https://github.com/firebase/firebase-ios-sdk.git", // Repository URL
                names = listOf("FirebaseAnalytics", "FirebaseCore"),     // Libraries from the package
                packageName = "firebase-ios-sdk",                        // (Optional) Package name, can be required in some cases
                version = "11.6.0",                                      // Package version
                exportToKotlin = true                                    // Export to Kotlin for use in shared Kotlin code
            )
        )
        // and more Swift packages...
    }
}
```

### 3.3 Supported Swift Package Types

The plugin supports the following configurations :
- **Remote Package**: A Swift package hosted on a remote repository using version, commit, or branch.
- **Local Package**: A Swift package located in a local directory.
- **Local Binary Package**: A xcFramework in a local directory.
- **Remote Binary Package**: A xcFramework hosted on a remote repository.

For more information, refer to the [SwiftDependency](https://github.com/frankois944/spm4Kmp/blob/main/plugin-build/plugin/src/main/java/io/github/frankois944/spmForKmp/definition/SwiftDependency.kt) file.

---

### 4. Add your own Swift code

You can now add your own Swift code in the `src/swift` folder.

```swift
import Foundation
// inside the folder src/swift
// everything will be automatically accessible from your Kotlin code
@objcMembers public class MySwiftDummyClass: NSObject {
    func mySwiftDummyFunction() -> String {
        return "Hello from Swift!"
    }
}
```

```kotlin
package com.example
import nativeExample.MySwiftDummyClass
@kotlinx.cinterop.ExperimentalForeignApi
val dummyClass = MySwiftDummyClass()
```

### 5. Add your own Swift code and uses the dependencies

You can also use the Swift packages you have added in the `swiftPackageConfig` block in your Swift code.

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

```kotlin
package com.example
import dummy.MySwiftClass
```

---

Please take a look at the functional tests in the [plugin-test](https://github.com/frankois944/spm4Kmp/tree/main/plugin-build/plugin/src/functionalTest/kotlin/io/github/frankois944/spmForKmp) folder for more examples.

The `example` module of this repository is a playground where you can try to use the plugin

---

## License

This plugin is distributed under the MIT License. For more details, refer to the plugin repository/documentation.

---

For additional help or detailed documentation, refer to the official repository or contact the plugin maintainers. Happy coding! 🎉
