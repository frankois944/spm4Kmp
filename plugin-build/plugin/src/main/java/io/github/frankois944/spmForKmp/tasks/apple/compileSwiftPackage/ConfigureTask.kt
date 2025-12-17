package io.github.frankois944.spmForKmp.tasks.apple.compileSwiftPackage

import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.config.PackageDirectoriesConfig
import io.github.frankois944.spmForKmp.definition.PackageRootDefinitionExtension
import io.github.frankois944.spmForKmp.tasks.utils.computeOsVersion
import io.github.frankois944.spmForKmp.tasks.utils.isTraceEnabled

internal fun CompileSwiftPackageTask.configureTask(
    cinteropTarget: AppleCompileTarget,
    swiftPackageEntry: PackageRootDefinitionExtension,
    packageDirectoriesConfig: PackageDirectoriesConfig,
) {
    this.cinteropTarget.set(cinteropTarget)
    this.debugMode.set(swiftPackageEntry.debug)
    this.workingDir.set(packageDirectoriesConfig.spmWorkingDir.absolutePath)
    this.packageScratchDir.set(packageDirectoriesConfig.packageScratchDir)
    this.bridgeSourceDir.set(packageDirectoriesConfig.bridgeSourceDir)
    this.osVersion.set(computeOsVersion(cinteropTarget, swiftPackageEntry))
    this.sharedCacheDir.set(packageDirectoriesConfig.sharedCacheDir?.absolutePath)
    this.sharedConfigDir.set(packageDirectoriesConfig.sharedConfigDir?.absolutePath)
    this.sharedSecurityDir.set(packageDirectoriesConfig.sharedSecurityDir?.absolutePath)
    this.swiftBinPath.set(swiftPackageEntry.swiftBinPath)
    this.bridgeSourceBuiltDir.set(packageDirectoriesConfig.spmWorkingDir.resolve("Sources"))
    this.traceEnabled.set(project.isTraceEnabled)
    this.storedTraceFile.set(
        project.projectDir
            .resolve("spmForKmpTrace")
            .resolve(packageDirectoriesConfig.spmWorkingDir.name)
            .resolve(cinteropTarget.toString())
            .resolve("CompileSwiftPackageTask.html"),
    )
    this.packageResolveFile.set(packageDirectoriesConfig.spmWorkingDir.resolve("Package.resolved"))
}
