package io.github.frankois944.spmForKmp.tasks.apple.generateManifest

import io.github.frankois944.spmForKmp.SPM_TRACE_NAME
import io.github.frankois944.spmForKmp.SWIFT_PACKAGE_NAME
import io.github.frankois944.spmForKmp.config.PackageDirectoriesConfig
import io.github.frankois944.spmForKmp.definition.PackageRootDefinitionExtension
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.definition.packageSetting.BridgeSettings
import io.github.frankois944.spmForKmp.tasks.utils.isTraceEnabled

internal fun GenerateManifestTask.configureTask(
    swiftPackageEntry: PackageRootDefinitionExtension,
    packageDirectoriesConfig: PackageDirectoriesConfig,
    packageDependencies: List<SwiftDependency>,
) {
    this.packageDependencies.set(packageDependencies)
    this.packageName.set(swiftPackageEntry.internalName)
    this.minIos.set(swiftPackageEntry.minIos)
    this.minTvos.set(swiftPackageEntry.minTvos)
    this.minMacos.set(swiftPackageEntry.minMacos)
    this.minWatchos.set(swiftPackageEntry.minWatchos)
    this.toolsVersion.set(swiftPackageEntry.toolsVersion)
    this.manifestFile.set(packageDirectoriesConfig.spmWorkingDir.resolve(SWIFT_PACKAGE_NAME))
    this.targetSettings.set(swiftPackageEntry.bridgeSettings as BridgeSettings)
    this.swiftBinPath.set(swiftPackageEntry.swiftBinPath)
    this.traceEnabled.set(project.isTraceEnabled)
    this.storedTraceFile.set(
        project.projectDir
            .resolve(SPM_TRACE_NAME)
            .resolve(
                packageDirectoriesConfig.spmWorkingDir.name,
            ).resolve("GenerateManifestTask.html"),
    )
}
