package io.github.frankois944.spmForKmp.tasks.apple.generateCInteropDefinition

import io.github.frankois944.spmForKmp.SPM_TRACE_NAME
import io.github.frankois944.spmForKmp.SWIFT_PACKAGE_NAME
import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.config.PackageDirectoriesConfig
import io.github.frankois944.spmForKmp.definition.PackageRootDefinitionExtension
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.tasks.utils.computeOsVersion
import io.github.frankois944.spmForKmp.tasks.utils.isTraceEnabled
import io.github.frankois944.spmForKmp.utils.Hashing
import java.io.File

internal fun GenerateCInteropDefinitionTask.configureTask(
    targetBuildDir: File,
    cinteropTarget: AppleCompileTarget,
    swiftPackageEntry: PackageRootDefinitionExtension,
    packageDirectoriesConfig: PackageDirectoriesConfig,
    packageDependencies: List<SwiftDependency>,
) {
    this.compiledBinary.set(
        targetBuildDir
            .resolve("lib${swiftPackageEntry.internalName}.a"),
    )
    this.currentBuildDirectory.set(targetBuildDir)
    this.target.set(cinteropTarget)
    this.productName.set(swiftPackageEntry.internalName)
    this.packages.set(packageDependencies)
    this.debugMode.set(swiftPackageEntry.debug)
    this.osVersion.set(
        computeOsVersion(cinteropTarget, swiftPackageEntry),
    )
    this.manifestFile.set(packageDirectoriesConfig.spmWorkingDir.resolve(SWIFT_PACKAGE_NAME))
    this.scratchDir.set(packageDirectoriesConfig.packageScratchDir.absolutePath)
    this.packageDependencyPrefix.set(swiftPackageEntry.packageDependencyPrefix)
    this.compilerOpts.set(swiftPackageEntry.compilerOpts)
    this.linkerOpts.set(swiftPackageEntry.linkerOpts)
    this.swiftBinPath.set(swiftPackageEntry.swiftBinPath)
    this.currentBridgeHash.set(Hashing.hashDirectory(packageDirectoriesConfig))
    this.strictEnums.set(swiftPackageEntry.strictEnums)
    this.nonStrictEnums.set(swiftPackageEntry.nonStrictEnums)
    this.foreignExceptionMode.set(swiftPackageEntry.foreignExceptionMode)
    this.disableDesignatedInitializerChecks.set(swiftPackageEntry.disableDesignatedInitializerChecks)
    this.userSetupHint.set(swiftPackageEntry.userSetupHint)
    this.traceEnabled.set(project.isTraceEnabled)
    this.packageSwift.set(packageDirectoriesConfig.spmWorkingDir.resolve(SWIFT_PACKAGE_NAME))
    this.storedTraceFile.set(
        project.projectDir
            .resolve(SPM_TRACE_NAME)
            .resolve(packageDirectoriesConfig.spmWorkingDir.name)
            .resolve(cinteropTarget.toString())
            .resolve("GenerateCInteropDefinitionTask.html"),
    )
    this.definitionFolder.set(
        packageDirectoriesConfig
            .spmWorkingDir
            .resolve("defFiles")
            .resolve(cinteropTarget.toString()),
    )
}
