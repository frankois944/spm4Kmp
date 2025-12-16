package io.github.frankois944.spmForKmp.tasks.apple.resolveManifest

import io.github.frankois944.spmForKmp.SWIFT_PACKAGE_NAME
import io.github.frankois944.spmForKmp.config.PackageDirectoriesConfig
import io.github.frankois944.spmForKmp.definition.PackageRootDefinitionExtension
import io.github.frankois944.spmForKmp.tasks.utils.isTraceEnabled

internal fun ResolveManifestTask.configureTask(
    swiftPackageEntry: PackageRootDefinitionExtension,
    packageDirectoriesConfig: PackageDirectoriesConfig,
) {
    this.manifestFile.set(packageDirectoriesConfig.spmWorkingDir.resolve(SWIFT_PACKAGE_NAME))
    this.storedTraceFile.set(
        project.projectDir
            .resolve("spmForKmpTrace")
            .resolve(
                packageDirectoriesConfig.spmWorkingDir.name,
            ).resolve("ResolveManifestTask.html"),
    )
    this.sharedCacheDir.set(packageDirectoriesConfig.sharedCacheDir)
    this.sharedConfigDir.set(packageDirectoriesConfig.sharedConfigDir)
    this.sharedSecurityDir.set(packageDirectoriesConfig.sharedSecurityDir)
    this.swiftBinPath.set(swiftPackageEntry.swiftBinPath)
    this.traceEnabled.set(project.isTraceEnabled)
    this.packageScratchPath.set(packageDirectoriesConfig.packageScratchDir.absolutePath)
    this.packageScratchFiles.set(
        buildList {
            add(packageDirectoriesConfig.spmWorkingDir.resolve("Package.resolved"))
            add(packageDirectoriesConfig.packageScratchDir.resolve(".my.lock"))
            add(packageDirectoriesConfig.packageScratchDir.resolve(".my.workspace-state.json"))
        },
    )
    this.packageScratchDirectories.set(
        buildList {
            val scratchEntries =
                listOf(
                    "artifacts",
                    "checkouts",
                    "registry",
                    "repositories",
                )
            for (dirName in scratchEntries) {
                add(packageDirectoriesConfig.packageScratchDir.resolve(dirName))
            }
        },
    )
}
