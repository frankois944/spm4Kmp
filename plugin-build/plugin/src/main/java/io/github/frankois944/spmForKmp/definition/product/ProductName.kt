package io.github.frankois944.spmForKmp.definition.product

import java.io.Serializable

/**
 * Represents the name of a product, with an optional alias and more to come.
 *
 * @property name The name of the product within the Swift package ecosystem. This is required for proper
 * identification and resolution of the dependency.
 * @property alias An optional alias that can be used as an alternate name to the product.
 * Import Clang modules where the name doesn’t match the product name inside the manifest.
 *
 * Ex: `ProductName("FirebaseFirestoreInternal", alias = "FirebaseFirestore")`
 *
 */
public data class ProductName(
    val name: String,
    val alias: String? = null,
) : Serializable {
    internal companion object {
        private const val serialVersionUID: Long = 5
    }
}
