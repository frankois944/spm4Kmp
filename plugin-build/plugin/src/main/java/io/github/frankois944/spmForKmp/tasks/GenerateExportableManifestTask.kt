package io.github.frankois944.spmForKmp.tasks

import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.manifest.generateManifest
import io.github.frankois944.spmForKmp.operations.swiftFormat
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

internal abstract class GenerateExportableManifestTask : DefaultTask() {
    @get:Input
    abstract val packageDependencies: ListProperty<SwiftDependency>

    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    abstract val minIos: Property<String>

    @get:Input
    abstract val minMacos: Property<String>

    @get:Input
    abstract val minTvos: Property<String>

    @get:Input
    abstract val minWatchos: Property<String>

    @get:Input
    abstract val toolsVersion: Property<String>

    @get:OutputFile
    abstract val manifestFile: RegularFileProperty

    init {
        description = "Generate a Swift Package manifest with exported product"
        group = "io.github.frankois944.spmForKmp.tasks"
    }

    private fun prepareExportableDir() {
        val sourceDir =
            manifestFile
                .get()
                .asFile
                .parentFile
                .resolve("Sources")
                .also { it.mkdirs() }
        sourceDir.resolve("DummySPMFile.swift").createNewFile()
        sourceDir.resolve("DummySPMFile.swift").writeText("import Foundation")
    }

    @TaskAction
    fun generateFile() {
        prepareExportableDir()
        val manifest =
            generateManifest(
                packageDependencies.get(),
                generatedPackageDirectory =
                    manifestFile
                        .get()
                        .asFile.parentFile
                        .toPath(),
                productName = packageName.get(),
                minIos = minIos.get(),
                minMacos = minMacos.get(),
                minTvos = minTvos.get(),
                minWatchos = minWatchos.get(),
                toolsVersion = toolsVersion.get(),
            )
        manifestFile.asFile.get().writeText(manifest)
        try {
            project.swiftFormat(
                manifestFile.asFile.get(),
            )
            logger.warn(
                "Spm4Kmp: A local package has been generated in ${manifestFile.get().asFile.parentFile.path}",
            )
            logger.warn(
                "Please add it to your project as a local package dependency. " +
                    "You can find more information here: https://github.com/frankois944/spm-for-kmp" +
                    "#exporting-package-to-your-project",
            )
        } catch (ex: Exception) {
            logger.error(
                """
                Manifest file generated :
                ${manifestFile.get().asFile}
                ${manifestFile.get().asFile.readText()}
                """.trimIndent(),
            )
            throw ex
        }
    }
}
