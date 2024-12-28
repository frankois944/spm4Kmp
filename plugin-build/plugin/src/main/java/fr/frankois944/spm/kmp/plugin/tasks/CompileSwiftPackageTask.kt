package fr.frankois944.spm.kmp.plugin.tasks

import fr.frankois944.spm.kmp.plugin.CompileTarget
import org.gradle.api.DefaultTask
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

internal abstract class CompileSwiftPackageTask
    @Inject
    constructor(
        @get:InputFile
        val manifestFile: File,
        @get:Input
        val target: CompileTarget,
        @get:Input
        val debugMode: Boolean,
        @get:OutputDirectory
        val packageBuildOutputDirectory: File,
        @get:InputDirectory
        val customSourcePackage: File,
        @get:Input
        val osVersion: String,
    ) : DefaultTask() {
        @get:Inject
        abstract val operation: ExecOperations

        init {
            description = "Compile a Swift Package manifest"
            group = BasePlugin.BUILD_GROUP
        }

        private fun prepareWorkingDir(): File {
            val workingDir = manifestFile.parentFile
            val sourceDir = workingDir.resolve("Source")
            if (sourceDir.exists()) {
                sourceDir.deleteRecursively()
            }
            sourceDir.mkdirs()
            if (customSourcePackage.list()?.isNotEmpty() == true) {
                customSourcePackage.copyRecursively(sourceDir)
            } else {
                sourceDir.resolve("Dummy.swift").createNewFile()
            }
            return workingDir
        }

        private fun getSDKPath(): String {
            val args =
                listOf(
                    "--sdk",
                    target.sdk(),
                    "--show-sdk-path",
                )

            logger.warn(
                """
            |getSDKPath
            |Build args :
            |${args.joinToString(" ")}
                """.trimMargin(),
            )

            val output = ByteArrayOutputStream()
            operation
                .exec {
                    it.executable = "xcrun"
                    it.args = args
                    it.standardOutput = output
                }
            return output.toString().trim()
        }

        @TaskAction
        fun compilePackage() {
            logger.warn("Compile the manifest $manifestFile")
            val sdkPath = getSDKPath()
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
                    packageBuildOutputDirectory.path,
                    "-c",
                    if (debugMode) "debug" else "release",
                )

            logger.warn(
                """
            |compileManifest
            |Build args :
            |$workingDir
            |${args.joinToString(" ")}
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
