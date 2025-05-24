package io.github.frankois944.spmForKmp.manifest

import io.github.frankois944.spmForKmp.definition.SwiftDependency
import java.nio.file.Path
import kotlin.io.path.relativeToOrSelf

internal fun getDependencies(
    dependencies: List<SwiftDependency>,
    forExportedPackage: Boolean,
): String =
    buildList {
        dependencies
            .filter { !it.isBinaryDependency }
            .forEach { dependency ->
                if (!forExportedPackage || hasExportedDependencyProduct(dependency)) {
                    dependency.toDependencyDeclaration()?.let {
                        add(it)
                    }
                }
            }
    }.joinToString(",")

internal fun getDependenciesTargets(
    dependencies: List<SwiftDependency>,
    forExportedPackage: Boolean,
): String =
    buildList {
        dependencies
            .forEach { dependency ->
                if (dependency.isBinaryDependency) {
                    add("\"${dependency.packageName}\"")
                } else if (dependency is SwiftDependency.Package) {
                    dependency.productsConfig.productPackages.forEach { config ->
                        config.products
                            .filter {
                                !forExportedPackage || !it.copyResourcesToApp
                            }.forEach { product ->
                                val name = product.alias ?: product.name
                                add(".product(name: \"$name\", package: \"${dependency.packageName}\")")
                            }
                    }
                }
            }
    }.joinToString(",")

internal fun hasExportedDependencyProduct(dependency: SwiftDependency): Boolean {
    if (dependency is SwiftDependency.Package) {
        return dependency.productsConfig.productPackages.any { config ->
            config.products.any { !it.copyResourcesToApp }
        }
    }

    return dependency.isBinaryDependency
}

internal fun getLocaleBinary(
    dependencies: List<SwiftDependency>,
    swiftBuildDir: Path,
): String =
    buildList {
        dependencies
            .filterIsInstance<SwiftDependency.Binary.Local>()
            .forEach { dependency ->
                // package path MUST be relative to somewhere, let's choose the swiftBuildDir
                val path = Path.of(dependency.path).relativeToOrSelf(swiftBuildDir)
                add(".binaryTarget(name: \"${dependency.packageName}\", path:\"${path}\")")
            }
    }.joinToString(",")

internal fun getRemoteBinary(dependencies: List<SwiftDependency>): String =
    buildList {
        dependencies
            .filterIsInstance<SwiftDependency.Binary.Remote>()
            .forEach { dependency ->
                // checksum is MANDATORY
                add(
                    ".binaryTarget(name: \"${dependency.packageName}\", " +
                        "url:\"${dependency.url}\", " +
                        "checksum:\"${dependency.checksum}\")",
                )
            }
    }.joinToString(",")
