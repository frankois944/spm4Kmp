# Use External Dependencies

Building on the [basic bridge setup](bridge.md), you can pull in external Swift packages and use them inside your bridge.

!!! note
    If a package doesn't work with the plugin, please [open an issue](https://github.com/frankois944/spm4Kmp/issues).

---

## How It Works

The plugin fetches dependencies via Swift Package Manager and makes them available inside your bridge Swift files. The dependency is linked into the final binary automatically where possible.

### Automatic Dependency Inclusion {#automatic-dependency-build-inclusion}

The plugin attempts to automatically include native dependencies in your application. It can also detect when a dependency [needs to be added to your Xcode project](#local-package), though detection is not 100% reliable.

!!! warning "Build error or runtime crash from a native dependency?"
    If you see `Undefined symbol: ...` at build time or a native crash at runtime:

    1. Run your app in Xcode and check the error:
        - `Undefined symbol` → you **must** add the dependency to Xcode
        - ObjC / C++ / C runtime crash → you **must** add the dependency to Xcode
    2. Choose one of:
        - Use the [`includeProduct`](./references/exportedPackageConfig.md#includeproduct) option to generate a [local package](#local-package) that Xcode picks up automatically
        - Manually add the dependency to your Xcode project

### Making Dependencies Available to Your App

By default, bridge dependencies are not visible to your app code. To expose them, add them to the [`includeProduct`](./references/exportedPackageConfig.md#includeproduct) configuration — the plugin will bundle them into a local package.

### Local Package {#local-package}

When a dependency must be declared on the Xcode side, the plugin prints a message during the build:

```
Spm4Kmp: The following dependencies [some_dependency_name] need to be added to your Xcode project.
A local Swift package has been generated at /path/to/the/local/package
Please add it to your Xcode project as a local package dependency.
```

Add the generated package to your Xcode project as a local dependency. To silence the message once done, set `spmforkmp.hideLocalPackageMessage=true` in `gradle.properties`.

---

## Example

Adding [CryptoSwift](https://github.com/krzyzanowskim/CryptoSwift) — a pure Swift library that cannot be used directly in Kotlin, so the bridge wraps it.

### Gradle

```kotlin title="build.gradle.kts"
kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
        // and more Apple targets...
    ).forEach { target ->
        target.swiftPackageConfig(cinteropName = "[cinteropName]") {
            dependency {
                remotePackageVersion(
                    url = uri("https://github.com/krzyzanowskim/CryptoSwift.git"),
                    version = "1.8.4",
                    products = {
                        add("CryptoSwift")
                    },
                )
                // more dependencies...
            }
        }
    }
}
```

<details>
<summary>Legacy (< 1.1.0)</summary>

```kotlin title="build.gradle.kts"
swiftPackageConfig {
    create("[cinteropName]") {
        dependency {
            remotePackageVersion(
                url = uri("https://github.com/krzyzanowskim/CryptoSwift.git"),
                version = "1.8.4",
                products = {
                    add("CryptoSwift")
                },
            )
        }
    }
}
```

</details>

### Bridge

!!! warning "Objective-C Compatibility Required"
    Bridge code must be annotated with [`@objc` / `@objcMembers`](https://www.hackingwithswift.com/example-code/language/what-is-the-objcmembers-attribute) and declared `public` to be visible in Kotlin.

    See the [playground](./section-help/tips.md#how-can-i-import-swift-code-into-my-kotlin-code) for interoperability examples.

```swift title="src/swift/[cinteropName]/mySwiftFile.swift"
import Foundation
import CryptoSwift

@objcMembers public class MySwiftBridge: NSObject {
    public func toMD5(value: String) -> String {
        return value.md5()
    }
}
```

```kotlin title="iosMain/kotlin/com/example/myKotlinFile.kt"
import [cinteropName].MySwiftBridge

val hash = MySwiftBridge().toMD5(value = "someString")
```

---

## Supported Dependency Sources

=== "Version"

    ```kotlin
    remotePackageVersion(
        url = uri("https://github.com/krzyzanowskim/CryptoSwift.git"),
        version = "1.8.4",
        products = {
            add("CryptoSwift")
        },
    )
    ```

=== "Branch"

    ```kotlin
    remotePackageBranch(
        url = uri("https://github.com/krzyzanowskim/CryptoSwift.git"),
        branch = "main",
        products = {
            add("CryptoSwift")
        },
    )
    ```

=== "Commit"

    ```kotlin
    remotePackageCommit(
        url = uri("https://github.com/krzyzanowskim/CryptoSwift.git"),
        revision = "729e01bc9b9dab466ac85f21fb9ee2bc1c61b258",
        products = {
            add("CryptoSwift")
        },
    )
    ```

=== "Local"

    ```kotlin
    localPackage(
        path = "/absolute/path/to/local/package",
        packageName = "LocalSourceDummyFramework",
        products = {
            add("LocalSourceDummyFramework")
        },
    )
    ```

=== "Local Binary"

    ```kotlin
    localBinary(
        path = "/path/to/LocalFramework.xcframework",
        packageName = "LocalFramework",
    )
    ```

=== "Remote Binary"

    ```kotlin
    remoteBinary(
        url = uri("https://.../RemoteBinaryFramework.xcframework.zip"),
        checksum = "[checksum]",
        packageName = "RemoteBinaryFramework",
    )
    ```

See the full [SwiftDependency reference](./references/dependency/dependencyConfig.md) for all available options.

### XCFrameworks

XCFrameworks are used for local/remote binary dependencies and for distributing pre-built code without exposing source. [Learn more](https://www.avanderlee.com/swift/binary-targets-swift-package-manager) or browse the [example](https://github.com/frankois944/spm4Kmp/tree/main/BinaryPackageSource).
