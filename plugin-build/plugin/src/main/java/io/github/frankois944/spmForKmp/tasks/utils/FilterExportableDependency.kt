package io.github.frankois944.spmForKmp.tasks.utils

import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.definition.product.dsl.ProductPackageConfigImpl

internal fun List<SwiftDependency>.filterExportableDependency(): List<SwiftDependency> =
    buildList(size) {
        this@filterExportableDependency.forEach { swiftDependency ->
            when (swiftDependency) {
                is SwiftDependency.Binary -> {
                    if (swiftDependency.exportToKotlin) add(swiftDependency)
                }

                is SwiftDependency.Package.Local -> {
                    val filteredProducts =
                        swiftDependency.productsConfig.productPackages.filterTo(ArrayList()) { it.exportToKotlin }
                    if (filteredProducts.isNotEmpty()) {
                        val newConfig = ProductPackageConfigImpl(filteredProducts)
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
    val filteredProducts = remotePackage.productsConfig.productPackages.filterTo(ArrayList()) { it.exportToKotlin }
    if (filteredProducts.isNotEmpty()) {
        val newConfig = ProductPackageConfigImpl(filteredProducts)
        add(
            when (remotePackage) {
                is SwiftDependency.Package.Remote.Version -> remotePackage.copy()
                is SwiftDependency.Package.Remote.Branch -> remotePackage.copy()
                is SwiftDependency.Package.Remote.Commit -> remotePackage.copy()
                is SwiftDependency.Package.Remote.Registry -> remotePackage.copy()
            }.also { it.productsConfig = newConfig },
        )
    }
}
