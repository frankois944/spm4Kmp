package io.github.frankois944.spmForKmp.tasks.apple

import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.definition.exported.ExportedPackage
import io.github.frankois944.spmForKmp.manifest.TemplateParameters
import io.github.frankois944.spmForKmp.manifest.generateManifest
import io.github.frankois944.spmForKmp.operations.swiftFormat
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import javax.inject.Inject

@CacheableTask
internal abstract class GenerateExportableManifestTask : DefaultTask() {
    @get:Input
    abstract val packageDependencies: ListProperty<SwiftDependency>

    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    @get:Optional
    abstract val minIos: Property<String?>

    @get:Input
    @get:Optional
    abstract val minMacos: Property<String?>

    @get:Input
    @get:Optional
    abstract val minTvos: Property<String?>

    @get:Input
    @get:Optional
    abstract val minWatchos: Property<String?>

    @get:Input
    abstract val toolsVersion: Property<String>

    @get:Input
    abstract val exportedPackage: Property<ExportedPackage>

    @get:OutputFile
    abstract val manifestFile: RegularFileProperty

    @get:OutputFile
    val exportedSource: File
        get() =
            manifestFile
                .get()
                .asFile
                .parentFile
                .resolve("Sources")
                .resolve("DummySPMFile.swift")
                .also {
                    it.parentFile.mkdirs()
                    it.writeText(
                        """
                        // This file has been generated by Spm4Kmp plugin
                        // DO NO EDIT THIS FILE AS IT WILL BE OVERWRITTEN ON EACH BUILD
                        import Foundation
                        """.trimIndent(),
                    )
                }

    @get:Inject
    abstract val execOps: ExecOperations

    init {
        description = "Generate a Swift Package manifest with exported product"
        group = "io.github.frankois944.spmForKmp.tasks"
        onlyIf {
            HostManager.hostIsMac
        }
    }

    @TaskAction
    fun generateFile() {
        val manifest =
            generateManifest(
                parameters =
                    TemplateParameters(
                        forExportedPackage = true,
                        dependencies = packageDependencies.get(),
                        generatedPackageDirectory =
                            manifestFile
                                .get()
                                .asFile.parentFile
                                .toPath(),
                        productName = packageName.get(),
                        minIos = minIos.orNull.orEmpty(),
                        minMacos = minMacos.orNull.orEmpty(),
                        minTvos = minTvos.orNull.orEmpty(),
                        minWatchos = minWatchos.orNull.orEmpty(),
                        toolsVersion = toolsVersion.get(),
                        targetSettings = null,
                        exportedPackage = exportedPackage.get(),
                    ),
            )
        manifestFile.asFile.get().writeText(manifest)
        try {
            execOps.swiftFormat(
                manifestFile.asFile.get(),
                logger,
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
