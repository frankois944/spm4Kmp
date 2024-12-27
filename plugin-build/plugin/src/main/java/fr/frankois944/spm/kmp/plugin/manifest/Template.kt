package fr.frankois944.spm.kmp.plugin.manifest

import fr.frankois944.spm.kmp.plugin.definition.SwiftPackageDependencyDefinition
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.relativeTo

internal fun generateManifest(
    dependencies: List<SwiftPackageDependencyDefinition>,
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
                    ])
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
    dependencies: List<SwiftPackageDependencyDefinition>,
): String =
    buildList {
        add("\"$cinteropName\"")
        dependencies
            .filter { it.isBinaryDependency }
            .forEach { dependency ->
                add("\"${dependency.packageName}\"")
            }
    }.joinToString(",")

private fun getDependencies(dependencies: List<SwiftPackageDependencyDefinition>): String =
    buildList {
        dependencies
            .filter { !it.isBinaryDependency }
            .forEach { dependency ->
                dependency.toDependencyDeclaration()?.let {
                    add(it)
                }
            }
    }.joinToString(",")

private fun getDependenciesTargets(dependencies: List<SwiftPackageDependencyDefinition>): String =
    buildList {
        dependencies
            .forEach { dependency ->
                if (dependency.isBinaryDependency) {
                    add("\"${dependency.packageName}\"")
                } else {
                    dependency.names.forEach { library ->
                        add(".product(name: \"${library}\", package: \"${dependency.packageName}\")")
                    }
                }
            }
    }.joinToString(",\n")

private fun buildLocaleBinary(
    dependencies: List<SwiftPackageDependencyDefinition>,
    swiftBuildDir: Path,
): String =
    buildList {
        dependencies
            .filterIsInstance<SwiftPackageDependencyDefinition.LocalBinary>()
            .forEach { dependency ->
                // package path MUST be relative to somewhere, let's choose the swiftBuildDir
                val path = Path(dependency.path).relativeTo(swiftBuildDir).toString()
                add(".binaryTarget(name: \"${dependency.names.first()}\", path:\"${path}\")")
            }
    }.joinToString(",\n")

private fun buildRemoteBinary(dependencies: List<SwiftPackageDependencyDefinition>): String =
    buildList {
        dependencies
            .filterIsInstance<SwiftPackageDependencyDefinition.RemoteBinary>()
            .forEach { dependency ->
                // checksum is MANDATORY
                add(
                    ".binaryTarget(name: \"${dependency.names.first()}\", " +
                        "url:\"${dependency.url}\", " +
                        "checksum:\"${dependency.checksum}\")",
                )
            }
    }.joinToString(",\n")

private val SwiftPackageDependencyDefinition.isBinaryDependency: Boolean
    get() =
        (this is SwiftPackageDependencyDefinition.LocalBinary) ||
            (this is SwiftPackageDependencyDefinition.RemoteBinary)

private fun SwiftPackageDependencyDefinition.toDependencyDeclaration(): String? =
    when (this) {
        is SwiftPackageDependencyDefinition.Local ->
            """
            .package(path: "$path")
            """.trimIndent()

        is SwiftPackageDependencyDefinition.RemoteDefinition.Version ->
            """
            .package(url: "$url", exact: "$version")
            """.trimIndent()

        is SwiftPackageDependencyDefinition.RemoteDefinition.Commit -> {
            """
            .package(url: "$url", revision: "$revision")
            """.trimIndent()
        }

        is SwiftPackageDependencyDefinition.RemoteDefinition.Branch ->
            """
            .package(url: "$url", branch: "$branch")
            """.trimIndent()

        is SwiftPackageDependencyDefinition.LocalBinary -> null
        is SwiftPackageDependencyDefinition.RemoteBinary -> null
    }
