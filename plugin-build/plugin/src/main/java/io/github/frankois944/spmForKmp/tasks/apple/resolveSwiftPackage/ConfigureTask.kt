package io.github.frankois944.spmForKmp.tasks.apple.resolveSwiftPackage

import io.github.frankois944.spmForKmp.SPM_TRACE_NAME
import io.github.frankois944.spmForKmp.SWIFT_PACKAGE_NAME
import io.github.frankois944.spmForKmp.SWIFT_PACKAGE_RESOLVE_NAME
import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.config.PackageDirectoriesConfig
import io.github.frankois944.spmForKmp.definition.PackageRootDefinitionExtension
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.tasks.utils.resolveBuildSystem
import io.github.frankois944.spmForKmp.tasks.utils.getArtifactsDirectory
import io.github.frankois944.spmForKmp.tasks.utils.getCheckoutsDirectory
import io.github.frankois944.spmForKmp.tasks.utils.isTraceEnabled
import io.github.frankois944.spmForKmp.tasks.utils.sharedResolveDirectory
import io.github.frankois944.spmForKmp.tasks.utils.targetScratchDirectory

internal fun ResolveSwiftPackageTask.configureTask(
    swiftPackageEntry: PackageRootDefinitionExtension,
    packageDirectoriesConfig: PackageDirectoriesConfig,
    packageDependencies: List<SwiftDependency>,
    targets: List<AppleCompileTarget>,
) {
    val sharedResolveDir =
        sharedResolveDirectory(
            buildSystem = project.resolveBuildSystem(swiftPackageEntry),
            packageScratchDir = packageDirectoriesConfig.packageScratchDir,
        )
    this.workingDir.set(packageDirectoriesConfig.spmWorkingDir.absolutePath)
    this.packageSwift.set(packageDirectoriesConfig.spmWorkingDir.resolve(SWIFT_PACKAGE_NAME))
    this.packageScratchDir.set(sharedResolveDir.absolutePath)
    this.sharedCacheDir.set(packageDirectoriesConfig.sharedCacheDir?.absolutePath)
    this.sharedConfigDir.set(packageDirectoriesConfig.sharedConfigDir?.absolutePath)
    this.sharedSecurityDir.set(packageDirectoriesConfig.sharedSecurityDir?.absolutePath)
    this.swiftBinPath.set(swiftPackageEntry.swiftBinPath)
    this.toolchain.set(swiftPackageEntry.toolchain)
    this.packageResolveFile.set(packageDirectoriesConfig.spmWorkingDir.resolve(SWIFT_PACKAGE_RESOLVE_NAME))
    this.artifactDir.set(getArtifactsDirectory(sharedResolveDir))
    this.checkoutDir.set(getCheckoutsDirectory(sharedResolveDir))
    this.targetScratchDirs.set(
        targets.map { target ->
            targetScratchDirectory(
                buildSystem = swiftPackageEntry.let { project.resolveBuildSystem(it) },
                packageScratchDir = packageDirectoriesConfig.packageScratchDir,
                target = target,
            ).absolutePath
        },
    )
    this.expectsCheckouts.set(packageDependencies.any { it.isRemoteSourceDependency() })
    this.expectsArtifacts.set(packageDependencies.any { it is SwiftDependency.Binary.Remote })
    this.traceEnabled.set(project.isTraceEnabled)
    this.storedTraceFile.set(
        project.projectDir
            .resolve(SPM_TRACE_NAME)
            .resolve(packageDirectoriesConfig.spmWorkingDir.name)
            .resolve("ResolveSwiftPackageTask.html"),
    )
}

/**
 * A dependency SwiftPM checks out in `scratch/checkouts`: the local ones are used in place and
 * the binary ones are extracted in `scratch/artifacts`.
 */
private fun SwiftDependency.isRemoteSourceDependency(): Boolean = this is SwiftDependency.Package.Remote
