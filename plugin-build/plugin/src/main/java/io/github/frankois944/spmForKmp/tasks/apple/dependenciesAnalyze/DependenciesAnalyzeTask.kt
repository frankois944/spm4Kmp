package io.github.frankois944.spmForKmp.tasks.apple.dependenciesAnalyze

import io.github.frankois944.spmForKmp.operations.getPackageImplicitDependencies
import io.github.frankois944.spmForKmp.tasks.utils.TaskTracer
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import javax.inject.Inject

@CacheableTask
internal abstract class DependenciesAnalyzeTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val manifestFile: RegularFileProperty

    @get:Input
    abstract val packageScratchDir: Property<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val scratchLockFile: RegularFileProperty

    @get:OutputFile
    abstract val dependencyDataFile: RegularFileProperty

    @get:Input
    @get:Optional
    abstract val swiftBinPath: Property<String>

    @get:Inject
    abstract val execOps: ExecOperations

    @get:OutputFile
    abstract val storedTraceFile: RegularFileProperty

    @get:Input
    abstract val traceEnabled: Property<Boolean>

    init {
        description = "Analyze a Swift Package manifest dependency"
        group = "io.github.frankois944.spmForKmp.tasks"
        onlyIf {
            HostManager.Companion.hostIsMac
        }
    }

    @TaskAction
    fun compilePackage() {
        val tracer =
            TaskTracer(
                "DependenciesAnalyzeTask",
                traceEnabled.get(),
                outputFile =
                    storedTraceFile
                        .get()
                        .asFile,
            )
        tracer.trace("DependenciesAnalyzeTask") {
            tracer.trace("prepare source dir") {
                prepareSourceDir()
            }
            tracer.trace("getPackageImplicitDependencies") {
                logger.debug("Start check dependencies")
                val content =
                    execOps.getPackageImplicitDependencies(
                        workingDir = manifestFile.get().asFile.parentFile,
                        scratchPath = File(packageScratchDir.get()),
                        logger = logger,
                        swiftBinPath = swiftBinPath.orNull,
                    )
                dependencyDataFile.get().asFile.writeText(content.toJsonString())
            }
        }
        tracer.writeHtmlReport()
    }

    private fun prepareSourceDir() {
        // TODO: remove this file
        val sourceDir =
            manifestFile
                .get()
                .asFile
                .parentFile
                .resolve("Sources")
        sourceDir.mkdirs()
        sourceDir
            .resolve("Dummy.swift")
            .writeText("import Foundation")
    }
}
