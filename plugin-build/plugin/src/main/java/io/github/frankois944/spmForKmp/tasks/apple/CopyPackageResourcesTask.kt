package io.github.frankois944.spmForKmp.tasks.apple

import io.github.frankois944.spmForKmp.operations.isDynamicLibrary
import io.github.frankois944.spmForKmp.resources.FrameworkResource
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

@CacheableTask
internal abstract class CopyPackageResourcesTask : DefaultTask() {
    @get:Input
    abstract val inputFrameworks: ListProperty<FrameworkResource>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val listOfResourcesToCopy: ListProperty<File>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputBundles: ListProperty<File>

    @get:OutputDirectory
    abstract val outputFrameworkDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputBundleDirectory: DirectoryProperty

    @get:Inject
    abstract val execOps: ExecOperations

    init {
        description = "Copy package resource to application"
        group = "io.github.frankois944.spmForKmp.tasks"
    }

    @TaskAction
    fun copyResources() {
        logger.debug("Start copy bundle resources")
        inputBundles.get().forEach {
            val destination = File(outputBundleDirectory.asFile.get(), it.name)
            logger.debug("copy resources bundle {} to {}", it, outputBundleDirectory.get())
            it.copyRecursively(destination, overwrite = true)
        }
        logger.debug("End copy bundle resources")

        logger.debug("Start copy framework resources")
        val buildFrameworkDir =
            outputFrameworkDirectory.asFile
                .get()
                .parentFile.parentFile
        val builtAppDir = outputFrameworkDirectory.asFile.get()
        listOf(buildFrameworkDir, builtAppDir).forEach { appDir ->
            inputFrameworks
                .get()
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
                        logger.debug("copy framework file ${file.name} to ${destination.resolve(file.name).path}")
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
