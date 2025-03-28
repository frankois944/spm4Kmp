package io.github.frankois944.spmForKmp.manifest

import io.github.frankois944.spmForKmp.definition.SwiftDependency

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

    val getTargetSetting =
        getTargetSettings(
            swiftBuildDir = parameters.generatedPackageDirectory,
            settings = parameters.targetSettings,
        ).takeIf { it.isNotEmpty() }

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
                ${getDependencies(parameters.dependencies, parameters.forExportedPackage)}
            ],
            targets: [
                .target(
                    name: "${parameters.productName}",
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
