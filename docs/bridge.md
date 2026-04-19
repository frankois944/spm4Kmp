# Bridge the Native API

The bridge is a folder of Swift files that acts as the glue between your Swift/Apple SDK code and Kotlin. The plugin compiles these files and exposes them to Kotlin via `cinterop`.

## How It Works

When syncing the project, the plugin creates the bridge folder at:

- `src/swift/[cinteropName]` — when `cinteropName` is set
- `src/swift/[targetName]` — otherwise (e.g. `src/swift/iosArm64`)

Everything inside this folder is copied to the build directory and compiled into the bridge.

!!! note "Getting Started"
    When the bridge folder is empty, the plugin adds a `StartYourBridgeHere.swift` template with examples to help you get started.

    To disable this behavior, add `spmforkmp.disableStartupFile=true` to your `gradle.properties`.

---

## Gradle Configuration

A minimal bridge setup — no external dependencies, just the Apple native SDK:

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

---

## Writing Your Bridge

!!! warning "Objective-C Compatibility Required"
    Swift code must be annotated with [`@objc` / `@objcMembers`](https://www.hackingwithswift.com/example-code/language/what-is-the-objcmembers-attribute) and declared `public` to be visible in Kotlin. Pure Swift types are not bridgeable.

    See the [playground](./section-help/tips.md#how-can-i-import-swift-code-into-my-kotlin-code) for practical interoperability examples.

```swift title="src/swift/[cinteropName]/mySwiftFile.swift"
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
import [cinteropName].MySwiftBridge

val contentFromSwift = MySwiftBridge().exportedMethod()

val aView = MySwiftBridge().exportedView() as UIView
```

---

## Bundle Resources (since v1.7.1)

To bundle resources (images, fonts, data files, etc.) alongside your bridge:

1. Create a `Resources` folder inside your bridge directory
2. Place your files there (e.g. `bridgeString.txt`)
3. Access them from Swift using `Bundle.module`:

```swift
let url = Bundle.module.url(forResource: "bridgeString", withExtension: "txt")
```
