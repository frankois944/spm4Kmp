package io.github.frankois944.spmForKmp.tasks.apple

import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.operations.getNbJobs
import io.github.frankois944.spmForKmp.operations.getSDKPath
import io.github.frankois944.spmForKmp.operations.printExecLogs
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

@CacheableTask
internal abstract class CompileSwiftPackageTask : DefaultTask() {
    init {
        description = "Compile the Swift Package manifest"
        group = "io.github.frankois944.spmForKmp.tasks"
        onlyIf {
            HostManager.hostIsMac
        }
    }

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val manifestFile: Property<File>

    @get:Input
    abstract val target: Property<AppleCompileTarget>

    @get:Input
    abstract val debugMode: Property<Boolean>

    @get:OutputDirectory
    abstract val compiledTargetDir: Property<File>

    @get:Input
    abstract val packageScratchDir: Property<File>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourcePackage: DirectoryProperty

    @get:OutputDirectory
    val copiedSourcePackage: File
        get() = manifestFile.get().parentFile.resolve("Sources")

    @get:Input
    abstract val osVersion: Property<String>

    @get:Input
    @get:Optional
    abstract val sharedCacheDir: Property<File?>

    @get:Input
    @get:Optional
    abstract val sharedConfigDir: Property<File?>

    @get:Input
    @get:Optional
    abstract val sharedSecurityDir: Property<File?>

    @get:Inject
    abstract val operation: ExecOperations

    private fun prepareWorkingDir() {
        val sourceDir = copiedSourcePackage
        if (sourceDir.exists()) {
            sourceDir.deleteRecursively()
        }
        sourceDir.mkdirs()
        if (!sourcePackage.get().asFileTree.isEmpty) {
            logger.debug(
                """
                Copy User Swift files to directory $sourceDir
                ${sourcePackage.get().asFile.list()?.toList()}
                """.trimIndent(),
            )
            sourcePackage.get().asFile.copyRecursively(sourceDir)
        } else {
            logger.debug("Copy Dummy swift file to directory {}", sourceDir)
            sourceDir.resolve("DummySPMFile.swift").writeText("import Foundation")
        }
    }

    @TaskAction
    fun compilePackage() {
        logger.debug("Compile the manifest {}", manifestFile.get().path)
        val sdkPath = project.getSDKPath(target.get())
        prepareWorkingDir()

        val args =
            mutableListOf(
                "--sdk",
                "macosx",
                "swift",
                "build",
                "--sdk",
                sdkPath,
                "--triple",
                target.get().getTriple(osVersion.get()),
                "--scratch-path",
                packageScratchDir.get().path,
                "-c",
                if (debugMode.get()) "debug" else "release",
                "--jobs",
                project.getNbJobs(),
            )
        sharedCacheDir.orNull?.let {
            args.add("--cache-path")
            args.add(it.path)
        }
        sharedConfigDir.orNull?.let {
            args.add("--config-path")
            args.add(it.path)
        }
        sharedSecurityDir.orNull?.let {
            args.add("--security-path")
            args.add(it.path)
        }

        val standardOutput = ByteArrayOutputStream()
        val errorOutput = ByteArrayOutputStream()
        operation
            .exec {
                it.executable = "xcrun"
                it.workingDir = manifestFile.get().parentFile
                it.args = args
                it.standardOutput = standardOutput
                it.errorOutput = errorOutput
                it.isIgnoreExitValue = true
            }.also {
                project.printExecLogs(
                    "buildPackage",
                    args,
                    it.exitValue != 0,
                    standardOutput,
                    errorOutput,
                )
            }
    }
}
