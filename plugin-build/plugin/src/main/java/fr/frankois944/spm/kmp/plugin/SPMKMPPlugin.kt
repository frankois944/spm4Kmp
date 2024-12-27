@file:OptIn(ExperimentalStdlibApi::class)

package fr.frankois944.spm.kmp.plugin

import fr.frankois944.spm.kmp.plugin.definition.PackageRootDefinitionExtension
import fr.frankois944.spm.kmp.plugin.tasks.CompileSwiftPackageTask
import fr.frankois944.spm.kmp.plugin.tasks.GenerateCInteropDefinitionTask
import fr.frankois944.spm.kmp.plugin.tasks.GenerateManifestTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

internal const val EXTENSION_NAME: String = "swiftPackageConfig"
internal const val TASK_GENERATE_MANIFEST: String = "generateSwiftPackage"
internal const val TASK_BUILD_PACKAGE: String = "compileSwiftPackage"
internal const val TASK_GENERATE_CINTEROP_DEF: String = "generateCInteropDefinition"

@Suppress("UnnecessaryAbstractClass")
public abstract class SPMKMPPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        if (!HostManager.hostIsMac) {
            println("The plugin spm-kmp can only run on macos")
            return
        }

        val extension = project.extensions.create(EXTENSION_NAME, PackageRootDefinitionExtension::class.java, project)
        val sourcePackageDir =
            project.layout.buildDirectory.asFile
                .get()
                .resolve("spmKmpPlugin")
                .resolve("input")
                .also {
                    it.mkdirs()
                }
        val buildPackageDir =
            project.layout.buildDirectory.asFile
                .get()
                .resolve("spmKmpPlugin")
                .resolve("output")
                .also {
                    it.mkdirs()
                }
        val customPackageSource =
            Path(extension.customPackageSourcePath)
                .also {
                    it.createDirectories()
                }.toFile()

        val generateManifestTask =
            project.tasks
                .register(TASK_GENERATE_MANIFEST, GenerateManifestTask::class.java) {
                    it.productName.set(extension.productName)
                    it.generatedPackageDirectory.set(sourcePackageDir)
                    it.toolsVersion.set(extension.toolsVersion)
                    it.minIos.set(extension.minIos)
                    it.minTvos.set(extension.minTvos)
                    it.minMacos.set(extension.minMacos)
                    it.minWatchos.set(extension.minWatchos)
                    it.packages.set(extension.packages)
                    it.outputFile.set(File(sourcePackageDir, "Package.swift"))
                }

        val buildPackageTask =
            project.tasks
                .register(TASK_BUILD_PACKAGE, CompileSwiftPackageTask::class.java) {
                    it.manifestFile.set(File(sourcePackageDir, "Package.swift"))
                    it.packageBuildOutputDirectory.set(buildPackageDir)
                    it.target.set("arm64-apple-ios${extension.minIos}-simulator")
                    it.customSourcePackage.set(customPackageSource)
                    it.isDebugMode.set(true)
                }

        val generateCInteropDefinitionTask =
            project.tasks
                .register(TASK_GENERATE_CINTEROP_DEF, GenerateCInteropDefinitionTask::class.java) {
                    it.packageBuildOutputDirectory.set(buildPackageDir)
                    it.target.set("arm64-apple-ios-simulator")
                    it.isDebugMode.set(true)
                    it.packages.set(extension.packages)
                    it.productName.set(extension.productName)
                    // it.outputFiles.set()
                }

        val firstStep = buildPackageTask.get().dependsOn(generateManifestTask.get())
        val secondStep = generateCInteropDefinitionTask.get().dependsOn(firstStep)
    }
}
