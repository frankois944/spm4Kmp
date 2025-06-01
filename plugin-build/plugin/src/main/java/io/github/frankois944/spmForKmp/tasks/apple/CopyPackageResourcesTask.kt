package io.github.frankois944.spmForKmp.tasks.apple

import io.github.frankois944.spmForKmp.operations.isDynamicLibrary
import io.github.frankois944.spmForKmp.resources.CopiedResourcesFactory
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import javax.inject.Inject

@CacheableTask
internal abstract class CopyPackageResourcesTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val builtDirectory: DirectoryProperty

    @get:Inject
    abstract val execOps: ExecOperations

    @get:Input
    abstract val buildProductDir: Property<String>

    @get:Input
    abstract val contentFolderPath: Property<String>

    init {
        description = "Copy package resource to application"
        group = "io.github.frankois944.spmForKmp.tasks"
        onlyIf {
            HostManager.hostIsMac
        }
    }

    @TaskAction
    fun copyResources() {
        logger.debug("preparing resources")
        val copiedResources =
            CopiedResourcesFactory(
                inputBuiltDirectory = builtDirectory.get().asFile,
                contentFolderPath = contentFolderPath.get(),
                buildProductDir = buildProductDir.get(),
                logger = logger,
            )

        logger.debug("Start copy bundle resources")
        copiedResources.bundles.forEach {
            val destination = File(copiedResources.outputBundleDirectory, it.name)
            logger.debug("copy resources bundle {} to {}", it.absolutePath, destination.absolutePath)
            it.copyRecursively(destination, overwrite = true)
        }
        logger.debug("End copy bundle resources")

        logger.debug("Start copy framework resources")
        val buildFrameworkDir =
            copiedResources.outputFrameworkDirectory
                .parentFile
                .parentFile
        val builtAppDir =
            copiedResources
                .outputFrameworkDirectory
        listOf(
            buildFrameworkDir,
            builtAppDir,
        ).forEach { appDir ->
            copiedResources.frameworks
                .filter { framework ->
                    // A static framework/library can't be copied to the app.
                    // A dynamic library and his resources must be copied inside the Apple app.
                    if (!execOps.isDynamicLibrary(framework.binaryFile, logger)) {
                        logger.debug("Ignore {} because it is not a dynamic library", framework.binaryFile)
                        false
                    } else {
                        true
                    }
                }.also { filteredFramework ->
                    filteredFramework.forEach { frameworkResource ->
                        frameworkResource.framework.listFiles()?.forEach {
                            if (!it.isDirectory) {
                                frameworkResource.files.add(it)
                            } else if (!listOf("Modules", "Headers", "_CodeSignature").contains(it.name)) {
                                frameworkResource.files.add(it)
                            }
                        }
                    }
                }.forEach { framework ->
                    val destination =
                        File(
                            appDir,
                            framework.name,
                        )
                    if (!destination.exists()) {
                        destination.mkdirs()
                    }
                    logger.debug("copy framework {} to {}", framework.name, destination)
                    framework.files.forEach { file ->
                        logger.debug("copy framework file ${file.name} to ${destination.resolve(file.name)}")
                        file.copyRecursively(
                            destination.resolve(file.name),
                            overwrite = true,
                        )
                    }
                }
        }
        logger.debug("End copy framework resources")
    }
}
