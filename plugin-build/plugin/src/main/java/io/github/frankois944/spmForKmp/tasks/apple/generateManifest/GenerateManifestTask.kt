package io.github.frankois944.spmForKmp.tasks.apple.generateManifest

import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.definition.packageSetting.BridgeSettings
import io.github.frankois944.spmForKmp.manifest.TemplateParameters
import io.github.frankois944.spmForKmp.manifest.generateManifest
import io.github.frankois944.spmForKmp.tasks.utils.TaskTracer
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
import javax.inject.Inject

@CacheableTask
internal abstract class GenerateManifestTask : DefaultTask() {
    @get:Input
    abstract val packageDependencies: ListProperty<SwiftDependency>

    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    @get:Optional
    abstract val minIos: Property<String>

    @get:Input
    @get:Optional
    abstract val minMacos: Property<String>

    @get:Input
    @get:Optional
    abstract val minTvos: Property<String>

    @get:Input
    @get:Optional
    abstract val minWatchos: Property<String>

    @get:Input
    abstract val toolsVersion: Property<String>

    @get:Input
    abstract val targetSettings: Property<BridgeSettings>

    @get:Input
    @get:Optional
    abstract val swiftBinPath: Property<String?>

    @get:OutputFile
    abstract val manifestFile: RegularFileProperty

    @get:Input
    abstract val traceEnabled: Property<Boolean>

    @get:OutputFile
    abstract val storedTraceFile: RegularFileProperty

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
        val tracer =
            TaskTracer(
                "GenerateManifestTask",
                traceEnabled.get(),
                outputFile =
                    storedTraceFile
                        .get()
                        .asFile,
            )
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
                                        .asFile
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
                manifestFile.get().asFile.writeText(manifest)
            }
        }
        tracer.writeHtmlReport()
    }
}
