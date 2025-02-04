# Multi target configuration


## Configuration by target

You can set a different configuration for each target you manage.

``` kotlin title="build.gradle.kts"
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
