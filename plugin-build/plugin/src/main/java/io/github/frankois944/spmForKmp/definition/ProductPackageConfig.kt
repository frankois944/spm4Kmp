package io.github.frankois944.spmForKmp.definition

import java.io.Serializable

/**
 * Represents the configuration for a product's package used in a Kotlin Multiplatform project.
 *
 * @property name The name of the product within the Swift package ecosystem. This is required for proper
 * identification and resolution of the dependency.
 * @property alias An optional alias that can be used as an alternate reference to the product.
 * This can be useful for avoiding naming conflicts or providing more contextually relevant names.
 * @property exportToKotlin Indicates whether this product should be exposed and made available for usage
 * in Kotlin code. Defaults to `false`.
 */
public data class ProductPackageConfig(
    val name: String,
    val exportToKotlin: Boolean = false,
    val alias: String? = null,
) : Serializable {
    internal companion object {
        private const val serialVersionUID: Long = 1
    }
}
