package io.github.frankois944.spmForKmp.tasks

import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.manifest.generateManifest
import io.github.frankois944.spmForKmp.operations.resolvePackage
import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

internal abstract class GenerateManifestTask
    @Suppress("LongParameterList")
    @Inject
    constructor() : DefaultTask() {
        @get:Inject
        abstract val operation: ExecOperations

        @get:Input
        abstract val packages: ListProperty<SwiftDependency>

        @get:Input
        abstract val productName: Property<String>

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

        @get:InputDirectory
        abstract val packageDirectory: Property<File>

        @get:OutputDirectory
        abstract val scratchDirectory: Property<File>

        @get:OutputDirectory
        @get:Optional
        abstract val sharedCacheDir: Property<File?>

        @get:OutputDirectory
        @get:Optional
        abstract val sharedConfigDir: Property<File?>

        @get:OutputDirectory
        @get:Optional
        abstract val sharedSecurityDir: Property<File?>

        init {
            description = "Generate a Swift Package manifest"
            group = "io.github.frankois944.spmForKmp.tasks"
        }

        @TaskAction
        fun generateFile() {
            val manifest =
                generateManifest(
                    packages.get(),
                    generatedPackageDirectory =
                        packageDirectory.get().toPath(),
                    productName = productName.get(),
                    minIos = minIos.get(),
                    minMacos = minMacos.get(),
                    minTvos = minTvos.get(),
                    minWatchos = minWatchos.get(),
                    toolsVersion = toolsVersion.get(),
                )
            packageDirectory.get().resolve("Package.swift").writeText(manifest)
            logger.debug(
                """
                Manifest file generated :
                ${packageDirectory.get().resolve("Package.swift")}
                ${packageDirectory.get().resolve("Package.swift").readText()}
                """.trimIndent(),
            )
            operation.resolvePackage(
                workingDir = packageDirectory.get(),
                scratchPath = scratchDirectory.get(),
                sharedCachePath = sharedCacheDir.orNull,
                sharedConfigPath = sharedConfigDir.orNull,
                sharedSecurityPath = sharedSecurityDir.orNull,
            )
        }
    }
