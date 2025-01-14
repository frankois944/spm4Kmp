package io.github.frankois944.spmForKmp.definition.helpers

import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.definition.product.dsl.ProductPackageConfigImpl

internal fun List<SwiftDependency>.filterExportableDependency(): List<SwiftDependency> =
    buildList {
        this@filterExportableDependency.forEach { swiftDependency ->
            when (swiftDependency) {
                is SwiftDependency.Binary -> {
                    if (swiftDependency.exportToKotlin) add(swiftDependency)
                }

                is SwiftDependency.Package.Local -> {
                    val filteredProducts = swiftDependency.productsConfig.productPackages.filter { it.exportToKotlin }
                    if (filteredProducts.isNotEmpty()) {
                        val newConfig = ProductPackageConfigImpl(filteredProducts.toMutableList())
                        add(swiftDependency.copy().also { it.productsConfig = newConfig })
                    }
                }

                is SwiftDependency.Package.Remote -> {
                    addFilteredRemotePackage(swiftDependency)
                }
            }
        }
    }

private fun MutableList<SwiftDependency>.addFilteredRemotePackage(remotePackage: SwiftDependency.Package.Remote) {
    val filteredProducts = remotePackage.productsConfig.productPackages.filter { it.exportToKotlin }
    if (filteredProducts.isNotEmpty()) {
        val newConfig = ProductPackageConfigImpl(filteredProducts.toMutableList())
        when (remotePackage) {
            is SwiftDependency.Package.Remote.Version ->
                add(
                    remotePackage.copy().also { it.productsConfig = newConfig },
                )
            is SwiftDependency.Package.Remote.Branch -> add(remotePackage.copy().also { it.productsConfig = newConfig })
            is SwiftDependency.Package.Remote.Commit -> add(remotePackage.copy().also { it.productsConfig = newConfig })
        }
    }
}
