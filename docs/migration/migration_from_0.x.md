# Migration from versions < 1.0

- `copyDependenciesToApp` has been **deleted** and is now the default behavior.

The exported package is now generated when needed or when you explicitly need it with the [includeProduct](../references/exportedPackageConfig.md#includeproduct) configuration.

- `isIncludedInExportedPackage` has been **deleted**

Replaced by [includeProduct](../references/exportedPackageConfig.md#includeproduct) configuration.

- The configuration `linkerOpts` and `compilerOpts` has been removed from dependency configuration, only the [root one are available.](../references/swiftPackageConfig.md#linkeropts)

- Legacy `SwiftDependency` has been removed from public api.
