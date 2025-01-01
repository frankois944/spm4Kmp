package io.github.frankois944.spmForKmp.plugin.tasks

import io.github.frankois944.spmForKmp.plugin.CompileTarget
import io.github.frankois944.spmForKmp.plugin.operations.getSDKPath
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

internal abstract class CompileSwiftPackageTask
    @Suppress("LongParameterList")
    @Inject
    constructor(
        @get:InputFile
        val manifestFile: File,
        @get:Input
        val target: CompileTarget,
        @get:Input
        val debugMode: Boolean,
        @get:InputDirectory
        val originalPackageScratchDir: File,
        @get:OutputDirectory
        val packageScratchDir: File,
        @get:InputDirectory
        val customSourcePackage: File,
        @get:Input
        val osVersion: String,
    ) : DefaultTask() {
        @get:Inject
        abstract val operation: ExecOperations

        init {
            description = "Compile the Swift Package manifest"
            group = "io.github.frankois944.spmForKmp.plugin.tasks"
        }

        private fun copyOriginalPackageScratchDir() {
            if (!packageScratchDir.exists()) {
                packageScratchDir.mkdirs()
                originalPackageScratchDir.copyRecursively(packageScratchDir)
            }
        }

        private fun prepareWorkingDir(): File {
            val workingDir = manifestFile.parentFile
            val sourceDir = workingDir.resolve("Source")
            if (sourceDir.exists()) {
                sourceDir.deleteRecursively()
            }
            sourceDir.mkdirs()
            if (customSourcePackage.list()?.isNotEmpty() == true) {
                logger.debug(
                    """
                    Copy User Swift files to directory $sourceDir
                    ${customSourcePackage.list()?.toList()}
                    """.trimIndent(),
                )
                customSourcePackage.copyRecursively(sourceDir)
            } else {
                logger.debug(
                    """
                    Copy Dummy swift file to directory $sourceDir
                    """.trimIndent(),
                )
                sourceDir.resolve("Dummy.swift").createNewFile()
                sourceDir.resolve("Dummy.swift").writeText("import Foundation")
            }
            copyOriginalPackageScratchDir()
            return workingDir
        }

        @TaskAction
        fun compilePackage() {
            logger.debug("Compile the manifest {}", manifestFile.path)
            val sdkPath = operation.getSDKPath(target)
            val workingDir = prepareWorkingDir()

            val args =
                listOf(
                    "swift",
                    "build",
                    "--sdk",
                    sdkPath,
                    "--triple",
                    target.getTriple(osVersion),
                    "--scratch-path",
                    packageScratchDir.path,
                    "-c",
                    if (debugMode) "debug" else "release",
                )

            logger.debug(
                """
                RUN compileManifest
                ARGS xcrun ${args.joinToString(" ")}
                From ${workingDir.path}
                """.trimMargin(),
            )

            operation
                .exec {
                    it.executable = "xcrun"
                    it.workingDir = workingDir
                    it.args = args
                }
        }
    }
