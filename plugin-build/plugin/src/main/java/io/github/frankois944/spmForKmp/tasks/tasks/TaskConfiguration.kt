package io.github.frankois944.spmForKmp.tasks.tasks

import io.github.frankois944.spmForKmp.SWIFT_PACKAGE_NAME
import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.config.PackageDirectoriesConfig
import io.github.frankois944.spmForKmp.definition.PackageRootDefinitionExtension
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.definition.packageSetting.BridgeSettings
import io.github.frankois944.spmForKmp.resources.getCurrentPackagesBuiltDir
import io.github.frankois944.spmForKmp.tasks.apple.CompileSwiftPackageTask
import io.github.frankois944.spmForKmp.tasks.apple.CopyPackageResourcesTask
import io.github.frankois944.spmForKmp.tasks.apple.GenerateCInteropDefinitionTask
import io.github.frankois944.spmForKmp.tasks.apple.GenerateExportableManifestTask
import io.github.frankois944.spmForKmp.tasks.apple.GenerateManifestTask
import io.github.frankois944.spmForKmp.tasks.apple.ResolveManifestTask
import io.github.frankois944.spmForKmp.tasks.utils.computeOsVersion
import io.github.frankois944.spmForKmp.utils.Hashing
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import java.io.File

internal val Project.isTraceEnabled: Boolean
    get() =
        project.extraProperties.properties
            .getOrDefault("spmforkmp.enableTracing", false)
            .toString()
            .toBoolean()

@Suppress("LongParameterList")
internal fun GenerateManifestTask.configureManifestTask(
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
    this.sharedCacheDir.set(packageDirectoriesConfig.sharedCacheDir)
    this.sharedConfigDir.set(packageDirectoriesConfig.sharedConfigDir)
    this.sharedSecurityDir.set(packageDirectoriesConfig.sharedSecurityDir)
    this.targetSettings.set(swiftPackageEntry.bridgeSettings as BridgeSettings)
    this.swiftBinPath.set(swiftPackageEntry.swiftBinPath)
    this.traceEnabled.set(this.project.isTraceEnabled)
    this.storedTracePath.set(project.projectDir)
}

internal fun GenerateExportableManifestTask.configureExportableManifestTask(
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
    this.traceEnabled.set(this.project.isTraceEnabled)
    this.storedTracePath.set(project.projectDir)
}

@Suppress("LongParameterList")
internal fun ResolveManifestTask.configureResolveManifestTask(
    swiftPackageEntry: PackageRootDefinitionExtension,
    packageDirectoriesConfig: PackageDirectoriesConfig,
) {
    this.manifestFile.set(packageDirectoriesConfig.spmWorkingDir.resolve(SWIFT_PACKAGE_NAME))
    this.packageScratchDir.set(packageDirectoriesConfig.packageScratchDir)
    this.sharedCacheDir.set(packageDirectoriesConfig.sharedCacheDir)
    this.sharedConfigDir.set(packageDirectoriesConfig.sharedConfigDir)
    this.sharedSecurityDir.set(packageDirectoriesConfig.sharedSecurityDir)
    this.swiftBinPath.set(swiftPackageEntry.swiftBinPath)
    this.traceEnabled.set(this.project.isTraceEnabled)
    this.storedTracePath.set(project.projectDir)
}

@Suppress("LongParameterList")
internal fun CompileSwiftPackageTask.configureCompileTask(
    target: AppleCompileTarget,
    swiftPackageEntry: PackageRootDefinitionExtension,
    targetBuildDir: File,
    packageDirectoriesConfig: PackageDirectoriesConfig,
) {
    val manifestFile = packageDirectoriesConfig.spmWorkingDir.resolve(SWIFT_PACKAGE_NAME)
    this.manifestFile.set(manifestFile)
    this.target.set(target)
    this.debugMode.set(swiftPackageEntry.debug)
    this.packageScratchDir.set(packageDirectoriesConfig.packageScratchDir)
    this.compiledTargetDir.set(targetBuildDir)
    this.bridgeSourceDir.set(packageDirectoriesConfig.bridgeSourceDir)
    this.osVersion.set(computeOsVersion(target, swiftPackageEntry))
    this.sharedCacheDir.set(packageDirectoriesConfig.sharedCacheDir)
    this.sharedConfigDir.set(packageDirectoriesConfig.sharedConfigDir)
    this.sharedSecurityDir.set(packageDirectoriesConfig.sharedSecurityDir)
    this.swiftBinPath.set(swiftPackageEntry.swiftBinPath)
    this.bridgeSourceBuiltDir.set(manifestFile.parentFile.resolve("Sources"))
    this.traceEnabled.set(this.project.isTraceEnabled)
    this.storedTracePath.set(project.projectDir)
}

internal fun GenerateCInteropDefinitionTask.configureGenerateCInteropDefinitionTask(
    targetBuildDir: File,
    cinteropTarget: AppleCompileTarget,
    swiftPackageEntry: PackageRootDefinitionExtension,
    packageDirectoriesConfig: PackageDirectoriesConfig,
    packageDependencies: List<SwiftDependency>,
) {
    this.compiledBinary.set(targetBuildDir.resolve("lib${swiftPackageEntry.internalName}.a"))
    this.target.set(cinteropTarget)
    this.productName.set(swiftPackageEntry.internalName)
    this.packages.set(packageDependencies)
    this.debugMode.set(swiftPackageEntry.debug)
    this.osVersion.set(
        computeOsVersion(cinteropTarget, swiftPackageEntry),
    )
    this.manifestFile.set(packageDirectoriesConfig.spmWorkingDir.resolve(SWIFT_PACKAGE_NAME))
    this.scratchDir.set(packageDirectoriesConfig.packageScratchDir)
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
    this.traceEnabled.set(this.project.isTraceEnabled)
    this.storedTracePath.set(project.projectDir)
}

@Suppress("LongParameterList")
internal fun CopyPackageResourcesTask.configureCopyPackageResourcesTask(
    packageDirectoriesConfig: PackageDirectoriesConfig,
    buildMode: String,
    cinteropTarget: AppleCompileTarget,
) {
    val buildProductDir: String? =
        project.findProperty("io.github.frankois944.spmForKmp.BUILT_PRODUCTS_DIR") as? String
            ?: System.getenv("BUILT_PRODUCTS_DIR")
    val contentFolderPath: String? =
        project.findProperty("io.github.frankois944.spmForKmp.CONTENTS_FOLDER_PATH") as? String
            ?: System.getenv("CONTENTS_FOLDER_PATH")
    val archs: String? =
        project.findProperty("io.github.frankois944.spmForKmp.ARCHS") as? String
            ?: System.getenv("ARCHS")
    val platformName: String? =
        project.findProperty("io.github.frankois944.spmForKmp.PLATFORM_NAME") as? String
            ?: System.getenv("PLATFORM_NAME")

    logger.debug("buildProductDir $buildProductDir")
    logger.debug("contentFolderPath $contentFolderPath")
    logger.debug("archs $archs")
    logger.debug("platformName $platformName")

    @Suppress("ComplexCondition")
    if (archs.isNullOrEmpty() ||
        platformName.isNullOrEmpty() ||
        buildProductDir.isNullOrEmpty() ||
        contentFolderPath.isNullOrEmpty()
    ) {
        enabled = false
        logger.debug("Missing variable for coping the resources, skipping the task")
        return
    }

    if (cinteropTarget.sdk() != platformName) {
        logger.debug(
            "The current cinteropTarget {} is different from the xcode platformName {}",
            cinteropTarget,
            platformName,
        )
        isEnabled = false
        return
    }

    this.builtDirectory.set(
        getCurrentPackagesBuiltDir(
            packageScratchDir = packageDirectoriesConfig.packageScratchDir,
            platformName = platformName,
            archs = archs,
            buildPackageMode = buildMode,
            logger = logger,
        ),
    )
    this.codeSignIdentityName.set(System.getenv("EXPANDED_CODE_SIGN_IDENTITY_NAME"))
    this.buildProductDir.set(buildProductDir)
    this.contentFolderPath.set(contentFolderPath)
    this.traceEnabled.set(this.project.isTraceEnabled)
    this.storedTracePath.set(project.projectDir)
}
