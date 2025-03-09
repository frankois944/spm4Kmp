package io.github.frankois944.spmForKmp.tasks.apple

import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.operations.printExecLogs
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
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

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val clonedSourcePackages: DirectoryProperty

    @get:OutputDirectory
    abstract val buildWorkingDir: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourcePackage: Property<File>

    @get:Inject
    abstract val operation: ExecOperations

    @get:Input
    abstract val xcodeBuildArgs: ListProperty<String>

    init {
        description = "Compile the Swift Package manifest"
        group = "io.github.frankois944.spmForKmp.tasks"
    }

    private fun prepareWorkingDir(): File {
        val workingDir = manifestFile.get().parentFile
        val sourceDir = workingDir.resolve("Sources")
        if (sourceDir.exists()) {
            sourceDir.deleteRecursively()
        }
        sourceDir.mkdirs()
        if (sourcePackage.get().list()?.isNotEmpty() == true) {
            logger.debug(
                """
                Copy User Swift files to directory $sourceDir
                ${sourcePackage.get().list()?.toList()}
                """.trimIndent(),
            )
            sourcePackage.get().copyRecursively(sourceDir)
        } else {
            logger.debug("Copy Dummy swift file to directory {}", sourceDir)
            sourceDir.resolve("DummySPMFile.swift").writeText("import Foundation")
        }
        return workingDir
    }

    @TaskAction
    fun compilePackage() {
        logger.debug("Compile the manifest {}", manifestFile.get().path)
        val workingDir = prepareWorkingDir()

        val args =
            buildList {
                add("xcodebuild")
                add("-scheme")
                add(manifestFile.get().parentFile.name)
                add("-derivedDataPath")
                add(buildWorkingDir.get().asFile.path)
                add("-clonedSourcePackagesDirPath")
                add(clonedSourcePackages.get().asFile.path)
                add("-configuration")
                add(if (debugMode.get()) "Debug" else "Release")
                add("-sdk")
                add(target.get().sdk())
                add("-destination")
                add("generic/platform=${target.get().destination()}")
                addAll(xcodeBuildArgs.get().orEmpty())
                add("COMPILER_INDEX_STORE_ENABLE=NO")
                add("DEFINES_MODULE=YES")
                add("BUILD_LIBRARY_FOR_DISTRIBUTION=YES")
                add("CLANG_ENABLE_MODULES=YES")
                add("ENABLE_MODULES=YES")
                add("OTHER_CFLAGS=\"-fmodules -fcxx-modules\"")
            }
        val standardOutput = ByteArrayOutputStream()
        val errorOutput = ByteArrayOutputStream()
        operation
            .exec {
                it.executable = "xcrun"
                it.workingDir = workingDir
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
