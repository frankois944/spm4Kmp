# ProductPackageConfig

## Example

```Kotlin
remotePackageVersion(
    url = URI("https://github.com/appmetrica/appmetrica-sdk-ios"),
    version = "5.0.0",
    products = {
        // Can be only used in your "src/swift" code.
        add("AppMetricaCore", exportToKotlin = true)
    },
)
// or
remotePackageVersion(
    url = URI("https://github.com/appmetrica/appmetrica-sdk-ios"),
    version = "5.0.0",
    products = {
        // Can be only used in your "src/swift" code.
        add(ProductName("AppMetricaCore"), exportToKotlin = true)
    },
)
```

## Add by ProductName

Adds one or more products to the product package configuration.

- **[products](./productName.md)** The products to be added. Each product is represented by a [ProductName],
  which includes details such as the name and an optional alias.
- **exportToKotlin** Determines whether the added products should be exported
  to Kotlin. Defaults to `false` if not specified.

```Kotlin
fun add(
    vararg products: ProductName,
    exportToKotlin: Boolean = false,
)
```

## Add by Names

Adds one or more product names to the product package configuration using their string representation.

- **names** The string representations of the product names to be added.
  Each string corresponds to the name of the product within the Swift package ecosystem.
- **exportToKotlin** Determines whether the added products should be exported
  to Kotlin. Defaults to `false` if not specified.

```Kotlin
fun add(
    vararg names: String,
    exportToKotlin: Boolean = false,
)
```
