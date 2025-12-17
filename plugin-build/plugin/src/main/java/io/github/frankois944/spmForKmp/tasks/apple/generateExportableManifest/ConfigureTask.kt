package io.github.frankois944.spmForKmp.tasks.apple.generateExportableManifest

import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.config.PackageDirectoriesConfig
import io.github.frankois944.spmForKmp.definition.PackageRootDefinitionExtension
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.tasks.utils.getBuildMode
import io.github.frankois944.spmForKmp.tasks.utils.getTargetBuildDirectory
import io.github.frankois944.spmForKmp.tasks.utils.isTraceEnabled
import org.gradle.internal.extensions.stdlib.capitalized
import org.jetbrains.kotlin.gradle.plugin.extraProperties

internal fun GenerateExportableManifestTask.configureTask(
    swiftPackageEntry: PackageRootDefinitionExtension,
    packageDependencies: List<SwiftDependency>,
    packageDirectoriesConfig: PackageDirectoriesConfig,
    targets: List<AppleCompileTarget>,
) {
    val productName = "exported${swiftPackageEntry.internalName.capitalized()}"
    this.packageDependencies.set(packageDependencies)
    this.packageName.set(productName)
    this.minIos.set(swiftPackageEntry.minIos)
    this.minTvos.set(swiftPackageEntry.minTvos)
    this.minMacos.set(swiftPackageEntry.minMacos)
    this.minWatchos.set(swiftPackageEntry.minWatchos)
    this.toolsVersion.set(swiftPackageEntry.toolsVersion)
    this.exportedPackage.set(swiftPackageEntry.exportedPackageSettings)
    val exportedManifestDirectory =
        project.layout.projectDirectory
            .asFile
            .resolve(productName)
    this.exportedDirectory.set(exportedManifestDirectory)
    this.compiledTargetDir.set(
        getTargetBuildDirectory(
            packageScratchDir = packageDirectoriesConfig.packageScratchDir,
            cinteropTarget = targets.first(),
            buildMode = getBuildMode(swiftPackageEntry),
        ).absolutePath,
    )
    this.includeProduct.set(swiftPackageEntry.exportedPackageSettings.includeProduct)
    this.hideLocalPackageMessage.set(
        project.extraProperties.properties
            .getOrDefault("spmforkmp.hideLocalPackageMessage", false)
            .toString()
            .toBoolean(),
    )
    this.traceEnabled.set(project.isTraceEnabled)
    this.storedTraceFile.set(
        project.projectDir
            .resolve("spmForKmpTrace")
            .resolve(
                productName,
            ).resolve("GenerateExportableManifestTask.html"),
    )
}
