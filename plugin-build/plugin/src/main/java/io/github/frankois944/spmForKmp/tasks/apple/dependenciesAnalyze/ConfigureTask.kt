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
    this.workingDir.set(packageDirectoriesConfig.spmWorkingDir)
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
    val resolveFile =
        packageDirectoriesConfig.spmWorkingDir
            .resolve("Package.resolved")
    val lockFile =
        packageDirectoriesConfig.packageScratchDir
            .resolve(".my.lock")
    val workspaceStateFile =
        packageDirectoriesConfig.packageScratchDir
            .resolve(".my.workspace-state.json")
    this.scratchLockFile.set(
        listOf(resolveFile, lockFile, workspaceStateFile)
            .firstOrNull { it.exists() }
            ?: workspaceStateFile,
    )
}
