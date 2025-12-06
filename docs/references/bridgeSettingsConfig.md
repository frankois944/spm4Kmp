# BridgeSettingsConfig


## Example

```kotlin
bridgeSettings {
    cSetting {
        defines = listOf(Pair("C_DEBUG", "2"))
        headerSearchPath = listOf("./includes/")
        unsafeFlags = listOf("-W")
    }
    cxxSetting {
        defines = listOf(Pair("CXX_DEBUG", "1"))
        headerSearchPath = listOf("./includes/")
        unsafeFlags = listOf("-W")
    }
    linkerSetting {
        linkedFramework = listOf("UIKit")
        linkedLibrary = listOf("-W")
        unsafeFlags = listOf("-W")
    }
    swiftSettings {
        defines = listOf("CUSTOM_DEFINE")
        enableExperimentalFeature = "CImplementation"
        enableUpcomingFeature = "ExistentialAny"
        interoperabilityMode = "Cxx"
    }
}
```

## cSetting

The target’s C build settings.

[Swift Reference](https://developer.apple.com/documentation/packagedescription/target/csettings)

```kotlin
public fun cSetting(setting: CSettingConfig.() -> Unit)
```

## cxxSetting

The target’s C++ build settings.

[Swift Reference](https://developer.apple.com/documentation/packagedescription/target/cxxsettings)

```kotlin
public fun cxxSetting(setting: CxxSettingConfig.() -> Unit)
```

## swiftSettings

The target’s Swift build settings.

[Swift Reference](https://developer.apple.com/documentation/packagedescription/target/swiftsettings)

```kotlin
public fun swiftSettings(setting: SwiftSettingConfig.() -> Unit)
```

## linkerSetting

The target’s linker settings.

[Swift Reference](https://developer.apple.com/documentation/packagedescription/target/linkersettings)

```kotlin
public fun linkerSetting(setting: LinkerSettingConfig.() -> Unit)
```
