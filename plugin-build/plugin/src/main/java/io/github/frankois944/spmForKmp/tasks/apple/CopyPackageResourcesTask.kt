package io.github.frankois944.spmForKmp.tasks.apple

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logger
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileFilter
import java.nio.file.Path
import kotlin.io.path.exists

/*
CONFIGURATION : Debug
BUILT_PRODUCTS_DIR : /Users/francoisdabonot/Library/Developer/Xcode/DerivedData/iosApp-gonppftpduwfmfbbmejictosfjsk/Build/Products/Debug-iphonesimulator
CONTENTS_FOLDER_PATH : iosApp.app
ARCHS : arm64
PLATFORM_NAME : iphonesimulator
 */

@CacheableTask
internal abstract class CopyPackageResourcesTask : DefaultTask() {

    companion object {
        fun getCurrentPackagesBuiltPath(
            packageScratchDir: File,
            platformName: String,
            archs: String,
            buildPackageMode: String,
            logger: Logger
        ): Path {
            logger.warn("Looking for a match with platformName $platformName")
            val systemType: String? = when {
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
                throw RuntimeException("Not matching package build name with platformName $platformName")
            }
            val simulator: String = if (platformName.contains("simulator")) {
                "-simulator"
            } else {
                ""
            }
            val buildPackageDirName = "${archs}-apple-$systemType$simulator"
            logger.warn("buildPackageDir created $buildPackageDirName")
            val buildPackagePath = packageScratchDir
                .resolve(buildPackageDirName)
                .resolve(buildPackageMode).toPath()
            if (!buildPackagePath.exists()) {
                logger.error("The buildPackagePath doesn't exist at $buildPackagePath")
                throw RuntimeException("Can't find the package build dir")
            } else {
                logger.warn("Found {} as packages resources path", buildPackagePath)
            }
            return buildPackagePath
        }
    }


    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputFrameworks: ListProperty<File>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputBundles: ListProperty<File>

    @get:OutputDirectory
    abstract val outputFrameworkDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputBundleDirectory: DirectoryProperty

    init {
        description = "Copy package resource to application"
        group = "io.github.frankois944.spmForKmp.tasks"
    }

    @TaskAction
    fun copyResources() {
      //  val sourcePathWithResource = getCurrentPackagesBuiltPath()
        //  val bundleDestinationDir = File("${buildProductDir.get()}/${contentFolderPath.get()}")

        // copy on needed bundles
        /*sourcePathWithResource.toFile().listFiles(FileFilter { it.extension == "bundle" })?.forEach {
            logger.info("copy resources bundle $it to $bundleDestinationDir")
            it.copyRecursively(File(bundleDestinationDir, it.name), overwrite = true)
        }

        val frameworkDestination = bundleDestinationDir.resolve("Frameworks")

        // copy frameworks
        sourcePathWithResource.toFile().listFiles(FileFilter { it.extension == "framework" })?.forEach {
            if (listOf("Headers", "Modules").contains(it.name)) {
                return
            }
            logger.info("copy resources framework $it to $frameworkDestination")
            it.copyRecursively(File(bundleDestinationDir, it.name), overwrite = true)
        }*/
    }


}
