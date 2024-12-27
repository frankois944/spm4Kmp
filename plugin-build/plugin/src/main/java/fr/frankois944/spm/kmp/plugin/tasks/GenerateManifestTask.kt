package fr.frankois944.spm.kmp.plugin.tasks

import fr.frankois944.spm.kmp.plugin.definition.SwiftPackageDependencyDefinition
import fr.frankois944.spm.kmp.plugin.manifest.generateManifest
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

internal abstract class GenerateManifestTask : DefaultTask() {
    init {
        description = "Generate a Swift Package manifest"
        group = BasePlugin.BUILD_GROUP
    }

    @get:Input
    val packages: ListProperty<SwiftPackageDependencyDefinition> =
        project.objects.listProperty(
            SwiftPackageDependencyDefinition::class.java,
        )

    @get:Input
    val productName: Property<String> = project.objects.property(String::class.java)

    @get:Input
    val minIos: Property<String> = project.objects.property(String::class.java)

    @get:Input
    val minMacos: Property<String> = project.objects.property(String::class.java)

    @get:Input
    val minTvos: Property<String> = project.objects.property(String::class.java)

    @get:Input
    val minWatchos: Property<String> = project.objects.property(String::class.java)

    @get:Input
    val toolsVersion: Property<String> = project.objects.property(String::class.java)

    @get:InputDirectory
    abstract val generatedPackageDirectory: DirectoryProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generateFile() {
        val manifest =
            generateManifest(
                packages.get(),
                generatedPackageDirectory =
                    generatedPackageDirectory.get().asFile.toPath(),
                productName = productName.get(),
                minIos = minIos.get(),
                minMacos = minMacos.get(),
                minTvos = minTvos.get(),
                minWatchos = minWatchos.get(),
                toolsVersion = toolsVersion.get(),
            )
        println("Generated manifest\n$manifest")
        outputFile.get().asFile.writeText(manifest)
    }
}
