package io.github.frankois944.spmForKmp.tasks.apple.copyPackageResources

import io.github.frankois944.spmForKmp.SPM_TRACE_NAME
import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.config.PackageDirectoriesConfig
import io.github.frankois944.spmForKmp.resources.getCurrentPackagesBuiltDir
import io.github.frankois944.spmForKmp.tasks.utils.isTraceEnabled

internal fun CopyPackageResourcesTask.configureTask(
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
    this.traceEnabled.set(project.isTraceEnabled)
    this.storedTraceFile.set(
        project.projectDir
            .resolve(SPM_TRACE_NAME)
            .resolve(packageDirectoriesConfig.spmWorkingDir.name)
            .resolve("CopyPackageResourcesTask.html"),
    )
}
