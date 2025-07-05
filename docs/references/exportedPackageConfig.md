# ExportedPackageConfig

## includeProduct

Manually include inside the exported local package the following Products.

```kotlin
var includeProduct: List<String> = emptyList()
```

### Example

```kotlin
swiftPackageConfig {
    create("dummy") {
        // Manually include the KeychainAccess library
        exportedPackageSettings { includeProduct = listOf("KeychainAccess") }
        dependency {
            remotePackageBranch(
                url = URI("https://github.com/krzyzanowskim/CryptoSwift.git"),
                branch = "main",
                products = {
                    add(
                        ProductName(
                            "CryptoSwift"
                        ), // will not be included inside the exported package
                    )
                },
            )
            remotePackageBranch(
                url =
                    URI(
                        "https://github.com/kishikawakatsumi/KeychainAccess.git"
                    ), // will be included inside the exported package
                products = { add("KeychainAccess") },
                branch = "master",
            )
        }
    }
}
```

## isStatic

Set the compiled package way, static or dynamic.

By default, static = true.

```kotlin
var isStatic = true
```

## name

Set the exported product name

By default, `exported[cinteropName]`

```kotlin
var name: String? = null
```
