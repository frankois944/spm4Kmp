package io.github.frankois944.spmForKmp.definition.product

import io.github.frankois944.spmForKmp.utils.ExperimentalAPI
import java.io.Serializable

/**
 * Represents the name of a product, with an optional alias and more to come.
 *
 * @property name The name of the product within the Swift package ecosystem. This is required for proper
 * identification and resolution of the dependency.
 * @property alias An optional alias that can be used as an alternate reference to the product.
 * Some Package use indirect name for a product.
 * @property linkerOpts Add custom linker flag when exporting the product to kotlin
 * @property compilerOpts Add custom compiler flag when exporting the product to kotlin
 * @property copyResourcesToApp Copy the product's resources into the application (only when running from xcode); also, it won't be included inside the local package.
 */
public data class ProductName(
    val name: String,
    val alias: String? = null,
    var linkerOpts: List<String> = emptyList(),
    var compilerOpts: List<String> = emptyList(),
    @property:ExperimentalAPI
    var copyResourcesToApp: Boolean = false
) : Serializable {
    internal companion object {
        private const val serialVersionUID: Long = 3
    }
}
