# Bridge The Native API

## How It Works

When syncing the project, the plugin creates a folder at `src/swift/[cinteropname]`.

The content of this folder is your bridge between Swift and Kotlin, **everything inside is copied to the build directory**.

## Example

### Gradle

The following configuration is a simple bridge between Kotlin and the Swift Apple Native SDK.

The plug-in uses the [cinterop feature of KMP](https://kotlinlang.org/docs/native-get-started.html) to export the compatible code to your Apple target code.

```kotlin title="build.gradle.kts"
swiftPackageConfig {
    create("[cinteropName]") {
    }
}
```

### Bridge

!!! warning "Make your Swift code compatible with Kotlin."

    Your Swift code needs to be marked as [@objc/@objcMembers](https://www.hackingwithswift.com/example-code/language/what-is-the-objcmembers-attribute) and the visibility set as `public`
    or it won't be exported and available from your Kotlin code.

Some tips [here](./section-help/tips.md#working-with-objcnamesclasses-types).

``` swift title="src/swift/[cinteropname]/mySwiftFile.swift"
import UIKit

@objcMembers public class MySwiftBridge: NSObject {
    public func exportedMethod() -> String {
        return "value"
    }
    public func exportedView() -> NSObject {
        return UIView()
    }
}
```

``` kotlin title="iosMain/kotlin/com/example/myKotlinFile.kt"
import [cinteropname].MySwiftBridge

val contentFromSwift = MySwiftBridge().exportedMethod()

val aView = MySwiftBridge().exportedView() as UIView

```
