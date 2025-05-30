# Avoid the usage of the exported local package

**Introduced in version 0.10.0**

This major feature avoids the mandatory [usage of the exported local package](https://github.com/frankois944/spm4Kmp/discussions/108) by copying the necessary resources directly to the application.

## Requirement

### Gradle

Set the [`copyDependenciesToApp`](../references/swiftPackageConfig.md#copydependenciestoapp) at `true` inside your plugin configuration.

### Xcode

Call the following gradle task after the build of your KMP library inside the build phase of Xcode.

:**yourKotlinModule**:**cInteropName**CopyPackageResources

Example :

```
./gradlew :example:embedAndSignAppleFrameworkForXcode // build the library
./gradlew :example:nativeIosSharedCopyPackageResources // copy the SPM resources
```

#### Custom configuration

You can call this task with custom parameters and override the Xcode build environment.

```
./gradlew :example:nativeIosSharedCopyPackageResources \
                                -PPLATFORM_NAME=iphone \
                                -PARCHS=arm64 \
                                -PBUILT_PRODUCTS_DIR=... \
                                -PCONTENTS_FOLDER_PATH=... \
```

## Optional

You can control what's inside the exported package by setting [isIncludedInExportedPackage](https://frankois944.github.io/spm4Kmp/section-experimental/copyPackageResources/?q=isIncludedInExportedPackage) and exclude non needed package.

## Example

The [example project](https://github.com/frankois944/spm4Kmp/tree/main/example) uses this new feature, and you can take it as a reference for implementing in your project, especially the [Gradle file](https://github.com/frankois944/spm4Kmp/blob/437f6982a9dffa13ad9f1af7bea846a800cc685e/example/build.gradle.kts#L109-L152).


## Limitation

- It doesn't work with static frameworks (like FirebaseAnalytics).

You will have the error `Undefined symbol` error message and [need to include the local package](https://github.com/frankois944/spm4Kmp/blob/437f6982a9dffa13ad9f1af7bea846a800cc685e/example/build.gradle.kts#L114-L119).

- You lose the capability to use the external dependency inside your application code.

## Feedback

As this feature is experimental, create an issue if needed :) 