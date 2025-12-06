# Use Swift Package Registry

Since version 1.2.0, importing a package from a registry is supported.

Learn more [about package registry](https://docs.swift.org/swiftpm/documentation/packagemanagerdocs/usingswiftpackageregistry/).

## Requirement

- Add the server [configuration](../references/swiftPackageConfig.md#registry) depending on your settings.
- Add the [dependency](../references/dependency/dependencyConfig.md#registrypackage).

## Example

```kotlin
iosArm64 {
    swiftPackageConfig {
        registry(
            url = uri("https://your/server/configuration")
            token = "YourCredential",
        )
        dependency {
            registryPackage(
                id = "spm.dummy",
                version = "1.0.1",
                products = {
                    add("registrydummy")
                },
            )
        }
    }
}
```
