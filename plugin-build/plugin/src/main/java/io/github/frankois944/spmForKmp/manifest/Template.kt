package io.github.frankois944.spmForKmp.manifest

import io.github.frankois944.spmForKmp.definition.SwiftDependency
import java.nio.file.Path
import kotlin.io.path.relativeToOrSelf

internal fun generateManifest(parameters: TemplateParameters): String {
    var binaryDependencies =
        listOfNotNull(
            getLocaleBinary(parameters.dependencies, parameters.generatedPackageDirectory).takeIf { it.isNotEmpty() },
            getRemoteBinary(parameters.dependencies).takeIf { it.isNotEmpty() },
        ).joinToString(",")
    if (binaryDependencies.isNotEmpty()) {
        binaryDependencies = ",$binaryDependencies"
    }

    val platforms =
        getPlatformBlock(
            minIos = parameters.minIos,
            minMacos = parameters.minMacos,
            minTvos = parameters.minTvos,
            minWatchos = parameters.minWatchos,
        )

    return """
        // swift-tools-version: ${parameters.toolsVersion}
        import PackageDescription

        let package = Package(
            name: "${parameters.productName}",
            $platforms,
            products: [
                .library(
                    name: "${parameters.productName}",
                    type: .static,
                    targets: [${getProductsTargets(parameters.productName, parameters.dependencies)}])
            ],
            dependencies: [
                ${getDependencies(parameters.dependencies)}
            ],
            targets: [
                .target(
                    name: "${parameters.productName}",
                    dependencies: [
                        ${getDependenciesTargets(parameters.dependencies)}
                    ],
                    path: "Sources")
                $binaryDependencies
            ]
        )
        """
}

private fun getPlatformBlock(
    minIos: String,
    minMacos: String,
    minTvos: String,
    minWatchos: String,
): String {
    val entries =
        listOfNotNull(
            ".iOS(\"$minIos\")".takeIf { minIos.isNotEmpty() },
            ".macOS(\"$minMacos\")".takeIf { minMacos.isNotEmpty() },
            ".tvOS(\"$minTvos\")".takeIf { minTvos.isNotEmpty() },
            ".watchOS(\"$minWatchos\")".takeIf { minWatchos.isNotEmpty() },
        ).joinToString(",")
    return "platforms: [$entries]"
}

private fun getProductsTargets(
    cinteropName: String,
    dependencies: List<SwiftDependency>,
): String =
    buildList {
        add("\"$cinteropName\"")
        dependencies
            .filter { it.isBinaryDependency }
            .forEach { dependency ->
                add("\"${dependency.packageName}\"")
            }
    }.joinToString(",")

private fun getDependencies(dependencies: List<SwiftDependency>): String =
    buildList {
        dependencies
            .filter { !it.isBinaryDependency }
            .forEach { dependency ->
                dependency.toDependencyDeclaration()?.let {
                    add(it)
                }
            }
    }.joinToString(",")

private fun getDependenciesTargets(dependencies: List<SwiftDependency>): String =
    buildList {
        dependencies
            .forEach { dependency ->
                if (dependency.isBinaryDependency) {
                    add("\"${dependency.packageName}\"")
                } else if (dependency is SwiftDependency.Package) {
                    dependency.productsConfig.productPackages.forEach { config ->
                        config.products.forEach { product ->
                            val name = product.alias ?: product.name
                            add(".product(name: \"$name\", package: \"${dependency.packageName}\")")
                        }
                    }
                }
            }
    }.joinToString(",")

private fun getLocaleBinary(
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

private fun getRemoteBinary(dependencies: List<SwiftDependency>): String =
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
