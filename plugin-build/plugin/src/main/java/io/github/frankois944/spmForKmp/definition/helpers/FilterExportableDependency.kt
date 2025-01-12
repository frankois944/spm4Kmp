package io.github.frankois944.spmForKmp.definition.helpers

import io.github.frankois944.spmForKmp.definition.SwiftDependency

internal fun List<SwiftDependency>.filterExportableDependency(): List<SwiftDependency> =
    buildList {
        this@filterExportableDependency.forEach { swiftDependency ->
            when (swiftDependency) {
                is SwiftDependency.Binary -> {
                    if (swiftDependency.exportToKotlin) add(swiftDependency)
                }

                is SwiftDependency.Package.Local -> {
                    add(swiftDependency.copy(products = swiftDependency.products.filter { it.exportToKotlin }))
                }

                is SwiftDependency.Package.Remote -> {
                    addFilteredRemotePackage(swiftDependency)
                }
            }
        }
    }

private fun MutableList<SwiftDependency>.addFilteredRemotePackage(remotePackage: SwiftDependency.Package.Remote) {
    val filteredProducts = remotePackage.products.filter { it.exportToKotlin }
    when (remotePackage) {
        is SwiftDependency.Package.Remote.Version -> add(remotePackage.copy(products = filteredProducts))
        is SwiftDependency.Package.Remote.Branch -> add(remotePackage.copy(products = filteredProducts))
        is SwiftDependency.Package.Remote.Commit -> add(remotePackage.copy(products = filteredProducts))
    }
}
