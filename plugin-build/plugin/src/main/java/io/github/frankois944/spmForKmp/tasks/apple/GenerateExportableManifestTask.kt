package io.github.frankois944.spmForKmp.tasks.apple

import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.manifest.generateManifest
import io.github.frankois944.spmForKmp.operations.swiftFormat
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.konan.target.HostManager

@CacheableTask
internal abstract class GenerateExportableManifestTask : DefaultTask() {
    init {
        onlyIf {
            HostManager.hostIsMac
        }
    }

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
                .takeIf { !it.exists() }
                ?.also { it.mkdirs() }
        sourceDir?.resolve("DummySPMFile.swift")?.writeText("import Foundation")
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
            logger.lifecycle("Spm4Kmp: A local Swift package has been generated at")
            logger.lifecycle(
                manifestFile
                    .get()
                    .asFile.parentFile.path,
            )
            logger.lifecycle("Please add it to your xcode project as a local package dependency.")
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
