package io.github.frankois944.spmForKmp.definition.product.dsl

import io.github.frankois944.spmForKmp.definition.product.ProductConfig
import io.github.frankois944.spmForKmp.definition.product.ProductName
import java.io.Serializable

internal data class ProductPackageConfigImpl(
    override var productPackages: MutableList<ProductConfig> = mutableListOf(),
) : ProductPackageConfig,
    Serializable {
    override fun add(
        vararg products: ProductName,
        exportToKotlin: Boolean,
    ) {
        this.productPackages.add(
            ProductConfig(
                products = products.toList(),
                exportToKotlin = exportToKotlin,
            ),
        )
    }

    override fun add(
        vararg names: String,
        exportToKotlin: Boolean,
    ) {
        this.productPackages.add(
            ProductConfig(
                products =
                    names.map {
                        ProductName(
                            it,
                        )
                    },
                exportToKotlin = exportToKotlin,
            ),
        )
    }

    internal companion object {
        private const val serialVersionUID: Long = 4
    }
}
