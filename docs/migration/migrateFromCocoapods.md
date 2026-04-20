# Migrate from the CocoaPods Plugin to the SPM Plugin

This guide covers the essential steps to migrate from the Kotlin CocoaPods plugin to the `spmForKmp` plugin.

!!! info "Diff notation"
    - Lines prefixed with `--` should be **removed**
    - Lines prefixed with `++` should be **added**

---

## Gradle

### `libs.versions.toml`

``` toml linenums="1" hl_lines="5 11 12" title="libs.versions.toml"
[versions]
compose-multiplatform = "1.8.0"
kotlin = "2.2.20"
firebaseIOS = "12.0.0"
++ spmForKmp = "1.0.4"

[plugins]
composeMultiplatform = { id = "org.jetbrains.compose", version.ref = "compose-multiplatform" }
composeCompiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlinMultiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
-- kotlinCocoapods = { id = "org.jetbrains.kotlin.native.cocoapods", version.ref = "kotlin" }
++ spmForKmp = { id = "io.github.frankois944.spmForKmp", version.ref = "spmForKmp" }
```

### Root `build.gradle.kts`

```kotlin linenums="1" hl_lines="5 6" title="build.gradle.kts"
plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.kotlinMultiplatform) apply false
    -- alias(libs.plugins.kotlinCocoapods).apply(false)
    ++ alias(libs.plugins.spmForKmp).apply(false)
    alias(libs.plugins.composeMultiplatform).apply(false)
    alias(libs.plugins.composeCompiler).apply(false)
}
```

### Module `build.gradle.kts`

Replace the `cocoapods {}` block with `swiftPackageConfig {}` for each target:

``` kotlin linenums="1" hl_lines="3 4 11-26 28-50" title="myKMPApp/build.gradle.kts"
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    -- alias(libs.plugins.kotlinCocoapods)
    ++ alias(libs.plugins.spmForKmp)
}

group = "org.jetbrains.kotlin.shared"
version = "1.0-SNAPSHOT"

kotlin {
    - iosArm64()
    - iosSimulatorArm64()
    - cocoapods {
    -    summary = "Kotlin module with cocoapods dependencies"
    -    ios.deploymentTarget = "16.6"
    -    pod("FirebaseCore") {
    -        version = libs.versions.firebaseIOS.get()
    -    }
    -    pod("FirebaseAnalytics") {
    -        version = libs.versions.firebaseIOS.get()
    -    }
    -    pod("CryptoSwift") {
    -        version = "1.2.3"
    -        linkOnly = true
    -    }
    - }

    + listOf(
    +    iosArm64(),
    +    iosSimulatorArm64()
    + ).forEach {
    +    it.swiftPackageConfig(cinteropName = "nativeBridge") {
    +        minIos = "16.6"
    +        dependency {
    +            remotePackageVersion(
    +                url = uri("https://github.com/firebase/firebase-ios-sdk.git"),
    +                products = {
    +                    add("FirebaseCore", "FirebaseAnalytics", exportToKotlin = true)
    +                },
    +                version = libs.versions.firebaseIOS.get(),
    +            )
    +            remotePackageVersion(
    +                url = uri("https://github.com/krzyzanowskim/CryptoSwift"),
    +                products = {
    +                    add("CryptoSwift")
    +                },
    +                version = "1.2.3",
    +            )
    +        }
    +    }
    + }
}
```

---

## Xcode

### Remove CocoaPods Integration

From your iOS app folder, run:

``` title="Terminal"
pod deintegrate
```

### Add a New Build Phase

In the **Build Phases** tab of your Xcode project:

1. Click **+** and choose **New Run Script Phase**
2. Drag it to the **top** of the phase list
3. Paste the following script:

``` bash title="Run Script"
cd "$SRCROOT/../../"
./gradlew :shared:embedAndSignAppleFrameworkForXcode
```

### Update Build Settings

In the **Build Settings** tab, set or update the following:

| Setting | Value |
| --- | --- |
| **Other Linker Flags** | `-framework shared` |
| **Framework Search Paths** | `$(SRCROOT)/../build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)` |

---

## Kotlin Code

Update import statements — the `cocoapods` package prefix is removed:

```diff
-- import cocoapods.FireBaseCore.FIRApp
++ import FireBaseCore.FIRApp
```

!!! tip
    The original prefix can be restored by configuring [`packageDependencyPrefix`](../references/swiftPackageConfig.md#packagedependencyprefix).

---

## Troubleshooting

If you run into dependency resolution issues or crash in Xcode, you may need to include the native dependency directly in your Xcode project. See [Automatic Dependency Build Inclusion](../bridgeWithDependencies.md#automatic-dependency-build-inclusion) for details.
