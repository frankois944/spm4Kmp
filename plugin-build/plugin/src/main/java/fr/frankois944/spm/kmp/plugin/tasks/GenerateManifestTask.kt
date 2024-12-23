package fr.frankois944.spm.kmp.plugin.tasks

import fr.frankois944.spm.kmp.plugin.definition.PackageRootDefinition
import fr.frankois944.spm.kmp.plugin.manifest.generateManifest
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

internal abstract class GenerateManifestTask : DefaultTask() {
    init {
        description = "Generate a Swift Package manifest"
        group = BasePlugin.BUILD_GROUP
    }

    @get:Input
    abstract val definition: Property<PackageRootDefinition>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:Inject
    abstract val operation: ExecOperations

    @TaskAction
    public fun generateFile() {
        val manifest = generateManifest(definition.get())
        logger.trace(manifest)
        outputFile.get().asFile.writeText(manifest)
    }
}
