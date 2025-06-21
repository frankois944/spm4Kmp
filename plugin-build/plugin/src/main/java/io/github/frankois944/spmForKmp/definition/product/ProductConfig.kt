package io.github.frankois944.spmForKmp.definition.product

import java.io.Serializable

public class ProductConfig internal constructor(
    public val products: List<ProductName>,
    public val exportToKotlin: Boolean = false,
) : Serializable {
    internal companion object {
        private const val serialVersionUID: Long = 2
    }
}
