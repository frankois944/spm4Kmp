@file:OptIn(ExperimentalStdlibApi::class)

package fr.frankois944.spm.kmp.plugin

import fr.frankois944.spm.kmp.plugin.definition.PackageRootDefinition
import fr.frankois944.spm.kmp.plugin.tasks.GenerateManifestTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.internal.cc.base.logger
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File

internal const val EXTENSION_NAME: String = "swiftPackageConfig"
internal const val TASK_NAME: String = "generateSwiftPackage"

@Suppress("UnnecessaryAbstractClass")
public abstract class SPMKMPPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        if (!HostManager.hostIsMac) {
            logger.warn("The plugin spm-kmp can only run on macos")
            return
        }

        val extension = project.extensions.create(EXTENSION_NAME, PackageRootDefinition::class.java, project)

        // build and generate the package with depenencies
        // link to each kotlin target the packages

        // Add a task that uses configuration from the extension object
        project.tasks.register(TASK_NAME, GenerateManifestTask::class.java) {
            it.definition.set(extension)
            it.outputFile.set(File(extension.generatedPackageDirectory, "Package.swift"))
        }
    }
}
