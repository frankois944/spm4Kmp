package io.github.frankois944.spmForKmp.tasks.apple.configRegistryPackage

import io.github.frankois944.spmForKmp.config.PackageDirectoriesConfig
import io.github.frankois944.spmForKmp.definition.PackageRootDefinitionExtension
import io.github.frankois944.spmForKmp.tasks.utils.isTraceEnabled

internal fun ConfigRegistryPackageTask.configureTask(
    swiftPackageEntry: PackageRootDefinitionExtension,
    packageDirectoriesConfig: PackageDirectoriesConfig,
) {
    this.workingDir.set(packageDirectoriesConfig.spmWorkingDir.absolutePath)
    this.swiftBinPath.set(swiftPackageEntry.swiftBinPath)
    this.registries.set(swiftPackageEntry.packageRegistryConfigs)
    this.traceEnabled.set(project.isTraceEnabled)
    this.storedTraceFile.set(
        project.projectDir
            .resolve("spmForKmpTrace")
            .resolve(packageDirectoriesConfig.spmWorkingDir.name)
            .resolve("ConfigRegistryPackageTask.html"),
    )
}
