# ExportedPackageConfig

## includeProduct

Manually include inside the exported local package the following Products.

```kotlin
var includeProduct: List<String> = emptyList()
```

### Example


!!! warning

    When using `includeProduct`, a local package is created, and you will see the following message in the build output.
    ```
    Spm4Kmp: The following dependencies [some_dependency_name] need to be added to your xcode project
    A local Swift package has been generated at
    /path/to/the/local/package
    Please add it to your xcode project as a local package dependency; it will add the missing content.
    ****You can ignore this messaging if you have already added these dependencies to your Xcode project****
    ```

```kotlin
kotlin {
    iosArm64 {
        swiftPackageConfig {
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
