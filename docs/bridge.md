# Bridge The Native API

## How It Works

When syncing the project, the plugin creates a folder at `src/swift/[cinteropName]` or `src/swift/[targetName]` if `cinteropName` **is not defined**, for example `src/swift/iosArm64`.

The content of this folder is your bridge between Swift and Kotlin, **everything inside is copied to the build directory**.

!!! note "StartYourBridgeHere.swift"

    A template file named `StartYourBridgeHere.swift` is added when the bridge is empty; it contains some example and
    this is your starting point to create your bridge.

    This behavior can be disabled by adding `spmforkmp.disableStartupFile=true` to your `gradle.properties` file.


## Example

### Gradle

The following configuration is a simple bridge between Kotlin and the Swift Apple Native SDK.

The plug-in uses the [cinterop feature of KMP](https://kotlinlang.org/docs/native-get-started.html) to export the compatible code to your Apple target code.

```kotlin title="build.gradle.kts"
kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
        // and more Apple targets...
    ).forEach { target ->
        target.swiftPackageConfig(cinteropName = "[cinteropName]") {
        }
    }
}
```

<details>
<summary>Legacy (< 1.1.0)</summary>
```kotlin title="build.gradle.kts"
swiftPackageConfig {
    create("[cinteropName]") {
    }
}
```
</details>



### Bridge

!!! warning "Make your Swift code compatible with Kotlin."

    Your Swift code needs to be marked as [@objc/@objcMembers](https://www.hackingwithswift.com/example-code/language/what-is-the-objcmembers-attribute) and the visibility set as `public`
    or it won't be exported and available from your Kotlin code.

A [playground](./section-help/tips.md#how-can-i-import-swift-code-into-my-kotlin-code) to help you import Swift code into your Kotlin code.

```swift title="src/swift/[cinteropname]/mySwiftFile.swift"
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

```kotlin title="iosMain/kotlin/com/example/myKotlinFile.kt"
import [cinteropname].MySwiftBridge

val contentFromSwift = MySwiftBridge().exportedMethod()

val aView = MySwiftBridge().exportedView() as UIView
```

## Handle Resources (since v1.7.1)

To handle resources in your bridge, like images, fonts, etc.

- Add a folder named `Resources` and put your resources inside like `bridgeString.txt`.
- Access from Swift with `Bundle.module.url(forResource: "bridgeString", withExtension: "txt")`.
