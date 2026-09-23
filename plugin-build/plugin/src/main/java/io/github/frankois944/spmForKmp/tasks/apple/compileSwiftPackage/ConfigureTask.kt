package io.github.frankois944.spmForKmp.tasks.apple.compileSwiftPackage

import io.github.frankois944.spmForKmp.SPM_TRACE_NAME
import io.github.frankois944.spmForKmp.SWIFT_PACKAGE_NAME
import io.github.frankois944.spmForKmp.SWIFT_PACKAGE_RESOLVE_NAME
import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.config.PackageDirectoriesConfig
import io.github.frankois944.spmForKmp.definition.PackageRootDefinitionExtension
import io.github.frankois944.spmForKmp.tasks.utils.resolveBuildSystem
import io.github.frankois944.spmForKmp.tasks.utils.computeOsVersion
import io.github.frankois944.spmForKmp.tasks.utils.isTraceEnabled
import io.github.frankois944.spmForKmp.tasks.utils.sharedResolveDirectory
import io.github.frankois944.spmForKmp.tasks.utils.spmBuildLayout
import io.github.frankois944.spmForKmp.tasks.utils.targetScratchDirectory
import java.io.File

internal fun CompileSwiftPackageTask.configureTask(
    cinteropTarget: AppleCompileTarget,
    swiftPackageEntry: PackageRootDefinitionExtension,
    packageDirectoriesConfig: PackageDirectoriesConfig,
    targetBuildDir: File,
) {
    val buildSystem = project.resolveBuildSystem(swiftPackageEntry)
    this.buildSystem.set(buildSystem)
    this.cinteropTarget.set(cinteropTarget)
    this.debugMode.set(swiftPackageEntry.debug)
    this.packageSwift.set(packageDirectoriesConfig.spmWorkingDir.resolve(SWIFT_PACKAGE_NAME))
    this.workingDir.set(packageDirectoriesConfig.spmWorkingDir.absolutePath)
    this.packageScratchDir.set(
        targetScratchDirectory(
            buildSystem = buildSystem,
            packageScratchDir = packageDirectoriesConfig.packageScratchDir,
            target = cinteropTarget,
        ).absolutePath,
    )
    this.sharedResolveDir.set(
        sharedResolveDirectory(
            buildSystem = buildSystem,
            packageScratchDir = packageDirectoriesConfig.packageScratchDir,
        ).absolutePath,
    )
    this.bridgeSourceDir.set(packageDirectoriesConfig.bridgeSourceDir)
    this.osVersion.set(computeOsVersion(cinteropTarget, swiftPackageEntry))
    this.sharedCacheDir.set(packageDirectoriesConfig.sharedCacheDir?.absolutePath)
    this.sharedConfigDir.set(packageDirectoriesConfig.sharedConfigDir?.absolutePath)
    this.sharedSecurityDir.set(packageDirectoriesConfig.sharedSecurityDir?.absolutePath)
    this.swiftBinPath.set(swiftPackageEntry.swiftBinPath)
    this.bridgeSourceBuiltDir.set(packageDirectoriesConfig.spmWorkingDir.resolve("Sources"))
    this.traceEnabled.set(project.isTraceEnabled)
    this.toolchain.set(swiftPackageEntry.toolchain)
    this.storedTraceFile.set(
        project.projectDir
            .resolve(SPM_TRACE_NAME)
            .resolve(packageDirectoriesConfig.spmWorkingDir.name)
            .resolve(cinteropTarget.toString())
            .resolve("CompileSwiftPackageTask.html"),
    )
    this.packageResolveFile.setFrom(packageDirectoriesConfig.spmWorkingDir.resolve(SWIFT_PACKAGE_RESOLVE_NAME))
    this.generatedDirs.set(
        spmBuildLayout(
            buildSystem = buildSystem,
            packageScratchDir =
                targetScratchDirectory(
                    buildSystem = buildSystem,
                    packageScratchDir = packageDirectoriesConfig.packageScratchDir,
                    target = cinteropTarget,
                ),
            target = cinteropTarget,
            buildMode = if (swiftPackageEntry.debug) "debug" else "release",
        ).compileOutputDirectories(),
    )
}
