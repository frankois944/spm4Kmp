# Use External Dependencies

## How It works

On completion with the [basic configuration](bridge.md), it's possible to use external dependency with your bridge.

The Plug-in reproduces the CocoaPods plugin behavior with the same kind of issues about third-party dependency but less intrusively.

!!! note

    If your package doesn't work with the plugin, please create an [issue](https://github.com/frankois944/spm4Kmp/issues).

## Supported Dependency Sources

The plugin supports the following configurations :

=== "Version"

    ``` kotlin
    SwiftDependency.Package.Remote.Version(
        url = URI("https://github.com/krzyzanowskim/CryptoSwift.git"),
        version = "1.8.4",
        products = {
            add("CryptoSwift")
        },
    )
    ```

=== "Commit"

    ``` kotlin
    SwiftDependency.Package.Remote.Commit(
        url = URI("https://github.com/krzyzanowskim/CryptoSwift.git"),
        revision = "729e01bc9b9dab466ac85f21fb9ee2bc1c61b258",
        products = {
            add("CryptoSwift")
        },
    )
    ```

=== "Branch"

    ``` kotlin
    SwiftDependency.Package.Remote.Branch(
        url = URI("https://github.com/krzyzanowskim/CryptoSwift.git"),
        branch = "main",
        products = {
            add("CryptoSwift")
        },
    ),
    ```
=== "Local"

    ``` kotlin
    SwiftDependency.Package.Local(
        path = "Absolute path to the local package folder",
        packageName = "LocalSourceDummyFramework",
        products = {
            add("LocalSourceDummyFramework")
        },
    ),
    ```

=== "Local Binary"

    ``` kotlin
    SwiftDependency.Binary.Local(
        path = "/path/to/LocalFramework.xcframework"
        packageName = "LocalFramework"
    ),
    ```

=== "Remote Binary"

    ``` kotlin
    SwiftDependency.Binary.Remote(
        url = URI("https://.../RemoteBinaryFramework.xcframework.zip"),
        checksum = "[checksum]",
        packageName = "RemoteBinaryFramework",
    )
    ```

[SwiftDependency reference](./references/swiftDependency.md)

### XCFramework

The XCFramework are used for Local/Remote Binary and protecting source code distribution, learn [more](https://www.avanderlee.com/swift/binary-targets-swift-package-manager).

An example is [available](https://github.com/frankois944/spm4Kmp/tree/main/BinaryPackageSource).

## Gradle

The following configuration imports the package [CryptoSwift](https://github.com/krzyzanowskim/CryptoSwift) which is a pure Swift library, that can't be used directly in Kotlin.
``` kotlin title="build.gradle.kts"
swiftPackageConfig {
    create("[cinteropName]") {
        dependency(
            SwiftDependency.Package.Remote.Version(
                url = URI("https://github.com/krzyzanowskim/CryptoSwift.git"),
                products = {
                    add("CryptoSwift")
                },
                version = "1.8.4",
            ),
            // Another SwiftDependency
            // ...
        )
    }
}
```

!!! warning

    A local swift package is being generated during the build and this message diplayed
    ```
    Spm4Kmp: A local Swift package has been generated at
    /path/to/the/local/package
    Please add it to your xcode project as a local package dependency.
    ```
    Add the folder to your Xcode project as a Local package, that's all.

    Note : When updating your configuration, reset the package cache to apply the modification.

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

