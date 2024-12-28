package fr.frankois944.spm.kmp.plugin.tasks

import fr.frankois944.spm.kmp.plugin.definition.SwiftPackageDependencyDefinition
import fr.frankois944.spm.kmp.plugin.manifest.generateManifest
import org.gradle.api.DefaultTask
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

internal abstract class GenerateManifestTask
    @Inject
    constructor(
        @get:Input
        val packages: List<SwiftPackageDependencyDefinition>,
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
        val generatedPackageDirectory: File,
        @get:OutputFile
        val outputFile: File,
    ) : DefaultTask() {
        init {
            description = "Generate a Swift Package manifest"
            group = BasePlugin.BUILD_GROUP
        }

        @TaskAction
        fun generateFile() {
            val manifest =
                generateManifest(
                    packages,
                    generatedPackageDirectory =
                        generatedPackageDirectory.toPath(),
                    productName = productName,
                    minIos = minIos,
                    minMacos = minMacos,
                    minTvos = minTvos,
                    minWatchos = minWatchos,
                    toolsVersion = toolsVersion,
                )
            outputFile.writeText(manifest)
        }
    }
