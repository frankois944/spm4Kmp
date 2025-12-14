package io.github.frankois944.spmForKmp.tasks.apple

import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.definition.packageSetting.BridgeSettings
import io.github.frankois944.spmForKmp.manifest.TemplateParameters
import io.github.frankois944.spmForKmp.manifest.generateManifest
import io.github.frankois944.spmForKmp.operations.swiftFormat
import io.github.frankois944.spmForKmp.tasks.utils.TaskTracer
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
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
    @get:Optional
    abstract val sharedCacheDir: Property<File?>

    @get:Input
    @get:Optional
    abstract val sharedConfigDir: Property<File?>

    @get:Input
    @get:Optional
    abstract val sharedSecurityDir: Property<File?>

    @get:Input
    abstract val targetSettings: Property<BridgeSettings>

    @get:Input
    @get:Optional
    abstract val swiftBinPath: Property<String?>

    @get:OutputFile
    abstract val manifestFile: Property<File>

    @Internal
    val packageScratchDir: DirectoryProperty = project.objects.directoryProperty()

    @get:Input
    abstract val traceEnabled: Property<Boolean>

    @get:Internal
    val tracer: TaskTracer by lazy {
        TaskTracer(
            "GenerateManifestTask",
            traceEnabled.get(),
            outputFile =
                project.projectDir
                    .resolve("spmForKmpTrace")
                    .resolve(manifestFile.get().parentFile.name)
                    .resolve("GenerateManifestTask.html"),
        )
    }

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
        tracer.trace("GenerateManifestTask") {
            tracer.trace("generateManifest") {
                val manifest =
                    generateManifest(
                        parameters =
                            TemplateParameters(
                                forExportedPackage = false,
                                dependencies = packageDependencies.get(),
                                generatedPackageDirectory =
                                    manifestFile
                                        .get()
                                        .parentFile
                                        .toPath(),
                                productName = packageName.get(),
                                minIos = minIos.orNull.orEmpty(),
                                minMacos = minMacos.orNull.orEmpty(),
                                minTvos = minTvos.orNull.orEmpty(),
                                minWatchos = minWatchos.orNull.orEmpty(),
                                toolsVersion = toolsVersion.get(),
                                targetSettings = targetSettings.get(),
                                exportedPackage = null,
                            ),
                    )
                manifestFile.get().writeText(manifest)
            }
            tracer.trace("swiftFormat") {
                try {
                    execOps.swiftFormat(
                        manifestFile.get(),
                        logger,
                    )
                } catch (ex: Exception) {
                    logger.error(
                        "Manifest file generated :\n{}\n{}",
                        manifestFile.get(),
                        manifestFile.get().readText(),
                    )
                    throw ex
                }
            }
        }
        tracer.writeHtmlReport()
    }
}
