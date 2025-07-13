# Tips

## Reduce Build Time

- **Since the version 0.4.0**

[spmWorkingPath](../references/swiftPackageConfig.md#spmworkingpath) has been added to change the path to Swift Package working file.

By setting [spmWorkingPath](https://github.com/frankois944/spm4Kmp/blob/cf80e65b3076d9e0bcd94a847e1209d4b9b91141/example/build.gradle.kts#L108C1-L108C104) outside the build folder, the working files won't be removed if you clean the project, and you can **exclude** the folder from [indexing](https://www.jetbrains.com/help/idea/indexing.html#exclude).

Swift Package Manager has its own cache, so it's fine to detach it from the Kotlin build folder.

### CI/CD Caching

Add to your cache the content of the `build/spmKmpPlugin` folder or the `spmWorkingDir` value if set.

Also, check my [GitHub action workflow](https://github.com/frankois944/spm4Kmp/blob/main/.github/workflows/pre-merge.yaml) where I build the example app with cached built files.

## Firebase

An [full example](https://github.com/frankois944/FirebaseKmpDemo) of how to implement Firebase with the plugin

## Working With _objcnames.classes_ Types

For example, when using a UIView (work with any ObjC Types, ex: UIViewController...).

``` swift title="mySwiftBridge.swift"

// Force cinterop to include `platform.UIKit.UIView`
@objcMembers public class MyDummyView: UIView {}

// Or force by inheritance
@objcMembers public class TestClass: NSObject /* or UIView */ {

    // return `UIView` is not enough to let cinterop use the correct type
    public func getView() -> UIView {
        return UIView()
    }

    public func setView(view: UIView) {
        // store view
    }

    // or if you don't want to declare an extra MyDummyView

    public func getViewWithNSObject() -> NSObject {
        return UIView()
    }

    public func setViewWithNSObject(view: NSObject) {
        // store view
    }

}
```
``` kotlin title="iosMain/myKotlinFile.kt"
fun getView(): UIView = TestClass().getView()
fun setView(view: UIView) = TestClass().setViewWithView(view)

// or

fun getView(): UIView = TestClass().getViewWithNSObject() as UIView
fun setView(view: UIView) = TestClass().setViewWithNSObject(view)
```

## Support Xcode 15 and earlier

!!! warning "Experimental"

    This is experimental; this tips is not fully tested for every use cases.
    You can create an issue if needed.


As Xcode < 16 is unstable with Swift Package and has some weird behavior.
It's recommended to use the latest version of the IDE.

But, it is possible to support older versions of Xcode by using a more recent version of the Swift Compiler than the Xcode one.

The property [swiftBinPath](../references/swiftPackageConfig.md#swiftbinpath) has been added to change the swift command used by the plugin.

This official tool [swiftly](https://www.swift.org/blog/introducing-swiftly_10/) has been recently added to easily install another version of swift on macOS.

So follow the swiftly guide to install another swift version and set the swiftBinPath property correctly.

```kotlin
swiftBinPath = "/path/to/.swiftly/bin/swift"
```

## Support concurrency in KMP iOS Test

If you're trying to test your code and your bridge contains async method, you will face a compiler error like

```
Failed to look up symbolic reference at 0x10497d0f5 - offset 342419 - symbol symbolic _____y___________pG ScC 4Nats10ServerInfoV s5ErrorP in .../debugTest/test.kexe
```

That's because the minimal target of the KMP test is too low, iOS 12 for physical device and iOS 14 for simulator.

Swift concurrency is available only from iOS 15.0, that can be fixed:

example :

```kotlin
iosSimulatorArm64().binaries.getTest("debug").apply {
freeCompilerArgs +=
    listOf(
        "-Xoverride-konan-properties=osVersionMin.ios_simulator_arm64=16.0",
    )
}
```


