package io.github.frankois944.spmForKmp.tasks.apple

import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.definition.packageSetting.BridgeSettings
import io.github.frankois944.spmForKmp.manifest.TemplateParameters
import io.github.frankois944.spmForKmp.manifest.generateManifest
import io.github.frankois944.spmForKmp.operations.resolvePackage
import io.github.frankois944.spmForKmp.operations.swiftFormat
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import javax.inject.Inject

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

    @get:OutputDirectory
    abstract val packageScratchDir: DirectoryProperty

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
    abstract val manifestFile: Property<File>

    @get:Input
    abstract val targetSettings: Property<BridgeSettings>

    @get:Inject
    abstract val execOps: ExecOperations

    init {
        description = "Generate a Swift Package manifest"
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
                        dependencies = packageDependencies.get(),
                        generatedPackageDirectory =
                            manifestFile
                                .get()
                                .parentFile
                                .toPath(),
                        productName = packageName.get(),
                        minIos = minIos.get(),
                        minMacos = minMacos.get(),
                        minTvos = minTvos.get(),
                        minWatchos = minWatchos.get(),
                        toolsVersion = toolsVersion.get(),
                        targetSettings = targetSettings.get(),
                    ),
            )
        manifestFile.get().writeText(manifest)
        try {
            execOps.swiftFormat(
                manifestFile.get(),
                logger,
            )
            execOps.resolvePackage(
                workingDir = manifestFile.get().parentFile,
                scratchPath = packageScratchDir.get().asFile,
                sharedCachePath = sharedCacheDir.orNull,
                sharedConfigPath = sharedConfigDir.orNull,
                sharedSecurityPath = sharedSecurityDir.orNull,
                logger,
            )
        } catch (ex: Exception) {
            logger.error(
                """
                Manifest file generated :
                ${manifestFile.get()}
                ${manifestFile.get().readText()}
                """.trimIndent(),
            )
            throw ex
        }
    }
}
