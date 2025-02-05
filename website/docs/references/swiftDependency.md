# SwiftDependency

## SwiftDependency.Package

### Remote

#### Version

``` kotlin
/**
 * Represents a specific version of a remote Swift package to be used
 * as a dependency in a Kotlin Multiplatform project.
 *
 * @property url The URL of the remote Git repository where the package is hosted.
 * @property products A list of the product's package used during dependency configuration.
 * @property packageName The name of the package, by default base of the url.
 * @property version The specific version of the Swift package to be imported.
 */
SwiftDependency.Package.Remote.Version(
    public override val url: URI,
    public override val packageName: String = buildPackageName(url),
    public val version: String,
    public override val products: ProductPackageConfig.() -> Unit,
)
```

#### Commit

``` kotlin
/**
 * Represents a specific remote commit dependency for a Swift Package.
 *
 * @property url The URL of the remote Git repository where the package is hosted.
 * @property products A list of the product's package used during dependency configuration.
 * @property packageName The name of the package, by default base of the url.
 * @property revision A specific commit hash representing the dependency version.
 */
SwiftDependency.Package.Remote.Commit(
    public override val url: URI,
    public override val packageName: String = buildPackageName(url),
    public val revision: String,
    override val products: ProductPackageConfig.() -> Unit,
)
```

#### Branch

``` kotlin
/**
 * Represents a branch-based remote Swift dependency in a Kotlin Multiplatform project.
 *
 * @property url The URL of the remote Git repository where the package is hosted.
 * @property products A list of the product's package used during dependency configuration.
 * @property packageName The name of the package, by default base of the url.
 * @property branch The branch name of the remote Git repository used for the dependency.
 */
SwiftDependency.Package.Remote.Branch(
    public override val url: URI,
    public override val packageName: String = buildPackageName(url),
    public val branch: String,
    override val products: ProductPackageConfig.() -> Unit,
),
```

### Local

``` kotlin
/**
 * Represents a local Swift package dependency.
 *
 * @property path The local file URL (file://...) to the local Swift package folder.
 * @property packageName The name of the package, by default base of the url.
 * @property products A list of the product's package used during dependency configuration.
 */
SwiftDependency.Package.Local(
    val path: String,
    override var packageName: String = "", // by default the first ProductName
    override val products: ProductPackageConfig.() -> Unit,
),
```

## Binary

### Local

``` kotlin
/**
 * Represents a local binary dependency in the Kotlin Multiplatform project.
 *
 * @see <a href="https://www.avanderlee.com/swift/binary-targets-swift-package-manager/#local-binary-targets">How to make one</a>
 *
 * @property path The local file URL (file://...) to the xcFramework.
 * @property packageName The name of the package associated with this binary.
 * @property exportToKotlin Defines whether the dependency should be exported for use in Kotlin code.
 */
SwiftDependency.Binary.Local(
    val path: String,
    override val packageName: String,
    override val exportToKotlin: Boolean = false,
    override var linkerOpts: List<String> = emptyList(),
    override var compilerOpts: List<String> = emptyList(),
),
```

### Remote

``` kotlin
/**
 * Represents a remote binary dependency as a zipped xcFramework
 *
 * @see <a href="https://www.avanderlee.com/swift/binary-targets-swift-package-manager/#defining-a-binary-target">How to make one</a>
 *
 * @property url The URL pointing to the remote binary artifact.
 * @property packageName The name of the package associated with this binary dependency.
 * @property exportToKotlin Defines whether this dependency should be exported for use in Kotlin code.
 * @property checksum The checksum of the remote binary to verify its integrity.
 */
SwiftDependency.Binary.Remote(
    val url: URI,
    override val packageName: String,
    override val exportToKotlin: Boolean = false,
    val checksum: String,
    override var linkerOpts: List<String> = emptyList(),
    override var compilerOpts: List<String> = emptyList(),
)
```
