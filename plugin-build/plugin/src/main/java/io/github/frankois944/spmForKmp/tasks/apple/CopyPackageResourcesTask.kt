package io.github.frankois944.spmForKmp.tasks.apple

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
import java.io.File

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

    init {
        description = "Copy package resource to application"
        group = "io.github.frankois944.spmForKmp.tasks"
    }

    @TaskAction
    fun copyResources() {
        inputBundles.get().forEach {
            val destination = File(outputBundleDirectory.asFile.get(), it.name)
            logger.warn("copy resources bundle $it to ${outputBundleDirectory.get()}")
            it.copyRecursively(destination, overwrite = true)
        }

        val buildFrameworkDir =
            outputFrameworkDirectory.asFile
                .get()
                .parentFile.parentFile
        val builtAppDir = outputFrameworkDirectory.asFile.get()
        listOf(buildFrameworkDir, builtAppDir).forEach { appDir ->
            inputFrameworks.get().forEach { framework ->
                val destination =
                    File(
                        appDir,
                        framework.name,
                    )
                if (!destination.exists()) {
                    destination.mkdirs()
                }
                logger.warn("copy framework ${framework.name} to $destination")
                framework.files.forEach { file ->
                    logger.warn("copy framework file ${file.name} to ${destination.resolve(file.name).path}")
                    file.copyRecursively(
                        destination.resolve(file.name),
                        overwrite = true,
                    )
                }
            }
        }
    }
}
