package io.github.frankois944.spmForKmp.tasks.apple

import io.github.frankois944.spmForKmp.operations.getPackageImplicitDependencies
import io.github.frankois944.spmForKmp.tasks.utils.TaskTracer
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

@CacheableTask
internal abstract class DependenciesAnalyzeTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val manifestFile: Property<File>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val packageScratchDir: DirectoryProperty

    @get:OutputFile
    val dependencyData: File
        get() {
            return manifestFile
                .get()
                .parentFile
                .resolve(".dependencies_data.json")
        }

    @get:Input
    @get:Optional
    abstract val swiftBinPath: Property<String?>

    @get:Inject
    abstract val execOps: ExecOperations

    @get:Input
    abstract val traceEnabled: Property<Boolean>

    @get:Internal
    val tracer: TaskTracer by lazy {
        TaskTracer(
            "DependenciesAnalyzeTask",
            traceEnabled.get(),
            outputFile =
                project.projectDir
                    .resolve("spmForKmpTrace")
                    .resolve(manifestFile.get().parentFile.name)
                    .resolve("DependenciesAnalyzeTask.html"),
        )
    }

    @TaskAction
    fun compilePackage() {
        tracer.trace("DependenciesAnalyzeTask") {
            tracer.trace("getPackageImplicitDependencies") {
                logger.debug("Start check dependencies")
                val content =
                    execOps.getPackageImplicitDependencies(
                        workingDir = manifestFile.get().parentFile,
                        scratchPath = packageScratchDir.get().asFile,
                        logger = logger,
                        swiftBinPath = swiftBinPath.orNull,
                    )
                dependencyData.writeText(content.toJsonString())
            }
        }
    }
}
