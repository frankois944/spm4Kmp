package fr.frankois944.spm.kmp.plugin

import fr.frankois944.spm.kmp.plugin.definition.PackageRootDefinition
import fr.frankois944.spm.kmp.plugin.tasks.GenerateManifestTask
import org.gradle.internal.impldep.org.junit.Rule
import org.gradle.internal.impldep.org.junit.rules.TemporaryFolder
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import java.io.File

class SPMPluginTest {
    @JvmField
    @Rule
    var testProjectDir: TemporaryFolder = TemporaryFolder()

    @Test
    fun `plugin is applied correctly to the project`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("fr.frankois944.spm.kmp.plugin")

        assert(project.tasks.getByName("generateSwiftPackage") is GenerateManifestTask)
    }

    @Test
    fun `extension templateExampleConfig is created correctly`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("fr.frankois944.spm.kmp.plugin")

        // assert(project.extensions.getByName("swiftPackageConfig"))
    }

    @Test
    fun `parameters are passed correctly from extension to task`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("fr.frankois944.spm.kmp.plugin")
        // val aFile = File(project.projectDir, ".tmp")
        (project.extensions.getByName("swiftPackageConfig") as PackageRootDefinition).apply {
            generatedPackageDirectory = File("src/Package.swift")
            minIos = "12.0"
            minMacos = "10.13"
        }

        val task = project.tasks.getByName("generateSwiftPackage") as GenerateManifestTask

        // assertEquals("a-sample-tag", task.tag.get())
        // assertEquals("just-a-message", task.message.get())
        // assertEquals(aFile, task.outputFile.get().asFile)
    }

    @Test
    fun `task generates file with message`() {
        val message = "Just trying this gradle plugin..."
        testProjectDir.root.removeRecursively()
        File(testProjectDir.root, "build.gradle")
            .writeText(
                generateBuildFile(
                    """
                    generatedPackageDirectory = File("src/Package.swift")
                    minIos = "12.0"
                    minMacos = "10.13"
                    """.trimIndent(),
                ),
            )

        val gradleResult = executeGradleRun("generateSwiftPackage")
        assert(gradleResult.output.contains("message is: $message"))

        val generatedFileText = (testProjectDir.root / "build" / "template-example.txt").readText()
        assert(generatedFileText == "[tag] $message")
    }

    private fun executeGradleRun(task: String): BuildResult =
        GradleRunner
            .create()
            .withProjectDir(testProjectDir.root)
            .withArguments(task)
            .withPluginClasspath()
            .build()

    private fun generateBuildFile(config: String) =
        """
        plugins {
            id 'fr.frankois944.spm.kmp.plugin'
        }
        swiftPackageConfig {
            $config
        }
        """.trimIndent()
}

private fun File.removeRecursively() =
    this
        .walkBottomUp()
        .filter { it != this }
        .forEach { it.deleteRecursively() }

private operator fun File.div(s: String): File = this.resolve(s)
