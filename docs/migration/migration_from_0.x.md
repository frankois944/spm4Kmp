# Migration from 0.x to 1.0

This page lists all breaking changes introduced in 1.0.

---

## `copyDependenciesToApp` removed

This option has been deleted — its behavior is now the default.

The exported package is generated automatically when needed, or explicitly via the [`includeProduct`](../references/exportedPackageConfig.md#includeproduct) configuration.

## `isIncludedInExportedPackage` removed

Replaced by the [`includeProduct`](../references/exportedPackageConfig.md#includeproduct) configuration.

## `linkerOpts` and `compilerOpts` removed from dependency config

Per-dependency `linkerOpts` and `compilerOpts` are no longer available. Use the [root-level options](../references/swiftPackageConfig.md#linkeropts) on `swiftPackageConfig` instead.

## `SwiftDependency` removed from public API

The legacy `SwiftDependency` type has been removed. Use the DSL methods (`remotePackageVersion`, `remotePackageBranch`, etc.) available in the [`dependency {}`](../references/dependency/dependencyConfig.md) block.
