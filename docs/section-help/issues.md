# Known Issues & Troubleshooting

Solutions for common errors, build failures, and environment-specific issues.

---

## "Undefined symbol" or Native Runtime Crashes {#undefined-symbol}

These errors typically occur when a native library is missing from the linking phase or isn't properly included in the final Xcode artifact.

!!! failure "Common Symptoms"
    - Build failure with `Undefined symbol: _OBJC_CLASS_$_...`
    - Runtime crash with `Library not loaded: @rpath/...`

!!! success "The Fix"
    Ensure your native library is explicitly added to your Xcode project. For most cases, we recommend using the [Automatic Dependency Build Inclusion](../bridgeWithDependencies.md#automatic-dependency-build-inclusion) feature to handle this automatically.

---

## Failed to Store Cache Entry {#failed-to-store-cache-entry}

This is a known Gradle caching issue that can occur in certain environments.

!!! warning "Status: Unfixable"
    This issue is related to internal Gradle cache management and [cannot be directly fixed](https://github.com/frankois944/spm4Kmp/issues/89) by the plugin.

!!! tip "Workaround"
    The most reliable way to avoid this error is to disable the Gradle build cache and configuration cache in your `gradle.properties`:
    ```properties
    org.gradle.caching=false
    org.gradle.configuration-cache=false
    ```

---

## Module `_stddef` requires feature `found_incompatible_headers...` {#stddef-incompatibility}

This compilation error stems from a specific incompatibility between Kotlin and newer versions of Xcode/iOS SDKs.

!!! info "Root Cause"
    There is a known conflict between **Kotlin <= 2.1.20** and **Xcode >= 16.3 (iPhoneOS 18.4 SDK)** regarding search path configurations.

### Solutions
Choose one of the following:

- **Upgrade:** Move to **Kotlin 2.1.21** or later (Recommended).
- **Downgrade:** Use **Xcode 16.2** or earlier.

---

## `libswift_Concurrency.dylib` not loaded / Symbolic Reference Error {#concurrency-errors}

This error occurs when trying to use Swift `async/await` features in an environment with an insufficient minimum deployment target.

!!! example "Error Message"
    `Failed to look up symbolic reference at 0x... in .../test.kexe`
    OR
    `Library not loaded: libswift_Concurrency.dylib`

!!! success "The Fix"
    Increase the minimum OS version for your test binaries to **iOS 15.0** or higher. See the [Concurrency Troubleshooting Tip](./tips.md#support-concurrency-in-kmp-ios-test) for specific configuration steps.

---

## Issues with Multiple Configurations on the Same Target {#multiple-configurations}

While the plugin supports applying multiple configurations to a single target, doing so can introduce stability and performance issues.

!!! danger "Potential Risks"
    - **Increased Build Times**: Each configuration creates its own isolated workspace, significantly prolonging the build process.
    - **Workspace Conflicts**: Simultaneous configurations can lead to "missing file" errors due to caching conflicts.

!!! tip "Best Practice"
    Whenever possible, consolidate your configurations or use separate targets to avoid these complications.
