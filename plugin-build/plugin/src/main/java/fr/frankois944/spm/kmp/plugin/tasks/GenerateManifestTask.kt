package fr.frankois944.spm.kmp.plugin.tasks

import fr.frankois944.spm.kmp.plugin.definition.SwiftDependency
import fr.frankois944.spm.kmp.plugin.manifest.generateManifest
import fr.frankois944.spm.kmp.plugin.operations.resolvePackage
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

internal abstract class GenerateManifestTask
    @Inject
    constructor(
        @get:Input
        val packages: List<SwiftDependency>,
        @get:Input
        val productName: String,
        @get:Input
        val minIos: String,
        @get:Input
        val minMacos: String,
        @get:Input
        val minTvos: String,
        @get:Input
        val minWatchos: String,
        @get:Input
        val toolsVersion: String,
        @get:InputDirectory
        val packageDirectory: File,
        @get:OutputDirectory
        val scratchDirectory: File,
    ) : DefaultTask() {
        @get:Inject
        abstract val operation: ExecOperations

        init {
            description = "Generate a Swift Package manifest"
            group = "fr.frankois944.spm.kmp.plugin.tasks"
        }

        @TaskAction
        fun generateFile() {
            val manifest =
                generateManifest(
                    packages,
                    generatedPackageDirectory =
                        packageDirectory.toPath(),
                    productName = productName,
                    minIos = minIos,
                    minMacos = minMacos,
                    minTvos = minTvos,
                    minWatchos = minWatchos,
                    toolsVersion = toolsVersion,
                )
            packageDirectory.resolve("Package.swift").writeText(manifest)
            logger.debug(
                """
                Manifest file generated :
                ${packageDirectory.resolve("Package.swift")}
                ${packageDirectory.resolve("Package.swift").readText()}
                """.trimIndent(),
            )
            operation.resolvePackage(packageDirectory, scratchDirectory)
        }
    }
