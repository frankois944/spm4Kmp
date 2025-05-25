package io.github.frankois944.spmForKmp.definition.product.dsl

import io.github.frankois944.spmForKmp.definition.product.ProductConfig
import io.github.frankois944.spmForKmp.definition.product.ProductName
import java.io.Serializable

public interface ProductPackageConfig : Serializable {
    public val productPackages: MutableList<ProductConfig>

    /**
     * Adds one or more products to the product package configuration.
     *
     * @param products The products to be added. Each product is represented by a [ProductName],
     * which includes details such as the name and an optional alias.
     * @param exportToKotlin Determines whether the added products should be exported
     * to Kotlin. Defaults to `false` if not specified.
     * @param isIncludedInExportedPackage if false, the products will be ignored inside the local package.
     */
    public fun add(
        vararg products: ProductName,
        exportToKotlin: Boolean = false,
        isIncludedInExportedPackage: Boolean = true,
    )

    /**
     * Adds one or more product names to the product package configuration using their string representation.
     *
     * @param names The string representations of the product names to be added.
     * Each string corresponds to the name of the product within the Swift package ecosystem.
     * @param exportToKotlin Determines whether the added products should be exported
     * to Kotlin. Defaults to `false` if not specified.
     * @param isIncludedInExportedPackage if false, the products will be ignored inside the local package.
     */
    public fun add(
        vararg names: String,
        exportToKotlin: Boolean = false,
        isIncludedInExportedPackage: Boolean = true,
    )
}
