# ProductName

## ProductName

Represents the name of a product, with an optional alias and more to come.

- **name** The name of the product within the Swift package ecosystem. This is required for proper
  identification and resolution of the dependency.
- **alias** An optional alias that can be used as an alternate reference to the product.
  Some Package use indirect name for a product.

```Kotlin
data class ProductName(
    val name: String,
    val alias: String? = null,
) : Serializable
```
