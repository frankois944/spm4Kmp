package io.github.frankois944.spmForKmp.definition

import java.io.Serializable

@ConsistentCopyVisibility
public data class ProductPackageConfig private constructor(
    public val names: List<ProductName>,
    public val exportToKotlin: Boolean = false,
) : Serializable {
    /**
     * A list of products with customizable configuration
     *
     * @property names a list of products with parameters
     * @property exportToKotlin Indicates whether these products should be exposed and made available for usage
     * in Kotlin code.
     **/
    public constructor(
        vararg names: ProductName,
        exportToKotlin: Boolean = false,
    ) : this(names = names.toList(), exportToKotlin)

    /**
     * A list of products using the default configuration
     *
     * @property names a list of products by name; it uses the default configuration
     * @property exportToKotlin Indicates whether these products should be exposed and made available for usage
     * in Kotlin code.
     *
     **/
    public constructor(
        vararg names: String,
        exportToKotlin: Boolean = false,
    ) : this(names = names.map { ProductName(it) }, exportToKotlin)

    internal companion object {
        private const val serialVersionUID: Long = 1
    }
}

/**
 * Represents the name of a product, with an optional alias and more to come.
 *
 * @property name The name of the product within the Swift package ecosystem. This is required for proper
 * identification and resolution of the dependency.
 * @property alias An optional alias that can be used as an alternate reference to the product.
 * Some Package use indirect name for a product.
 *
 * For example:
 *
 * - Name : FirebaseAppDistribution
 * - Alias : FirebaseAppDistribution-Beta
 */
public data class ProductName(
    val name: String,
    val alias: String? = null,
    // more to come
) : Serializable {
    internal companion object {
        private const val serialVersionUID: Long = 1
    }
}
