# Frequently Asked Questions

Common questions about Swift Package Manager integration, Kotlin Multiplatform interoperability, and troubleshooting.

---

## What is a "Pure Swift" Package? {#whats-a-pure-swift-package}

A **Pure Swift** package is one written entirely in Swift without Objective-C headers or compatibility layers. This accounts for approximately 90% of modern Swift packages.

!!! failure "Interoperability Limitation"
    Kotlin Multiplatform (KMP) does not currently support direct Swift interoperability. It relies on the Objective-C runtime for communication between languages.

### Why does this matter?
Unlike the seamless integration between Kotlin and Java, Swift code must be [explicitly marked for Objective-C compatibility](https://www.hackingwithswift.com/example-code/language/what-is-the-objc-attribute) (using `@objc` or `@objcMembers`) to be visible to Kotlin.

!!! info "Key Points"
    - Many modern library authors prefer Swift-only APIs and do not include Objective-C compatibility.
    - Older libraries (like Firebase) often maintain Objective-C compatibility, making them easier to export directly.
    - You can check the **Languages** section on a repository's GitHub page to see if it's "Pure Swift."

---

## Why are only `SWIFT_TYPEDEFS` or `swift_...` visible in Kotlin? {#when-exporting-a-product-i-have-only-swift_typedefs-or-swift_-available-in-my-kotlin-code}

If you export a product and see only generic Swift type definitions in your Kotlin code, it typically means the product has no Objective-C compatible interface.

### How to verify
During compilation, the Swift compiler generates an Objective-C header containing only the compatible code. If this header is empty (save for boilerplate), the product is not directly accessible.

You can inspect the generated module maps here:
```bash
# Framework-based products
[project]/build/spmKmpPlugin/[cinteropName]/scratch/release/[product].framework/Modules/module.modulemap

# Static library products
[project]/build/spmKmpPlugin/[cinteropName]/scratch/release/[product].build/module.modulemap
```

!!! tip "The Solution"
    If a package is "Pure Swift," you cannot export it directly to Kotlin. Instead, you must create a **Swift Bridge** (a local Swift file) that wraps the package's functionality using `@objc` compatible classes and methods.

    See the [Bridge Documentation](../bridge.md) for more details.

---

## How do I handle native crashes or "Undefined symbol" errors?

These errors often occur when the native binary is not correctly linked or included in the final artifact.

!!! quote "Recommendation"
    Ensure your native library is correctly added to your Xcode project. For automatic inclusion, refer to the [Automatic Dependency Build Inclusion](../bridgeWithDependencies.md#automatic-dependency-build-inclusion) guide.

---

## Is Swift Concurrency supported?

Yes, but it requires a minimum deployment target of **iOS 15.0** or higher.

!!! warning "Common Error"
    If you see `Library not loaded: libswift_Concurrency.dylib` during tests, it's because the test runner is defaulting to an older iOS version. Check the [Concurrency Tip](./tips.md#support-concurrency-in-kmp-ios-test) for the fix.

---

## Why is my IDE slow after adding a Swift Package?

By default, some IDEs attempt to resolve `Package.swift` manifests automatically, which can lead to high CPU usage and wasted disk space.

!!! tip "Optimization"
    We recommend disabling **Sync Project after changes in the build script** in your IDE settings. For detailed steps, see the [Performance Tips](./tips.md#disable-swift-package-automatic-ide-resolution).
