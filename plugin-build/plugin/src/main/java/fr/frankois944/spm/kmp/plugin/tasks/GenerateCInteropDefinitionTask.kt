package fr.frankois944.spm.kmp.plugin.tasks

import fr.frankois944.spm.kmp.plugin.definition.SwiftPackageDependencyDefinition
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

internal abstract class GenerateCInteropDefinitionTask : DefaultTask() {
    init {
        description = "Generate the cinterop definition"
        group = BasePlugin.BUILD_GROUP
    }

    @get:InputDirectory
    abstract val packageBuildOutputDirectory: DirectoryProperty

    @get:Input
    abstract val target: Property<String>

    @get:Input
    val productName: Property<String> = project.objects.property(String::class.java)

    @get:Input
    val packages: ListProperty<SwiftPackageDependencyDefinition> =
        project.objects.listProperty(
            SwiftPackageDependencyDefinition::class.java,
        )

    /*@get:OutputFiles
    val outputFiles: List<File>
        get() {
            return packageBuildOutputDirectory.asFile
                .get()
                .resolve("arm64-apple-ios-simulator")
                .resolve(if (isDebugMode.get()) "debug" else "release")
                .listFiles()
                ?.toList() ?: emptyList()
        }*/

    val isDebugMode: Property<Boolean> = project.objects.property(Boolean::class.java)

    private fun getBuildDirectory(): File =
        packageBuildOutputDirectory.asFile
            .get()
            .resolve(target.get())
            .resolve(if (isDebugMode.get()) "debug" else "release")

    private fun getBuildDirectoriesContent(): List<File> =
        getBuildDirectory()
            .listFiles()
            ?.toList() ?: emptyList()

    @TaskAction
    fun generateDefinitions() {
        val buildDir = getBuildDirectoriesContent()

        val moduleNames =
            packages
                .get()
                .flatMap {
                    it.names
                }.map {
                    it.lowercase()
                }.distinct()

        logger.warn(
            """
            moduleNames
            $moduleNames
            """.trimIndent(),
        )

        val moduleDirectories =
            buildDir.filter { item ->
                moduleNames.contains(item.nameWithoutExtension.lowercase())
            }

        logger.warn(
            """
            moduleDirectories
            $moduleDirectories
            """.trimIndent(),
        )

        moduleDirectories.forEach { item ->
            val mapFile = item.resolve("module.modulemap")
            if (!mapFile.exists()) {
                logger.error("Can't generate definition for ${item.nameWithoutExtension} because no modulemap found")
                logger.error(
                    """
                    File: $mapFile
                    """.trimIndent(),
                )
            }
            val definitionFile =
                """
                language = Objective-C
                modules = CryptoSwift
                package = CryptoSwift

                staticLibraries = lib${productName.get()}.a
                libraryPaths = ${getBuildDirectory().path}
                compilerOpts = -ObjC -fmodules ${getBuildDirectoriesContent().joinToString(" -I")}
                """.trimIndent()

            val definitionFrameworkFile =
                """
                language = Objective-C
                modules = CryptoSwift
                package = CryptoSwift

                staticLibraries = lib${productName.get()}.a
                libraryPaths = ${getBuildDirectory().path}
                compilerOpts = -framework -fmodules -F${getBuildDirectory().path}
                """.trimIndent()
        }
    }
}
