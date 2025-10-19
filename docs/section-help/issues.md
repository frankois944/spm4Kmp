# Known Issues

## I have this "Undefined symbol" error during the build or a native crash during runtime.

You need to add your [native library to your Xcode project](../bridgeWithDependencies.md#automatic-dependency-build-inclusion).

## Failed to store cache entry

The error message `Failed to store cache entry` is a Gradle cache issue that [can't be fixed](https://github.com/frankois944/spm4Kmp/issues/89), but can only be avoided by disabling the cache.

```
org.gradle.caching=false
org.gradle.configuration-cache=false
```

## Error: module *_stddef* requires feature *found_incompatible_headers__check_search_paths*

This compilation error is due to an [incompatibility between Kotlin <= 2.1.20 and Xcode >= 16.3](https://youtrack.jetbrains.com/issue/KT-76460/Kotlin-Native-iOS-build-error-with-Xcode-16.3-iPhoneOS18.4.sdk-due-to-incorrectly-set-up-search-paths).

You have two choices:

- Use Xcode version 16.2 and earlier.
- Upgrade your Kotlin version to 2.1.21 and later.

If you encounter this error with a later version of Xcode/Kotlin, you **MUST** use the latest version of Kotlin.

## Failed to look up symbolic reference at 0x... - offset ... - symbol symbolic ... in .../debugTest/test.kexe or *Library not loaded: libswift_Concurrency.dylib*

This is an issue about using the bridge with an invalid version of swift.

More details [here](./tips.md#support-concurrency-in-kmp-ios-test)
