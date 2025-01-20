# The Swift Package Manager for Kotlin multiplatform Plugin

[![GitHub Release](https://img.shields.io/github/v/release/frankois944/spm4Kmp)](https://github.com/frankois944/spm4Kmp/releases/)
[![build&tests](https://github.com/frankois944/spm4Kmp/actions/workflows/pre-merge.yaml/badge.svg)](https://github.com/frankois944/spm4Kmp/actions/workflows/pre-merge.yaml)
[![codecov](https://codecov.io/gh/frankois944/spm4Kmp/graph/badge.svg?token=OXEHFLQG1I)](https://codecov.io/gh/frankois944/spm4Kmp)
[![GitHub License](https://img.shields.io/github/license/frankois944/spm4kmp)](https://github.com/frankois944/spm4Kmp/blob/main/LICENSE)

The Swift Package Manager for Kotlin multiplatform Plugin aka `spmForKmp` gradle plugin is a Gradle plugin designed to simplify integrating Swift Package Manager (SPM) dependencies into Kotlin Multiplatform (KMP) projects. It allows you to (almost) effortlessly configure and use Swift packages in your Kotlin projects targeting Apple platforms, such as iOS.

Also, You can **embed your own Swift code** for a direct import to your Kotlin library.

---

## Feedback

This project greatly needs feedback and information about the edge case for progressing; the discussion tab is welcomed.

## Features

- **Create a bridge easily**: Import your own Swift code for functionality can't be done in Kotlin for example.
- **Import Swift compatible code to Kotlin**: Enable **SPM dependencies** and your **own Swift code** to be exposed directly in your Kotlin code (if compatible).
- **Automatic CInterop Configuration**: Simplify the process of creating native CInterop definitions for your Swift packages with dependencies.

> [!WARNING]  
> Pure Swift packages can't be exported to Kotlin; creating a bridge with this plugin is a solution for avoiding this issue.

---

## Getting Started

A fully working sample is [available](https://github.com/frankois944/spm4Kmp/tree/main/example) as a playground, and a sample with CMP/Firebase [here](https://github.com/frankois944/FirebaseKmpDemo).

### 1. Apply the Plugin

Add the plugin to your `build.gradle.kts` or the appropriate Gradle module’s `plugins` block:

[![GitHub Release](https://img.shields.io/github/v/release/frankois944/spm4Kmp)](https://github.com/frankois944/spm4Kmp/releases/)
```kotlin
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("io.github.frankois944.spmForKmp").version("0.0.8") // Apply the spmForKmp plugin
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
        // add your embedded swift code and/or your external dependencies, it's optional
         dependency(
            SwiftDependency.Package.Remote.Version(
                // Repository URL
                url = URI("https://github.com/firebase/firebase-ios-sdk.git"),
                // Libraries from the package
                products = {
                    // Export to Kotlin for use in shared Kotlin code and use it in your swift code
                    add("FirebaseCore", "FirebaseAnalytics", exportToKotlin = true)
                    // Add FirebaseDatabase to your swift code but don't export it
                    add(ProductName("FirebaseDatabase"))
                },
                // (Optional) Package name, can be required in some cases
                packageName = "firebase-ios-sdk",
                // Package version
                version = "11.6.0",
            ),
            SwiftDependency.Binary.Local(
                path = "$testRessources/DummyFramework.xcframework.zip",
                packageName = "DummyFramework",
                exportToKotlin = true,
            ),
            SwiftDependency.Package.Local(
                path = "$testRessources/LocalSourceDummyFramework",
                packageName = "LocalSourceDummyFramework",
                products = {
                    // Export to Kotlin for use in shared Kotlin code, false by default
                    add("LocalSourceDummyFramework", exportToKotlin = true)
                },
            ),
            SwiftDependency.Package.Remote.Version(
                url = URI("https://github.com/krzyzanowskim/CryptoSwift.git"),
                version = "1.8.1",
                products = {
                    // Can be only used in your "src/swift/nativeExample" code.
                    add("CryptoSwift")
                },
            ),
            // see SwiftDependency class for more use cases
        )
    }
}
```

### 2.1. Supported Swift Package Dependency Types

The plugin supports the following configurations :
- **Remote Package**: A Swift package hosted on a remote repository using version, commit, or branch.
- **Local Package**: A Swift package located in a local directory.
- **Local Binary xcFramework**: A xcFramework in a local directory.
- **Remote Binary xcFramework**: A xcFramework hosted on a remote repository.

For more information, refer to the [SwiftDependency](https://github.com/frankois944/spm4Kmp/blob/main/plugin-build/plugin/src/main/java/io/github/frankois944/spmForKmp/definition/SwiftDependency.kt) file.

---

### 3. Add your embedded Swift code

You can now add your embedded Swift code in the `src/swift/[cinteropname]` folder; it will be your bridge between Swift and Kotlin.

> [!TIP]
> Your swift code need to be mark as [@objc/@objcMembers](https://akdebuging.com/posts/what-is-objc-and-objcmember/) and the visibility set as `public`
> or it won't be exported and available from your Kotlin code
> ```swift
> @objcMembers public class MyOwnSwiftCode: NSObject {
>    public func exportedMethod() -> String {
>        return "value"
>    }
> }
> ```

### 4. Add external dependencies

The Plug-in is reproducing the CocoaPods plugin behavior with the same kind of issues about third-party dependency but less intrusively.

> [!IMPORTANT]
>
> A local swift package is being generated during the build and this message diplayed
> ```
> Spm4Kmp: A local Swift package has been generated at
> /path/to/the/local/package
> Please add it to your xcode project as a local package dependency.
> ```
> Add the folder to your project as a Local package, that's all.
>
> Note : When updating your configuration, reset the package cache to apply the modification.
> 


### 4.1. Using inside your bridge

You can also use the dependency you have added in the `swiftPackageConfig` block in your Swift code.

For example, `CryptoSwift` is not a library that can be used directly in Kotlin code, but you can create a bridge in your Swift code.

```kotlin
swiftPackageConfig {
    create("dummy") {
        dependency(
            SwiftDependency.Package.Remote.Version(
                url = URI("https://github.com/krzyzanowskim/CryptoSwift.git"),
                products = {
                    add("CryptoSwift")
                },                           
                version = "1.8.4",                                         
            )
        )
    }
}
```

```swift
import Foundation
import CryptoSwift
// inside the folder src/swift/dummy
// the class will be automatically accessible from your Kotlin code
@objcMembers public class MySwiftDummyClassWithDependencies: NSObject {
    public func toMD5(value: String) -> String {
        return value.md5()
    }
}
```

### 4.2. Using inside your Kotlin

You can also use the dependency you have added in the `swiftPackageConfig` in your Kotlin applications.

```kotlin
swiftPackageConfig {
    create("dummy") {
        dependency(
            SwiftDependency.Binary.Local(
                path = "path/to/DummyFramework.xcframework.zip",
                products = {
                     add("DummyFramework", exportToKotlin = true)
                }
            ),
        )
    }
}
```


### 5. Configuration by target

You can set a different configuration for each each target you manage.

```kotlin
listOf(
    iosX64(),
    iosSimulatorArm64(),
).forEach {
    it.compilations {
        val main by getting {
            cinterops.create("nativeIosShared") // a config for iOS
        }
    }
}

listOf(
    macosArm64(),
).forEach {
    it.compilations {
        val main by getting {
            cinterops.create("nativeMacosShared") // a config for macos
        }
    }
}

swiftPackageConfig {
    create("nativeIosShared") {
        // your embedded swift is inside the folder src/swift/nativeIosShared
        // your config for iOS
    }
    create("nativeMacosShared") {
        // your embedded swift is inside the folder src/swift/nativeMacosShared
        // your config for macOS
    }
}
```

---

## License

This plugin is distributed under the MIT License. For more details, refer to the plugin repository/documentation.

---

For additional help or detailed documentation, refer to the official repository or contact the plugin maintainers. Happy coding! 🎉
