package fr.frankois944.spmForKmp.plugin.manifest

import fr.frankois944.spmForKmp.plugin.definition.SwiftDependency
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.relativeToOrSelf

internal fun generateManifest(
    dependencies: List<SwiftDependency>,
    generatedPackageDirectory: Path,
    productName: String,
    minIos: String,
    minMacos: String,
    minTvos: String,
    minWatchos: String,
    toolsVersion: String,
): String {
    var binaryDependencies =
        listOfNotNull(
            buildLocaleBinary(dependencies, generatedPackageDirectory).takeIf { it.isNotEmpty() },
            buildRemoteBinary(dependencies).takeIf { it.isNotEmpty() },
        ).joinToString(",")
    if (binaryDependencies.isNotEmpty()) {
        binaryDependencies = ",$binaryDependencies"
    }

    return """
        // swift-tools-version: $toolsVersion
        import PackageDescription

        let package = Package(
            name: "$productName",
            ${getPlatformBlock(minIos, minMacos, minTvos, minWatchos)},
            products: [
                .library(
                    name: "$productName",
                    type: .static,
                    targets: [${getProductsTargets(productName, dependencies)}])
            ],
            dependencies: [
                ${getDependencies(dependencies)}
            ],
            targets: [
                .target(
                    name: "$productName",
                    dependencies: [
                        ${getDependenciesTargets(dependencies)}
                    ],
                    path: "Source")
                $binaryDependencies
            ]

        )
        """.trimIndent()
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
                    dependency.names.forEach { library ->
                        add(".product(name: \"${library}\", package: \"${dependency.packageName}\")")
                    }
                }
            }
    }.joinToString(",\n")

private fun buildLocaleBinary(
    dependencies: List<SwiftDependency>,
    swiftBuildDir: Path,
): String =
    buildList {
        dependencies
            .filterIsInstance<SwiftDependency.Binary.Local>()
            .forEach { dependency ->
                // package path MUST be relative to somewhere, let's choose the swiftBuildDir
                val path = Path(dependency.path).relativeToOrSelf(swiftBuildDir)
                add(".binaryTarget(name: \"${dependency.packageName}\", path:\"${path}\")")
            }
    }.joinToString(",\n")

private fun buildRemoteBinary(dependencies: List<SwiftDependency>): String =
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
    }.joinToString(",\n")
