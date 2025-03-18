# SwiftDependency (Deprecated)

!!! warning "Please Be Aware"

    Will be removed on version 1.0.0 and replaced by [DependencyConfig](../swiftPackageConfig.md#dependency)

## SwiftDependency.Package

### Remote

#### Version

Represents a specific version of a remote Swift package to be used
as a dependency in a Kotlin Multiplatform project.

- url The URL of the remote Git repository where the package is hosted.
- products A list of the product's package used during dependency configuration.
- packageName The name of the package, by default base of the url.
- version The specific version of the Swift package to be imported .

``` kotlin
SwiftDependency.Package.Remote.Version(
    public override val url: URI,
    public override val packageName: String = buildPackageName(url),
    public val version : String,
    public override val products: ProductPackageConfig.() -> Unit,
)
```

#### Commit

Represents a specific remote commit dependency for a Swift Package.

- url The URL of the remote Git repository where the package is hosted.
- products A list of the product's package used during dependency configuration.
- packageName The name of the package, by default base of the url.
- revision A specific commit hash representing the dependency version .

``` kotlin
SwiftDependency.Package.Remote.Commit(
    public override val url: URI,
    public override val packageName: String = buildPackageName(url),
    public val revision : String,
    override val products: ProductPackageConfig.() -> Unit,
)
```

#### Branch

Represents a branch-based remote Swift dependency in a Kotlin Multiplatform project.

- url The URL of the remote Git repository where the package is hosted.
- products A list of the product's package used during dependency configuration.
- packageName The name of the package, by default base of the url.
- branch The branch name of the remote Git repository used for the dependency .

``` kotlin
SwiftDependency.Package.Remote.Branch(
    public override val url: URI,
    public override val packageName: String = buildPackageName(url),
    public val branch : String,
    override val products: ProductPackageConfig.() -> Unit,
)
```

### Local

Represents a local Swift package dependency .

- path The local file URL (file://...) to the local Swift package folder.
- packageName The name of the package, by default the first product name.
- products A list of the product's package used during dependency configuration.

``` kotlin
SwiftDependency.Package.Local(
    val path : String,
    override var packageName: String = "", // by default the first ProductName
    override val products: ProductPackageConfig.() -> Unit,
)
```

## Binary

### Local

Represents a local binary dependency in the Kotlin Multiplatform project .

[How to make one](https://www.avanderlee.com/swift/binary-targets-swift-package-manager/#local-binary-targets)

- path The local file URL (file://...) to the xcFramework.
- packageName The name of the package associated with this binary .
- exportToKotlin Defines whether the dependency should be exported for use in Kotlin code .

```
kotlin
SwiftDependency.Binary.Local(
    val path : String,
    override val packageName: String,
    override val exportToKotlin: Boolean = false,
    override var linkerOpts: List<String> = emptyList(),
    override var compilerOpts: List<String> = emptyList(),
),
```

### Remote

Represents a remote binary dependency as a zipped xcFramework

[How to make one](https://www.avanderlee.com/swift/binary-targets-swift-package-manager/#defining-a-binary-target)

- url The URL pointing to the remote binary artifact.
- packageName The name of the package associated with this binary dependency.
- exportToKotlin Defines whether this dependency should be exported for use in Kotlin code.
- checksum The checksum of the remote binary to verify its integrity.

``` kotlin
SwiftDependency.Binary.Remote(
    val url: URI,
    override val packageName: String,
    override val exportToKotlin: Boolean = false,
    val checksum: String,
    override var linkerOpts: List<String> = emptyList(),
    override var compilerOpts: List<String> = emptyList(),
)
```
