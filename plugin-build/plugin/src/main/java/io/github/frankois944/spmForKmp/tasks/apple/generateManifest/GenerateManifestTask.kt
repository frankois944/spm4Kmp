package io.github.frankois944.spmForKmp.tasks.apple.generateManifest

import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.definition.packageSetting.BridgeSettings
import io.github.frankois944.spmForKmp.manifest.ResourcesPaths
import io.github.frankois944.spmForKmp.manifest.TemplateParameters
import io.github.frankois944.spmForKmp.manifest.generateManifest
import io.github.frankois944.spmForKmp.operations.compareDeploymentVersions
import io.github.frankois944.spmForKmp.operations.getSdkDeploymentFloor
import io.github.frankois944.spmForKmp.tasks.utils.TaskTracer
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
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
    abstract val resourcesPaths: Property<ResourcesPaths>

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

    /** The Apple targets configured for this package, to only check platforms in use. */
    @get:Input
    abstract val targets: SetProperty<AppleCompileTarget>

    @get:Input
    abstract val targetSettings: Property<BridgeSettings>

    @get:Input
    @get:Optional
    abstract val swiftBinPath: Property<String>

    @get:OutputFile
    abstract val manifestFile: RegularFileProperty

    @get:Input
    abstract val traceEnabled: Property<Boolean>

    @get:Internal
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

    /**
     * One platform's deployment target, and the SDKs a target of that platform builds against.
     */
    private data class PlatformDeploymentTarget(
        val property: String,
        val sdk: String,
        val version: String?,
        val platformSdks: Set<String>,
    )

    private fun platformDeploymentTargets(): List<PlatformDeploymentTarget> =
        listOf(
            PlatformDeploymentTarget(
                "minIos",
                "iphoneos",
                minIos.orNull,
                setOf("iphoneos", "iphonesimulator"),
            ),
            PlatformDeploymentTarget(
                "minMacos",
                "macosx",
                minMacos.orNull,
                setOf("macosx"),
            ),
            PlatformDeploymentTarget(
                "minTvos",
                "appletvos",
                minTvos.orNull,
                setOf("appletvos", "appletvsimulator"),
            ),
            PlatformDeploymentTarget(
                "minWatchos",
                "watchos",
                minWatchos.orNull,
                setOf("watchos", "watchsimulator"),
            ),
        )

    /**
     * Warns when a configured deployment target is below what the installed SDK accepts.
     *
     * The deprecated `native` build system builds it anyway, so this stays a warning: failing
     * here would break projects that build fine today. `swiftbuild`, which SwiftPM now defaults
     * to, rejects it — once the plugin builds with it, this becomes an error.
     *
     * Only the platforms the package actually has a target for are checked. The manifest always
     * declares all four, so checking them all would warn an iOS-only project about watchOS.
     */
    private fun warnOnUnsupportedDeploymentTargets() {
        val configuredSdks = targets.get().mapTo(mutableSetOf()) { it.sdk() }
        platformDeploymentTargets().forEach { platform ->
            val version = platform.version
            if (version.isNullOrEmpty()) return@forEach
            if (platform.platformSdks.none { it in configuredSdks }) return@forEach
            val floor = execOps.getSdkDeploymentFloor(platform.sdk, logger) ?: return@forEach
            if (compareDeploymentVersions(version, floor) < 0) {
                logger.warn(
                    "spmForKmp: swiftPackageConfig.{} is {}, but the installed {} SDK supports {} and above. " +
                        "The deprecated `native` build system still accepts it, the `swiftbuild` one does not. " +
                        "Raise it to {} to stay buildable.",
                    platform.property,
                    version,
                    platform.sdk,
                    floor,
                    floor,
                )
            }
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
            tracer.trace("checkDeploymentTargets") {
                warnOnUnsupportedDeploymentTargets()
            }
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
                                resourcesPaths = resourcesPaths.get(),
                            ),
                    )
                manifestFile.get().asFile.writeText(manifest)
            }
        }
        tracer.writeHtmlReport()
    }
}
