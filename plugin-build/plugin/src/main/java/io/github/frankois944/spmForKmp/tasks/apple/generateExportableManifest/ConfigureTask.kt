package io.github.frankois944.spmForKmp.tasks.apple.generateExportableManifest

import io.github.frankois944.spmForKmp.SWIFT_PACKAGE_NAME
import io.github.frankois944.spmForKmp.definition.PackageRootDefinitionExtension
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.tasks.utils.isTraceEnabled
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import java.io.File

internal fun GenerateExportableManifestTask.configureTask(
    swiftPackageEntry: PackageRootDefinitionExtension,
    manifestDir: File,
    packageDependencies: List<SwiftDependency>,
    targetBuildDir: File,
) {
    this.packageDependencies.set(packageDependencies)
    this.packageName.set(manifestDir.name)
    this.minIos.set(swiftPackageEntry.minIos)
    this.minTvos.set(swiftPackageEntry.minTvos)
    this.minMacos.set(swiftPackageEntry.minMacos)
    this.minWatchos.set(swiftPackageEntry.minWatchos)
    this.toolsVersion.set(swiftPackageEntry.toolsVersion)
    manifestDir.mkdirs()
    this.manifestFile.set(manifestDir.resolve(SWIFT_PACKAGE_NAME))
    this.exportedPackage.set(swiftPackageEntry.exportedPackageSettings)
    this.compiledTargetDir.set(targetBuildDir)
    this.includeProduct.set(swiftPackageEntry.exportedPackageSettings.includeProduct)
    this.hideLocalPackageMessage.set(
        project.extraProperties.properties
            .getOrDefault("spmforkmp.hideLocalPackageMessage", false)
            .toString()
            .toBoolean(),
    )
    this.traceEnabled.set(project.isTraceEnabled)
    this.storedTracePath.set(project.projectDir)
}
