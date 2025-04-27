# DependencyConfig

## Example

```Kotlin
dependency {
    remotePackageVersion(
        url = URI("https://github.com/appmetrica/appmetrica-sdk-ios"),
        version = "5.0.0",
        products = {
            add("AppMetricaCore", exportToKotlin = true)
        },
    )
}
```

## Binary Packages

### localBinary

Represents a local binary dependency.

<a href="https://www.avanderlee.com/swift/binary-targets-swift-package-manager/#local-binary-targets">How to make
one</a>

* **path** The local file URL (file://...) to the xcFramework.
* **packageName** The name of the package associated with this binary.
* **exportToKotlin** Defines whether the dependency should be exported for use in Kotlin code.
* **linkerOpts**
* **compilerOpts**

```kotlin
fun localBinary(
    path: String,
    packageName: String,
    exportToKotlin: Boolean = false,
    linkerOpts: List<String> = emptyList(),
    compilerOpts: List<String> = emptyList(),
)
```

### remoteBinary

Represents a remote binary dependency as a zipped xcFramework

[How to make one](https://www.avanderlee.com/swift/binary-targets-swift-package-manager/#defining-a-binary-target)

* **url** The URL pointing to the remote binary artifact.
* **packageName** The name of the package associated with this binary dependency.
* **exportToKotlin** Defines whether this dependency should be exported for use in Kotlin code.
* **checksum** The checksum of the remote binary to verify its integrity.
* **linkerOpts**
* **compilerOpts**

```kotlin
fun remoteBinary(
    url: URI,
    packageName: String,
    exportToKotlin: Boolean = false,
    checksum: String,
    linkerOpts: List<String> = emptyList(),
    compilerOpts: List<String> = emptyList(),
)
```

## Local & Remote Package

### localPackage

Represents a local Swift package dependency.

* **path** The local file URL (file://...) to the local Swift package folder.
* **packageName** The name of the package, by default the first product name.
* **[products](productPackageConfig.md)** A list of the product's package used during dependency configuration.

```kotlin
fun localPackage(
    path: String,
    packageName: String = "",
    products: ProductPackageConfig.() -> Unit,
)
```

### remotePackageVersion

Represents a specific version of a remote Swift package.a

* **url** The URL of the remote Git repository where the package is hosted.
* **packageName** The name of the package, by default base of the url.
* **version** The specific version of the Swift package to be imported.
* **[products](productPackageConfig.md)** A list of the product's package used during dependency configuration.

```kotlin
fun remotePackageVersion(
    url: URI,
    packageName: String = "",
    version: String,
    products: ProductPackageConfig.() -> Unit,
)
```

### remotePackageBranch

Represents a branch-based remote Swift dependency.

* **url** The URL of the remote Git repository where the package is hosted.
* **packageName** The name of the package, by default base of the url.
* **branch** The branch name of the remote Git repository used for the dependency.
* **[products](productPackageConfig.md)** A list of the product's package used during dependency configuration.

```kotlin
fun remotePackageBranch(
    url: URI,
    packageName: String = "",
    branch: String,
    products: ProductPackageConfig.() -> Unit,
)
```

### remotePackageCommit

Represents a specific remote commit dependency.

* **url** The URL of the remote Git repository where the package is hosted.
* **packageName** The name of the package, by default base of the url.
* **revision** A specific commit hash representing the dependency version.
* **[products](productPackageConfig.md)** A list of the product's package used during dependency configuration.

```kotlin
fun remotePackageCommit(
    url: URI,
    packageName: String = "",
    revision: String,
    products: ProductPackageConfig.() -> Unit,
)
```




