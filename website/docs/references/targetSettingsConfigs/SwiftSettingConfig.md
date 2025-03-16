# SwiftSettingConfig

[Swift Documentation](https://developer.apple.com/documentation/packagedescription/swiftsetting)

## defines

Defines a compilation condition.

``` kotlin
public var defines: List<String>
```

## unsafeFlags

Set unsafe flags to pass arbitrary command-line flags to the corresponding build tool.

``` kotlin
public var unsafeFlags: List<String>
```

## enableExperimentalFeature

Enable an experimental feature with the given name.

``` kotlin
public var enableExperimentalFeature: String?
```

## enableUpcomingFeature

Enable an upcoming feature with the given name.

``` kotlin
public var enableUpcomingFeature: String?
```

## swiftLanguageMode

Defines a -language-mode to pass to the corresponding build tool.

A user-defined value for the Swift version: 4, 5, 6

Available on swift-tools-version > 6.0

[Reference](https://developer.apple.com/documentation/packagedescription/swiftlanguagemode)

``` kotlin
public var swiftLanguageMode: String?
```

## interoperabilityMode

Enable Swift interoperability with a given language.

available values : C, Cxx

``` kotlin
public var interoperabilityMode: String?
```

