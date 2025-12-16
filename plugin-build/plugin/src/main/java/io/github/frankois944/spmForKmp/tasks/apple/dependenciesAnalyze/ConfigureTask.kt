package io.github.frankois944.spmForKmp.tasks.apple.dependenciesAnalyze

import io.github.frankois944.spmForKmp.SWIFT_PACKAGE_NAME
import io.github.frankois944.spmForKmp.config.PackageDirectoriesConfig
import io.github.frankois944.spmForKmp.definition.PackageRootDefinitionExtension
import io.github.frankois944.spmForKmp.tasks.utils.isTraceEnabled

internal fun DependenciesAnalyzeTask.configureTask(
    swiftPackageEntry: PackageRootDefinitionExtension,
    packageDirectoriesConfig: PackageDirectoriesConfig,
) {
    this.swiftBinPath.set(swiftPackageEntry.swiftBinPath)
    this.manifestFile.set(packageDirectoriesConfig.spmWorkingDir.resolve(SWIFT_PACKAGE_NAME))
    this.packageScratchDir.set(packageDirectoriesConfig.packageScratchDir.absolutePath)
    this.traceEnabled.set(project.isTraceEnabled)
    this.storedTraceFile.set(
        project.projectDir
            .resolve("spmForKmpTrace")
            .resolve(packageDirectoriesConfig.spmWorkingDir.name)
            .resolve("DependenciesAnalyzeTask.html"),
    )
    this.dependencyDataFile.set(
        packageDirectoriesConfig.spmWorkingDir
            .resolve(".dependencies_data.json"),
    )
    val lockFile =
        packageDirectoriesConfig.packageScratchDir
            .resolve(".my.lock")
    this.scratchLockFile.set(
        if (lockFile.exists()) {
            lockFile
        } else {
            packageDirectoriesConfig.packageScratchDir
                .resolve("my.workspace-state.json")
        },
    )
}
