package io.github.frankois944.spmForKmp.tasks.utils

import io.github.frankois944.spmForKmp.definition.SwiftDependency

internal fun List<SwiftDependency>.getCopyablePackageResourceName(): List<String> =
    buildList {
        this@getCopyablePackageResourceName.forEach { swiftDependency ->
            when (swiftDependency) {
                is SwiftDependency.Binary -> {
                    if (swiftDependency.copyResourcesToApp) add(swiftDependency.packageName.lowercase())
                }

                is SwiftDependency.Package -> {
                    swiftDependency
                        .productsConfig
                        .productPackages
                        .flatMap { it.products }
                        .filter { it.copyResourcesToApp }
                        .map {it.name.lowercase() }
                        .let { addAll(it) }
                }
            }
        }
    }
