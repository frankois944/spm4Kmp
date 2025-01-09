package io.github.frankois944.spmForKmp.tasks

import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.manifest.generateManifest
import io.github.frankois944.spmForKmp.operations.resolvePackage
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
import java.io.File

@CacheableTask
internal abstract class GenerateManifestTask : DefaultTask() {
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
    abstract val packageScratchDir: Property<File>

    @get:Input
    @get:Optional
    abstract val sharedCacheDir: Property<File?>

    @get:Input
    @get:Optional
    abstract val sharedConfigDir: Property<File?>

    @get:Input
    @get:Optional
    abstract val sharedSecurityDir: Property<File?>

    @get:OutputFile
    abstract val manifestFile: RegularFileProperty

    init {
        description = "Generate a Swift Package manifest"
        group = "io.github.frankois944.spmForKmp.tasks"
    }

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
            project.resolvePackage(
                workingDir = manifestFile.asFile.get().parentFile,
                scratchPath = packageScratchDir.get(),
                sharedCachePath = sharedCacheDir.orNull,
                sharedConfigPath = sharedConfigDir.orNull,
                sharedSecurityPath = sharedSecurityDir.orNull,
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
