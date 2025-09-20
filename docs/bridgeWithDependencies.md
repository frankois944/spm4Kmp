# Use External Dependencies

## How It works

On completion with the [basic configuration](bridge.md), it's possible to use external dependency with your bridge.

The plug-in uses the Swift Packages features to gather all dependency inside the bridge.

!!! note

    If your package doesn't work with the plugin, please create an [issue](https://github.com/frankois944/spm4Kmp/issues).

#### Automatic Dependency Build Inclusion

The plugin will automatically include the dependency requirement in your application, if possible.

Also, it can detect if the dependency [must be included in your Xcode project](./bridgeWithDependencies.md#local-package), **BUT** the detection is not 100% accurate.

!!! warning "I have build error or runtime crash from my native dependency"
    If you encounter during the compilation an error like `Undefined symbol: ...` or a dependency crash during the runtime
    please follow the steps below.

    - Run your application on Xcode and check for errors
        * If you have a `Undefined symbol` build error, you *MUST* add your dependency to Xcode
        * If you have objc/C++/C runtime crash , you *MUST* add your dependency to Xcode

    - You have two choices:
        * Use the [includeProduct](./references/exportedPackageConfig.md#includeproduct) option to generate a [local package](./bridgeWithDependencies.md#local-package) to include in your Xcode project
            * Your Xcode project will be automatically synched with the required dependency
        * Manually add the dependency to your Xcode project

### Use Dependencies In Your Application

By default, the dependencies are not available from your application, but if you need them, you can add them inside the [includeProduct](./references/exportedPackageConfig.md#includeproduct) configuration,
and then the dependency will be included inside a local package.

### Local Package

If the dependencies must be declared on the Xcode side; in that case, you will see the following message during the build:
```
Spm4Kmp: The following dependencies [some_dependency_name] need to be added to your xcode project
A local Swift package has been generated at
/path/to/the/local/package
Please add it to your xcode project as a local package dependency; it will add the missing content.
****You can ignore this messaging if you have already added these dependencies to your Xcode project****
```

## Supported Dependency Sources

The plugin supports the following configurations :

=== "Version"

    ``` kotlin
    remotePackageVersion(
        url = URI("https://github.com/krzyzanowskim/CryptoSwift.git"),
        version = "1.8.4",
        products = {
            add("CryptoSwift")
        },
    )
    ```

=== "Commit"

    ``` kotlin
    remotePackageCommit(
        url = URI("https://github.com/krzyzanowskim/CryptoSwift.git"),
        revision = "729e01bc9b9dab466ac85f21fb9ee2bc1c61b258",
        products = {
            add("CryptoSwift")
        },
    )
    ```

=== "Branch"

    ``` kotlin
    remotePackageBranch(
        url = URI("https://github.com/krzyzanowskim/CryptoSwift.git"),
        branch = "main",
        products = {
            add("CryptoSwift")
        },
    ),
    ```
=== "Local"

    ``` kotlin
    localPackage(
        path = "Absolute path to the local package folder",
        packageName = "LocalSourceDummyFramework",
        products = {
            add("LocalSourceDummyFramework")
        },
    ),
    ```

=== "Local Binary"

    ``` kotlin
    localBinary(
        path = "/path/to/LocalFramework.xcframework"
        packageName = "LocalFramework"
    ),
    ```

=== "Remote Binary"

    ``` kotlin
    remoteBinary(
        url = URI("https://.../RemoteBinaryFramework.xcframework.zip"),
        checksum = "[checksum]",
        packageName = "RemoteBinaryFramework",
    )
    ```

[SwiftDependency reference](./references/dependency/dependencyConfig.md)

### XCFramework

The XCFrameworks are used for Local/Remote Binary and protecting source code distribution, learn [more](https://www.avanderlee.com/swift/binary-targets-swift-package-manager).

An example is [available](https://github.com/frankois944/spm4Kmp/tree/main/BinaryPackageSource).

## Gradle

The following configuration imports the package [CryptoSwift](https://github.com/krzyzanowskim/CryptoSwift) which is a pure Swift library, that can't be used directly in Kotlin.
``` kotlin title="build.gradle.kts"
swiftPackageConfig {
    create("[cinteropName]") {
        dependency {
            remotePackageVersion(
                url = URI("https://github.com/krzyzanowskim/CryptoSwift.git"),
                products = {
                    add("CryptoSwift")
                },
                version = "1.8.4",
            )
            // Another SwiftDependency
            // ...
        }
    }
}
```


## Example

!!! warning "Make your Swift code compatible with Kotlin."

    Your Swift code needs to be marked as [@objc/@objcMembers](https://www.hackingwithswift.com/example-code/language/what-is-the-objcmembers-attribute) and the visibility set as `public`
    or it won't be exported and available from your Kotlin code.


```swift title="src/swift/[cinteropname]/mySwiftFile.swift"
import Foundation
import CryptoSwift

@objcMembers public class MySwiftBridge: NSObject {
    public func toMD5(value: String) -> String {
        return value.md5()
    }
}
```

``` kotlin title="iosMain/kotlin/com/example/myKotlinFile.kt"
import [cinteropname].MySwiftBridge

val contentFromSwift = MySwiftBridge().toMD5(value = "someString")
```

