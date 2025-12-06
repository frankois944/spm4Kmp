# Migrate From The Cocoapods Plugin To The Swift Package Manager Plugin


This is a short guide for doing a quick migration from the Cocoapods KMP plugin to spmForKmp Plugin, it's **covering only the basics**.

* -- : removed lines
* ++ : added lines

## Gradle

### Toml

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

### Build.kts

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
    -     }
    -     pod("FirebaseAnalytics") {
    -         version = libs.versions.firebaseIOS.get()
    -     }
    -     pod("CryptoSwift") {
    -        version = "1.2.3"
    -        linkOnly = true
    -     }
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
}
```


## Xcode

### Remove CocoaPods Integration

- From your iOSApp folder :

``` title="run command"
pod deintegrate
```
### Add New Build Phase

From the *Build Phases* tab of your project, create a `New Run Script Phase` and put it at the **top** of the list.

Add inside the newly created script:

``` title="Run script"
cd "$SRCROOT/../../"
./gradlew :shared:embedAndSignAppleFrameworkForXcode
```

### Update The Build Settings

From the *Build Settings* tab of your project, add or Update the following settings :

``` title="Other Linker Flags "
-framework shared
```
``` title="Framework Search Paths"
$(SRCROOT)/../build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)
```

## Kotlin Code

```
-- import cocoapods.FireBaseCore.FIRApp
++ import FireBaseCore.FIRApp
```

Basicaly every `cocoapods` package prefix has been removed but could be recovered by setting [packageDependencyPrefix](../references/swiftPackageConfig.md#packagedependencyprefix).
