# Basic configuration

## Gradle

The following configuration is a simple bridge between Kotlin and Swift.

``` kotlin title="build.gradle.kts"
swiftPackageConfig {
    create("[cinteropName]") {
    }
}
```

When syncing the project, the plugin creates a folder at `src/swift/[cinteropname]`.

The content of this folder is your bridge between Swift and Kotlin, it can contain only Swift source files or no file.

## Example

!!! warning "Make your Swift code compatible with Kotlin."

    Your Swift code needs to be marked as [@objc/@objcMembers](https://akdebuging.com/posts/what-is-objc-and-objcmember/) and the visibility set as `public`
    or it won't be exported and available from your Kotlin code.

``` swift title="src/swift/[cinteropname]/mySwiftFile.swift"
@objcMembers public class MySwiftBridge: NSObject {
    public func exportedMethod() -> String {
        return "value"
    }
}
```

``` kotlin title="iosMain/kotlin/com/example/myKotlinFile.kt"
import [cinteropname].MySwiftBridge

val contentFromSwift = MySwiftBridge().exportedMethod()

```
