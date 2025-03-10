package io.github.frankois944.spmForKmp.tasks.apple

import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.manifest.generateManifest
import io.github.frankois944.spmForKmp.operations.resolvePackage
import io.github.frankois944.spmForKmp.operations.swiftFormat
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.konan.target.HostManager
import kotlin.io.resolve

@CacheableTask
internal abstract class GenerateManifestTask : DefaultTask() {
    init {
        description = "Generate a Swift Package manifest"
        group = "io.github.frankois944.spmForKmp.tasks.apple"
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

    @get:Input
    @get:Optional
    abstract val packageCachePath: Property<String?>

    @get:OutputFile
    abstract val manifestFile: RegularFileProperty

    @get:OutputDirectory
    abstract val clonedSourcePackages: DirectoryProperty

    @TaskAction
    fun generateFile() {
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
            prepareWorkingDir()
            project.resolvePackage(
                workingDir = manifestFile.asFile.get().parentFile,
                clonedSourcePackages = clonedSourcePackages.get().asFile,
                packageCachePath = packageCachePath.orNull,
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

    // create a empty Source Dir for xcode to resolve the package
    private fun prepareWorkingDir() {
        val sourceDirectory =
            manifestFile.asFile
                .get()
                .parentFile
                .resolve("Sources")
        if (!sourceDirectory.exists()) {
            sourceDirectory.mkdirs()
            sourceDirectory.resolve("DummyFile.swift").writeText("import Foundation")
        }
    }
}
