package io.github.frankois944.spmForKmp.manifest

import io.github.frankois944.spmForKmp.definition.SwiftDependency

@Suppress("LongMethod")
internal fun generateManifest(parameters: TemplateParameters): String {
    var binaryDependencies =
        listOfNotNull(
            getLocaleBinary(
                parameters.dependencies,
                parameters.generatedPackageDirectory,
                parameters.forExportedPackage,
            ).takeIf { it.isNotEmpty() },
            getRemoteBinary(
                parameters.dependencies,
                parameters.forExportedPackage,
            ).takeIf { it.isNotEmpty() },
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

    val getTargetSetting =
        getTargetSettings(
            swiftBuildDir = parameters.generatedPackageDirectory,
            settings = parameters.targetSettings,
        ).takeIf { it.isNotEmpty() }

    val name = parameters.exportedPackage?.name ?: parameters.productName
    val type =
        parameters.exportedPackage?.isStatic?.let {
            if (it) ".static" else ".dynamic"
        } ?: run {
            ".static"
        }

    return """
        // swift-tools-version: ${parameters.toolsVersion}
        import PackageDescription

        let package = Package(
            name: "$name",
            $platforms
            products: [
                .library(
                    name: "$name",
                    type: $type,
                    targets: [${
        getProductsTargets(
            name,
            parameters.dependencies,
            parameters.forExportedPackage,
        )
    }])
            ],
            dependencies: [
                ${getDependencies(parameters.dependencies, parameters.forExportedPackage)}
            ],
            targets: [
                .target(
                    name: "$name",
                    dependencies: [
                        ${getDependenciesTargets(parameters.dependencies, parameters.forExportedPackage)}
                    ],
                    path: "Sources"
                    ${getTargetSetting?.let { ",$it" }.orEmpty()}
                )
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
    return if (entries.isNotEmpty()) "platforms: [$entries]," else ""
}

private fun getProductsTargets(
    cinteropName: String,
    dependencies: List<SwiftDependency>,
    forExportedPackage: Boolean,
): String =
    buildList {
        add("\"$cinteropName\"")
        dependencies
            .filter { it.isBinaryDependency && !forExportedPackage }
            .forEach { dependency ->
                add("\"${dependency.packageName}\"")
            }
    }.joinToString(",")
