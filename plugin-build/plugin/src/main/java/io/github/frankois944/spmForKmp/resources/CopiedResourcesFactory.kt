package io.github.frankois944.spmForKmp.resources

import io.github.frankois944.spmForKmp.utils.getPlistValue
import org.gradle.api.logging.Logger
import java.io.File
import java.io.Serializable
import kotlin.io.path.Path
import kotlin.io.path.exists

internal data class FrameworkResource(
    val framework: File,
    val name: String = framework.name,
    val binaryFile: File,
    var files: MutableList<File> = mutableListOf(),
) : Serializable {
    internal companion object {
        private const val serialVersionUID: Long = 2
    }
}

@Suppress("LongParameterList")
internal class CopiedResourcesFactory(
    private val packageScratchDir: File,
    baseDir: File,
    private val platformName: String,
    private val archs: String,
    private val buildPackageMode: String,
    contentFolderPath: String,
    buildProductDir: String,
    private val logger: Logger,
) {
    val outputBundleDirectory: File
    val outputFrameworkDirectory: File
    val frameworks: List<FrameworkResource>
    val bundles: List<File>

    init {
        val inputBuiltDirectory =
            getCurrentPackagesBuiltDir(
                packageScratchDir = packageScratchDir,
                platformName = platformName,
                archs = archs,
                buildPackageMode = buildPackageMode,
                logger = logger,
            )

        bundles =
            buildList {
                // get all bundle folders from build directory
                inputBuiltDirectory
                    .listFiles {
                        it.extension == "bundle"
                    }.let {
                        addAll(it)
                    }
                // get all bundles from frameworks
                inputBuiltDirectory
                    .listFiles {
                        it.extension == "framework"
                    }.forEach { framework ->
                        framework
                            .listFiles {
                                it.extension == "bundle"
                            }?.let {
                                addAll(it)
                            }
                    }
            }

        frameworks =
            buildList {
                inputBuiltDirectory
                    .listFiles {
                        it.extension == "framework"
                    }.forEach { framework ->
                        val plist = framework.resolve("Info.plist")
                        logger.debug("Looking inside the Info.plist {}", plist)
                        val libraryName = getPlistValue(plist, "CFBundleExecutable")
                        logger.debug("Found libraryName $libraryName")
                        val binaryFile = framework.resolve(libraryName)
                        val newFramework = FrameworkResource(framework, binaryFile = binaryFile)
                        add(newFramework)
                    }
            }

        val destinationDir = Path(buildProductDir, contentFolderPath).toFile()

        outputBundleDirectory = destinationDir.relativeTo(baseDir)
        outputFrameworkDirectory = destinationDir.resolve("Frameworks").relativeTo(baseDir)
        logger.debug("outputBundleDirectory: {}", outputBundleDirectory)
        logger.debug("outputFrameworkDirectory: {}", outputFrameworkDirectory)
        logger.debug("inputBundles: {}", bundles.map { it.name })
        logger.debug("inputFrameworks: {}", frameworks.map { it.name + it.files.map { f -> f.name } })
    }

    private fun getCurrentPackagesBuiltDir(
        packageScratchDir: File,
        platformName: String,
        archs: String,
        buildPackageMode: String,
        logger: Logger,
    ): File {
        logger.debug("Looking for a match with platformName $platformName")
        val systemType: String? =
            when {
                platformName.contains("iphone") -> {
                    "ios"
                }

                platformName.contains("watch") -> {
                    "watchos"
                }

                platformName.contains("mac") -> {
                    "macosx"
                }

                platformName.contains("tv") -> {
                    "tvos"
                }

                else -> {
                    null
                }
            }
        if (systemType == null) {
            throw RuntimeException("No matching systemType from platformName $platformName")
        }
        val simulator: String =
            if (platformName.contains("simulator")) {
                "-simulator"
            } else {
                ""
            }
        val buildPackageDirName = "$archs-apple-$systemType$simulator"
        logger.debug("buildPackageDir created $buildPackageDirName")
        val buildPackagePath =
            packageScratchDir
                .resolve(buildPackageDirName)
                .resolve(buildPackageMode)
                .toPath()
        if (!buildPackagePath.exists()) {
            logger.error("The buildPackagePath doesn't exist at $buildPackagePath")
            throw RuntimeException("Can't find the package build dir")
        } else {
            logger.debug("Found {} as packages resources path", buildPackagePath)
        }
        return buildPackagePath.toFile()
    }
}
